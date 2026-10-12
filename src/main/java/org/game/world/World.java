package org.game.world;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joml.Vector3f;
import org.engine.utils.Debug;
import org.engine.utils.Logger;

/**
 * Pure world data and logic, no rendering.
 * Can be used on client or server.
 */
public class World {
    private HashMap<String, Chunk> chunks;
    private int viewDistance = 6;
    private long blockRevision;
    private final WorldGenerator generator = new WorldGenerator(4000);
    private final ExecutorService chunkGenerationWorkers = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() - 1)), runnable -> {
                Thread thread = new Thread(runnable, "ChunkGenerationWorker");
                thread.setDaemon(true);
                return thread;
            });
    private final Set<Long> pendingChunkKeys = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkGenerationResult> completedChunks = new ConcurrentLinkedQueue<>();
    private final LinkedHashMap<String, BlockCoordinate> activeFluidBlocks = new LinkedHashMap<>();
    private final LinkedHashMap<String, DrainingFluid> drainingFluidBlocks = new LinkedHashMap<>();
    private int generationCenterX;
    private int generationCenterZ;

    // FIX: Track which chunk keys have already been sent to the client.
    // Without this, GenerateNewChunks re-sends every chunk in view on every
    // player move, which floods the client with LoadSingleChunkCommands and
    // causes every already-meshed chunk to be replaced and re-meshed.
    private final Set<String> sentChunkKeys = new HashSet<>();

    public World() {
        chunks = new HashMap<>();
    }

    public void generateNewChunks(Vector3f playerPosition) {
        generationCenterX = Math.floorDiv((int) Math.floor(playerPosition.x), Chunk.SIZE_X);
        generationCenterZ = Math.floorDiv((int) Math.floor(playerPosition.z), Chunk.SIZE_Z);
        scheduleChunksNear(generationCenterX, generationCenterZ);
    }

    public void generateInitialChunks() {
        generationCenterX = 0;
        generationCenterZ = 0;
        scheduleChunksNear(generationCenterX, generationCenterZ);
    }

    private void scheduleChunksNear(int centerChunkX, int centerChunkZ) {
        List<ChunkCoordinate> requestedChunks = new ArrayList<>();
        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                requestedChunks.add(new ChunkCoordinate(centerChunkX + x, centerChunkZ + z));
            }
        }
        requestedChunks.sort(Comparator.comparingLong(coordinate ->
                distanceSquared(coordinate.x - centerChunkX, coordinate.z - centerChunkZ)));

        for (ChunkCoordinate coordinate : requestedChunks) {
            String chunkKey = key(coordinate.x, coordinate.z);
            long packedKey = packedKey(coordinate.x, coordinate.z);
            if (chunks.containsKey(chunkKey) || !pendingChunkKeys.add(packedKey))
                continue;

            chunkGenerationWorkers.execute(() -> {
                try {
                    Chunk chunk = new Chunk(coordinate.x, coordinate.z);
                    generator.generateChunk(chunk);
                    completedChunks.offer(new ChunkGenerationResult(packedKey, chunk, null));
                } catch (RuntimeException exception) {
                    completedChunks.offer(new ChunkGenerationResult(packedKey, null, exception));
                }
            });
        }
    }

    private static long distanceSquared(int x, int z) {
        return (long) x * x + (long) z * z;
    }

    private static long packedKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
    }

    /** Installs completed worker results on the world-owning thread. */
    public int collectGeneratedChunks(int maximum) {
        int installed = 0;
        ChunkGenerationResult result;
        while (installed < maximum && (result = completedChunks.poll()) != null) {
            pendingChunkKeys.remove(result.packedKey);
            if (result.failure != null) {
                Logger.log("Failed to generate chunk.", result.failure);
                continue;
            }

            Chunk chunk = result.chunk;
            String chunkKey = key(chunk.getChunkX(), chunk.getChunkZ());
            if (chunks.putIfAbsent(chunkKey, chunk) == null) {
                blockRevision++;
                installed++;
                activateFluidBlocks(chunk);
            }
        }
        if (installed > 0)
            Debug.log("Installed " + installed + " generated chunk(s); "
                    + pendingChunkKeys.size() + " still generating.");
        return installed;
    }

    public int getGenerationCenterX() {
        return generationCenterX;
    }

    public int getGenerationCenterZ() {
        return generationCenterZ;
    }

    /** Returns loaded chunks near the player that have not been delivered yet. */
    public List<Chunk> getNewChunksNear(int playerChunkX, int playerChunkZ) {
        List<Chunk> newChunks = new ArrayList<>();

        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                int targetX = playerChunkX + x;
                int targetZ = playerChunkZ + z;
                String chunkKey = key(targetX, targetZ);

                Chunk chunk = chunks.get(chunkKey);
                if (chunk != null && !sentChunkKeys.contains(chunkKey)) {
                    newChunks.add(chunk);
                }
            }
        }

        newChunks.sort(Comparator.comparingLong(chunk -> distanceSquared(
                chunk.getChunkX() - playerChunkX, chunk.getChunkZ() - playerChunkZ)));
        return newChunks;
    }

    /** Mark a chunk as already sent so it won't be returned by getNewChunksNear again. */
    public void markChunkSent(Chunk chunk) {
        sentChunkKeys.add(key(chunk.getChunkX(), chunk.getChunkZ()));
    }

    /** Call this if you want to force-resend a chunk (e.g. after a block change). */
    public void markChunkUnsent(int chunkX, int chunkZ) {
        sentChunkKeys.remove(key(chunkX, chunkZ));
    }

    public void copyFrom(World other) {
        this.chunks.clear();
        this.sentChunkKeys.clear();
        for (Map.Entry<String, Chunk> entry : other.chunks.entrySet()) {
            this.chunks.put(entry.getKey(), entry.getValue().clone());
        }
        this.sentChunkKeys.addAll(other.sentChunkKeys);
        this.viewDistance = other.viewDistance;
        blockRevision++;
    }

    private String key(int cx, int cz) {
        return cx + "," + cz;
    }

    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        if (chunk == null || !chunk.inBounds(localX, localY, localZ))
            return null;
        int id = chunk.getBlock(localX, localY, localZ);
        return BlockRegistry.getBlock(id);
    }

    public int getBlockIdAt(int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        return chunk == null ? 0 : chunk.getBlock(Math.floorMod(worldX, Chunk.SIZE_X),
                worldY, Math.floorMod(worldZ, Chunk.SIZE_Z));
    }

    public Chunk setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        if (chunk == null || !chunk.inBounds(localX, localY, localZ))
            return null;
        if (chunk.getBlock(localX, localY, localZ) == blockId)
            return chunk;

        int previousId = chunk.getBlock(localX, localY, localZ);
        boolean removedFluidSource = previousId == BlockRegistry.getId("water")
                && chunk.isFluidSource(localX, localY, localZ) && blockId != previousId;
        blockRevision++;
        chunk.setBlock(localX, localY, localZ, blockId);
        if (blockId != BlockRegistry.getId("water"))
            activeFluidBlocks.remove(positionKey(worldX, worldY, worldZ));
        if (removedFluidSource)
            scheduleFluidDrain(worldX, worldY, worldZ);
        return chunk;
    }

    public int getFluidLevelAt(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunk(Math.floorDiv(worldX, Chunk.SIZE_X), Math.floorDiv(worldZ, Chunk.SIZE_Z));
        return chunk == null ? 0 : chunk.getFluidLevel(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                Math.floorMod(worldZ, Chunk.SIZE_Z));
    }

    public boolean isFluidSourceAt(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunk(Math.floorDiv(worldX, Chunk.SIZE_X), Math.floorDiv(worldZ, Chunk.SIZE_Z));
        return chunk != null && chunk.isFluidSource(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                Math.floorMod(worldZ, Chunk.SIZE_Z));
    }

    public float getFluidFlowTimerAt(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunk(Math.floorDiv(worldX, Chunk.SIZE_X), Math.floorDiv(worldZ, Chunk.SIZE_Z));
        return chunk == null ? 0.0f : chunk.getFluidFlowTimer(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                Math.floorMod(worldZ, Chunk.SIZE_Z));
    }

    public void setFluidFlowTimerAt(int worldX, int worldY, int worldZ, float timer) {
        Chunk chunk = getChunk(Math.floorDiv(worldX, Chunk.SIZE_X), Math.floorDiv(worldZ, Chunk.SIZE_Z));
        if (chunk != null)
            chunk.setFluidFlowTimer(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                    Math.floorMod(worldZ, Chunk.SIZE_Z), timer);
    }

    public void setFluidStateAt(int worldX, int worldY, int worldZ,
            int level, boolean source, float flowTimer) {
        Chunk chunk = getChunk(Math.floorDiv(worldX, Chunk.SIZE_X), Math.floorDiv(worldZ, Chunk.SIZE_Z));
        if (chunk != null && chunk.getBlock(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                Math.floorMod(worldZ, Chunk.SIZE_Z)) == BlockRegistry.getId("water")) {
            chunk.setFluidState(Math.floorMod(worldX, Chunk.SIZE_X), worldY,
                    Math.floorMod(worldZ, Chunk.SIZE_Z), level, source, flowTimer);
        }
    }

    public boolean placeFluidAt(int x, int y, int z, int level, boolean source, float flowTimer) {
        if (y < 0 || y >= Chunk.SIZE_Y || level < 1 || level > FluidBlock.MAX_LEVEL)
            return false;
        int waterId = BlockRegistry.getId("water");
        if (waterId == 0)
            return false;

        int currentId = getBlockIdAt(x, y, z);
        if (currentId != 0 && currentId != waterId)
            return false;

        int currentLevel = getFluidLevelAt(x, y, z);
        boolean currentSource = isFluidSourceAt(x, y, z);
        if (currentId == waterId && (currentLevel > level || currentSource
                || (currentLevel == level && !source)))
            return false;

        if (currentId == 0 && setBlockAt(x, y, z, waterId) == null)
            return false;
        setFluidStateAt(x, y, z, level, source, flowTimer);
        if (level > 1)
            activeFluidBlocks.put(positionKey(x, y, z), new BlockCoordinate(x, y, z));
        else
            activeFluidBlocks.remove(positionKey(x, y, z));
        return true;
    }
    public void removeBlockAt(int x, int y, int z) {
        if (getBlockIdAt(x, y, z) == BlockRegistry.getId("water")) {
            activeFluidBlocks.remove(positionKey(x, y, z));
        }
        setBlockAt(x, y, z, 0);
    }
    public List<FluidUpdate> updateFluidBlocks(float delta, int maximum) {
        List<FluidUpdate> updates = new ArrayList<>();
        int waterId = BlockRegistry.getId("water");
        Block block = BlockRegistry.getBlock(waterId);
        if (!(block instanceof FluidBlock fluidBlock) || maximum <= 0)
            return updates;

        List<BlockCoordinate> pending = new ArrayList<>(activeFluidBlocks.values());
        activeFluidBlocks.clear();
        int processed = 0;
        for (int i = 0; i < pending.size(); i++) {
            BlockCoordinate position = pending.get(i);
            if (getBlockIdAt(position.x, position.y, position.z) != waterId
                    || getFluidLevelAt(position.x, position.y, position.z) <= 1)
                continue;
            if (processed >= maximum) {
                activeFluidBlocks.put(positionKey(position.x, position.y, position.z), position);
                continue;
            }
            processed++;
            updates.addAll(fluidBlock.update(this, position.x, position.y, position.z, delta));
            if (getBlockIdAt(position.x, position.y, position.z) == waterId
                    && getFluidLevelAt(position.x, position.y, position.z) > 1) {
                activeFluidBlocks.put(positionKey(position.x, position.y, position.z), position);
            }
        }
        updateDrainingFluids(Math.max(delta, 0.0f), updates);
        return updates;
    }

    private void scheduleFluidDrain(int x, int y, int z) {
        int waterId = BlockRegistry.getId("water");
        ArrayList<BlockCoordinate> component = new ArrayList<>();
        ArrayList<BlockCoordinate> pending = new ArrayList<>();
        HashSet<String> visited = new HashSet<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] direction : directions) {
            pending.add(new BlockCoordinate(x + direction[0], y, z + direction[1]));
        }
        while (!pending.isEmpty()) {
            BlockCoordinate position = pending.remove(pending.size() - 1);
            String key = positionKey(position.x, position.y, position.z);
            if (!visited.add(key) || getBlockIdAt(position.x, position.y, position.z) != waterId)
                continue;
            if (isFluidSourceAt(position.x, position.y, position.z))
                return;

            component.add(position);
            for (int[] direction : directions) {
                pending.add(new BlockCoordinate(position.x + direction[0], y,
                        position.z + direction[1]));
            }
        }

        component.sort(Comparator
                .comparingInt((BlockCoordinate position) ->
                        getFluidLevelAt(position.x, position.y, position.z))
                .reversed()
                .thenComparingInt(position -> position.x)
                .thenComparingInt(position -> position.z));
        drainingFluidBlocks.clear();
        for (BlockCoordinate position : component) {
            activeFluidBlocks.remove(positionKey(position.x, position.y, position.z));
            drainingFluidBlocks.put(positionKey(position.x, position.y, position.z),
                    new DrainingFluid(position.x, position.y, position.z, 0.05f));
        }
    }

    private void updateDrainingFluids(float delta, List<FluidUpdate> updates) {
        while (!drainingFluidBlocks.isEmpty()) {
            Map.Entry<String, DrainingFluid> first =
                    drainingFluidBlocks.entrySet().iterator().next();
            DrainingFluid draining = first.getValue();
            if (getBlockIdAt(draining.x, draining.y, draining.z) != BlockRegistry.getId("water")
                    || isFluidSourceAt(draining.x, draining.y, draining.z)) {
                drainingFluidBlocks.remove(first.getKey());
                continue;
            }
            if (hasConnectedFluidSource(draining.x, draining.y, draining.z)) {
                drainingFluidBlocks.clear();
                return;
            }

            draining.remainingTime -= delta;
            if (draining.remainingTime > 0.0f)
                return;

            drainingFluidBlocks.remove(first.getKey());
            setBlockAt(draining.x, draining.y, draining.z, 0);
            updates.add(new FluidUpdate(draining.x, draining.y, draining.z, 0, false, 0));
            return;
        }
    }

    private boolean hasConnectedFluidSource(int startX, int y, int startZ) {
        int waterId = BlockRegistry.getId("water");
        ArrayList<BlockCoordinate> pending = new ArrayList<>();
        HashSet<String> visited = new HashSet<>();
        pending.add(new BlockCoordinate(startX, y, startZ));
        while (!pending.isEmpty()) {
            BlockCoordinate position = pending.remove(pending.size() - 1);
            String key = positionKey(position.x, position.y, position.z);
            if (!visited.add(key) || getBlockIdAt(position.x, position.y, position.z) != waterId)
                continue;
            if (isFluidSourceAt(position.x, position.y, position.z))
                return true;
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] direction : directions) {
                int neighborX = position.x + direction[0];
                int neighborZ = position.z + direction[1];
                if (getBlockIdAt(neighborX, y, neighborZ) == waterId)
                    pending.add(new BlockCoordinate(neighborX, y, neighborZ));
            }
        }
        return false;
    }

    private void activateFluidBlocks(Chunk chunk) {
        int waterId = BlockRegistry.getId("water");
        if (waterId == 0)
            return;
        for (int x = 0; x < Chunk.SIZE_X; x++)
            for (int y = 0; y < Chunk.SIZE_Y; y++)
                for (int z = 0; z < Chunk.SIZE_Z; z++)
                    if (chunk.getBlock(x, y, z) == waterId
                            && chunk.isFluidSource(x, y, z)
                            && chunk.getFluidLevel(x, y, z) > 1) {
                        int worldX = chunk.getChunkX() * Chunk.SIZE_X + x;
                        int worldZ = chunk.getChunkZ() * Chunk.SIZE_Z + z;
                        activeFluidBlocks.put(positionKey(worldX, y, worldZ),
                                new BlockCoordinate(worldX, y, worldZ));
                    }
    }

    private static String positionKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public long getBlockRevision() {
        return blockRevision;
    }

    public List<Chunk> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(key(cx, cz));
    }

    public HashMap<String, Chunk> getChunks() {
        return this.chunks;
    }

    public List<Chunk> getChunksNear(int centerChunkX, int centerChunkZ) {
        List<Chunk> visible = new ArrayList<>();
        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                Chunk chunk = chunks.get((centerChunkX + x) + "," + (centerChunkZ + z));
                if (chunk != null) {
                    visible.add(chunk);
                }
            }
        }
        return visible;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = Math.max(1, Math.min(32, viewDistance));
    }

    public void update(float delta) { }

    public void setChunk(Chunk chunk) {
        Chunk copy = chunk.clone();
        chunks.put(key(chunk.getChunkX(), chunk.getChunkZ()), copy);
        blockRevision++;
    }

    public static final class FluidUpdate {
        public final int x;
        public final int y;
        public final int z;
        public final int level;
        public final boolean source;
        public final int blockId;

        public FluidUpdate(int x, int y, int z, int level, boolean source) {
            this(x, y, z, level, source, BlockRegistry.getId("water"));
        }

        public FluidUpdate(int x, int y, int z, int level, boolean source, int blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
            this.source = source;
            this.blockId = blockId;
        }
    }

    private static final class DrainingFluid {
        private final int x;
        private final int y;
        private final int z;
        private float remainingTime;

        private DrainingFluid(int x, int y, int z, float remainingTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.remainingTime = remainingTime;
        }
    }

    private static final class BlockCoordinate {
        private final int x;
        private final int y;
        private final int z;

        private BlockCoordinate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class ChunkCoordinate {
        private final int x;
        private final int z;

        private ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class ChunkGenerationResult {
        private final long packedKey;
        private final Chunk chunk;
        private final RuntimeException failure;

        private ChunkGenerationResult(long packedKey, Chunk chunk, RuntimeException failure) {
            this.packedKey = packedKey;
            this.chunk = chunk;
            this.failure = failure;
        }
    }
}
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

        blockRevision++;
        chunk.setBlock(localX, localY, localZ, blockId);
        return chunk;
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
        chunks.put(key(chunk.getChunkX(), chunk.getChunkZ()), chunk.clone());
        blockRevision++;
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
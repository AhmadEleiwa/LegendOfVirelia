package org.game.world;

import java.util.*;
import org.joml.Vector3f;

/**
 * Pure world data and logic, no rendering.
 * Can be used on client or server.
 */
public class World {
    private HashMap<String, Chunk> chunks;
    private int viewDistance = 6;
    private final WorldGenerator generator = new WorldGenerator(4000);

    // FIX: Track which chunk keys have already been sent to the client.
    // Without this, GenerateNewChunks re-sends every chunk in view on every
    // player move, which floods the client with LoadSingleChunkCommands and
    // causes every already-meshed chunk to be replaced and re-meshed.
    private final Set<String> sentChunkKeys = new HashSet<>();

    public World() {
        chunks = new HashMap<>();
    }

    public void generateNewChunks(Vector3f playerPosition) {
        int playerChunkX = (int) Math.floor(playerPosition.x / 16.0);
        int playerChunkZ = (int) Math.floor(playerPosition.z / 16.0);

        for (int x = -viewDistance; x <= viewDistance; x++) {
            for (int z = -viewDistance; z <= viewDistance; z++) {
                int targetChunkX = playerChunkX + x;
                int targetChunkZ = playerChunkZ + z;

                String chunkKey = key(targetChunkX, targetChunkZ);

                if (!chunks.containsKey(chunkKey)) {
                    Chunk chunk = new Chunk(targetChunkX, targetChunkZ);
                    generator.generateChunk(chunk);
                    chunks.put(chunkKey, chunk);
                }
            }
        }
    }

    public void generateInitialChunks() {
        for (int cx = -15; cx < 15; cx++) {
            for (int cz = -15; cz < 15; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                generator.generateChunk(chunk);
                chunks.put(key(cx, cz), chunk);
            }
        }
    }

    /**
     * FIX: Returns only chunks that exist but have NOT been sent to the client yet.
     * Call markChunkSent() for each one after you actually send it.
     * This is the key method GenerateNewChunks should use instead of getAllChunks().
     */
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

    public Chunk setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        if (chunk == null || !chunk.inBounds(localX, localY, localZ))
            return null;
        chunk.setBlock(localX, localY, localZ, blockId);
        return chunk;
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
    }
}
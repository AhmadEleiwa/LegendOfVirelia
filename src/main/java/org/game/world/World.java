package org.game.world;

import java.util.*;

/**
 * Pure world data and logic, no rendering.
 * Can be used on client or server.
 */
public class World {
    private HashMap<String, Chunk> chunks;
    private int viewDistance = 6;

    public World() {
        chunks = new HashMap<>();
    }

    public void generateInitialChunks() {
        for (int cx = -15; cx < 15; cx++) {
            for (int cz = -15; cz < 15; cz++) {
                Chunk chunk = new Chunk(cx, cz);
             
                // simple terrain generation
                for (int x = 0; x < Chunk.SIZE_X; x++) {
                    for (int z = 0; z < Chunk.SIZE_Z; z++) {
                        for (int y = 0; y < Chunk.SIZE_Y; y++) {
                            int height = 8 + (int) (Math.sin(cx * 0.1) * 3 + Math.cos(cz * 0.1) * 3);
                            if (y <= height) {
                                chunk.setBlock(x, y, z, BlockRegistry.getId("dirt"));
                            }
                        }
                    }
                }
                
                chunks.put(key(cx, cz), chunk);
            }
        }
    }

    public void copyFrom(World other) {
        this.chunks.clear();
        for (Map.Entry<String, Chunk> entry : other.chunks.entrySet()) {
            this.chunks.put(entry.getKey(), entry.getValue().clone());
        }
        this.viewDistance = other.viewDistance;
    }
    private String key(int cx, int cz) {
        return cx + "," + cz;
    }

    /** Get block at world coords */
    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);

        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);

        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        if (chunk == null || !chunk.inBounds(localX, localY, localZ)) return null;

        int id = chunk.getBlock(localX, localY, localZ);
        return BlockRegistry.getBlock(id);
    }

    /** Set block at world coords (server-side authoritative) */
    public Chunk setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);

        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);

        Chunk chunk = chunks.get(key(chunkX, chunkZ));
        if (chunk == null || !chunk.inBounds(localX, localY, localZ)) return null;

        chunk.setBlock(localX, localY, localZ, blockId);
        // In server version: broadcast packet to clients here
        return chunk;
    }

    public List<Chunk> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }
    public Chunk getChunk(int cx, int cz){
        return chunks.get(key(cx,cz));
    }
    public HashMap<String,Chunk> getChunks(){
        return this.chunks;
    }
    public List<Chunk> getChunksNear(int chunkX, int chunkZ) {
        List<Chunk> visible = new ArrayList<>();
        for (Chunk c : chunks.values()) {
            int dx = c.getChunkX() - chunkX;
            int dz = c.getChunkZ() - chunkZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist <= viewDistance) visible.add(c);
        }
        return visible;
    }

    public int getViewDistance() { return viewDistance; }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = Math.max(1, Math.min(32, viewDistance));
    }

    public void update(float delta) {
        // streaming/loading logic here if needed
    }
}

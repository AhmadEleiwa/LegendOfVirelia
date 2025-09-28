package org.game.world;

import java.util.ArrayDeque;
import java.util.Queue;

public class ChunkLightingSystem {
    private static final int MAX_LIGHT_LEVEL = 15;
    private World world;
    
    public ChunkLightingSystem(World world) {
        this.world = world;
    }
    
    /**
     * Calculate initial sunlight for a chunk
     */
    public void calculateSunlight(Chunk chunk) {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                byte currentLight = MAX_LIGHT_LEVEL;
                
                // Cast sunlight down from top
                for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
                    if (!chunk.isTransparent(x, y, z)) {
                        currentLight = 0; // Block stops sunlight
                    }
                    chunk.setSunlight(x, y, z, currentLight);
                }
            }
        }
        
        // Propagate sunlight to neighboring blocks
        propagateSunlight(chunk);
    }
    
    /**
     * Calculate block light from light sources
     */
    public void calculateBlockLight(Chunk chunk) {
        // Reset all block light
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    chunk.setBlocklight(x, y, z, (byte) 0);
                }
            }
        }
        
        // Find light sources and propagate light
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    int lightLevel = chunk.getLightEmission(x, y, z);
                    if (lightLevel > 0) {
                        chunk.setBlocklight(x, y, z, (byte) lightLevel);
                        propagateBlockLight(chunk, x, y, z, lightLevel);
                    }
                }
            }
        }
    }
    
    /**
     * Propagate sunlight within a chunk
     */
    private void propagateSunlight(Chunk chunk) {
        Queue<LightNode> lightQueue = new ArrayDeque<>();
        
        // Add all sunlit blocks to queue
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    if (chunk.getSunlight(x, y, z) > 0) {
                        lightQueue.offer(new LightNode(x, y, z, chunk.getSunlight(x, y, z)));
                    }
                }
            }
        }
        
        propagateLight(chunk, lightQueue, true);
    }
    
    /**
     * Propagate block light from a source
     */
    private void propagateBlockLight(Chunk chunk, int startX, int startY, int startZ, int lightLevel) {
        Queue<LightNode> lightQueue = new ArrayDeque<>();
        lightQueue.offer(new LightNode(startX, startY, startZ, lightLevel));
        propagateLight(chunk, lightQueue, false);
    }
    
    /**
     * Generic light propagation using BFS
     */
    private void propagateLight(Chunk chunk, Queue<LightNode> lightQueue, boolean isSunlight) {
        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};
        
        while (!lightQueue.isEmpty()) {
            LightNode node = lightQueue.poll();
            int currentLight = node.lightLevel;
            
            if (currentLight <= 1) continue;
            
            // Check all 6 neighbors
            for (int i = 0; i < 6; i++) {
                int nx = node.x + dx[i];
                int ny = node.y + dy[i];
                int nz = node.z + dz[i];
                
                // Skip if out of chunk bounds (TODO: handle cross-chunk propagation)
                if (nx < 0 || nx >= Chunk.SIZE_X || 
                    ny < 0 || ny >= Chunk.SIZE_Y || 
                    nz < 0 || nz >= Chunk.SIZE_Z) {
                    continue;
                }
                
                // Skip if neighbor is not transparent
                if (!chunk.isTransparent(nx, ny, nz)) {
                    continue;
                }
                
                int neighborLight = isSunlight ? chunk.getSunlight(nx, ny, nz) : chunk.getBlocklight(nx, ny, nz);
                int newLight = currentLight - 1;
                
                // Only update if new light is brighter
                if (newLight > neighborLight) {
                    if (isSunlight) {
                        chunk.setSunlight(nx, ny, nz, (byte) newLight);
                    } else {
                        chunk.setBlocklight(nx, ny, nz, (byte) newLight);
                    }
                    lightQueue.offer(new LightNode(nx, ny, nz, newLight));
                }
            }
        }
    }
    
    /**
     * Update lighting when a block changes
     */
    public void updateLightingAt(Chunk chunk, int x, int y, int z) {
        // Mark chunk as dirty
        chunk.setLightingDirty(true);
        
        // For now, recalculate entire chunk lighting
        // TODO: Implement incremental lighting updates
        calculateSunlight(chunk);
        calculateBlockLight(chunk);
        
        chunk.markLightingClean();
    }
    
    /**
     * Update all lighting for a chunk
     */
    public void updateChunkLighting(Chunk chunk) {
        if (!chunk.isLightingDirty()) return;
        
        calculateSunlight(chunk);
        calculateBlockLight(chunk);
        chunk.markLightingClean();
    }
    
    /**
     * Get light level with cross-chunk support
     */
    public float getLightLevel(int worldX, int worldY, int worldZ, float dayNightCycle) {
        // Convert world coordinates to chunk coordinates
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) return 1.0f; // Default to full light for unloaded chunks
        
        return chunk.getLightLevel(localX, localY, localZ, dayNightCycle);
    }
    
    /**
     * Helper class for light propagation
     */
    private static class LightNode {
        int x, y, z, lightLevel;
        
        LightNode(int x, int y, int z, int lightLevel) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lightLevel = lightLevel;
        }
    }
}
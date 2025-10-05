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
     * Propagate sunlight within a chunk and to neighbors
     */
    private void propagateSunlight(Chunk chunk) {
        Queue<LightNode> lightQueue = new ArrayDeque<>();

        // Add all sunlit blocks from current chunk to queue
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    if (chunk.getSunlight(x, y, z) > 0) {
                        lightQueue.offer(new LightNode(x, y, z, chunk.getSunlight(x, y, z), chunk));
                    }
                }
            }
        }

        // Add edge blocks from neighboring chunks that might illuminate this chunk
        addNeighborEdgeBlocks(chunk, lightQueue, true);

        propagateLight(lightQueue, true);
    }

    /**
     * Propagate block light from a source
     */
    private void propagateBlockLight(Chunk chunk, int startX, int startY, int startZ, int lightLevel) {
        Queue<LightNode> lightQueue = new ArrayDeque<>();
        lightQueue.offer(new LightNode(startX, startY, startZ, lightLevel, chunk));
        
        // Add edge blocks from neighboring chunks
        addNeighborEdgeBlocks(chunk, lightQueue, false);
        
        propagateLight(lightQueue, false);
    }

    /**
     * Add edge blocks from neighboring chunks to the light propagation queue.
     * This ensures that light from adjacent chunks can flow into the current chunk.
     */
    private void addNeighborEdgeBlocks(Chunk chunk, Queue<LightNode> lightQueue, boolean isSunlight) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Check all 8 neighboring chunks (4 cardinal + 4 diagonal)
        int[][] neighborOffsets = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},  // Cardinal neighbors
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonal neighbors
        };
        
        for (int[] offset : neighborOffsets) {
            Chunk neighbor = world.getChunk(chunkX + offset[0], chunkZ + offset[1]);
            if (neighbor == null) continue;
            
            // Determine which edge blocks to check based on neighbor position
            if (offset[0] == -1 && offset[1] == 0) {
                // West neighbor: check its east edge (x = SIZE_X - 1)
                addEdgeBlocksFromNeighbor(neighbor, Chunk.SIZE_X - 1, Chunk.SIZE_X - 1, 
                                         0, Chunk.SIZE_Z - 1, lightQueue, isSunlight);
            } else if (offset[0] == 1 && offset[1] == 0) {
                // East neighbor: check its west edge (x = 0)
                addEdgeBlocksFromNeighbor(neighbor, 0, 0, 
                                         0, Chunk.SIZE_Z - 1, lightQueue, isSunlight);
            } else if (offset[0] == 0 && offset[1] == -1) {
                // North neighbor: check its south edge (z = SIZE_Z - 1)
                addEdgeBlocksFromNeighbor(neighbor, 0, Chunk.SIZE_X - 1, 
                                         Chunk.SIZE_Z - 1, Chunk.SIZE_Z - 1, lightQueue, isSunlight);
            } else if (offset[0] == 0 && offset[1] == 1) {
                // South neighbor: check its north edge (z = 0)
                addEdgeBlocksFromNeighbor(neighbor, 0, Chunk.SIZE_X - 1, 
                                         0, 0, lightQueue, isSunlight);
            } else if (offset[0] == -1 && offset[1] == -1) {
                // Northwest diagonal: check corner
                addEdgeBlocksFromNeighbor(neighbor, Chunk.SIZE_X - 1, Chunk.SIZE_X - 1, 
                                         Chunk.SIZE_Z - 1, Chunk.SIZE_Z - 1, lightQueue, isSunlight);
            } else if (offset[0] == -1 && offset[1] == 1) {
                // Southwest diagonal: check corner
                addEdgeBlocksFromNeighbor(neighbor, Chunk.SIZE_X - 1, Chunk.SIZE_X - 1, 
                                         0, 0, lightQueue, isSunlight);
            } else if (offset[0] == 1 && offset[1] == -1) {
                // Northeast diagonal: check corner
                addEdgeBlocksFromNeighbor(neighbor, 0, 0, 
                                         Chunk.SIZE_Z - 1, Chunk.SIZE_Z - 1, lightQueue, isSunlight);
            } else if (offset[0] == 1 && offset[1] == 1) {
                // Southeast diagonal: check corner
                addEdgeBlocksFromNeighbor(neighbor, 0, 0, 
                                         0, 0, lightQueue, isSunlight);
            }
        }
    }
    
    /**
     * Add light-emitting blocks from a specific region of a neighbor chunk to the queue.
     */
    private void addEdgeBlocksFromNeighbor(Chunk neighbor, int xStart, int xEnd, 
                                           int zStart, int zEnd, 
                                           Queue<LightNode> lightQueue, boolean isSunlight) {
        for (int x = xStart; x <= xEnd; x++) {
            for (int z = zStart; z <= zEnd; z++) {
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    int lightLevel = isSunlight 
                        ? neighbor.getSunlight(x, y, z)
                        : neighbor.getBlocklight(x, y, z);
                    
                    if (lightLevel > 1) { // Only add if strong enough to propagate
                        lightQueue.offer(new LightNode(x, y, z, lightLevel, neighbor));
                    }
                }
            }
        }
    }

    private void propagateLight(Queue<LightNode> lightQueue, boolean isSunlight) {
        // Six directions: -X, +X, -Y, +Y, -Z, +Z
        int[] dx = { -1, 1, 0, 0, 0, 0 };
        int[] dy = { 0, 0, -1, 1, 0, 0 };
        int[] dz = { 0, 0, 0, 0, -1, 1 };

        while (!lightQueue.isEmpty()) {
            LightNode node = lightQueue.poll();
            int currentLight = node.lightLevel;

            // Stop propagation if light is too weak
            if (currentLight <= 1)
                continue;

            // Check all 6 neighboring blocks
            for (int i = 0; i < 6; i++) {
                int nx = node.x + dx[i];
                int ny = node.y + dy[i];
                int nz = node.z + dz[i];

                // Start with current chunk coordinates
                int targetChunkX = node.chunk.getChunkX();
                int targetChunkZ = node.chunk.getChunkZ();
                int localX = nx;
                int localY = ny;
                int localZ = nz;

                // Handle X-axis chunk boundary crossing
                if (nx < 0) {
                    targetChunkX--;
                    localX = Chunk.SIZE_X - 1;
                } else if (nx >= Chunk.SIZE_X) {
                    targetChunkX++;
                    localX = 0;
                }

                // Handle Z-axis chunk boundary crossing
                if (nz < 0) {
                    targetChunkZ--;
                    localZ = Chunk.SIZE_Z - 1;
                } else if (nz >= Chunk.SIZE_Z) {
                    targetChunkZ++;
                    localZ = 0;
                }

                // Skip if Y is out of world bounds
                if (ny < 0 || ny >= Chunk.SIZE_Y)
                    continue;

                // Get the target chunk (may be different from source chunk)
                Chunk targetChunk = world.getChunk(targetChunkX, targetChunkZ);
                
                // Skip if neighbor chunk is not loaded
                if (targetChunk == null)
                    continue;

                // Skip if neighbor block is opaque (blocks light)
                if (!targetChunk.isTransparent(localX, localY, localZ))
                    continue;

                // Get current light level at neighbor position
                int neighborLight = isSunlight 
                    ? targetChunk.getSunlight(localX, localY, localZ)
                    : targetChunk.getBlocklight(localX, localY, localZ);

                // Calculate new light level (decreases by 1 per block)
                int newLight = currentLight - 1;

                // Only update if new light is brighter than existing light
                if (newLight > neighborLight) {
                    // Set the new light value
                    if (isSunlight) {
                        targetChunk.setSunlight(localX, localY, localZ, (byte) newLight);
                    } else {
                        targetChunk.setBlocklight(localX, localY, localZ, (byte) newLight);
                    }

                    // Mark neighbor chunk as dirty if light crossed chunk boundary
                    if (targetChunk != node.chunk) {
                        targetChunk.setLightingDirty(true);
                    }

                    // Add to queue to continue propagation
                    lightQueue.offer(new LightNode(localX, localY, localZ, newLight, targetChunk));
                }
            }
        }
    }

    /**
     * Get sunlight level with neighbor chunk support.
     * This is used during rendering to correctly light faces at chunk edges.
     * 
     * @param chunk The source chunk
     * @param x Local X coordinate (can be -1 to SIZE_X)
     * @param y Local Y coordinate
     * @param z Local Z coordinate (can be -1 to SIZE_Z)
     * @return Sunlight level (0-15)
     */
    public byte getSunlightWithNeighbors(Chunk chunk, int x, int y, int z) {
        // Handle Y bounds - return default values
        if (y < 0 || y >= Chunk.SIZE_Y)
            return 0;

        // Calculate which chunk to query
        int targetChunkX = chunk.getChunkX();
        int targetChunkZ = chunk.getChunkZ();
        int localX = x;
        int localZ = z;

        // Handle X-axis boundary
        if (x < 0) {
            targetChunkX--;
            localX = Chunk.SIZE_X - 1;
        } else if (x >= Chunk.SIZE_X) {
            targetChunkX++;
            localX = 0;
        }

        // Handle Z-axis boundary
        if (z < 0) {
            targetChunkZ--;
            localZ = Chunk.SIZE_Z - 1;
        } else if (z >= Chunk.SIZE_Z) {
            targetChunkZ++;
            localZ = 0;
        }

        // Get target chunk
        Chunk targetChunk = world.getChunk(targetChunkX, targetChunkZ);
        if (targetChunk == null)
            return MAX_LIGHT_LEVEL; // Default to bright for unloaded chunks

        return targetChunk.getSunlight(localX, y, localZ);
    }

    /**
     * Get block light level with neighbor chunk support.
     * This is used during rendering to correctly light faces at chunk edges.
     * 
     * @param chunk The source chunk
     * @param x Local X coordinate (can be -1 to SIZE_X)
     * @param y Local Y coordinate
     * @param z Local Z coordinate (can be -1 to SIZE_Z)
     * @return Block light level (0-15)
     */
    public byte getBlocklightWithNeighbors(Chunk chunk, int x, int y, int z) {
        // Handle Y bounds - return default values
        if (y < 0 || y >= Chunk.SIZE_Y)
            return 0;

        // Calculate which chunk to query
        int targetChunkX = chunk.getChunkX();
        int targetChunkZ = chunk.getChunkZ();
        int localX = x;
        int localZ = z;

        // Handle X-axis boundary
        if (x < 0) {
            targetChunkX--;
            localX = Chunk.SIZE_X - 1;
        } else if (x >= Chunk.SIZE_X) {
            targetChunkX++;
            localX = 0;
        }

        // Handle Z-axis boundary
        if (z < 0) {
            targetChunkZ--;
            localZ = Chunk.SIZE_Z - 1;
        } else if (z >= Chunk.SIZE_Z) {
            targetChunkZ++;
            localZ = 0;
        }

        // Get target chunk
        Chunk targetChunk = world.getChunk(targetChunkX, targetChunkZ);
        if (targetChunk == null)
            return 0; // Default to dark for unloaded chunks

        return targetChunk.getBlocklight(localX, y, localZ);
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
        if (!chunk.isLightingDirty())
            return;

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
        if (chunk == null)
            return 1.0f; // Default to full light for unloaded chunks

        return chunk.getLightLevel(localX, localY, localZ, dayNightCycle);
    }

    /**
     * Helper class for light propagation.
     * Now includes chunk reference to properly track cross-chunk propagation.
     */
    private static class LightNode {
        int x, y, z, lightLevel;
        Chunk chunk; // Reference to the chunk containing this node

        LightNode(int x, int y, int z, int lightLevel, Chunk chunk) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lightLevel = lightLevel;
            this.chunk = chunk;
        }
    }
}
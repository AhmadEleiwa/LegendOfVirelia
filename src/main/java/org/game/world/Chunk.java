package org.game.world;

import org.game.meshes.Model;
import org.joml.Vector3f;

public class Chunk implements Cloneable {
    public static final int SIZE_X = 16, SIZE_Y = 16, SIZE_Z = 16;
    private static final int MAX_LIGHT_LEVEL = 15;

    private int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    private Model model; // mesh/model of this chunk

    // Lighting data
    private byte[][][] sunlight = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    private byte[][][] blocklight = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    private boolean lightingDirty = true; // Flag to track if lighting needs recalculation

    // Chunk coordinates and world position
    private int chunkX, chunkZ;
    private Vector3f position = new Vector3f();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.position.set(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        
        // Initialize lighting arrays
        initializeLighting();
    }

    private void initializeLighting() {
        // Initialize all light levels to 0
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    sunlight[x][y][z] = 0;
                    blocklight[x][y][z] = 0;
                }
            }
        }
    }

    @Override
    public Chunk clone() {
        Chunk clonedChunk = new Chunk(this.chunkX, this.chunkZ);
        
        // Copy block data
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                System.arraycopy(this.blocks[x][y], 0, clonedChunk.blocks[x][y], 0, SIZE_Z);
                System.arraycopy(this.sunlight[x][y], 0, clonedChunk.sunlight[x][y], 0, SIZE_Z);
                System.arraycopy(this.blocklight[x][y], 0, clonedChunk.blocklight[x][y], 0, SIZE_Z);
            }
        }
        
        clonedChunk.model = this.model;
        clonedChunk.lightingDirty = this.lightingDirty;
        return clonedChunk;
    }

    // Existing methods
    public Vector3f getPosition() { return position; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public Model getModel() { return model; }
    public void setModel(Model model) { this.model = model; }

    public int getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return blocks[x][y][z];
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public void setBlock(int x, int y, int z, int id) {
        if (inBounds(x, y, z)) {
            blocks[x][y][z] = id;
            lightingDirty = true; // Mark lighting for recalculation
        }
    }

    // Lighting methods
    public byte getSunlight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return sunlight[x][y][z];
    }

    public void setSunlight(int x, int y, int z, byte level) {
        if (inBounds(x, y, z)) {
            sunlight[x][y][z] = (byte) Math.max(0, Math.min(MAX_LIGHT_LEVEL, level));
        }
    }

    public byte getBlocklight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return blocklight[x][y][z];
    }

    public void setBlocklight(int x, int y, int z, byte level) {
        if (inBounds(x, y, z)) {
            blocklight[x][y][z] = (byte) Math.max(0, Math.min(MAX_LIGHT_LEVEL, level));
        }
    }

    /**
     * Get the combined light level (max of sun and block light)
     */
    public float getLightLevel(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 1.0f;
        
        byte sun = sunlight[x][y][z];
        byte block = blocklight[x][y][z];
        byte maxLight = (byte) Math.max(sun, block);
        
        return maxLight / (float) MAX_LIGHT_LEVEL;
    }

    /**
     * Get light level with day/night cycle consideration
     */
    public float getLightLevel(int x, int y, int z, float dayNightCycle) {
        if (!inBounds(x, y, z)) return 1.0f;
        
        byte sun = sunlight[x][y][z];
        byte block = blocklight[x][y][z];
        
        // Reduce sunlight during night
        float effectiveSunLight = sun * dayNightCycle;
        
        // Take the maximum of effective sun and block light
        float maxLight = Math.max(effectiveSunLight, block);
        
        // Add minimum ambient light
        maxLight = Math.max(maxLight, 2.0f);
        
        return maxLight / (float) MAX_LIGHT_LEVEL;
    }

    /**
     * Check if a block is transparent (lets light through)
     */
    public boolean isTransparent(int x, int y, int z) {
        if (!inBounds(x, y, z)) return true;
        int blockId = blocks[x][y][z];
        if (blockId == 0) return true; // Air is transparent
        
        Block block = BlockRegistry.getBlock(blockId);
        return block != null && block.isTransparent();
    }

    /**
     * Check if a block emits light
     */
    public int getLightEmission(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        int blockId = blocks[x][y][z];
        if (blockId == 0) return 0;
        
        Block block = BlockRegistry.getBlock(blockId);
        return block != null ? block.getLightLevel() : 0;
    }

    public boolean isLightingDirty() { return lightingDirty; }
    public void setLightingDirty(boolean dirty) { this.lightingDirty = dirty; }
    public void markLightingClean() { this.lightingDirty = false; }

    public int getMaxHeight() { return SIZE_Y; }
}
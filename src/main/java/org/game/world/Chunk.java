package org.game.world;

import org.game.meshes.Model;
import org.joml.Vector3f;

public class Chunk implements Cloneable {
    public static final int SIZE_X = 16, SIZE_Y = 64, SIZE_Z = 16;
    private static final int MAX_LIGHT_LEVEL = 15;

    private int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    private Model model;

    // Lighting data
    private byte[][][] sunlight = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    private byte[][][] blocklight = new byte[SIZE_X][SIZE_Y][SIZE_Z];
    private boolean lightingDirty = true;

    // Chunk coordinates and world position
    private int chunkX, chunkZ;
    private Vector3f position = new Vector3f();

    // FIX: Collapsed meshCompiled + isMeshBuilt into a single boolean.
    // The old code had both fields but isMeshBuilt() checked BOTH model != null
    // AND isMeshBuilt, while setMeshBuilt(false) nulled the model. This meant
    // the gatekeeper in generateVisibleMeshes could behave inconsistently when
    // one flag was stale. One field is enough: a chunk has a mesh or it doesn't.
    private boolean meshBuilt = false;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.position.set(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
        initializeLighting();
    }

    // --- Mesh state ---

    /** True only when the chunk has a valid compiled GPU mesh ready to draw. */
    public boolean isMeshBuilt() {
        return meshBuilt && model != null;
    }

    /**
     * Set to true after a successful buildModel + setModel call.
     * Set to false (with built=false) to delete GPU resources and require a rebuild.
     */
    public void setMeshBuilt(boolean built) {
        this.meshBuilt = built;
        if (!built && this.model != null) {
            this.model.delete(); // Free OpenGL buffers
            this.model = null;
        }
    }

    /** Force a mesh rebuild on next generateVisibleMeshes pass. */
    public void markDirty() {
        setMeshBuilt(false);
    }

    // --- Lighting ---

    private void initializeLighting() {
        for (int x = 0; x < SIZE_X; x++)
            for (int y = 0; y < SIZE_Y; y++)
                for (int z = 0; z < SIZE_Z; z++) {
                    sunlight[x][y][z] = 0;
                    blocklight[x][y][z] = 0;
                }
    }

    // --- Clone ---

    @Override
    public Chunk clone() {
        Chunk c = new Chunk(this.chunkX, this.chunkZ);
        for (int x = 0; x < SIZE_X; x++)
            for (int y = 0; y < SIZE_Y; y++) {
                System.arraycopy(this.blocks[x][y],     0, c.blocks[x][y],     0, SIZE_Z);
                System.arraycopy(this.sunlight[x][y],   0, c.sunlight[x][y],   0, SIZE_Z);
                System.arraycopy(this.blocklight[x][y], 0, c.blocklight[x][y], 0, SIZE_Z);
            }
        // FIX: Do NOT copy the model reference into the clone.
        // The server-side Chunk and the client-side Chunk must never share an
        // OpenGL Model object. Sharing it means both sides think they own the
        // GPU buffers; when either calls model.delete() the other side crashes.
        // Clones always start without a mesh — the client will build one.
        c.meshBuilt = false;
        c.model = null;
        c.lightingDirty = this.lightingDirty;
        return c;
    }

    // --- Position & coords ---

    public Vector3f getPosition() { return position; }
    public int getChunkX()        { return chunkX; }
    public int getChunkZ()        { return chunkZ; }

    // --- Model ---

    public Model getModel()              { return model; }
    public void  setModel(Model model)   { this.model = model; }

    // --- Blocks ---

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
            lightingDirty = true;
        }
    }

    // --- Lighting getters/setters ---

    public byte getSunlight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return sunlight[x][y][z];
    }

    public void setSunlight(int x, int y, int z, byte level) {
        if (inBounds(x, y, z))
            sunlight[x][y][z] = (byte) Math.max(0, Math.min(MAX_LIGHT_LEVEL, level));
    }

    public byte getBlocklight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return blocklight[x][y][z];
    }

    public void setBlocklight(int x, int y, int z, byte level) {
        if (inBounds(x, y, z))
            blocklight[x][y][z] = (byte) Math.max(0, Math.min(MAX_LIGHT_LEVEL, level));
    }

    public float getLightLevel(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 1.0f;
        byte maxLight = (byte) Math.max(sunlight[x][y][z], blocklight[x][y][z]);
        return maxLight / (float) MAX_LIGHT_LEVEL;
    }

    public float getLightLevel(int x, int y, int z, float dayNightCycle) {
        if (!inBounds(x, y, z)) return 1.0f;
        float effectiveSun = sunlight[x][y][z] * dayNightCycle;
        float maxLight = Math.max(effectiveSun, blocklight[x][y][z]);
        maxLight = Math.max(maxLight, 2.0f);
        return maxLight / (float) MAX_LIGHT_LEVEL;
    }

    public boolean isTransparent(int x, int y, int z) {
        if (!inBounds(x, y, z)) return true;
        int blockId = blocks[x][y][z];
        if (blockId == 0) return true;
        Block block = BlockRegistry.getBlock(blockId);
        return block != null && block.isTransparent();
    }

    public int getLightEmission(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        int blockId = blocks[x][y][z];
        if (blockId == 0) return 0;
        Block block = BlockRegistry.getBlock(blockId);
        return block != null ? block.getLightLevel() : 0;
    }

    public boolean isLightingDirty()          { return lightingDirty; }
    public void setLightingDirty(boolean d)   { this.lightingDirty = d; }
    public void markLightingClean()           { this.lightingDirty = false; }
    public int  getMaxHeight()                { return SIZE_Y; }
}
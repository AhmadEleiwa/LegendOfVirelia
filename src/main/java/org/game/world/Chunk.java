package org.game.world;

import org.game.meshes.Model;
import org.joml.Vector3f;

public class Chunk implements Cloneable {
    public static final int SIZE_X = 16, SIZE_Y = 16, SIZE_Z = 16;

    private int[][][] blocks = new int[SIZE_X][SIZE_Y][SIZE_Z];
    private Model model; // mesh/model of this chunk

    // NEW: chunk coordinates and world position
    private int chunkX, chunkZ;           // index in chunk grid
    private Vector3f position = new Vector3f(); // world-space translation

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        // convert chunk coords into world coordinates
        this.position.set(chunkX * SIZE_X, 0, chunkZ * SIZE_Z);
    }
    @Override
    public Chunk clone() {
        Chunk clonedChunk = new Chunk(this.chunkX, this.chunkZ);
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                System.arraycopy(this.blocks[x][y], 0, clonedChunk.blocks[x][y], 0, SIZE_Z);
            }
        }
        clonedChunk.model = this.model; // shallow copy, assuming Model is immutable or handled elsewhere
        return clonedChunk;
    }

    public Vector3f getPosition() {
        return position;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public int getBlock(int x, int y, int z) {
        return blocks[x][y][z];
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public void setBlock(int x, int y, int z, int id) {
        if (inBounds(x, y, z))
            blocks[x][y][z] = id;
    }
}

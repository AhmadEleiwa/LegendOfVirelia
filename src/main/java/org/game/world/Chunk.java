package org.game.world;

import org.game.meshes.Model;
import org.joml.Vector3f;

public class Chunk {
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

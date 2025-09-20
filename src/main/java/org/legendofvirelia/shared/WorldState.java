// shared/WorldState.java
package org.legendofvirelia.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.game.world.Chunk;
import org.game.world.World;

import org.game.world.BlockPlacer;
import org.joml.Vector3i;
import org.legendofvirelia.shared.command.Command;

public abstract class WorldState<T extends Command<?>, U extends Command<?>> {
    protected World world;
    protected long timeOfDay;
    protected boolean isRaining;
    protected boolean isWorldGenerated = false;

    protected Queue<T> incomingCommands; // Actions from server to apply
    protected Queue<U> outgoingCommands; // Actions to send to server

    protected boolean needRerender = false;
    protected List<Chunk> chunksToUpdate;
    protected BlockPlacer blockPlacer;

    // Action ID tracking for client-side prediction
    protected AtomicLong nextActionId = new AtomicLong(0);

    // Client/Server mode

    public WorldState() {
        world = new World();
        incomingCommands = new ConcurrentLinkedQueue<>();
        outgoingCommands = new ConcurrentLinkedQueue<>();
        chunksToUpdate = new ArrayList<>();

    }

    public abstract void init();

    public abstract void update(float delta);
    public abstract void sendCommand(U command);

    public void requestRerenderChunk(Chunk chunk) {
        if (chunk == null)
            return;
        synchronized (chunksToUpdate) {
            if (!chunksToUpdate.contains(chunk))
                chunksToUpdate.add(chunk);
        }
    }

    // Check if neighboring chunks need updates (for blocks on chunk boundaries)
    public void checkAndUpdateNeighboringChunks(Vector3i blockPos) {
        int chunkX = Math.floorDiv(blockPos.x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(blockPos.z, Chunk.SIZE_Z);

        int localX = blockPos.x - (chunkX * Chunk.SIZE_X);
        int localZ = blockPos.z - (chunkZ * Chunk.SIZE_Z);

        if (localX == 0) {
            Chunk neighborChunk = world.getChunk(chunkX - 1, chunkZ);
            if (neighborChunk != null)
                requestRerenderChunk(neighborChunk);
        }
        if (localX == Chunk.SIZE_X - 1) {
            Chunk neighborChunk = world.getChunk(chunkX + 1, chunkZ);
            if (neighborChunk != null)
                requestRerenderChunk(neighborChunk);
        }
        if (localZ == 0) {
            Chunk neighborChunk = world.getChunk(chunkX, chunkZ - 1);
            if (neighborChunk != null)
                requestRerenderChunk(neighborChunk);
        }
        if (localZ == Chunk.SIZE_Z - 1) {
            Chunk neighborChunk = world.getChunk(chunkX, chunkZ + 1);
            if (neighborChunk != null)
                requestRerenderChunk(neighborChunk);
        }
    }

    // Server-side method to receive actions
    public abstract void receiveServerCommands(T command);

    public World getCurrentWorld() {
        return this.world;
    }

    // Get actions to send to server (used by networking layer)
    public Queue<U> getOutgoingCommands() {
        return outgoingCommands;
    }

    public void copyWorld(World otherWorld) {
        this.world.copyFrom(otherWorld);
    }

    public BlockPlacer getBlockPlacer() {
        return blockPlacer;
    }

}
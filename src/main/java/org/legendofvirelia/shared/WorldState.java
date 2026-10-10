// shared/WorldState.java
package org.legendofvirelia.shared;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.game.world.Chunk;
import org.game.world.World;
import org.game.world.BlockPlacer;
import org.joml.Vector3i;
import org.legendofvirelia.shared.command.Command;

public abstract class WorldState<T extends Command<?>, U extends Command<?>> {
    protected World world;
    protected Queue<T> incomingCommands; // Actions from server to apply
    protected Queue<U> outgoingCommands; // Actions to send to server

    protected Set<Chunk> chunksToUpdate;
    protected BlockPlacer blockPlacer;

    public WorldState() {
        world = new World();
        incomingCommands = new ConcurrentLinkedQueue<>();
        outgoingCommands = new ConcurrentLinkedQueue<>();
        chunksToUpdate = new LinkedHashSet<>();

    }

    public abstract void init();

    public abstract void update(float delta);
    public abstract void sendCommand(U command);

    public void requestRerenderChunk(Chunk chunk) {
        if (chunk == null)
            return;
        synchronized (chunksToUpdate) {
            chunksToUpdate.add(chunk);
        }
    }

    protected Chunk pollChunkToUpdate() {
        synchronized (chunksToUpdate) {
            var iterator = chunksToUpdate.iterator();
            if (!iterator.hasNext())
                return null;
            Chunk chunk = iterator.next();
            iterator.remove();
            return chunk;
        }
    }

    // Refresh face culling only when the edit lies on a chunk border.
    public void checkAndUpdateNeighboringChunks(Vector3i blockPos) {
        int chunkX = Math.floorDiv(blockPos.x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(blockPos.z, Chunk.SIZE_Z);

        int localX = blockPos.x - (chunkX * Chunk.SIZE_X);
        int localZ = blockPos.z - (chunkZ * Chunk.SIZE_Z);

        if (localX == 0)
            requestRerenderChunk(world.getChunk(chunkX - 1, chunkZ));
        if (localX == Chunk.SIZE_X - 1)
            requestRerenderChunk(world.getChunk(chunkX + 1, chunkZ));
        if (localZ == 0)
            requestRerenderChunk(world.getChunk(chunkX, chunkZ - 1));
        if (localZ == Chunk.SIZE_Z - 1)
            requestRerenderChunk(world.getChunk(chunkX, chunkZ + 1));
        if (localX == 0 && localZ == 0)
            requestRerenderChunk(world.getChunk(chunkX - 1, chunkZ - 1));
        if (localX == 0 && localZ == Chunk.SIZE_Z - 1)
            requestRerenderChunk(world.getChunk(chunkX - 1, chunkZ + 1));
        if (localX == Chunk.SIZE_X - 1 && localZ == 0)
            requestRerenderChunk(world.getChunk(chunkX + 1, chunkZ - 1));
        if (localX == Chunk.SIZE_X - 1 && localZ == Chunk.SIZE_Z - 1)
            requestRerenderChunk(world.getChunk(chunkX + 1, chunkZ + 1));
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
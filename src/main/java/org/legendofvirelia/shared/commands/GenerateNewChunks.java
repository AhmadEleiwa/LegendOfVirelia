package org.legendofvirelia.shared.commands;

import org.joml.Vector3f;

import java.util.List;

import org.game.world.Chunk;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class GenerateNewChunks implements ServerCommand {
    public Vector3f position;

    public GenerateNewChunks(Vector3f position) {
        this.position = position;
    }

    @Override
    public void execute(ServerWorldState worldState) {
        int playerChunkX = (int) Math.floor(position.x / 16.0);
        int playerChunkZ = (int) Math.floor(position.z / 16.0);

        // Generate any missing chunks around the player (no-ops for existing ones)
        worldState.getCurrentWorld().generateNewChunks(position);

        // FIX: Use getNewChunksNear() instead of getAllChunks().
        //
        // The old code iterated getAllChunks() and re-sent EVERY chunk in view on
        // every player move. That meant the client received LoadSingleChunkCommands
        // for chunks it already had, replacing their Chunk objects with fresh clones
        // (isMeshBuilt = false), which caused generateVisibleMeshes() to rebuild
        // every visible mesh each time the player crossed a chunk border.
        //
        // getNewChunksNear() returns ONLY chunks that haven't been sent yet, and
        // markChunkSent() ensures each chunk is sent exactly once.
        List<Chunk> newChunks = worldState.getCurrentWorld()
                .getNewChunksNear(playerChunkX, playerChunkZ);

        for (Chunk chunk : newChunks) {
            worldState.sendCommand(new LoadSingleChunkCommand(chunk));
            worldState.getCurrentWorld().markChunkSent(chunk);
        }
    }
}
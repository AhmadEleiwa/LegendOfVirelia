package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class ClientReady implements ServerCommand {

    @Override
    public void execute(ServerWorldState worldState) {
        System.out.println("Client is ready!");

        worldState.getCurrentWorld().generateInitialChunks();

        // FIX: Mark every chunk in the initial batch as "sent" BEFORE sending
        // the GenerateWorld bulk command. This prevents GenerateNewChunks (which
        // fires on the first player move) from re-sending all the same chunks a
        // second time, which would replace the client's Chunk objects (resetting
        // isMeshBuilt to false) and trigger a full world re-mesh.
        for (Chunk chunk : worldState.getCurrentWorld().getAllChunks()) {
            worldState.getCurrentWorld().markChunkSent(chunk);
        }

        worldState.sendCommand(new GenerateWorld(worldState.getCurrentWorld()));
    }
}
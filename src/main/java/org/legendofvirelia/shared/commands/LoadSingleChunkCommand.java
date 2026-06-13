package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public class LoadSingleChunkCommand implements ClientCommand {
    private final Chunk chunk;
    private final int chunkX;
    private final int chunkZ;

    public LoadSingleChunkCommand(Chunk chunk) {
        this.chunk = chunk;
        this.chunkX = chunk.getChunkX();
        this.chunkZ = chunk.getChunkZ();
    }

    @Override
    public void execute(ClientWorldState clientState) {
 
        clientState.getCurrentWorld().setChunk(chunk);
        // clientState.requestRerenderChunk(chunk);
    }
}
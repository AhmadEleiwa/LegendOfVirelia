package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;
import org.joml.Vector3i;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public class BreakBlockConfirmCommand implements ClientCommand {
    public Vector3i position;

    public BreakBlockConfirmCommand(Vector3i position) {
        this.position = position;
    }

    @Override
    public void execute(ClientWorldState world) {
        // TODO Auto-generated method stub
        Chunk chunk = world.getBlockPlacer().breakBlock(position);

        if (chunk != null) {
            world.requestRerenderChunk(chunk);
            world.checkAndUpdateNeighboringChunks(position);
        }

    }

}

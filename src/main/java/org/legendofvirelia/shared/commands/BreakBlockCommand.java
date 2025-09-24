package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;
import org.joml.Vector3i;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class BreakBlockCommand implements ServerCommand {

    public Vector3i position;
    public BreakBlockCommand(Vector3i position) {
        this.position = position;

    }

    @Override
    public void execute(ServerWorldState world) {
        Chunk chunk = world.getBlockPlacer().breakBlock(position);
        if (chunk != null) {
            world.requestRerenderChunk(chunk);
            world.sendCommand(new BreakBlockConfirmCommand(position));
            world.checkAndUpdateNeighboringChunks(position);
        }

    }

}

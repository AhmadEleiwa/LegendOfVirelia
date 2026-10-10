package org.legendofvirelia.shared.commands;

import org.joml.Vector3f;

import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class GenerateNewChunks implements ServerCommand {
    public Vector3f position;

    public GenerateNewChunks(Vector3f position) {
        this.position = position;
    }

    @Override
    public void execute(ServerWorldState worldState) {
        worldState.getCurrentWorld().generateNewChunks(position);
    }
}
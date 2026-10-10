package org.legendofvirelia.shared.commands;

import java.util.List;
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
        new BlockChangesCommand(List.of(
                new BlockChange(position.x, position.y, position.z, 0))).execute(world);
    }

}

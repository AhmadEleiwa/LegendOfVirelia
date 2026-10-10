package org.legendofvirelia.shared.commands;
import java.util.List;
import org.joml.Vector3i;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class PlaceBlockCommand implements ServerCommand{
    public Vector3i position;
    public int blockId;
    
    public PlaceBlockCommand(Vector3i position, int blockId) {
        this.position = position;
        this.blockId = blockId;
    }

    @Override
    public void execute(ServerWorldState world) {
        new BlockChangesCommand(List.of(
                new BlockChange(position.x, position.y, position.z, blockId))).execute(world);
    }
}
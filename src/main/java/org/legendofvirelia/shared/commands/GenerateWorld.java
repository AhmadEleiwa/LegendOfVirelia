package org.legendofvirelia.shared.commands;


import org.game.world.World;
import org.game.world.WorldRenderer;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public class GenerateWorld implements ClientCommand {
    public World world;
    public GenerateWorld(World world){
        this.world = world;
    }
    @Override
    public void execute(ClientWorldState worldState) {
        worldState.copyWorld(world);
        WorldRenderer.generateMesh(worldState.getCurrentWorld());
    }
}

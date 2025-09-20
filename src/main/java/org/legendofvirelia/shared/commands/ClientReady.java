package org.legendofvirelia.shared.commands;


import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public class ClientReady implements ServerCommand {

    @Override
    public void execute(ServerWorldState worldState) {
        System.out.println("Client is ready!");
        worldState.getCurrentWorld().generateInitialChunks();
        worldState.sendCommand(new GenerateWorld(worldState.getCurrentWorld()));
    }
    
}

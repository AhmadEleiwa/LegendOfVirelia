package org.legendofvirelia.shared;

import org.engine.utils.Debug;
import org.game.world.BlockPlacer;
import org.legendofvirelia.shared.command.ClientCommand;
import org.legendofvirelia.shared.command.ServerCommand;

public class ServerWorldState extends WorldState<ServerCommand, ClientCommand>{
    public ServerWorldState() {
        super();
        
    }
    @Override
    public void init() {

        blockPlacer = new BlockPlacer(world);
        isWorldGenerated = true;

        // world.generateInitialChunks();
    }

    @Override
    public void update(float delta) {
        ServerCommand command;
        while ((command = incomingCommands.poll()) != null) {
            command.execute(this);
        }
        world.update(delta);
    }
    @Override
    public void sendCommand(ClientCommand command) {
        Debug.log("new Action from server!!");
        outgoingCommands.offer(command);
    }

    // Server-side method to receive actions
    @Override
    public void receiveServerCommands(ServerCommand command) {
        Debug.log("new Action from client!!");
        incomingCommands.offer(command);

    }

}

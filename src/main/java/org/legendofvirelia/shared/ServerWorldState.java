package org.legendofvirelia.shared;

import java.util.List;
import org.game.world.Chunk;
import org.game.world.BlockPlacer;
import org.legendofvirelia.shared.command.ClientCommand;
import org.legendofvirelia.shared.command.ServerCommand;
import org.legendofvirelia.shared.commands.LoadSingleChunkCommand;

public class ServerWorldState extends WorldState<ServerCommand, ClientCommand>{
    public ServerWorldState() {
        super();
        
    }
    @Override
    public void init() {
        blockPlacer = new BlockPlacer(world);
    }

    @Override
    public void update(float delta) {
        ServerCommand command;
        while ((command = incomingCommands.poll()) != null) {
            command.execute(this);
        }
        world.collectGeneratedChunks(4);
        sendAvailableChunks(2);
        world.update(delta);
    }

    private void sendAvailableChunks(int maximum) {
        List<Chunk> available = world.getNewChunksNear(
                world.getGenerationCenterX(), world.getGenerationCenterZ());
        int sent = 0;
        for (Chunk chunk : available) {
            if (sent >= maximum)
                break;
            sendCommand(new LoadSingleChunkCommand(chunk));
            world.markChunkSent(chunk);
            sent++;
        }
    }
    @Override
    public void sendCommand(ClientCommand command) {
        outgoingCommands.offer(command);
    }

    @Override
    public void receiveServerCommands(ServerCommand command) {
        incomingCommands.offer(command);
    }

}

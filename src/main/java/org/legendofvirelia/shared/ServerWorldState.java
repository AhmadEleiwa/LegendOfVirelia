package org.legendofvirelia.shared;

import java.util.ArrayList;
import java.util.List;
import org.game.world.Chunk;
import org.game.world.BlockPlacer;
import org.legendofvirelia.shared.command.ClientCommand;
import org.legendofvirelia.shared.commands.BlockChange;
import org.legendofvirelia.shared.commands.BlockChangesConfirmCommand;
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
        List<org.game.world.World.FluidUpdate> fluidUpdates = world.updateFluidBlocks(delta, 512);
        if (!fluidUpdates.isEmpty()) {
            List<BlockChange> changes = new ArrayList<>(fluidUpdates.size());
            for (org.game.world.World.FluidUpdate update : fluidUpdates)
                changes.add(new BlockChange(update.x, update.y, update.z,
                        update.blockId, update.level, update.source));
            sendCommand(new BlockChangesConfirmCommand(changes));
        }
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

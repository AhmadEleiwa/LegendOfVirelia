package org.legendofvirelia.shared;

import org.engine.rendering.Renderer;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.world.BlockPlacer;
import org.game.world.Chunk;
import org.game.world.WorldRenderer;
import org.legendofvirelia.shared.commands.ClientReady;
import org.legendofvirelia.shared.command.ClientCommand;
import org.legendofvirelia.shared.command.ServerCommand;

public class ClientWorldState extends WorldState<ClientCommand, ServerCommand> {

    @Override
    public void init() {
        blockPlacer = new BlockPlacer(world);
        WorldRenderer.initialize(this.world);
        sendCommand(new ClientReady());
    }

    @Override
    public void update(float delta) {
        // 1. Process server confirmations/corrections first
        processServerActions();

        // 2. Process new client actions with prediction
        processClientActions();
        world.update(delta);
    }

    private void processServerActions() {
        //
        ClientCommand command;
        while ((command = incomingCommands.poll()) != null) {
            command.execute(this);
        }
    }

    private void processClientActions() {
        // This processes actions that were just submitted by the client
        // They are applied immediately for client-side prediction
    }

    private void rerender() {
        WorldRenderer.generateMesh(world);
        needRerender = false;
    }

    private void rerenderChunks() {
        for (Chunk chunk : chunksToUpdate) {
            WorldRenderer.rebuildChunkAt(world, chunk);
        }
        chunksToUpdate.clear();
    }

    public void render(Renderer renderer, Camera camera, DirectionalLight light) {
        if (needRerender) {
            rerender();
        }
        if (!chunksToUpdate.isEmpty()) {
            rerenderChunks();
        }
        WorldRenderer.renderWorld(world, renderer, camera, light);
    }

    public void sendCommand(ServerCommand command) {
        outgoingCommands.offer(command);
    }

    public void requestRerender() {
        needRerender = true;
    }

    @Override
    public void receiveServerCommands(ClientCommand command) {
        incomingCommands.offer(command);

    }



}
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
        processServerActions();
        processClientActions();
        world.update(delta);
    }

    private void processServerActions() {
        ClientCommand command;
        while ((command = incomingCommands.poll()) != null) {
            command.execute(this);
        }
    }

    private void processClientActions() {
        // Prediction systems
    }

    private void rerenderChunks() {
        for (Chunk chunk : chunksToUpdate) {
            WorldRenderer.rebuildChunkAt(world, chunk);
        }
        chunksToUpdate.clear();
    }

    public void render(Renderer renderer, Camera camera, DirectionalLight light) {
        // 1. DYNAMIC STAGGERED GENERATION: Checks for missing chunk meshes smoothly every frame
        WorldRenderer.generateVisibleMeshes(world, camera.getPosition());

        // 2. Immediate Block Modifications (Placing/breaking torches/blocks)
        if (!chunksToUpdate.isEmpty()) {
            rerenderChunks();
        }

        // 3. Draw the final models to screen buffers
        WorldRenderer.renderWorld(world, renderer, camera, light);
    }

    public void sendCommand(ServerCommand command) {
        outgoingCommands.offer(command);
    }

    public void requestRerender() {
        // Safely kept if needed for fallback hooks, but bypasses global loop spikes
        this.needRerender = true; 
    }

    @Override
    public void receiveServerCommands(ClientCommand command) {
        incomingCommands.offer(command);
    }
}
package org.legendofvirelia.shared;

import java.util.ArrayList;
import java.util.List;

import org.engine.utils.Debug;
import org.engine.rendering.Renderer;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.world.BlockPlacer;
import org.game.world.Chunk;
import org.game.world.WorldRenderer;
import org.joml.Vector3i;
import org.legendofvirelia.shared.commands.BlockChange;
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
        world.update(delta);
    }

    private void processServerActions() {
        ClientCommand command;
        while ((command = incomingCommands.poll()) != null) {
            command.execute(this);
        }
    }

    public void applyBlockChanges(List<BlockChange> changes) {
        long startedAt = System.nanoTime();
        List<Vector3i> changedPositions = new ArrayList<>(changes.size());
        for (BlockChange change : changes) {
            Vector3i position = new Vector3i(change.x, change.y, change.z);
            Chunk targetChunk = world.getChunk(
                    Math.floorDiv(position.x, Chunk.SIZE_X),
                    Math.floorDiv(position.z, Chunk.SIZE_Z));
            if (targetChunk == null)
                continue;

            boolean sameBlock = targetChunk.getBlock(Math.floorMod(position.x, Chunk.SIZE_X), position.y,
                    Math.floorMod(position.z, Chunk.SIZE_Z)) == change.blockId;
            if (sameBlock && change.blockId != org.game.world.BlockRegistry.getId("water"))
                continue;

            Chunk chunk = sameBlock ? targetChunk : blockPlacer.placeBlock(position, change.blockId);
            if (chunk == null)
                continue;
            if (change.blockId == org.game.world.BlockRegistry.getId("water"))
                world.setFluidStateAt(position.x, position.y, position.z,
                        change.fluidLevel > 0 ? change.fluidLevel : org.game.world.FluidBlock.MAX_LEVEL,
                        change.fluidSource, org.game.world.FluidBlock.TIME_TO_NEXT_FLOW);

            changedPositions.add(position);
            requestRerenderChunk(chunk);
            checkAndUpdateNeighboringChunks(position);
        }

        for (Chunk chunk : WorldRenderer.updateBlockLighting(world, changedPositions))
            requestRerenderChunk(chunk);

        if (!changes.isEmpty()) {
            Debug.log("Applied block-change confirmation: " + changedPositions.size() + "/"
                    + changes.size() + " changed; " + pendingChunkUpdateCount()
                    + " chunks queued for remeshing in "
                    + ((System.nanoTime() - startedAt) / 1_000_000.0) + " ms.");
        }
    }

    private int pendingChunkUpdateCount() {
        synchronized (chunksToUpdate) {
            return chunksToUpdate.size();
        }
    }

    private boolean rerenderOneChunk() {
        Chunk chunk = pollChunkToUpdate();
        if (chunk == null)
            return false;
        WorldRenderer.rebuildChunkAt(world, chunk);
        return true;
    }

    public void render(Renderer renderer, Camera camera, DirectionalLight light) {
        if (!rerenderOneChunk())
            WorldRenderer.generateVisibleMeshes(world, camera.getPosition());

        // 3. Draw the final models to screen buffers.
        WorldRenderer.renderWorld(world, renderer, camera, light);
    }

    public void sendCommand(ServerCommand command) {
        outgoingCommands.offer(command);
    }

    @Override
    public void receiveServerCommands(ClientCommand command) {
        incomingCommands.offer(command);
    }
}
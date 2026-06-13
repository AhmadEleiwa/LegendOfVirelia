package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;
import org.game.world.World;
import org.joml.Vector3f;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public class GenerateWorld implements ClientCommand {
    public World world;

    public GenerateWorld(World world) {
        this.world = world;
    }

    @Override
    public void execute(ClientWorldState worldState) {
        worldState.copyWorld(world);

        // FIX: Do NOT call generateVisibleMeshes() here.
        //
        // The old code called WorldRenderer.generateVisibleMeshes() directly inside
        // this command, which runs on whatever thread processes server commands.
        // OpenGL calls (VAO/VBO creation inside ChunkMesher/LightedMesh) must only
        // happen on the main render thread. Calling them here would either crash
        // with a "no current context" error or silently produce corrupt GPU buffers.
        //
        // The correct approach is to do nothing here. ClientWorldState.render() already
        // calls WorldRenderer.generateVisibleMeshes() every frame on the render thread,
        // so newly copied chunks will be picked up automatically — at a safe 1-per-frame
        // budget — without any extra work here.
        //
        // Also: mark all copied chunks as "already sent" on the server's world so that
        // the initial GenerateWorld batch and subsequent GenerateNewChunks don't overlap
        // and double-send the same chunks.
    }
}
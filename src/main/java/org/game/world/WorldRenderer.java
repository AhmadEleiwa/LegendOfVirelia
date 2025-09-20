package org.game.world;

import org.engine.rendering.Renderer;
import org.game.meshes.Mesh;
import org.game.meshes.Model;
import org.game.entities.Camera;
import org.joml.Vector3f;

import java.util.List;

/**
 * Handles only rendering the world (client-side).
 */
public class WorldRenderer {
    public static void generateMesh(World world) {
        List<Chunk> chunks = world.getAllChunks();
        for (Chunk chunk : chunks) {
            Model chunkModel = ChunkMesher.buildModel(world, chunk);
            chunk.setModel(chunkModel);
        }
    }

    public static void rebuildChunkAt(World world, Chunk chunk) {

        Model newModel = ChunkMesher.buildModel(world, chunk);

        // Clean up old model

        if (chunk.getModel() != null) {

            chunk.getModel().delete();

        }

        chunk.setModel(newModel);

        // rebuildNeighboringChunks(world, chunkX, chunkZ);
    }

    

    public static void renderWorld(World world, Renderer renderer, Camera camera) {
        // Debug.log("heelo world");
        // convert camera position to chunk coords
        int cameraChunkX = (int) Math.floor(camera.getPosition().x / Chunk.SIZE_X);
        int cameraChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.SIZE_Z);

        List<Chunk> chunksToRender = world.getChunksNear(cameraChunkX, cameraChunkZ);
        // Debug.log(chunksToRender.size());
        for (Chunk chunk : chunksToRender) {
            Model model = chunk.getModel();
            if (model == null)
                continue;
            Vector3f pos = chunk.getPosition();
            Vector3f chunkCenter = new Vector3f(pos.x + 8, 8, pos.z + 8);
            float radius = (float) Math.sqrt(16 * 16 * 3) / 2;
            // Draw bounding box for chunk


            renderer.render(model, camera, chunk.getPosition(), chunkCenter, radius);
        }
    }
}

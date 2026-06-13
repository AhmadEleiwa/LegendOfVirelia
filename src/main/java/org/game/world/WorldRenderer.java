package org.game.world;

import org.engine.rendering.Renderer;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.meshes.Model;
import org.joml.Vector3f;
import java.util.List;

public class WorldRenderer {
    private static ChunkLightingSystem lightingSystem;
    private static float currentDayNightCycle = 1.0f;

    public static void initialize(World world) {
        lightingSystem = new ChunkLightingSystem(world);
        ChunkMesher.setLightingSystem(lightingSystem);
    }

    /**
     * Builds meshes for new (unmeshed) chunks only — one per frame to stay within budget.
     * Already-meshed chunks are never touched here; their model is permanent until
     * a block change or explicit markDirty() call.
     */
    public static void generateVisibleMeshes(World world, Vector3f playerPosition) {
        int playerChunkX = (int) Math.floor(playerPosition.x / 16.0);
        int playerChunkZ = (int) Math.floor(playerPosition.z / 16.0);

        List<Chunk> visibleChunks = world.getChunksNear(playerChunkX, playerChunkZ);

        int meshesBuiltThisFrame = 0;
        int maxMeshBuildsPerFrame = 1;

        for (Chunk chunk : visibleChunks) {
            // THE GATEKEEPER: skip every chunk that already has a valid mesh.
            if (chunk.isMeshBuilt()) continue;

            if (meshesBuiltThisFrame >= maxMeshBuildsPerFrame) break;

            lightingSystem.updateChunkLighting(chunk);
            Model chunkModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);
            chunk.setModel(chunkModel);
            chunk.setMeshBuilt(true);

            // When this chunk is meshed for the first time, its neighbors may have
            // culled their border faces because this chunk wasn't loaded yet.
            // Now that it exists, rebuild any already-meshed neighbors so they
            // re-evaluate those faces with the correct neighbor data.
            refreshNeighborBorders(world, chunk.getChunkX(), chunk.getChunkZ());

            meshesBuiltThisFrame++;
        }
    }

    /**
     * Explicitly rebuild a specific chunk (called after a block placement/break).
     * This is the only correct way to update an already-meshed chunk.
     */
    public static void rebuildChunkAt(World world, Chunk chunk) {
        if (chunk == null) return;

        lightingSystem.updateChunkLighting(chunk);
        Model newModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);

        // setMeshBuilt(false) frees the old GPU buffers before we replace the model
        chunk.setMeshBuilt(false);
        chunk.setModel(newModel);
        chunk.setMeshBuilt(true);
    }

    /**
     * Rebuild the chunk that contains the given world block, then conditionally
     * rebuild neighbors whose lighting was dirtied by the change.
     *
     * FIX: rebuildNeighboringChunksIfNeeded used to check isLightingDirty(), but
     * every chunk has lightingDirty=true by default (set whenever setBlock() is called
     * during world generation). That caused ALL neighbors of every changed chunk to be
     * rebuilt. Now we only rebuild a neighbor if its lighting is dirty AND it already
     * has a built mesh — newly generated chunks don't need neighbor-triggered rebuilds.
     */
    public static void rebuildChunkAtBlockChange(World world, int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = world.getChunk(chunkX, chunkZ);

        if (chunk == null) return;

        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);

        lightingSystem.updateLightingAt(chunk, localX, localY, localZ);
        rebuildChunkAt(world, chunk);
        rebuildNeighboringChunksIfNeeded(world, chunkX, chunkZ);
    }

    /**
     * After a brand-new chunk gets its first mesh, tell each already-meshed cardinal
     * neighbor to rebuild. Those neighbors previously culled their border faces toward
     * this chunk (because it was null at mesh time). Now they need to re-run
     * isFaceVisible so those faces appear correctly.
     *
     * Only rebuilds neighbors that are already meshed — unmeshed neighbors will
     * naturally include correct border faces when they get built later.
     */
    private static void refreshNeighborBorders(World world, int chunkX, int chunkZ) {
        int[][] neighbors = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] n : neighbors) {
            Chunk neighbor = world.getChunk(chunkX + n[0], chunkZ + n[1]);
            if (neighbor != null && neighbor.isMeshBuilt()) {
                rebuildChunkAt(world, neighbor);
            }
        }
    }

    private static void rebuildNeighboringChunksIfNeeded(World world, int chunkX, int chunkZ) {
        int[][] neighbors = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] n : neighbors) {
            Chunk neighbor = world.getChunk(chunkX + n[0], chunkZ + n[1]);
            // FIX: Only rebuild neighbors that (a) are dirty AND (b) already have a mesh.
            // New chunks have lightingDirty=true but no mesh yet; they'll be lit and
            // meshed normally by generateVisibleMeshes — no need to force-rebuild them here.
            if (neighbor != null && neighbor.isLightingDirty() && neighbor.isMeshBuilt()) {
                rebuildChunkAt(world, neighbor);
            }
        }
    }

    public static void updateDayNightCycle(World world, float newDayNightCycle) {
        if (Math.abs(currentDayNightCycle - newDayNightCycle) > 0.05f) {
            currentDayNightCycle = newDayNightCycle;
            // Pass as a uniform to cube.frag instead of rebuilding CPU meshes.
        }
    }

    public static void renderWorld(World world, Renderer renderer, Camera camera, DirectionalLight light) {
        int cameraChunkX = (int) Math.floor(camera.getPosition().x / Chunk.SIZE_X);
        int cameraChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.SIZE_Z);

        for (Chunk chunk : world.getChunksNear(cameraChunkX, cameraChunkZ)) {
            if (!chunk.isMeshBuilt()) continue;

            Model model = chunk.getModel();
            if (model == null) continue;

            Vector3f pos = chunk.getPosition();
            Vector3f chunkCenter = new Vector3f(pos.x + 8, 8, pos.z + 8);
            float radius = (float) Math.sqrt(16 * 16 * 3) / 2;

            renderer.render(model, camera, light, chunk.getPosition(), chunkCenter, radius);
        }
    }
}
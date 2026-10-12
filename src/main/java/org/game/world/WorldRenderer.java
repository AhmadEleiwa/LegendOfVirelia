package org.game.world;

import org.engine.utils.Debug;
import org.engine.rendering.Renderer;
import org.engine.rendering.WaterRenderer;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.meshes.Model;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector3f;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDepthMask;

public class WorldRenderer {
    private static ChunkLightingSystem lightingSystem;
    private static WaterRenderer waterRenderer;
    private static float currentDayNightCycle = 1.0f;
    private static final Set<Chunk> borderRefreshQueue = new LinkedHashSet<>();

    public static void initialize(World world) {
        borderRefreshQueue.clear();
        lightingSystem = new ChunkLightingSystem(world);
        ChunkMesher.setLightingSystem(lightingSystem);
    }

    public static void setWaterRenderer(WaterRenderer renderer) {
        waterRenderer = renderer;
    }

    /** Builds or refreshes at most one visible chunk mesh per frame. */
    public static void generateVisibleMeshes(World world, Vector3f playerPosition) {
        int playerChunkX = Math.floorDiv((int) Math.floor(playerPosition.x), Chunk.SIZE_X);
        int playerChunkZ = Math.floorDiv((int) Math.floor(playerPosition.z), Chunk.SIZE_Z);

        List<Chunk> visibleChunks = world.getChunksNear(playerChunkX, playerChunkZ);

        Chunk candidate = null;
        long nearestDistance = Long.MAX_VALUE;
        for (Chunk chunk : visibleChunks) {
            if (chunk.isMeshBuilt() && !chunk.isLightingDirty())
                continue;

            long dx = chunk.getChunkX() - playerChunkX;
            long dz = chunk.getChunkZ() - playerChunkZ;
            long distance = dx * dx + dz * dz;
            if (distance < nearestDistance) {
                candidate = chunk;
                nearestDistance = distance;
            }
        }

        if (candidate != null) {
            if (candidate.isMeshBuilt()) {
                rebuildChunkAt(world, candidate);
            } else {
                lightingSystem.updateChunkLighting(candidate);
                Model chunkModel = ChunkMesher.buildModel(world, candidate, currentDayNightCycle);
                Model waterModel = ChunkMesher.buildWaterModel(world, candidate);
                candidate.setModel(chunkModel);
                candidate.setWaterModel(waterModel);
                candidate.setMeshBuilt(true);
                refreshNeighborBorders(world, candidate.getChunkX(), candidate.getChunkZ());
            }
            return;
        }

        Chunk borderChunk = pollBorderRefresh(world);
        if (borderChunk != null)
            rebuildChunkAt(world, borderChunk);
    }

    private static Chunk pollBorderRefresh(World world) {
        var iterator = borderRefreshQueue.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            iterator.remove();
            if (world.getChunk(chunk.getChunkX(), chunk.getChunkZ()) == chunk && chunk.isMeshBuilt())
                return chunk;
        }
        return null;
    }

    /**
     * Explicitly rebuild a specific chunk (called after a block placement/break).
     * This is the only correct way to update an already-meshed chunk.
     */
    public static void rebuildChunkAt(World world, Chunk chunk) {
        if (chunk == null) return;

        long startedAt = System.nanoTime();
        lightingSystem.updateChunkLighting(chunk);
        Model newModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);
        Model newWaterModel = ChunkMesher.buildWaterModel(world, chunk);

        // setMeshBuilt(false) frees the old GPU buffers before we replace the model
        chunk.setMeshBuilt(false);
        chunk.setModel(newModel);
        chunk.setWaterModel(newWaterModel);
        chunk.setMeshBuilt(true);
        Debug.log("Rebuilt chunk mesh (" + chunk.getChunkX() + ", " + chunk.getChunkZ()
                + ") in " + ((System.nanoTime() - startedAt) / 1_000_000.0) + " ms.");
    }

    public static Set<Chunk> updateBlockLighting(World world, List<Vector3i> changedBlocks) {
        return lightingSystem.updateBlockLighting(changedBlocks);
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
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0)
                    continue;
                Chunk neighbor = world.getChunk(chunkX + dx, chunkZ + dz);
                if (neighbor != null && neighbor.isMeshBuilt())
                    borderRefreshQueue.add(neighbor);
            }
    }

    public static void renderWorld(World world, Renderer renderer, Camera camera, DirectionalLight light) {
        int cameraChunkX = (int) Math.floor(camera.getPosition().x / Chunk.SIZE_X);
        int cameraChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.SIZE_Z);

        List<Chunk> visibleChunks = world.getChunksNear(cameraChunkX, cameraChunkZ);
        for (Chunk chunk : visibleChunks) {
            if (!chunk.isMeshBuilt()) continue;

            Model model = chunk.getModel();
            if (model == null) continue;

            Vector3f pos = chunk.getPosition();
            Vector3f chunkCenter = new Vector3f(pos.x + 8, 8, pos.z + 8);
            float radius = (float) Math.sqrt(16 * 16 * 3) / 2;

            renderer.render(model, camera, light, chunk.getPosition(), chunkCenter, radius);
        }

        if (waterRenderer == null)
            return;

        List<Chunk> waterChunks = new ArrayList<>();
        for (Chunk chunk : visibleChunks) {
            if (chunk.isMeshBuilt() && chunk.getWaterModel() != null)
                waterChunks.add(chunk);
        }
        waterChunks.sort(Comparator.comparingDouble((Chunk chunk) ->
                chunk.getPosition().distanceSquared(camera.getViewPosition())).reversed());

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        Matrix4f projection = camera.getProjection();
        Matrix4f view = camera.getView();
        float animationTime = (float) (System.nanoTime() / 1_000_000_000.0);
        for (Chunk chunk : waterChunks)
            waterRenderer.render(chunk.getWaterModel(), camera, light, chunk.getPosition(),
                    projection, view, animationTime);
        glDepthMask(true);
        glDisable(GL_BLEND);
    }
}
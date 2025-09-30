package org.game.world;

import org.engine.rendering.Renderer;
import org.game.meshes.Model;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.lighting.PointLight;
import org.joml.Vector3f;

import java.util.List;

/**
 * Handles only rendering the world (client-side) with lighting support.
 */
public class WorldRenderer {
    private static ChunkLightingSystem lightingSystem;
    private static float currentDayNightCycle = 1.0f; // 1.0 = full day, 0.0 = full night
    
    /**
     * Initialize the world renderer with lighting system
     */
    public static void initialize(World world) {
        lightingSystem = new ChunkLightingSystem(world);
        ChunkMesher.setLightingSystem(lightingSystem);
    }
    
    /**
     * Generate mesh for all chunks with lighting
     */
    public static void generateMesh(World world) {
        List<Chunk> chunks = world.getAllChunks();
        for (Chunk chunk : chunks) {
            // Calculate lighting for the chunk
            lightingSystem.updateChunkLighting(chunk);
            
            // Generate mesh with lighting
            Model chunkModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);
            chunk.setModel(chunkModel);
        }
    }
    
    /**
     * Rebuild a specific chunk with lighting
     */
    public static void rebuildChunkAt(World world, Chunk chunk) {
        // Update lighting for this chunk
        lightingSystem.updateChunkLighting(chunk);
        
        // Generate new mesh with lighting
        Model newModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);

        // Clean up old model
        if (chunk.getModel() != null) {
            chunk.getModel().delete();
        }

        chunk.setModel(newModel);
        
        // TODO: Consider rebuilding neighboring chunks if lighting changes affect them
    }
    
    /**
     * Rebuild chunk when a block changes (handles lighting updates)
     */
    public static void rebuildChunkAtBlockChange(World world, int worldX, int worldY, int worldZ) {
        // Get the chunk containing this block
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        
        if (chunk == null) return;
        
        // Update lighting around the changed block
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        
        lightingSystem.updateLightingAt(chunk, localX, localY, localZ);
        
        // Rebuild the chunk mesh
        rebuildChunkAt(world, chunk);
        
        // Check if neighboring chunks need rebuilding (for cross-chunk lighting)
        rebuildNeighboringChunksIfNeeded(world, chunkX, chunkZ);
    }
    
    /**
     * Rebuild neighboring chunks if lighting changes affect them
     */
    private static void rebuildNeighboringChunksIfNeeded(World world, int chunkX, int chunkZ) {
        // Check 4 neighboring chunks
        int[][] neighbors = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        for (int[] neighbor : neighbors) {
            int nx = chunkX + neighbor[0];
            int nz = chunkZ + neighbor[1];
            Chunk neighborChunk = world.getChunk(nx, nz);
            
            if (neighborChunk != null && neighborChunk.isLightingDirty()) {
                rebuildChunkAt(world, neighborChunk);
            }
        }
    }
    
    /**
     * Update day/night cycle and regenerate meshes if needed
     */
    public static void updateDayNightCycle(World world, float newDayNightCycle) {
        if (Math.abs(currentDayNightCycle - newDayNightCycle) > 0.01f) {
            currentDayNightCycle = newDayNightCycle;
            
            // Regenerate meshes with new lighting
            // Note: In a real game, you might want to do this more efficiently
            // by only updating chunks that are currently visible
            List<Chunk> visibleChunks = world.getAllChunks(); // Or get only visible chunks
            for (Chunk chunk : visibleChunks) {
                if (chunk.getModel() != null) {
                    chunk.getModel().delete();
                    Model newModel = ChunkMesher.buildModel(world, chunk, currentDayNightCycle);
                    chunk.setModel(newModel);
                }
            }
        }
    }
    
    /**
     * Render the world with lighting
     */
    public static void renderWorld(World world, Renderer renderer, Camera camera, DirectionalLight light, PointLight pointLight) {
        // Convert camera position to chunk coords
        int cameraChunkX = (int) Math.floor(camera.getPosition().x / Chunk.SIZE_X);
        int cameraChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.SIZE_Z);

        List<Chunk> chunksToRender = world.getChunksNear(cameraChunkX, cameraChunkZ);
        
        for (Chunk chunk : chunksToRender) {
            Model model = chunk.getModel();
            if (model == null) continue;
            
            Vector3f pos = chunk.getPosition();
            Vector3f chunkCenter = new Vector3f(pos.x + 8, 8, pos.z + 8);
            float radius = (float) Math.sqrt(16 * 16 * 3) / 2;

            renderer.render(model, camera, light,pointLight, chunk.getPosition(), chunkCenter, radius);
        }
    }
    
    /**
     * Add a light source (like a torch) at world coordinates
     */
    public static void addLightSource(World world, int worldX, int worldY, int worldZ, int lightLevel) {
        // This would be called when a torch or other light source is placed
        // // Block torchBlock = new Block.TorchBlock(); // Assuming you have this
        // world.setBlockAt(worldX, worldY, worldZ, BlockRegistry.getId(torchBlock.getName()));
        
        // // Rebuild the chunk and neighbors
        // rebuildChunkAtBlockChange(world, worldX, worldY, worldZ);
    }
    
    /**
     * Remove a light source at world coordinates
     */
    public static void removeLightSource(World world, int worldX, int worldY, int worldZ) {
        world.setBlockAt(worldX, worldY, worldZ, 0); // Set to air
        rebuildChunkAtBlockChange(world, worldX, worldY, worldZ);
    }
    
    // Getters
    public static float getCurrentDayNightCycle() {
        return currentDayNightCycle;
    }
    
    public static ChunkLightingSystem getLightingSystem() {
        return lightingSystem;
    }
}
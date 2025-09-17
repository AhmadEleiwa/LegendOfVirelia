package org.game.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.engine.rendering.Renderer;
import org.engine.utils.Debug;
import org.game.entities.Camera;
import org.game.meshes.Model;
import org.joml.Vector3f;

public class World {
    private final Map<String, Chunk> chunks = new HashMap<>();
    
    // Render distance in chunks (not blocks)
    private int renderDistance = 6; // Render chunks within 6 chunk radius
    
    public World() {
        generateInitialChunks();
    }
    
    private void generateInitialChunks() {
        // Build a larger world for testing
        for (int cx = -15; cx < 15; cx++) {
            for (int cz = -15; cz < 15; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                
                // Generate some basic terrain
                for (int x = 0; x < Chunk.SIZE_X; x++) {
                    for (int z = 0; z < Chunk.SIZE_Z; z++) {
                        for (int y = 0; y < Chunk.SIZE_Y; y++) {
                            // Simple height-based terrain
                            int height = 8 + (int)(Math.sin(cx * 0.1) * 3 + Math.cos(cz * 0.1) * 3);
                            if (y <= height) {
                                chunk.setBlock(x, y, z, BlockRegistry.getId("dirt"));
                            }
                        }
                    }
                }
                
                String chunkKey = cx + "," + cz;
                chunks.put(chunkKey, chunk);
            }
        }
        
        // Now that all chunks are created, we can build their meshes
        // This avoids issues with meshing a chunk before its neighbors exist
        for (Chunk chunk : chunks.values()) {
            Model chunkModel = ChunkMesher.buildModel(this, chunk);
            chunk.setModel(chunkModel);
        }
        
        Debug.log("World initialized with " + chunks.size() + " total chunks");
    }

    /**
     * Get block at world coordinates
     */
    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        // Convert world coordinates to chunk coordinates
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        
        // Get local coordinates within the chunk
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        
        // Get the chunk
        String chunkKey = chunkX + "," + chunkZ;
        Chunk chunk = chunks.get(chunkKey);
        
        if (chunk == null || !chunk.inBounds(localX, localY, localZ)) {
            return null; // The block is outside the generated world
        }
        
        int blockId = chunk.getBlock(localX, localY, localZ);
        return BlockRegistry.getBlock(blockId);
    }
    
    /**
     * Set block at world coordinates
     */
    public boolean setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        // Convert world coordinates to chunk coordinates
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE_Z);
        
        // Get local coordinates within the chunk
        int localX = worldX - (chunkX * Chunk.SIZE_X);
        int localY = worldY;
        int localZ = worldZ - (chunkZ * Chunk.SIZE_Z);
        
        // Get the chunk
        String chunkKey = chunkX + "," + chunkZ;
        Chunk chunk = chunks.get(chunkKey);
        
        if (chunk == null || !chunk.inBounds(localX, localY, localZ)) {
            return false;
        }
        
        // Set the block
        chunk.setBlock(localX, localY, localZ, blockId);
        
        // Rebuild the chunk mesh
        rebuildChunk(chunk);
        
        // Also rebuild neighboring chunks if the block is on a chunk boundary
        rebuildNeighboringChunks(chunkX, chunkZ, localX, localY, localZ);
        
        Debug.log("Block placed at (" + worldX + ", " + worldY + ", " + worldZ + ")");
        return true;
    }
    
    /**
     * Rebuild a single chunk's mesh
     */
    private void rebuildChunk(Chunk chunk) {
        // Pass the 'this' reference to the mesher
        Model newModel = ChunkMesher.buildModel(this, chunk); 
        
        // Clean up old model
        if (chunk.getModel() != null) {
            chunk.getModel().delete();
        }
        
        chunk.setModel(newModel);
    }
    
    /**
     * Rebuild neighboring chunks if block is on boundary
     */
    private void rebuildNeighboringChunks(int chunkX, int chunkZ, int localX, int localY, int localZ) {
        // Check if block is on chunk boundaries and rebuild neighboring chunks
        // You've already correctly identified the neighbors that need rebuilding
        if (localX == 0) rebuildChunkAt(chunkX - 1, chunkZ);
        if (localX == Chunk.SIZE_X - 1) rebuildChunkAt(chunkX + 1, chunkZ);
        if (localZ == 0) rebuildChunkAt(chunkX, chunkZ - 1);
        if (localZ == Chunk.SIZE_Z - 1) rebuildChunkAt(chunkX, chunkZ + 1);
        // Note: You may also need to handle Y boundaries for vertical chunks
    }
    
    private void rebuildChunkAt(int chunkX, int chunkZ) {
        String chunkKey = chunkX + "," + chunkZ;
        Chunk chunk = chunks.get(chunkKey);
        if (chunk != null) {
            rebuildChunk(chunk);
        }
    }

    public List<Chunk> getChunks() {
        return new ArrayList<>(chunks.values());
    }

    /**
     * Return only chunks within render distance of the camera
     */
    public List<Chunk> getVisibleChunks(Camera camera) {
        List<Chunk> visibleChunks = new ArrayList<>();
        Vector3f cameraPos = camera.getPosition();
        
        // Convert camera world position to chunk coordinates
        int cameraChunkX = (int) Math.floor(cameraPos.x / Chunk.SIZE_X);
        int cameraChunkZ = (int) Math.floor(cameraPos.z / Chunk.SIZE_Z);
        
        for (Chunk chunk : chunks.values()) {
            // Calculate distance in chunk coordinates
            int deltaX = chunk.getChunkX() - cameraChunkX;
            int deltaZ = chunk.getChunkZ() - cameraChunkZ;
            double chunkDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            // Only include chunks within render distance
            if (chunkDistance <= renderDistance) {
                visibleChunks.add(chunk);
            }
        }
        
        return visibleChunks;
    }

    public void update(float delta) {
        // In a real engine, handle chunk streaming/loading here
    }

    public void render(Renderer renderer, Camera camera) {
        // Get only visible chunks within render distance
        List<Chunk> visibleChunks = getVisibleChunks(camera);
        
        for (Chunk chunk : visibleChunks) {
            Model model = chunk.getModel();
            if (model != null) {
                Vector3f pos = chunk.getPosition();
                Vector3f chunkCenter = new Vector3f(pos.x + 8, 8, pos.z + 8);
                float radius = (float) Math.sqrt(16 * 16 * 3) / 2; // half-diagonal 
                
                renderer.render(model, camera, chunk.getPosition(), chunkCenter, radius);
            }
        }
    }
    
    // Getters and setters for render distance
    public int getRenderDistance() {
        return renderDistance;
    }
    
    public void setRenderDistance(int renderDistance) {
        this.renderDistance = Math.max(1, Math.min(32, renderDistance)); // Clamp between 1-32
    }
    
    // Method to increase/decrease render distance at runtime
    public void adjustRenderDistance(int delta) {
        setRenderDistance(renderDistance + delta);
        Debug.log("Render distance set to: " + renderDistance + " chunks");
    }
}
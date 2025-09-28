package org.game.world;

import org.game.meshes.LightedMesh;
import org.game.meshes.Mesh;
import org.game.meshes.Model;
import org.game.meshes.Quad;
import org.game.utils.AtlasBuilder;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {
    private static ChunkLightingSystem lightingSystem;
    
    public static void setLightingSystem(ChunkLightingSystem system) {
        lightingSystem = system;
    }
    
    public static Model buildModel(World world, Chunk chunk) {
        return buildModel(world, chunk, 1.0f); // Default to full daylight
    }
    
    // Updated buildModel method with lighting support
    public static Model buildModel(World world, Chunk chunk, float dayNightCycle) {
        List<Float> vertices = new ArrayList<>(); // Now includes lighting data
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Get the chunk's world coordinates
        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldY = 0;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    int blockId = chunk.getBlock(x, y, z);
                    if (blockId == 0) continue; // Skip air blocks

                    // Check each face and add if visible
                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, 1, 0, 0)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.RIGHT_FACE, blockId, dayNightCycle);
                    }

                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, -1, 0, 0)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.LEFT_FACE, blockId, dayNightCycle);
                    }
                    
                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, 0, 1, 0)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.TOP_FACE, blockId, dayNightCycle);
                    }

                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, 0, -1, 0)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.BOTTOM_FACE, blockId, dayNightCycle);
                    }

                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, 0, 0, 1)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.FRONT_FACE, blockId, dayNightCycle);
                    }

                    if (isFaceVisible(world, chunkWorldX + x, chunkWorldY + y, chunkWorldZ + z, 0, 0, -1)) {
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, 
                                          Quad.FaceType.BACK_FACE, blockId, dayNightCycle);
                    }
                }
            }
        }

        // Create mesh with lighting data (now 4 floats per vertex: x, y, z, lightLevel)
        Mesh mesh = createMeshWithLighting(vertices, texCoords, indices);
        return new Model(mesh);
    }
    
    private static void addFaceWithLighting(
            List<Float> vertices, List<Float> texCoords, List<Integer> indices,
            Chunk chunk, int x, int y, int z,
            Quad.FaceType faceType, int blockId, float dayNightCycle) {

        Quad quad = Quad.getQuad(faceType);
        Block block = BlockRegistry.getBlock(blockId);
        if (block == null) return;

        // Get texture coordinates (same as before)
        float[] atlasRect = AtlasBuilder.getDefault().getUV(block.getName());
        float[] facePixels = getFacePixels(block, faceType);

        float u0b = facePixels[0] / 16f;
        float v0b = facePixels[1] / 16f;
        float u1b = facePixels[2] / 16f;
        float v1b = facePixels[3] / 16f;

        float uMinAtlas = atlasRect[0];
        float vMinAtlas = atlasRect[1];
        float uMaxAtlas = atlasRect[2];
        float vMaxAtlas = atlasRect[3];

        int baseIndex = vertices.size() / 4; // Now 4 floats per vertex (x,y,z,light)
        
        // Add vertices with lighting
        for (int i = 0; i < quad.positions.length; i += 3) {
            float vx = quad.positions[i] + x;
            float vy = quad.positions[i + 1] + y;
            float vz = quad.positions[i + 2] + z;
            
            // Calculate light level for this vertex
            float lightLevel = getVertexLightLevel(chunk, x, y, z, faceType, i / 3, dayNightCycle);
            
            vertices.add(vx);
            vertices.add(vy);
            vertices.add(vz);
            vertices.add(lightLevel); // Add light level as 4th component
        }

        // Add texture coordinates (same as before)
        for (int i = 0; i < quad.texCoords.length; i += 2) {
            float u = quad.texCoords[i];
            float v = quad.texCoords[i + 1];

            float uBlock = u0b + u * (u1b - u0b);
            float vBlock = v0b + v * (v1b - v0b);

            float atlasU = uMinAtlas + uBlock * (uMaxAtlas - uMinAtlas);
            float atlasV = vMinAtlas + vBlock * (vMaxAtlas - vMinAtlas);

            texCoords.add(atlasU);
            texCoords.add(atlasV);
        }

        // Add indices (same as before)
        for (int i = 0; i < quad.indices.length; i++) {
            indices.add(quad.indices[i] + baseIndex);
        }
    }
    
    /**
     * Calculate light level for a vertex based on surrounding blocks
     */
    private static float getVertexLightLevel(Chunk chunk, int blockX, int blockY, int blockZ, 
                                           Quad.FaceType faceType, int vertexIndex, float dayNightCycle) {
        // Sample light from the face direction
        int[] offset = getFaceOffset(faceType);
        int sampleX = blockX + offset[0];
        int sampleY = blockY + offset[1];
        int sampleZ = blockZ + offset[2];
        
        // Get light level, defaulting to current block if outside chunk
        if (chunk.inBounds(sampleX, sampleY, sampleZ)) {
            return chunk.getLightLevel(sampleX, sampleY, sampleZ, dayNightCycle);
        } else {
            // For faces at chunk boundaries, use the current block's light level
            return chunk.getLightLevel(blockX, blockY, blockZ, dayNightCycle);
        }
    }
    
    /**
     * Get the direction offset for a face
     */
    private static int[] getFaceOffset(Quad.FaceType faceType) {
        switch (faceType) {
            case TOP_FACE: return new int[]{0, 1, 0};
            case BOTTOM_FACE: return new int[]{0, -1, 0};
            case RIGHT_FACE: return new int[]{1, 0, 0};
            case LEFT_FACE: return new int[]{-1, 0, 0};
            case FRONT_FACE: return new int[]{0, 0, 1};
            case BACK_FACE: return new int[]{0, 0, -1};
            default: return new int[]{0, 0, 0};
        }
    }
    
    /**
     * Create a mesh that includes lighting data in vertices
     */
    private static Mesh createMeshWithLighting(List<Float> vertices, List<Float> texCoords, List<Integer> indices) {
        // Extract positions and lighting from combined vertex data
        float[] positions = new float[(vertices.size() / 4) * 3];
        float[] lighting = new float[vertices.size() / 4];
        
        for (int i = 0; i < vertices.size() / 4; i++) {
            positions[i * 3] = vertices.get(i * 4);
            positions[i * 3 + 1] = vertices.get(i * 4 + 1);
            positions[i * 3 + 2] = vertices.get(i * 4 + 2);
            lighting[i] = vertices.get(i * 4 + 3);
        }
        
        float[] texCoordArray = listToFloatArray(texCoords);
        int[] indexArray = listToIntArray(indices);
        
        // Create custom mesh constructor that handles lighting
        return new LightedMesh(positions, texCoordArray,lighting, indexArray);
    }
    
    // Helper methods (same as before)
    private static boolean isFaceVisible(World world, int worldX, int worldY, int worldZ, int dx, int dy, int dz) {
        int neighborX = worldX + dx;
        int neighborY = worldY + dy;
        int neighborZ = worldZ + dz;
        
        Block neighborBlock = world.getBlockAt(neighborX, neighborY, neighborZ);
        return neighborBlock == null || BlockRegistry.getId(neighborBlock.getName()) == 0;
    }
    
    private static float[] getFacePixels(Block block, Quad.FaceType faceType) {
        switch (faceType) {
            case TOP_FACE:
                return block.model.elements.get(0).faces.get("up").uv;
            case BOTTOM_FACE:
                return block.model.elements.get(0).faces.get("down").uv;
            case FRONT_FACE:
                return block.model.elements.get(0).faces.get("north").uv;
            case BACK_FACE:
                return block.model.elements.get(0).faces.get("south").uv;
            case LEFT_FACE:
                return block.model.elements.get(0).faces.get("west").uv;
            case RIGHT_FACE:
                return block.model.elements.get(0).faces.get("east").uv;
            default:
                return new float[]{0, 0, 16, 16};
        }
    }

    private static float[] listToFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static int[] listToIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
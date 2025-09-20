package org.game.world;

import org.game.meshes.Mesh;
import org.game.meshes.Model;
import org.game.meshes.Quad;
import org.game.utils.AtlasBuilder;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {
    
    // The buildModel method now needs a reference to the World object
    public static Model buildModel(World world, Chunk chunk) {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Get the chunk's world coordinates for easy lookup
        int chunkX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkY = 0; // Assuming chunks are along XZ plane
        int chunkZ = chunk.getChunkZ() * Chunk.SIZE_Z;

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    int blockId = chunk.getBlock(x, y, z);
                    if (blockId == 0) continue; // Skip air blocks

                    // --- NEW INTER-CHUNK CULLING LOGIC ---
                    
                    // RIGHT_FACE (x + 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, 1, 0, 0)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.RIGHT_FACE, blockId);
                    }

                    // LEFT_FACE (x - 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, -1, 0, 0)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.LEFT_FACE, blockId);
                    }
                    
                    // TOP_FACE (y + 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, 0, 1, 0)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.TOP_FACE, blockId);
                    }

                    // BOTTOM_FACE (y - 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, 0, -1, 0)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.BOTTOM_FACE, blockId);
                    }

                    // FRONT_FACE (z + 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, 0, 0, 1)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.FRONT_FACE, blockId);
                    }

                    // BACK_FACE (z - 1)
                    if (isFaceVisible(world, chunkX + x, chunkY + y, chunkZ + z, 0, 0, -1)) {
                        addFace(positions, texCoords, indices, x, y, z, Quad.FaceType.BACK_FACE, blockId);
                    }
                }
            }
        }

        Mesh mesh = new Mesh(
                listToFloatArray(positions),
                listToFloatArray(texCoords),
                listToIntArray(indices)
        );

        return new Model(mesh);
    }
    
    // Helper method to check if a face is visible by looking at the neighboring block
    private static boolean isFaceVisible(World world, int worldX, int worldY, int worldZ, int dx, int dy, int dz) {
        // Get the neighboring block's world coordinates
        int neighborX = worldX + dx;
        int neighborY = worldY + dy;
        int neighborZ = worldZ + dz;
        
        // Use the World's getBlockAt method to check the neighbor
        Block neighborBlock = world.getBlockAt(neighborX, neighborY, neighborZ);
        
        // The face is visible if the neighbor is null (outside world) or air (id 0)
        return neighborBlock == null || BlockRegistry.getId(neighborBlock.getName()) == 0;
    }

    private static void addFace(
            List<Float> positions, List<Float> texCoords, List<Integer> indices,
            int x, int y, int z,
            Quad.FaceType faceType, int blockId) {

        Quad quad = Quad.getQuad(faceType);
        Block block = BlockRegistry.getBlock(blockId);
        if (block == null) return;

        // ... (The rest of this method remains the same) ...
        float[] atlasRect = AtlasBuilder.getDefault().getUV(block.getName()); // [uMin,vMin,uMax,vMax]
        float[] facePixels;
        switch (faceType) {
            case TOP_FACE:
                facePixels = block.model.elements.get(0).faces.get("up").uv;
                break;
            case BOTTOM_FACE:
                facePixels = block.model.elements.get(0).faces.get("down").uv;
                break;
            case FRONT_FACE:
                facePixels = block.model.elements.get(0).faces.get("north").uv;
                break;
            case BACK_FACE:
                facePixels = block.model.elements.get(0).faces.get("south").uv;
                break;
            case LEFT_FACE:
                facePixels = block.model.elements.get(0).faces.get("west").uv;
                break;
            case RIGHT_FACE:
                facePixels = block.model.elements.get(0).faces.get("east").uv;
                break;
            default:
                facePixels = new float[]{0, 0, 16, 16};
        }

        float u0b = facePixels[0] / 16f;
        float v0b = facePixels[1] / 16f;
        float u1b = facePixels[2] / 16f;
        float v1b = facePixels[3] / 16f;

        float uMinAtlas = atlasRect[0];
        float vMinAtlas = atlasRect[1];
        float uMaxAtlas = atlasRect[2];
        float vMaxAtlas = atlasRect[3];

        int baseIndex = positions.size() / 3;
        for (int i = 0; i < quad.positions.length; i += 3) {
            positions.add(quad.positions[i] + x);
            positions.add(quad.positions[i + 1] + y);
            positions.add(quad.positions[i + 2] + z);
        }

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

        for (int i = 0; i < quad.indices.length; i++) {
            indices.add(quad.indices[i] + baseIndex);
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
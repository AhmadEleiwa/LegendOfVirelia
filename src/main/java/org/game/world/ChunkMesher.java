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
        return buildModel(world, chunk, 1.0f);
    }

    public static Model buildModel(World world, Chunk chunk, float dayNightCycle) {
        List<Float> vertices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldY = 0;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int y = 0; y < Chunk.SIZE_Y; y++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    int blockId = chunk.getBlock(x, y, z);
                    if (blockId == 0) continue;

                    int wx = chunkWorldX + x;
                    int wy = chunkWorldY + y;
                    int wz = chunkWorldZ + z;

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz,  1,  0,  0))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.RIGHT_FACE,  blockId, dayNightCycle);

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz, -1,  0,  0))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.LEFT_FACE,   blockId, dayNightCycle);

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz,  0,  1,  0))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.TOP_FACE,    blockId, dayNightCycle);

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz,  0, -1,  0))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.BOTTOM_FACE, blockId, dayNightCycle);

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz,  0,  0,  1))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.FRONT_FACE,  blockId, dayNightCycle);

                    if (isFaceVisible(world, chunk, x, y, z, wx, wy, wz,  0,  0, -1))
                        addFaceWithLighting(vertices, texCoords, indices, chunk, x, y, z, Quad.FaceType.BACK_FACE,   blockId, dayNightCycle);
                }
            }
        }

        Mesh mesh = createMeshWithLighting(vertices, texCoords, indices);
        return new Model(mesh);
    }

    /**
     * Decides whether the face of a solid block toward (dx,dy,dz) should be drawn.
     *
     * The old version called world.getBlockAt() for every neighbor and treated a
     * null return (unloaded chunk OR out-of-bounds Y) as "transparent" — so it drew
     * faces on every chunk border and on the top/bottom of the world, producing the
     * visible chunk-border walls and underground ceiling/floor geometry.
     *
     * Correct logic:
     *  - Neighbor Y out of world bounds  → CULL (no face needed at world edge)
     *  - Neighbor is inside THIS chunk    → use local block data (fast, no map lookup)
     *  - Neighbor is in an adjacent chunk → look up that chunk directly
     *      • chunk not loaded             → CULL (it will draw its own face when it loads)
     *      • chunk loaded, block opaque   → CULL
     *      • chunk loaded, block transparent (air/water/glass) → DRAW
     */
    private static boolean isFaceVisible(World world, Chunk chunk,
                                         int lx, int ly, int lz,   // local coords in chunk
                                         int wx, int wy, int wz,   // world coords of block
                                         int dx, int dy, int dz) { // face direction

        int ny = wy + dy;

        // 1. World vertical bounds — never draw faces above/below the world
        if (ny < 0 || ny >= Chunk.SIZE_Y) return false;

        int nx = lx + dx;
        int nz = lz + dz;

        // 2. Neighbor is inside the same chunk — use local data, no map lookup needed
        if (nx >= 0 && nx < Chunk.SIZE_X && nz >= 0 && nz < Chunk.SIZE_Z) {
            return chunk.isTransparent(nx, ny, nz);
        }

        // 3. Neighbor is in an adjacent chunk — resolve which one
        int neighborChunkX = chunk.getChunkX() + (nx < 0 ? -1 : nx >= Chunk.SIZE_X ? 1 : 0);
        int neighborChunkZ = chunk.getChunkZ() + (nz < 0 ? -1 : nz >= Chunk.SIZE_Z ? 1 : 0);

        Chunk neighborChunk = world.getChunk(neighborChunkX, neighborChunkZ);

        // 4. Neighbor chunk not loaded → CULL.
        //    The neighbor chunk will include this border face in its own mesh when it
        //    loads, so culling here avoids the double-drawn chunk-border wall.
        if (neighborChunk == null) return false;

        // 5. Resolve local coords within the neighbor chunk
        int localNX = ((nx % Chunk.SIZE_X) + Chunk.SIZE_X) % Chunk.SIZE_X;
        int localNZ = ((nz % Chunk.SIZE_Z) + Chunk.SIZE_Z) % Chunk.SIZE_Z;

        return neighborChunk.isTransparent(localNX, ny, localNZ);
    }

    // -------------------------------------------------------------------------
    // Everything below is unchanged from your original ChunkMesher
    // -------------------------------------------------------------------------

    private static void addFaceWithLighting(
            List<Float> vertices, List<Float> texCoords, List<Integer> indices,
            Chunk chunk, int x, int y, int z,
            Quad.FaceType faceType, int blockId, float dayNightCycle) {

        Quad quad = Quad.getQuad(faceType);
        Block block = BlockRegistry.getBlock(blockId);
        if (block == null) return;

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

        int baseIndex = vertices.size() / 4;

        for (int i = 0; i < quad.positions.length; i += 3) {
            float vx = quad.positions[i]     + x;
            float vy = quad.positions[i + 1] + y;
            float vz = quad.positions[i + 2] + z;

            float lightLevel = getVertexLightLevel(chunk, x, y, z, faceType, dayNightCycle);

            vertices.add(vx);
            vertices.add(vy);
            vertices.add(vz);
            vertices.add(lightLevel);
        }

        for (int i = 0; i < quad.texCoords.length; i += 2) {
            float u = quad.texCoords[i];
            float v = quad.texCoords[i + 1];

            float uBlock  = u0b + u * (u1b - u0b);
            float vBlock  = v0b + v * (v1b - v0b);
            float atlasU  = uMinAtlas + uBlock * (uMaxAtlas - uMinAtlas);
            float atlasV  = vMinAtlas + vBlock * (vMaxAtlas - vMinAtlas);

            texCoords.add(atlasU);
            texCoords.add(atlasV);
        }

        for (int i = 0; i < quad.indices.length; i++) {
            indices.add(quad.indices[i] + baseIndex);
        }
    }

    private static float getVertexLightLevel(Chunk chunk, int blockX, int blockY, int blockZ,
                                             Quad.FaceType faceType, float dayNightCycle) {
        if (lightingSystem == null) return 1.0f;

        int[] offset = getFaceOffset(faceType);
        int sampleX = blockX + offset[0];
        int sampleY = blockY + offset[1];
        int sampleZ = blockZ + offset[2];

        byte sunlight   = lightingSystem.getSunlightWithNeighbors(chunk, sampleX, sampleY, sampleZ);
        byte blocklight = lightingSystem.getBlocklightWithNeighbors(chunk, sampleX, sampleY, sampleZ);

        float effectiveSunLight = sunlight * dayNightCycle;
        float maxLight = Math.max(effectiveSunLight, blocklight);
        maxLight = Math.max(maxLight, 2.0f);

        return maxLight / 15.0f;
    }

    private static int[] getFaceOffset(Quad.FaceType faceType) {
        switch (faceType) {
            case TOP_FACE:    return new int[]{ 0,  1,  0};
            case BOTTOM_FACE: return new int[]{ 0, -1,  0};
            case RIGHT_FACE:  return new int[]{ 1,  0,  0};
            case LEFT_FACE:   return new int[]{-1,  0,  0};
            case FRONT_FACE:  return new int[]{ 0,  0,  1};
            case BACK_FACE:   return new int[]{ 0,  0, -1};
            default:          return new int[]{ 0,  0,  0};
        }
    }

    private static Mesh createMeshWithLighting(List<Float> vertices, List<Float> texCoords, List<Integer> indices) {
        float[] positions = new float[(vertices.size() / 4) * 3];
        float[] lighting  = new float[vertices.size() / 4];

        for (int i = 0; i < vertices.size() / 4; i++) {
            positions[i * 3]     = vertices.get(i * 4);
            positions[i * 3 + 1] = vertices.get(i * 4 + 1);
            positions[i * 3 + 2] = vertices.get(i * 4 + 2);
            lighting[i]          = vertices.get(i * 4 + 3);
        }

        float[] texCoordArray = listToFloatArray(texCoords);
        int[]   indexArray    = listToIntArray(indices);

        return new LightedMesh(positions, texCoordArray, lighting, indexArray);
    }

    private static float[] getFacePixels(Block block, Quad.FaceType faceType) {
        switch (faceType) {
            case TOP_FACE:    return block.model.elements.get(0).faces.get("up").uv;
            case BOTTOM_FACE: return block.model.elements.get(0).faces.get("down").uv;
            case FRONT_FACE:  return block.model.elements.get(0).faces.get("north").uv;
            case BACK_FACE:   return block.model.elements.get(0).faces.get("south").uv;
            case LEFT_FACE:   return block.model.elements.get(0).faces.get("west").uv;
            case RIGHT_FACE:  return block.model.elements.get(0).faces.get("east").uv;
            default:          return new float[]{0, 0, 16, 16};
        }
    }

    private static float[] listToFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] listToIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
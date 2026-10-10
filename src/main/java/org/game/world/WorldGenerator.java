package org.game.world;

public class WorldGenerator {
    private static final int BEDROCK_LEVEL = 0;
    private static final int RIVER_BED_LEVEL = 18;
    private static final int LAKE_BED_LEVEL = 17;
    private static final int SURFACE_BUFFER = 6;

    private final FastNoiseLite terrainNoise;
    private final FastNoiseLite biomeNoise;
    private final FastNoiseLite ridgeNoise;
    private final FastNoiseLite detailNoise;
    private final FastNoiseLite riverNoise;
    private final FastNoiseLite riverWarpX;
    private final FastNoiseLite riverWarpZ;
    private final FastNoiseLite lakeNoise;
    private final FastNoiseLite caveNoise;
    private final FastNoiseLite tunnelNoise;
    private final FastNoiseLite caveEntranceNoise;

    public WorldGenerator(int seed) {
        terrainNoise = createFractalNoise(seed, 3, 0.52f);
        biomeNoise = createNoise(seed + 1234);
        ridgeNoise = createFractalNoise(seed + 2468, 3, 0.55f);
        detailNoise = createFractalNoise(seed + 3141, 2, 0.5f);
        riverNoise = createNoise(seed + 3579);
        riverWarpX = createFractalNoise(seed + 4680, 2, 0.5f);
        riverWarpZ = createFractalNoise(seed + 5791, 2, 0.5f);
        lakeNoise = createFractalNoise(seed + 6802, 2, 0.5f);
        caveNoise = createFractalNoise(seed + 7913, 3, 0.5f);
        tunnelNoise = createFractalNoise(seed + 8024, 2, 0.5f);
        caveEntranceNoise = createFractalNoise(seed + 9135, 2, 0.5f);
    }

    private static FastNoiseLite createNoise(int seed) {
        FastNoiseLite noise = new FastNoiseLite(seed);
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        return noise;
    }

    private static FastNoiseLite createFractalNoise(int seed, int octaves, float gain) {
        FastNoiseLite noise = createNoise(seed);
        noise.SetFractalType(FastNoiseLite.FractalType.FBm);
        noise.SetFractalOctaves(octaves);
        noise.SetFractalLacunarity(2.0f);
        noise.SetFractalGain(gain);
        return noise;
    }

    public BiomeConfig getBiomeAt(int worldX, int worldZ) {
        float value = biomeNoise.GetNoise(worldX * 0.004f, worldZ * 0.004f);
        if (value < -0.22f) {
            return BiomeConfig.DESERT;
        }
        if (value > 0.22f) {
            return BiomeConfig.MOUNTAINS;
        }
        return BiomeConfig.PLAINS;
    }

    public void generateChunk(Chunk chunk) {
        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;
                BiomeConfig biome = getBiomeAt(worldX, worldZ);
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = BEDROCK_LEVEL; y <= surfaceY; y++) {
                    if (y > BEDROCK_LEVEL + 1 && isCave(worldX, y, worldZ, surfaceY))
                        continue;

                    int blockId;
                    if (y == BEDROCK_LEVEL) {
                        blockId = BlockRegistry.getId("dirt2");
                    } else if (y == surfaceY) {
                        blockId = biome.surfaceBlock;
                    } else if (y >= surfaceY - SURFACE_BUFFER) {
                        blockId = biome.fillerBlock;
                    } else {
                        blockId = BlockRegistry.getId("dirt2");
                    }
                    chunk.setBlock(x, y, z, blockId);
                }
            }
        }
    }

    private int getSurfaceHeight(int worldX, int worldZ) {
        float biomeValue = biomeNoise.GetNoise(worldX * 0.004f, worldZ * 0.004f);
        float warpX = riverWarpX.GetNoise(worldX * 0.008f, worldZ * 0.008f) * 20.0f;
        float warpZ = riverWarpZ.GetNoise(worldX * 0.008f, worldZ * 0.008f) * 20.0f;
        float shapedX = worldX + warpX;
        float shapedZ = worldZ + warpZ;
        float broadShape = terrainNoise.GetNoise(shapedX * 0.006f, shapedZ * 0.006f);
        float hills = terrainNoise.GetNoise(shapedX * 0.025f, shapedZ * 0.025f);
        float ridges = 1.0f - Math.abs(ridgeNoise.GetNoise(shapedX * 0.009f, shapedZ * 0.009f));

        float mountainWeight = smoothStep(0.0f, 0.7f, biomeValue);
        float desertWeight = smoothStep(0.0f, 0.7f, -biomeValue);
        float plainsWeight = 1.0f - Math.max(mountainWeight, desertWeight);
        float detail = detailNoise.GetNoise(shapedX * 0.075f, shapedZ * 0.075f);
        float height = 31.0f
                + broadShape * 11.0f
                + hills * (7.0f + plainsWeight * 3.0f)
                + mountainWeight * (10.0f + ridges * 24.0f)
                - desertWeight * 3.0f
                + detail * (1.5f + mountainWeight * 2.0f);

        float riverDistance = getRiverDistance(worldX, worldZ, warpX, warpZ);
        float riverFactor = 1.0f - smoothStep(0.018f, 0.082f, riverDistance);
        float riverValleyFactor = 1.0f - smoothStep(0.04f, 0.22f, riverDistance);
        float riverBed = RIVER_BED_LEVEL + hills * 1.5f;
        height = lowerTerrain(height, riverBed, riverFactor);
        height = lowerTerrain(height, riverBed + 5.0f + hills * 2.0f, riverValleyFactor * 0.7f);

        float lakeValue = lakeNoise.GetNoise(worldX * 0.008f, worldZ * 0.008f);
        float lakeFactor = smoothStep(0.55f, 0.78f, lakeValue);
        float lakeBed = LAKE_BED_LEVEL + hills * 2.5f + detail;
        height = lowerTerrain(height, lakeBed, lakeFactor);

        return Math.max(BEDROCK_LEVEL + 4,
                Math.min(Chunk.SIZE_Y - 8, Math.round(height)));
    }

    private float getRiverDistance(int worldX, int worldZ) {
        float warpX = riverWarpX.GetNoise(worldX * 0.008f, worldZ * 0.008f) * 20.0f;
        float warpZ = riverWarpZ.GetNoise(worldX * 0.008f, worldZ * 0.008f) * 20.0f;
        return getRiverDistance(worldX, worldZ, warpX, warpZ);
    }

    private float getRiverDistance(int worldX, int worldZ, float warpX, float warpZ) {
        return Math.abs(riverNoise.GetNoise(
                (worldX + warpX) * 0.006f,
                (worldZ + warpZ) * 0.006f));
    }

    private boolean isCave(int worldX, int y, int worldZ, int surfaceY) {
        float depthBelowSurface = surfaceY - y;
        float chamber = caveNoise.GetNoise(worldX * 0.045f, y * 0.055f, worldZ * 0.045f);
        float tunnel = tunnelNoise.GetNoise(worldX * 0.028f, y * 0.045f, worldZ * 0.028f);
        float entrance = caveEntranceNoise.GetNoise(worldX * 0.055f, worldZ * 0.055f);

        float entranceThreshold = 0.70f + depthBelowSurface * 0.003f;
        boolean caveMouth = depthBelowSurface <= 28.0f && entrance > entranceThreshold;
        boolean chamberCave = chamber > 0.56f && depthBelowSurface >= 7.0f;
        boolean connectingTunnel = Math.abs(tunnel) < 0.055f && depthBelowSurface >= 10.0f;
        return caveMouth || chamberCave || connectingTunnel;
    }

    private static float lowerTerrain(float terrainHeight, float basinHeight, float factor) {
        float targetHeight = terrainHeight + (basinHeight - terrainHeight) * factor;
        return Math.min(terrainHeight, targetHeight);
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float t = Math.max(0.0f, Math.min(1.0f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }
}

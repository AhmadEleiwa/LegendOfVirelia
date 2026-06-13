package org.game.world;

public class WorldGenerator {
    private final int seed;
    
    // Noise generators
    private final FastNoiseLite terrainNoise;
    private final FastNoiseLite biomeNoise;
    private final FastNoiseLite caveNoise; // NEW: Added back for cave generation

    private static final int SEA_LEVEL = 20;

    public WorldGenerator(int seed) {
        this.seed = seed;

        // Base terrain noise setup
        this.terrainNoise = new FastNoiseLite(seed);
        this.terrainNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.terrainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.terrainNoise.SetFractalOctaves(4);
        this.terrainNoise.SetFractalLacunarity(2.0f);
        this.terrainNoise.SetFractalGain(0.5f);

        // Biome selector noise setup
        this.biomeNoise = new FastNoiseLite(seed + 1234);
        this.biomeNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);

        // NEW: Simple 3D cave noise setup
        this.caveNoise = new FastNoiseLite(seed + 5678); // Shifted seed so caves match up globally
        this.caveNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.caveNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.caveNoise.SetFractalOctaves(2); // 2 octaves keep the caves smooth and fast to generate
    }

    public BiomeConfig getBiomeAt(int worldX, int worldZ) {
        float noiseVal = biomeNoise.GetNoise(worldX * 0.015625f, worldZ * 0.015625f);

        if (noiseVal < -0.05f) {
            return BiomeConfig.DESERT;
        } else if (noiseVal > 0.05f) {
            return BiomeConfig.MOUNTAINS;
        } else {
            return BiomeConfig.PLAINS;
        }
    }

    public void generateChunk(Chunk chunk) {
        int chunkWorldX = chunk.getChunkX() * Chunk.SIZE_X;
        int chunkWorldZ = chunk.getChunkZ() * Chunk.SIZE_Z;
        int blendRadius = 2; 

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int worldX = chunkWorldX + x;
                int worldZ = chunkWorldZ + z;

                // 1. Determine Biome & Smooth Blending
                BiomeConfig primaryBiome = getBiomeAt(worldX, worldZ);
                float blendedBaseHeight = 0;
                float blendedVariation = 0;
                float totalSampleCount = 0;

                for (int bx = -blendRadius; bx <= blendRadius; bx++) {
                    for (int bz = -blendRadius; bz <= blendRadius; bz++) {
                        BiomeConfig neighbor = getBiomeAt(worldX + bx, worldZ + bz);
                        blendedBaseHeight += neighbor.baseHeight;
                        blendedVariation += neighbor.heightVariation;
                        totalSampleCount++;
                    }
                }

                float finalBaseHeight = blendedBaseHeight / totalSampleCount;
                float finalVariation = blendedVariation / totalSampleCount;

                // 2. Get underlying structural height noise
                float rawHeightNoise = (terrainNoise.GetNoise(worldX * 0.2f, worldZ * 0.2f) + 1.0f) / 2.0f;
                float curvedNoise = (float) Math.pow(rawHeightNoise, 2.0); 

                // 3. Calculate final surface height
                int surfaceY = (int) (finalBaseHeight + (curvedNoise * finalVariation));
                surfaceY = Math.max(1, Math.min(surfaceY, Chunk.SIZE_Y - 1));

                // 4. Fill Column along the Y axis
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    if (y == 0) {
                        // Bedrock Floor
                        chunk.setBlock(x, y, z, BlockRegistry.getId("dirt"));
                    } else if (y < surfaceY - 4) {
                        // DEEP UNDERGROUND CORE: Carve caves here
                        
                        // We sample noise using X, Y, and Z. This creates 3D worm-like tunnels.
                        float caveSample = caveNoise.GetNoise(worldX * 0.5f, y * 0.9f, worldZ * 0.5f);
                        
                        // If the noise value is high (above 0.55), we turn the dirt into air!
                        if (caveSample > 0.44f) {
                            chunk.setBlock(x, y, z, BlockRegistry.getId("air")); 
                        } else {
                            chunk.setBlock(x, y, z, BlockRegistry.getId("dirt"));
                        }

                    } else if (y < surfaceY) {
                        // Sub-surface filler layer (Safe zone, no caves puncture here)
                        chunk.setBlock(x, y, z, BlockRegistry.getId("dirt"));
                    } else if (y == surfaceY) {
                        // Topmost layer
                        if (surfaceY < SEA_LEVEL + 1 && primaryBiome == BiomeConfig.PLAINS) {
                            chunk.setBlock(x, y, z, BiomeConfig.DESERT.surfaceBlock);
                        } else {
                            chunk.setBlock(x, y, z, primaryBiome.surfaceBlock);
                        }
                    } else {
                        // Above surface terrain
                        if (y <= SEA_LEVEL) {
                            chunk.setBlock(x, y, z, BlockRegistry.getId("water"));
                        } else {
                            chunk.setBlock(x, y, z, BlockRegistry.getId("air"));
                        }
                    }
                }
            }
        }
    }
}
package org.game.world;

public enum BiomeConfig {
    PLAINS(BlockRegistry.getId("dirt"), BlockRegistry.getId("dirt"), 20, 12),    // Min Y: 20, Max Y: 32 (Safe)
    DESERT(BlockRegistry.getId("dirt2"), BlockRegistry.getId("dirt2"), 20, 15),  // Min Y: 20, Max Y: 35 (Safe)
    
    // FIXED: Lowered baseHeight to 30 so peaks peak beautifully at Y = 55 (safely below 64)
    MOUNTAINS(BlockRegistry.getId("dirt2"), BlockRegistry.getId("dirt2"), 30, 25); 

    public final int surfaceBlock;
    public final int fillerBlock;
    public final int baseHeight;
    public final int heightVariation;

    BiomeConfig(int surfaceBlock, int fillerBlock, int baseHeight, int heightVariation) {
        this.surfaceBlock = surfaceBlock;
        this.fillerBlock = fillerBlock;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
    }
}
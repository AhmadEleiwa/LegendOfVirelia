package org.game.world;

public enum BiomeConfig {
    PLAINS(BlockRegistry.getId("dirt"), BlockRegistry.getId("dirt")),
    DESERT(BlockRegistry.getId("dirt2"), BlockRegistry.getId("dirt2")),
    MOUNTAINS(BlockRegistry.getId("dirt2"), BlockRegistry.getId("dirt"));

    public final int surfaceBlock;
    public final int fillerBlock;

    BiomeConfig(int surfaceBlock, int fillerBlock) {
        this.surfaceBlock = surfaceBlock;
        this.fillerBlock = fillerBlock;
    }
}
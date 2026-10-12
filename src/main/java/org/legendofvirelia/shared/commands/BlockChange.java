package org.legendofvirelia.shared.commands;

public final class BlockChange {
    public final int x;
    public final int y;
    public final int z;
    public final int blockId;
    public final int fluidLevel;
    public final boolean fluidSource;

    public BlockChange(int x, int y, int z, int blockId) {
        this(x, y, z, blockId, 0, false);
    }

    public BlockChange(int x, int y, int z, int blockId, int fluidLevel, boolean fluidSource) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
        this.fluidLevel = fluidLevel;
        this.fluidSource = fluidSource;
    }
}

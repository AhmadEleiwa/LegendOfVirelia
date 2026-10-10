package org.legendofvirelia.shared.commands;

public final class BlockChange {
    public final int x;
    public final int y;
    public final int z;
    public final int blockId;

    public BlockChange(int x, int y, int z, int blockId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockId = blockId;
    }
}

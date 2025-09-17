package org.game.utils;

public enum Direction {
    NORTH(0, 0, 1),
    SOUTH(0, 0, -1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;

    Direction(int x, int y, int z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }
}

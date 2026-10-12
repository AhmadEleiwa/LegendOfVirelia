package org.game.world;

import org.game.entities.Camera;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class BlockPlacer {
    private final World world;

    public BlockPlacer(World world) {
        this.world = world;
    }

    /**
     * Cast a ray from camera position in camera direction to find block
     * intersection
     */
    public static RaycastResult raycast(World world, Camera camera, float maxReach) {
        Vector3f rayStart = new Vector3f(camera.getPosition());
        return raycastWorld(world, rayStart, camera.getForward(), maxReach);
    }

    public static RaycastResult raycastForPlacement(World world, Camera camera, float maxReach) {
        return raycast(world, camera, maxReach);
    }

    /**
     * Raycast through the world to find block intersections
     */
    private static RaycastResult raycastWorld(World world, Vector3f start, Vector3f direction, float maxDistance) {
        Vector3f ray = new Vector3f(direction);
        if (ray.lengthSquared() == 0.0f || maxDistance < 0.0f)
            return new RaycastResult(false, null, null, maxDistance);
        ray.normalize();

        int blockX = (int) Math.floor(start.x);
        int blockY = (int) Math.floor(start.y);
        int blockZ = (int) Math.floor(start.z);
        int stepX = directionStep(ray.x);
        int stepY = directionStep(ray.y);
        int stepZ = directionStep(ray.z);
        float deltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.x);
        float deltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.y);
        float deltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.z);
        float nextX = initialBoundaryDistance(start.x, blockX, ray.x, stepX);
        float nextY = initialBoundaryDistance(start.y, blockY, ray.y, stepY);
        float nextZ = initialBoundaryDistance(start.z, blockZ, ray.z, stepZ);
        Vector3i previousEmpty = null;
        float distance = 0.0f;

        while (distance <= maxDistance) {
            int blockId = world.getBlockIdAt(blockX, blockY, blockZ);
            Block block = BlockRegistry.getBlock(blockId); // Ensure the block is registered
            
            if (blockId != 0 && !(block instanceof FluidBlock)) { // Ignore air and fluid blocks
                return new RaycastResult(true, new Vector3i(blockX, blockY, blockZ),
                        previousEmpty, distance);
            }

            previousEmpty = new Vector3i(blockX, blockY, blockZ);
            if (nextX <= nextY && nextX <= nextZ) {
                distance = nextX;
                nextX += deltaX;
                blockX += stepX;
            } else if (nextY <= nextZ) {
                distance = nextY;
                nextY += deltaY;
                blockY += stepY;
            } else {
                distance = nextZ;
                nextZ += deltaZ;
                blockZ += stepZ;
            }
        }

        return new RaycastResult(false, null, null, maxDistance);
    }

    private static float initialBoundaryDistance(float origin, int block, float direction, int step) {
        if (step == 0)
            return Float.POSITIVE_INFINITY;
        float boundary = step > 0 ? block + 1.0f : block;
        return (boundary - origin) / direction;
    }

    private static int directionStep(float direction) {
        return direction > 0.0f ? 1 : direction < 0.0f ? -1 : 0;
    }

    /**
     * Place a block at the target position
     */
    public Chunk placeBlock(Vector3i position, int blockId) {
        return world.setBlockAt(position.x, position.y, position.z, blockId);
    }

    /**
     * Remove/break a block at the target position
     */
    public Chunk breakBlock(Vector3i position) {
        return world.setBlockAt(position.x, position.y, position.z, 0); // 0 = air
    }

    /**
     * Result of a raycast operation
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Vector3i blockPosition; // Position of the block that was hit
        public final Vector3i placePosition; // Position where a new block can be placed
        public final float distance;

        public RaycastResult(boolean hit, Vector3i blockPosition, Vector3i placePosition, float distance) {
            this.hit = hit;
            this.blockPosition = blockPosition;
            this.placePosition = placePosition;
            this.distance = distance;
        }
    }
}
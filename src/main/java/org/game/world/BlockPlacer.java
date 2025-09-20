package org.game.world;

import org.game.entities.Camera;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class BlockPlacer {
    private World world;
    private float maxReach = 5.0f; // Maximum reach distance
    public BlockPlacer(World world) {
        this.world = world;
    }

    /**
     * Cast a ray from camera position in camera direction to find block
     * intersection
     */
    public static RaycastResult raycast(World world ,Camera camera, float maxReach) {
        Vector3f rayStart = new Vector3f(camera.getPosition());
        Vector3f rayDir = camera.getForward(); // You'll need to add this method to Camera

        return raycastWorld(world, rayStart, rayDir, maxReach);
    }

    /**
     * Raycast through the world to find block intersections
     */
    private static RaycastResult raycastWorld(World world, Vector3f start, Vector3f direction, float maxDistance) {
        // DDA algorithm for voxel raycasting
        Vector3f current = new Vector3f(start);
        Vector3f step = new Vector3f(direction).normalize().mul(0.1f); // Step size

        Vector3i lastEmptyPos = new Vector3i();

        for (float distance = 0; distance < maxDistance; distance += 0.1f) {
            // Current voxel position
            int blockX = (int) Math.floor(current.x);
            int blockY = (int) Math.floor(current.y);
            int blockZ = (int) Math.floor(current.z);

            // Get the block at this position
            Block hitBlock = world.getBlockAt(blockX, blockY, blockZ);
           
            if (hitBlock != null && BlockRegistry.getId(hitBlock.getName()) != 0) {
                // Hit a solid block
                 System.out.println(BlockRegistry.getId(hitBlock.getName())); System.out.println(BlockRegistry.getId(hitBlock.getName()));
                return new RaycastResult(true, new Vector3i(blockX, blockY, blockZ),
                        new Vector3i(lastEmptyPos), distance);
            }

            // Remember this empty position for block placement
            lastEmptyPos.set(blockX, blockY, blockZ);

            // Step forward
            current.add(step);
        }

        return new RaycastResult(false, null, null, maxDistance);
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

    public float getMaxReach() {
        return maxReach;
    }

    public void setMaxReach(float maxReach) {
        this.maxReach = maxReach;
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
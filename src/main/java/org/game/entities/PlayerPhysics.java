package org.game.entities;

import org.engine.io.Input;
import org.game.world.Block;
import org.game.world.BlockRegistry;
import org.game.world.Chunk;
import org.game.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

public final class PlayerPhysics {
    private static final float PLAYER_RADIUS = 0.3f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.62f;
    private static final float CROUCH_HEIGHT = 1.2f;
    private static final float CROUCH_EYE_HEIGHT = 1.05f;
    private static final float MOVE_SPEED = 14.2f;
    private static final float SPRINT_MULTIPLIER = 1.5f;
    private static final float CROUCH_MULTIPLIER = 0.45f;
    private static final float MOVE_ACCELERATION = 22.0f;
    private static final float MOVE_DECELERATION = 28.0f;
    private static final float GRAVITY = 19.0f;
    private static final float JUMP_SPEED = 8.2f;
    private static final float TERMINAL_SPEED = 50.0f;
    private static final float COLLISION_EPSILON = 0.0001f;

    private final World world;
    private final Vector3f horizontalVelocity = new Vector3f();
    private float verticalVelocity;
    private boolean grounded;
    private float currentEyeHeight = EYE_HEIGHT;

    public PlayerPhysics(World world) {
        this.world = world;
    }

    public void update(Camera camera, float delta) {
        int chunkX = Math.floorDiv((int) Math.floor(camera.position.x), Chunk.SIZE_X);
        int chunkZ = Math.floorDiv((int) Math.floor(camera.position.z), Chunk.SIZE_Z);
        if (world.getChunk(chunkX, chunkZ) == null) {
            horizontalVelocity.zero();
            verticalVelocity = 0.0f;
            grounded = false;
            return;
        }

        float step = Math.min(Math.max(delta, 0.0f), 0.1f);
        if (step == 0.0f)
            return;

        if (intersectsSolid(camera.position))
            moveOutOfBlocks(camera.position);

        boolean crouching = Input.isKeyDown(Input.KEY_LEFT_SHIFT);
        float targetEyeHeight = crouching ? CROUCH_EYE_HEIGHT : EYE_HEIGHT;
        float targetPlayerHeight = crouching ? CROUCH_HEIGHT : PLAYER_HEIGHT;
        float eyeHeightChange = targetEyeHeight - currentEyeHeight;
        if (eyeHeightChange != 0.0f) {
            Vector3f adjustedPosition = new Vector3f(camera.position);
            adjustedPosition.y += eyeHeightChange;
            if (!intersectsSolid(adjustedPosition, targetEyeHeight, targetPlayerHeight)) {
                camera.position.set(adjustedPosition);
                currentEyeHeight = targetEyeHeight;
            }
        }
        float playerHeight = currentEyeHeight == CROUCH_EYE_HEIGHT
                ? CROUCH_HEIGHT : PLAYER_HEIGHT;

        Vector3f forward = camera.getForward();
        forward.y = 0.0f;
        if (forward.lengthSquared() > 0.0f)
            forward.normalize();
        Vector3f right = camera.getRight();
        right.y = 0.0f;
        if (right.lengthSquared() > 0.0f)
            right.normalize();

        float moveX = 0.0f;
        float moveZ = 0.0f;
        if (Input.isKeyDown(Input.KEY_W)) {
            moveX += forward.x;
            moveZ += forward.z;
        }
        if (Input.isKeyDown(Input.KEY_S)) {
            moveX -= forward.x;
            moveZ -= forward.z;
        }
        if (Input.isKeyDown(Input.KEY_D)) {
            moveX += right.x;
            moveZ += right.z;
        }
        if (Input.isKeyDown(Input.KEY_A)) {
            moveX -= right.x;
            moveZ -= right.z;
        }
        float moveLength = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveLength > 1.0f) {
            moveX /= moveLength;
            moveZ /= moveLength;
        }
        float speedMultiplier = crouching ? CROUCH_MULTIPLIER
                : Input.isKeyDown(Input.KEY_LEFT_CONTROL) ? SPRINT_MULTIPLIER : 1.0f;
        float targetVelocityX = moveX * MOVE_SPEED * speedMultiplier;
        float targetVelocityZ = moveZ * MOVE_SPEED * speedMultiplier;
        float acceleration = moveLength > 0.0f ? MOVE_ACCELERATION : MOVE_DECELERATION;
        approachVelocity(targetVelocityX, targetVelocityZ, acceleration * step);
        if (moveAxis(camera.position, 0, horizontalVelocity.x * step))
            horizontalVelocity.x = 0.0f;
        if (moveAxis(camera.position, 2, horizontalVelocity.z * step))
            horizontalVelocity.z = 0.0f;

        if (Input.isKeyDown(Input.KEY_SPACE) && grounded) {
            verticalVelocity = JUMP_SPEED;
            grounded = false;
        }
        verticalVelocity = Math.max(-TERMINAL_SPEED, verticalVelocity - GRAVITY * step);
        grounded = false;
        if (moveAxis(camera.position, 1, verticalVelocity * step,
                currentEyeHeight, playerHeight)) {
            grounded = verticalVelocity < 0.0f;
            verticalVelocity = 0.0f;
        }
    }

    public void reset() {
        horizontalVelocity.zero();
        verticalVelocity = 0.0f;
        grounded = false;
        currentEyeHeight = EYE_HEIGHT;
    }

    public boolean overlapsPlayer(Camera camera, Vector3i blockPosition) {
        Vector3f eyePosition = camera.position;
        float playerMinX = eyePosition.x - PLAYER_RADIUS;
        float playerMaxX = eyePosition.x + PLAYER_RADIUS;
        float eyeHeight = currentEyeHeight;
        float playerHeight = eyeHeight == CROUCH_EYE_HEIGHT ? CROUCH_HEIGHT : PLAYER_HEIGHT;
        float playerMinY = eyePosition.y - eyeHeight;
        float playerMaxY = playerMinY + playerHeight;
        float playerMinZ = eyePosition.z - PLAYER_RADIUS;
        float playerMaxZ = eyePosition.z + PLAYER_RADIUS;

        return playerMinX < blockPosition.x + 1.0f - COLLISION_EPSILON
                && playerMaxX > blockPosition.x + COLLISION_EPSILON
                && playerMinY < blockPosition.y + 1.0f - COLLISION_EPSILON
                && playerMaxY > blockPosition.y + COLLISION_EPSILON
                && playerMinZ < blockPosition.z + 1.0f - COLLISION_EPSILON
                && playerMaxZ > blockPosition.z + COLLISION_EPSILON;
    }

    private void approachVelocity(float targetX, float targetZ, float maximumChange) {
        float differenceX = targetX - horizontalVelocity.x;
        float differenceZ = targetZ - horizontalVelocity.z;
        float differenceLength = (float) Math.sqrt(differenceX * differenceX
                + differenceZ * differenceZ);
        if (differenceLength <= maximumChange) {
            horizontalVelocity.x = targetX;
            horizontalVelocity.z = targetZ;
            return;
        }
        float scale = maximumChange / differenceLength;
        horizontalVelocity.x += differenceX * scale;
        horizontalVelocity.z += differenceZ * scale;
    }

    private boolean moveAxis(Vector3f position, int axis, float amount) {
        float playerHeight = currentEyeHeight == CROUCH_EYE_HEIGHT
                ? CROUCH_HEIGHT : PLAYER_HEIGHT;
        return moveAxis(position, axis, amount, currentEyeHeight, playerHeight);
    }

    private boolean moveAxis(Vector3f position, int axis, float amount,
            float eyeHeight, float playerHeight) {
        if (amount == 0.0f)
            return false;

        Vector3f candidate = new Vector3f(position);
        setAxis(candidate, axis, getAxis(position, axis) + amount);
        if (!intersectsSolid(candidate, eyeHeight, playerHeight)) {
            position.set(candidate);
            return false;
        }

        float clearDistance = 0.0f;
        float blockedDistance = amount;
        for (int i = 0; i < 16; i++) {
            float distance = (clearDistance + blockedDistance) * 0.5f;
            candidate.set(position);
            setAxis(candidate, axis, getAxis(position, axis) + distance);
            if (intersectsSolid(candidate, eyeHeight, playerHeight))
                blockedDistance = distance;
            else
                clearDistance = distance;
        }
        setAxis(position, axis, getAxis(position, axis) + clearDistance);
        return true;
    }

    private void moveOutOfBlocks(Vector3f position) {
        Vector3f candidate = new Vector3f(position);
        for (int i = 0; i < Chunk.SIZE_Y && intersectsSolid(candidate); i++)
            candidate.y += 0.1f;
        if (!intersectsSolid(candidate))
            position.set(candidate);
    }

    private boolean intersectsSolid(Vector3f eyePosition) {
        float playerHeight = currentEyeHeight == CROUCH_EYE_HEIGHT
                ? CROUCH_HEIGHT : PLAYER_HEIGHT;
        return intersectsSolid(eyePosition, currentEyeHeight, playerHeight);
    }

    private boolean intersectsSolid(Vector3f eyePosition, float eyeHeight, float playerHeight) {
        float minX = eyePosition.x - PLAYER_RADIUS;
        float maxX = eyePosition.x + PLAYER_RADIUS;
        float minY = eyePosition.y - eyeHeight;
        float maxY = minY + playerHeight;
        float minZ = eyePosition.z - PLAYER_RADIUS;
        float maxZ = eyePosition.z + PLAYER_RADIUS;

        int blockMinX = (int) Math.floor(minX + COLLISION_EPSILON);
        int blockMaxX = (int) Math.floor(maxX - COLLISION_EPSILON);
        int blockMinY = (int) Math.floor(minY + COLLISION_EPSILON);
        int blockMaxY = (int) Math.floor(maxY - COLLISION_EPSILON);
        int blockMinZ = (int) Math.floor(minZ + COLLISION_EPSILON);
        int blockMaxZ = (int) Math.floor(maxZ - COLLISION_EPSILON);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (isSolid(x, y, z))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean isSolid(int x, int y, int z) {
        if (y < 0)
            return true;
        if (y >= Chunk.SIZE_Y)
            return false;
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null)
            return true;
        int blockId = chunk.getBlock(Math.floorMod(x, Chunk.SIZE_X), y,
                Math.floorMod(z, Chunk.SIZE_Z));
        Block block = BlockRegistry.getBlock(blockId);
        return blockId != 0 && (block == null || !block.isTransparent());
    }

    private static float getAxis(Vector3f vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private static void setAxis(Vector3f vector, int axis, float value) {
        if (axis == 0)
            vector.x = value;
        else if (axis == 1)
            vector.y = value;
        else
            vector.z = value;
    }
}

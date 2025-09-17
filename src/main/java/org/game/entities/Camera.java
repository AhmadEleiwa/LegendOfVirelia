package org.game.entities;

import org.engine.io.Input;
import org.engine.io.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class Camera {
    public Vector3f position = new Vector3f(2, 2, 2);
    public Vector3f target = new Vector3f(0, 0, 0);
    public Vector3f up = new Vector3f(0, 1, 0);
    public float fov = (float) Math.toRadians(60.0f);
    public float near = 0.1f;
    public float far = 100.0f;
    public float aspect;

    // Mouse movement variables
    private float lastMouseX = 0;
    private float lastMouseY = 0;
    private boolean firstMouse = true;
    private float mouseSensitivity = 0.002f;

    // First-person variables
    private float fpYaw = 0;
    private float fpPitch = 0;

    public Camera(float aspect) {
        this.aspect = aspect;
        // Initialize first-person angles from current position/target
        Vector3f forward = new Vector3f();
        target.sub(position, forward).normalize();
        fpYaw = (float) Math.atan2(forward.z, forward.x);
        fpPitch = (float) Math.asin(-forward.y);
    }

    public Matrix4f getView() {
        return new Matrix4f().lookAt(position, target, up);
    }

    public Matrix4f getProjection() {
        return new Matrix4f().perspective(fov, aspect, near, far);
    }

    public void processInput(Window window) {
        // This method can be used for additional input processing if needed
    }

    public void update(float delta) {
        float cameraSpeed = 5f * delta;

        // Calculate movement vectors
        Vector3f forward = getForward();
        Vector3f right = getRight();
        Vector3f worldUp = new Vector3f(0, 1, 0);

        // WASD movement
        if (Input.isKeyDown(Input.KEY_W)) {
            position.add(forward.mul(cameraSpeed, new Vector3f()));
        }
        if (Input.isKeyDown(Input.KEY_S)) {
            position.sub(forward.mul(cameraSpeed, new Vector3f()));
        }
        if (Input.isKeyDown(Input.KEY_D)) {
            position.add(right.mul(cameraSpeed, new Vector3f()));
        }
        if (Input.isKeyDown(Input.KEY_A)) {
            position.sub(right.mul(cameraSpeed, new Vector3f()));
        }
        if (Input.isKeyDown(Input.KEY_SPACE)) {
            position.add(worldUp.mul(cameraSpeed, new Vector3f()));
        }
        if (Input.isKeyDown(Input.KEY_LEFT_SHIFT)) {
            position.sub(worldUp.mul(cameraSpeed, new Vector3f()));
        }

        // Reset camera position
        if (Input.isKeyPressed(Input.KEY_R)) {
            position.set(2, 2, 2);
            fpYaw = 0;
            fpPitch = 0;
            updateTarget();
            firstMouse = true;
        }

        // Handle mouse movement
        handleMouseMovement();

        // Handle mouse scroll
        handleMouseScroll();

        // Update target based on current view direction
        updateTarget();
    }

    private void handleMouseMovement() {
        if (Input.isMouseMoved()) {
            float currentMouseX = (float) Input.getMouseX();
            float currentMouseY = (float) Input.getMouseY();

            if (firstMouse) {
                lastMouseX = currentMouseX;
                lastMouseY = currentMouseY;
                firstMouse = false;
                return;
            }

            float deltaX = currentMouseX - lastMouseX;
            float deltaY = currentMouseY - lastMouseY;

            lastMouseX = currentMouseX;
            lastMouseY = currentMouseY;

            deltaX *= mouseSensitivity;
            deltaY *= mouseSensitivity;

            // Update angles
            fpYaw += deltaX;
            fpPitch -= deltaY;

            // Clamp pitch to prevent camera flipping
            fpPitch = Math.max(-1.5f, Math.min(1.5f, fpPitch));
        }
    }

    private void handleMouseScroll() {
        // Mouse scroll adjusts FOV
        /*
         * if (Input.isMouseScrolled()) {
         * float scroll = Input.getMouseScrollDelta();
         * fov -= scroll * 0.1f;
         * fov = Math.max(0.1f, Math.min(3.0f, fov));
         * }
         */
    }

    // Helper methods
    public Vector3f getForward() {
        Vector3f forward = new Vector3f();
        forward.x = (float) (Math.cos(fpPitch) * Math.cos(fpYaw));
        forward.y = (float) Math.sin(fpPitch);
        forward.z = (float) (Math.cos(fpPitch) * Math.sin(fpYaw));
        return forward.normalize();
    }

    public Vector3f getRight() {
        Vector3f forward = getForward();
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();
        forward.cross(worldUp, right);
        return right.normalize();
    }

    private void updateTarget() {
        Vector3f forward = getForward();
        target.set(position).add(forward);
    }

    // Setters
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public void setTarget(Vector3f target) {
        this.target.set(target);
        // Update angles based on new target
        Vector3f forward = new Vector3f();
        target.sub(position, forward).normalize();
        fpYaw = (float) Math.atan2(forward.z, forward.x);
        fpPitch = (float) Math.asin(-forward.y);
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }

    public void setAspect(float aspect) {
        this.aspect = aspect;
    }

    public void setUp(Vector3f up) {
        this.up = up;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public void setNear(float near) {
        this.near = near;
    }

    public void setFar(float far) {
        this.far = far;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getTarget() {
        return target;
    }

    public Vector3f getUp() {
        return up;
    }

    public float getFov() {
        return fov;
    }

    public float getNear() {
        return near;
    }

    public float getFar() {
        return far;
    }

    public float getAspect() {
        return aspect;
    }
}
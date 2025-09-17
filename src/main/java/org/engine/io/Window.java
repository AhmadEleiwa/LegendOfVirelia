package org.engine.io;

import org.joml.Vector2d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

public class Window {
    private long windowHandle;
    private final int width, height;

    private final String title;

    private boolean vsync;

    private Vector3f color = new Vector3f(0.52f, 0.81f, 0.90f);
    private Vector2d mousePos = new Vector2d();

    public Vector2d getMousePos() {
        return new Vector2d(mousePos);
    }

    public Window(String title, int width, int height, boolean vsync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vsync = vsync;
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to init GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        windowHandle = GLFW.glfwCreateWindow(width, height, title, 0, 0);

        if (windowHandle == 0)
            throw new RuntimeException("Failed to create window");

        GLFW.glfwMakeContextCurrent(windowHandle);
        if (vsync)
            GLFW.glfwSwapInterval(1);
        else
            GLFW.glfwSwapInterval(0);
        GL.createCapabilities();

        // glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        glEnable(GL_DEPTH_TEST);

        glDepthFunc(GL_LESS);

        // DISABLE backface culling for debugging - you can enable it later once faces
        // are correct
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW); // Counter-clockwise winding = front face

        // Enable blending for transparency (if needed)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GLFW.glfwSetCursorPosCallback(windowHandle, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mousePos.x = xpos;
                mousePos.y = ypos;
            }
        });

    }

    public void update() {
        glClearColor(color.x, color.y, color.z, 1.0f);
        GLFW.glfwSwapBuffers(windowHandle);
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void cleanup() {
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
        if (vsync)
            GLFW.glfwSwapInterval(1);
        else
            GLFW.glfwSwapInterval(0);
    }

    public void setShouldClose(boolean b) {
        GLFW.glfwSetWindowShouldClose(windowHandle, b);
    }
}

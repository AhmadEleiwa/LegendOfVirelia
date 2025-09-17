package org.engine.io;

import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;

import org.lwjgl.glfw.GLFW;

public class Input {
    public static final int KEY_UNKNOWN = GLFW.GLFW_KEY_UNKNOWN;
    public static final int KEY_Q = GLFW.GLFW_KEY_Q;
    public static final int KEY_W = GLFW.GLFW_KEY_W;
    public static final int KEY_E = GLFW.GLFW_KEY_E;
    public static final int KEY_R = GLFW.GLFW_KEY_R;
    public static final int KEY_T = GLFW.GLFW_KEY_T;
    public static final int KEY_Y = GLFW.GLFW_KEY_Y;
    public static final int KEY_U = GLFW.GLFW_KEY_U;
    public static final int KEY_I = GLFW.GLFW_KEY_I;
    public static final int KEY_O = GLFW.GLFW_KEY_O;
    public static final int KEY_P = GLFW.GLFW_KEY_P;
    public static final int KEY_A = GLFW.GLFW_KEY_A;
    public static final int KEY_S = GLFW.GLFW_KEY_S;
    public static final int KEY_D = GLFW.GLFW_KEY_D;
    public static final int KEY_F = GLFW.GLFW_KEY_F;
    public static final int KEY_G = GLFW.GLFW_KEY_G;
    public static final int KEY_H = GLFW.GLFW_KEY_H;
    public static final int KEY_J = GLFW.GLFW_KEY_J;
    public static final int KEY_K = GLFW.GLFW_KEY_K;
    public static final int KEY_L = GLFW.GLFW_KEY_L;
    public static final int KEY_Z = GLFW.GLFW_KEY_Z;
    public static final int KEY_X = GLFW.GLFW_KEY_X;
    public static final int KEY_C = GLFW.GLFW_KEY_C;
    public static final int KEY_V = GLFW.GLFW_KEY_V;
    public static final int KEY_B = GLFW.GLFW_KEY_B;
    public static final int KEY_N = GLFW.GLFW_KEY_N;
    public static final int KEY_M = GLFW.GLFW_KEY_M;

    public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
    public static final int KEY_SPACE = GLFW.GLFW_KEY_SPACE;
    public static final int KEY_LEFT_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    public static final int KEY_LEFT_CONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
    public static final int KEY_UP = GLFW.GLFW_KEY_UP;
    public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
    public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
    public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
    public static final int KEY_ENTER = GLFW.GLFW_KEY_ENTER;
    public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
    public static final int KEY_BACKSPACE = GLFW.GLFW_KEY_BACKSPACE;
    public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
    public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
    public static final int KEY_END = GLFW.GLFW_KEY_END;
    public static final int KEY_PAGE_UP = GLFW.GLFW_KEY_PAGE_UP;
    public static final int KEY_PAGE_DOWN = GLFW.GLFW_KEY_PAGE_DOWN;
    public static final int KEY_F1 = GLFW.GLFW_KEY_F1;
    public static final int KEY_F2 = GLFW.GLFW_KEY_F2;
    public static final int KEY_F3 = GLFW.GLFW_KEY_F3;
    public static final int KEY_F4 = GLFW.GLFW_KEY_F4;
    public static final int KEY_F5 = GLFW.GLFW_KEY_F5;
    public static final int KEY_F6 = GLFW.GLFW_KEY_F6;
    public static final int KEY_F7 = GLFW.GLFW_KEY_F7;
    public static final int KEY_F8 = GLFW.GLFW_KEY_F8;
    public static final int KEY_F9 = GLFW.GLFW_KEY_F9;
    public static final int KEY_F10 = GLFW.GLFW_KEY_F10;
    public static final int KEY_F11 = GLFW.GLFW_KEY_F11;
    public static final int KEY_F12 = GLFW.GLFW_KEY_F12;

    // Add more keys as needed
    public static final int MOUSE_LEFT = GLFW.GLFW_MOUSE_BUTTON_1;
    public static final int MOUSE_RIGHT = GLFW.GLFW_MOUSE_BUTTON_2;
    public static final int MOUSE_MIDDLE = GLFW.GLFW_MOUSE_BUTTON_3;
    public static final int MOUSE_BUTTON_4 = GLFW.GLFW_MOUSE_BUTTON_4;
    public static final int MOUSE_BUTTON_5 = GLFW.GLFW_MOUSE_BUTTON_5;

    public static final int MOD_SHIFT = GLFW.GLFW_MOD_SHIFT;
    public static final int MOD_CONTROL = GLFW.GLFW_MOD_CONTROL;

    public static Window defaultWindow = null;
    private static int[] lastKeys = new int[GLFW.GLFW_KEY_LAST + 1];

    public static boolean isMouseHovered(Window window) {
        return glfwGetKey(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS ||
                glfwGetKey(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseScrolled(Window window) {
        return glfwGetKey(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseMoved(Window window) {
        return glfwGetKey(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseButtonPressed(Window window, int button) {
        return glfwGetMouseButton(window.getWindowHandle(), button) == GLFW.GLFW_PRESS;
    }

    public static boolean isKeyPressed(Window window, int key) {
        // glfwGetKey(window, key) == GLFW_PRESS
        int state = glfwGetKey(window.getWindowHandle(), key);
        boolean current = state == GLFW.GLFW_PRESS;
        boolean last = lastKeys[key] == GLFW.GLFW_PRESS;
        lastKeys[key] = state;
        return current && !last; // true only on first frame of press
    }

    public static boolean isKeyReleased(Window window, int key) {
        // glfwGetKey(window, key) == GLFW_PRESS
        int state = glfwGetKey(window.getWindowHandle(), key);
        boolean current = state == GLFW.GLFW_RELEASE;
        boolean last = lastKeys[key] == GLFW.GLFW_PRESS;
        lastKeys[key] = state;
        return current && last; // true only on first frame of press

    }

    public static boolean isKeyReleased(int key) {
        // glfwGetKey(window, key) == GLFW_RELEASE
        return isKeyReleased(defaultWindow, key);
    }

    public static boolean isKeyPressed(int key) {
        // glfwGetKey(window, key) == GLFW_PRESS
        return isKeyPressed(defaultWindow, key);
    }

    public static boolean isKeyDown(int key) {
        // glfwGetKey(window, key) == GLFW_PRESS or GLFW_REPEAT
        int state = glfwGetKey(defaultWindow.getWindowHandle(), key);
        return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
    }

    public static boolean isKeyDown(Window window, int key) {
        // glfwGetKey(window, key) == GLFW_PRESS or GLFW_REPEAT
        int state = glfwGetKey(window.getWindowHandle(), key);
        return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
    }

    public static boolean isMouseHovered() {
        return glfwGetKey(defaultWindow.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS ||
                glfwGetKey(defaultWindow.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS ||
                glfwGetKey(defaultWindow.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseScrolled() {
        return glfwGetKey(defaultWindow.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
    }


    public static boolean isMouseButtonPressed(int button) {
  
        return glfwGetMouseButton(defaultWindow.getWindowHandle(), button) == GLFW.GLFW_PRESS;
    }
    public static boolean isMouseMoved() {
        return true;
    }

    public static double getMouseX(Window window) {
        return window.getMousePos().x;
    }

    public static double getMouseY(Window window) {
        return window.getMousePos().x;
    }

    public static double getMouseX() {
        return defaultWindow.getMousePos().x;
    }

    public static double getMouseY() {
        return defaultWindow.getMousePos().y;
    }

}

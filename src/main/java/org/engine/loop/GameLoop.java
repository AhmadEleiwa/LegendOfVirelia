package org.engine.loop;

import org.engine.io.Input;
import org.engine.io.Window;
import org.engine.utils.Debug;

public class GameLoop {
    private final Window window;
    private final ClientSide game;

    // 60 updates per second (fixed)
    private static  int TARGET_UPS = 60;
    private static  double TIME_BETWEEN_UPDATES = 1_000_000_000.0 / TARGET_UPS;
    private static  float FIXED_DELTA = 1f / TARGET_UPS;

    // set to 0 for unlimited (use GPU/VSync), >0 to manually cap FPS
    private static  int TARGET_FPS = 0; // change to 60 to force 60fps cap

    private int framesRendered = 0;

    public GameLoop(Window window, ClientSide game) {
        this.window = window;
        this.game = game;
        Input.defaultWindow = window;
        
        if(window.isVsync())
            GameLoop.TARGET_FPS = 60;
            
    }

    public void run() {
        long lastUpdateTime = System.nanoTime();
        long lastRenderTime = System.nanoTime();
        long lastFpsTime = System.currentTimeMillis();

        game.init();
        
        double accumulator = 0.0;

        while (!window.shouldClose()) {
            long now = System.nanoTime();
            accumulator += (now - lastUpdateTime) / TIME_BETWEEN_UPDATES;
            lastUpdateTime = now;

            // --- Fixed-step updates ---
            while (accumulator >= 1.0) {
                game.input(window);
                game.update(FIXED_DELTA);
                accumulator -= 1.0;
            }

            // --- Rendering ---
            boolean shouldRender = true;
            if (TARGET_FPS > 0) {
                double timeBetweenFrames = 1_000_000_000.0 / TARGET_FPS;
                shouldRender = (now - lastRenderTime) >= timeBetweenFrames;
            }

            if (shouldRender) {
                lastRenderTime = now;
                game.render();
                window.update();
                framesRendered++;
            }

            // --- FPS counter ---
            if (System.currentTimeMillis() - lastFpsTime >= 1000) {
                Debug.log("FPS: " + framesRendered);
                framesRendered = 0;
                lastFpsTime = System.currentTimeMillis();
            }

            try {
                Thread.sleep(1); // tiny pause for CPU
            } catch (InterruptedException ignored) {}
        }

        game.cleanup();
        window.cleanup();
    }
}

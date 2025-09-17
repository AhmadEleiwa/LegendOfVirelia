package org.legendofvirelia;

import org.engine.io.Window;
import org.engine.loop.GameLoop;
import org.legendofvirelia.client.ClientGameLogic;
import org.legendofvirelia.server.ServerGameLogic;
import org.legendofvirelia.shared.WorldState;

public class Main {
    public static void main(String[] args) {
        // GameLoop loop = new GameLoop(
        //         new Window("Vortex Game", 1280, 720, false), new VortexGame());
        // loop.run();

        WorldState world = new WorldState();

        // start server logic in its own thread
        // ServerGameLogic serverLogic = new ServerGameLogic(world);
        // new Thread(() -> {
        //     serverLogic.init();
        //     while (true) {
        //         serverLogic.update(1f / 20f); // tick server 20 times per second
        //         try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        //     }
        // }, "ServerThread").start();

        // start client loop (your existing engine code)
        Window window = new Window("Vortex Game", 1280, 720, false);
        ClientGameLogic clientLogic = new ClientGameLogic(world);
        GameLoop loop = new GameLoop(window, clientLogic);
        loop.run();
    }
}

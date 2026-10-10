// org/legendofvirelia/server/ServerGameLogic.java
package org.legendofvirelia.server;
import org.engine.loop.ServerSide;
import org.engine.utils.Debug;
import org.legendofvirelia.shared.ServerWorldState;
public class ServerGameLogic implements ServerSide {
    private final ServerWorldState world; // authoritative server world

    public ServerGameLogic(ServerWorldState world) {
        this.world = world;
    }

    @Override
    public void init() {
        world.init();
        Debug.log("Server world state initialized.");
    }

    @Override
    public void update(float interval) {
        world.update(interval);
    }
}
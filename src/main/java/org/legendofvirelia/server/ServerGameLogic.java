// org/legendofvirelia/server/ServerGameLogic.java
package org.legendofvirelia.server;
import org.engine.loop.ServerSide;
import org.legendofvirelia.shared.WorldState;

public class ServerGameLogic implements ServerSide {
    private final WorldState world;

    public ServerGameLogic(WorldState world) {
        this.world = world;
    }

    @Override
    public void init() {
        // init world, spawn entities
    }

    @Override
    public void update(float interval) {
        // update world state
    }

}

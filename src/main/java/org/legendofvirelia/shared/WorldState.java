// shared/WorldState.java
package org.legendofvirelia.shared;

import java.util.concurrent.CopyOnWriteArrayList;

public class WorldState {
    private final CopyOnWriteArrayList<EntityState> entities = new CopyOnWriteArrayList<>();

    public void addEntity(EntityState e) { entities.add(e); }
    public CopyOnWriteArrayList<EntityState> getEntities() { return entities; }
}

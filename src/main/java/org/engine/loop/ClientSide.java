package org.engine.loop;

import org.engine.io.Window;

public interface ClientSide extends Side{
    public void input(Window window);
    public void render();
    public void cleanup();
    public void placeBlock();
    
} 
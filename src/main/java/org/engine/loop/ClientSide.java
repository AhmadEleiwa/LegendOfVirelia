package org.engine.loop;

import org.engine.io.Window;

public interface ClientSide extends Side{

    
    public void init(Window window);
    public void input(Window window);
    public void render();
    public void cleanup();
    public void placeBlock();
    
} 
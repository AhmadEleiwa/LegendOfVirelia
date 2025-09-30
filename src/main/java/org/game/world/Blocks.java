package org.game.world;

import java.util.function.Supplier;

public class Blocks {
    final public static Block AIR = null;
    final public static Supplier<Block> DIRT = () -> new Block("dirt");
    final public static Supplier<Block> Torch = () -> new Block("dirt", 15);


  
}

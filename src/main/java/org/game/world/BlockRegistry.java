package org.game.world;

import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private static final Map<String, Integer> nameToId = new HashMap<>();
    private static final Map<Integer, Block> idToBlock = new HashMap<>();
    private static int nextId = 1;

    public static int register(String name, Block block) {
        int id = nextId++;
        nameToId.put(name, id);
        idToBlock.put(id, block);
        return id;
    }

    public static int getId(String name) {
        return nameToId.getOrDefault(name, 0);
    }

    public static Block getBlock(int id) {
        return idToBlock.get(id);
    }
}

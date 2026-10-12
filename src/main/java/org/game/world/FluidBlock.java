package org.game.world;

import java.util.ArrayList;
import java.util.List;

public final class FluidBlock extends Block {
    public static final int MAX_LEVEL = 7;
    public static final float TIME_TO_NEXT_FLOW = 0.35f;

    public FluidBlock(String modelName) {
        super(modelName, false);
        setTransparent(true);
    }

    public List<World.FluidUpdate> update(World world, int x, int y, int z, float delta) {
        int level = world.getFluidLevelAt(x, y, z);
        if (level <= 1)
            return List.of();

        float remaining = world.getFluidFlowTimerAt(x, y, z) - Math.max(delta, 0.0f);
        if (remaining > 0.0f) {
            world.setFluidFlowTimerAt(x, y, z, remaining);
            return List.of();
        }

        world.setFluidFlowTimerAt(x, y, z, TIME_TO_NEXT_FLOW);
        int nextLevel = level - 1;
        List<World.FluidUpdate> updates = new ArrayList<>(4);
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] direction : directions) {
            int targetX = x + direction[0];
            int targetZ = z + direction[1];
            if (world.placeFluidAt(targetX, y, targetZ, nextLevel, false, TIME_TO_NEXT_FLOW))
                updates.add(new World.FluidUpdate(targetX, y, targetZ,
                        world.getFluidLevelAt(targetX, y, targetZ),
                        world.isFluidSourceAt(targetX, y, targetZ)));
        }
        return updates;
    }
}

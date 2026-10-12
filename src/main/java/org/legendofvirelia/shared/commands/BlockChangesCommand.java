package org.legendofvirelia.shared.commands;

import java.util.ArrayList;
import java.util.List;

import org.engine.utils.Debug;
import org.game.world.Chunk;
import org.joml.Vector3i;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ServerCommand;

public final class BlockChangesCommand implements ServerCommand {
    public final List<BlockChange> changes;

    public BlockChangesCommand(List<BlockChange> changes) {
        this.changes = List.copyOf(changes);
    }

    @Override
    public void execute(ServerWorldState world) {
        long startedAt = System.nanoTime();
        List<BlockChange> appliedChanges = new ArrayList<>(changes.size());
        int skippedChanges = 0;
        for (BlockChange change : changes) {
            Vector3i position = new Vector3i(change.x, change.y, change.z);
            Chunk targetChunk = world.getCurrentWorld().getChunk(
                    Math.floorDiv(position.x, Chunk.SIZE_X),
                    Math.floorDiv(position.z, Chunk.SIZE_Z));
            if (targetChunk == null) {
                skippedChanges++;
                continue;
            }

            int localX = Math.floorMod(position.x, Chunk.SIZE_X);
            int localZ = Math.floorMod(position.z, Chunk.SIZE_Z);
            boolean sameBlock = targetChunk.getBlock(localX, position.y, localZ) == change.blockId;
            if (sameBlock && change.blockId != org.game.world.BlockRegistry.getId("water")) {
                skippedChanges++;
                continue;
            }

            Chunk chunk = sameBlock ? targetChunk
                    : world.getBlockPlacer().placeBlock(position, change.blockId);
            if (chunk == null) {
                skippedChanges++;
                continue;
            }
            org.game.world.World gameWorld = world.getCurrentWorld();
            if (change.blockId == org.game.world.BlockRegistry.getId("water")) {
                int level = change.fluidLevel > 0 ? change.fluidLevel
                        : org.game.world.FluidBlock.MAX_LEVEL;
                boolean source = change.fluidLevel > 0 ? change.fluidSource : true;
                gameWorld.placeFluidAt(position.x, position.y, position.z, level, source, 0);
            }
            appliedChanges.add(change.blockId == org.game.world.BlockRegistry.getId("water")
                    ? new BlockChange(position.x, position.y, position.z, change.blockId,
                            gameWorld.getFluidLevelAt(position.x, position.y, position.z),
                            gameWorld.isFluidSourceAt(position.x, position.y, position.z))
                    : change);
        }

        Debug.log("Server processed block-change batch: " + appliedChanges.size() + " applied, "
                + skippedChanges + " unchanged or outside loaded chunks in "
                + ((System.nanoTime() - startedAt) / 1_000_000.0) + " ms.");
        if (!appliedChanges.isEmpty()) {
            world.sendCommand(new BlockChangesConfirmCommand(appliedChanges));
        }
    }
}

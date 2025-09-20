package org.legendofvirelia.shared.commands;
import org.game.world.Chunk;

import org.joml.Vector3i;
import org.legendofvirelia.shared.ServerWorldState;

import org.legendofvirelia.shared.command.ServerCommand;

public class PlaceBlockCommand implements ServerCommand{
    public Vector3i position;
    public int blockId;
    public long actionId; // For client-side prediction tracking
    
    public PlaceBlockCommand(Vector3i position, int blockId) {
        this.position = position;
        this.blockId = blockId;
        this.actionId = 0; // Will be set by WorldState
    }

    @Override
    public void execute(ServerWorldState world) {
        // TODO Auto-generated method stub
        // world.getBlockPlacer().placeBlock(position, blockId);
        System.out.println("Placing block " + blockId + " at " + position);
        Chunk chunk = world.getBlockPlacer().placeBlock(position, blockId);
        if(chunk != null) {
            world.requestRerenderChunk(chunk);
            world.sendCommand(new PlaceBlockConfirmCommand(position, blockId));
            // world.checkAndUpdateNeighboringChunks(position);
        }
    }
}
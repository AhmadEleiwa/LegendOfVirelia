package org.legendofvirelia.shared.commands;

import org.game.world.Chunk;

import org.joml.Vector3i;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.command.ClientCommand;

public class PlaceBlockConfirmCommand implements ClientCommand {
    public Vector3i position;
    public int blockId;
    public long actionId; // For client-side prediction tracking
    
    public PlaceBlockConfirmCommand(Vector3i position, int blockId) {
        this.position = position;
        this.blockId = blockId;
        this.actionId = 0; // Will be set by WorldState
    }
    @Override
    public void execute(ClientWorldState world) {
        // TODO Auto-generated method stub
        Chunk chunk =world.getBlockPlacer().placeBlock(position, blockId);
        System.out.println("Confirming placement of block " + blockId + " at " + position);
        System.out.println(chunk);
        if(chunk != null)
             world.requestRerenderChunk(chunk);
    }

    
}

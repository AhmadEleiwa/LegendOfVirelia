package org.legendofvirelia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.engine.io.Window;
import org.engine.loop.GameLoop;
import org.engine.utils.Debug;
import org.engine.utils.Logger;
import org.joml.Vector3i;
import org.legendofvirelia.client.ClientGameLogic;
import org.legendofvirelia.server.ServerGameLogic;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.commands.BlockChange;
import org.legendofvirelia.shared.commands.BlockChangesCommand;
import org.legendofvirelia.shared.commands.BreakBlockCommand;
import org.legendofvirelia.shared.commands.PlaceBlockCommand;

public class Main {
    public static void main(String[] args) {
        // Create separate world states for client and server
        ClientWorldState clientWorld = new ClientWorldState();  // Client-side
        ServerWorldState serverWorld = new ServerWorldState(); // Server-side
        
        // Simple communication bridge (in real game, this would be network)
        CommunicationBridge bridge = new CommunicationBridge(clientWorld, serverWorld);

        // Start server logic in its own thread
        ServerGameLogic serverLogic = new ServerGameLogic(serverWorld);
        Thread serverThread = new Thread(() -> {
            serverLogic.init();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    bridge.transferClientToServer();
                    serverLogic.update(1f / 20f);
                    bridge.transferServerToClient();
                } catch (RuntimeException e) {
                    Logger.log("Server tick failed; skipping this tick.", e);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ServerThread");
        
        serverThread.setDaemon(true);
        serverThread.start();

        // Start client loop
        Window window = new Window("Vortex Game", 1280, 720, false);
        ClientGameLogic clientLogic = new ClientGameLogic(clientWorld);
        GameLoop loop = new GameLoop(window, clientLogic);

        loop.run();
        
        // Cleanup
        serverThread.interrupt();
    }
    
    /**
     * Simple communication bridge to simulate client-server networking
     * In a real game, this would be replaced with actual networking code
     */
    private static class CommunicationBridge {
        private final ClientWorldState clientWorld;
        private final ServerWorldState serverWorld;
        
        public CommunicationBridge(ClientWorldState clientWorld, ServerWorldState serverWorld) {
            this.clientWorld = clientWorld;
            this.serverWorld = serverWorld;
        
        }
        
        public void transferClientToServer() {
            BlockChangeBatch batch = new BlockChangeBatch();
            var clientActions = clientWorld.getOutgoingCommands();
            var action = clientActions.poll();
            while (action != null) {
                if (action instanceof PlaceBlockCommand place) {
                    batch.add(place.position.x, place.position.y, place.position.z, place.blockId);
                } else if (action instanceof BreakBlockCommand breaking) {
                    batch.add(breaking.position.x, breaking.position.y, breaking.position.z, 0);
                } else {
                    flushBlockChanges(batch);
                    serverWorld.receiveServerCommands(action);
                }
                action = clientActions.poll();
            }
            flushBlockChanges(batch);
        }
        
        private void flushBlockChanges(BlockChangeBatch batch) {
            if (!batch.changes.isEmpty()) {
                int count = batch.changes.size();
                serverWorld.receiveServerCommands(new BlockChangesCommand(batch.toList()));
                batch.changes.clear();
                Debug.log("Bridge queued " + count + " block edits for the server.");
            }
        }

        public void transferServerToClient() {
            var serverActions = serverWorld.getOutgoingCommands();
            var action = serverActions.poll();
            int transferred = 0;
            while (action != null) {
                clientWorld.receiveServerCommands(action);
                transferred++;
                action = serverActions.poll();
            }
            if (transferred > 0)
                Debug.log("Bridge delivered " + transferred + " server updates to the client queue.");
        }

        private static class BlockChangeBatch {
            private final Map<Vector3i, BlockChange> changes = new LinkedHashMap<>();

            private void add(int x, int y, int z, int blockId) {
                Vector3i position = new Vector3i(x, y, z);
                changes.put(position, new BlockChange(x, y, z, blockId));
            }

            private List<BlockChange> toList() {
                return new ArrayList<>(changes.values());
            }
        }
    }
}
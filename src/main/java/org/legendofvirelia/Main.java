package org.legendofvirelia;

import java.util.Queue;


import org.engine.io.Window;
import org.engine.loop.GameLoop;
import org.engine.utils.Debug;
import org.legendofvirelia.client.ClientGameLogic;
import org.legendofvirelia.server.ServerGameLogic;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.ServerWorldState;
import org.legendofvirelia.shared.command.ClientCommand;
import org.legendofvirelia.shared.command.ServerCommand;

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
            while (true) {
                try {
                    // Process client actions on server
                    bridge.processClientToServer();
                    
                    // Update server world
                    serverLogic.update(1f / 20f); // 20 TPS
                    
                    // Send server responses back to client
                    bridge.processServerToClient();
                    
                    Thread.sleep(50); // 20 TPS
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
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
        
        public void processClientToServer() {
            // Transfer actions from client to server
            Queue<ServerCommand> clientActions = clientWorld.getOutgoingCommands();
            ServerCommand action;
            while ((action = clientActions.poll()) != null) {
                serverWorld.receiveServerCommands(action);
            }
        }
        
        public void processServerToClient() {
            // Transfer server responses back to client
            Queue<ClientCommand> serverActions = serverWorld.getOutgoingCommands();
            ClientCommand action;
            
            while ((action = serverActions.poll()) != null) {
                Debug.log(action.toString());
                clientWorld.receiveServerCommands(action);
            }
        }
    }
}
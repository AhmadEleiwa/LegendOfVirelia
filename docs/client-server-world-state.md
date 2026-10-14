# 2. Client/server commands and world state

[← Previous: Architecture](architecture.md) · **2 / 10** · [Next: World generation →](world-generation.md)

## The important path: request → server world change → client world change

**The client sends intent. The server changes its world. The server sends the accepted change back. The client then updates its copy.**

```text
ClientGameLogic
  send PlaceBlockCommand
       ↓ client outgoing queue
Main.CommunicationBridge
  convert place/break requests into BlockChangesCommand batch
       ↓ server incoming queue
ServerWorldState.update → BlockChangesCommand.execute
  change server World; send BlockChangesConfirmCommand
       ↓ server outgoing queue
Main.CommunicationBridge
       ↓ client incoming queue
ClientWorldState.update → BlockChangesConfirmCommand.execute
  change client World; queue lighting/mesh updates
```

The two worlds are separate. A successful server edit does **not** automatically change the client’s copy; the confirmation command carries the edit back.

## 1. Client creates and sends a placement request

`ClientGameLogic.placeBlock()` raycasts from the camera, chooses an adjacent cell (or replaces water at the hit cell), checks that the new block won’t overlap the player, then queues the request. This is the complete method:

```java
@Override
public void placeBlock() {
    BlockPlacer.RaycastResult result = BlockPlacer.raycastForPlacement(
            worldState.getCurrentWorld(), camera, 5f);

    if (result.hit && result.blockPosition != null) {
        boolean replacingWater = worldState.getCurrentWorld().getBlockIdAt(
                result.blockPosition.x, result.blockPosition.y, result.blockPosition.z)
                == BlockRegistry.getId("water");
        Vector3i targetPosition = replacingWater ? result.blockPosition : result.placePosition;
        if (targetPosition == null)
            return;
        if (!replacingWater && playerPhysics.overlapsPlayer(camera, targetPosition)) {
            Debug.log("Cannot place a block inside the player.");
            return;
        }
        PlaceBlockCommand action = new PlaceBlockCommand(targetPosition, selectedBlockId);
        worldState.sendCommand(action);
        camera.triggerImpact(0.65f);
    } else {
        Debug.log("No valid placement position found");
    }
}
```

`ClientWorldState.sendCommand` only enqueues the command in its outgoing queue. It does not place the block or mean the server accepted it.

Relevant source: [`ClientGameLogic.placeBlock`](../src/main/java/org/legendofvirelia/client/ClientGameLogic.java), [`PlaceBlockCommand`](../src/main/java/org/legendofvirelia/shared/commands/PlaceBlockCommand.java).

## 2. The bridge delivers the request to the server

In this project, `Main.CommunicationBridge.transferClientToServer()` intercepts place and break requests and batches them as `BlockChangesCommand` objects. Full method:

```java
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
```

`ServerWorldState.receiveServerCommands` puts the command in the server’s incoming queue. During `ServerWorldState.update`, the server drains that queue and executes each command. These are the complete methods:

```java
@Override
public void receiveServerCommands(ServerCommand command) {
    incomingCommands.offer(command);
}

@Override
public void update(float delta) {
    ServerCommand command;
    while ((command = incomingCommands.poll()) != null) {
        command.execute(this);
    }
    world.collectGeneratedChunks(4);
    sendAvailableChunks(2);
    world.update(delta);
    List<org.game.world.World.FluidUpdate> fluidUpdates = world.updateFluidBlocks(delta, 512);
    if (!fluidUpdates.isEmpty()) {
        List<BlockChange> changes = new ArrayList<>(fluidUpdates.size());
        for (org.game.world.World.FluidUpdate update : fluidUpdates) {
            changes.add(new BlockChange(update.x, update.y, update.z,
                    update.blockId, update.level, update.source));
        }
        sendCommand(new BlockChangesConfirmCommand(changes));
    }
}
```

The imports and class wrapper are omitted here; the method bodies follow `ServerWorldState.java`.

Relevant source: [`Main.CommunicationBridge`](../src/main/java/org/legendofvirelia/Main.java), [`ServerWorldState.update`](../src/main/java/org/legendofvirelia/shared/ServerWorldState.java).

## 3. The server changes its own world and sends confirmation

`BlockChangesCommand.execute(ServerWorldState)` checks that the target chunk is loaded and attempts the edit through the server’s `BlockPlacer`. Successful changes are collected. If at least one edit succeeded, the server queues a client-bound confirmation. Complete method:

```java
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
            int level = change.fluidLevel > 0
                    ? change.fluidLevel : org.game.world.FluidBlock.MAX_LEVEL;
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
```

An unloaded or rejected edit is skipped; this code currently does not send a separate rejection command. Imports are omitted from the excerpt; in the source, `BlockRegistry` and `FluidBlock` are fully qualified at the use sites.

Relevant source: [`BlockChangesCommand.execute`](../src/main/java/org/legendofvirelia/shared/commands/BlockChangesCommand.java).

`world.sendCommand(...)` calls `ServerWorldState.sendCommand`, which adds the confirmation to the server’s outgoing queue:

```java
@Override
public void sendCommand(ClientCommand command) {
    outgoingCommands.offer(command);
}
```

The bridge transfers it to the client queue with this complete method:

```java
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
```

On the receiving side, `ClientWorldState.receiveServerCommands` queues it; the previously shown `processServerActions()` method executes it on the client update:

```java
@Override
public void receiveServerCommands(ClientCommand command) {
    incomingCommands.offer(command);
}
```

### What `PlaceBlockCommand.execute` does

The complete method delegates to a one-item change batch:

```java
@Override
public void execute(ServerWorldState world) {
    new BlockChangesCommand(List.of(
            new BlockChange(position.x, position.y, position.z, blockId))).execute(world);
}
```

`BreakBlockCommand.execute` does the same with block ID `0` (air). **In the current local bridge, these individual `execute` methods are bypassed for place/break:** `Main` batches those request objects directly into `BlockChangesCommand`. They remain useful when a server transport delivers an individual command without that batching step.

Relevant source: [`PlaceBlockCommand.execute`](../src/main/java/org/legendofvirelia/shared/commands/PlaceBlockCommand.java), [`BreakBlockCommand.execute`](../src/main/java/org/legendofvirelia/shared/commands/BreakBlockCommand.java).

## 4. Client receives the server command and updates its world

The bridge moves server outgoing commands to the client incoming queue. On the next client update, `ClientWorldState` executes them. Complete queue-processing method:

```java
private void processServerActions() {
    ClientCommand command;
    while ((command = incomingCommands.poll()) != null) {
        command.execute(this);
    }
}
```

The confirmation command’s `execute` applies the received edits:

```java
@Override
public void execute(ClientWorldState world) {
    world.applyBlockChanges(changes);
}
```

`applyBlockChanges` changes the client `World`, updates fluid metadata where applicable, recalculates affected lighting, and queues affected chunks/neighbors for remeshing. Complete method:

```java
public void applyBlockChanges(List<BlockChange> changes) {
    long startedAt = System.nanoTime();
    List<Vector3i> changedPositions = new ArrayList<>(changes.size());
    for (BlockChange change : changes) {
        Vector3i position = new Vector3i(change.x, change.y, change.z);
        Chunk targetChunk = world.getChunk(
                Math.floorDiv(position.x, Chunk.SIZE_X),
                Math.floorDiv(position.z, Chunk.SIZE_Z));
        if (targetChunk == null)
            continue;

        boolean sameBlock = targetChunk.getBlock(
                Math.floorMod(position.x, Chunk.SIZE_X), position.y,
                Math.floorMod(position.z, Chunk.SIZE_Z)) == change.blockId;
        if (sameBlock && change.blockId != org.game.world.BlockRegistry.getId("water"))
            continue;

        Chunk chunk = sameBlock ? targetChunk : blockPlacer.placeBlock(position, change.blockId);
        if (chunk == null)
            continue;
        if (change.blockId == org.game.world.BlockRegistry.getId("water")) {
            world.setFluidStateAt(position.x, position.y, position.z,
                    change.fluidLevel > 0 ? change.fluidLevel : org.game.world.FluidBlock.MAX_LEVEL,
                    change.fluidSource, org.game.world.FluidBlock.TIME_TO_NEXT_FLOW);
        }

        changedPositions.add(position);
        requestRerenderChunk(chunk);
        checkAndUpdateNeighboringChunks(position);
    }

    for (Chunk chunk : WorldRenderer.updateBlockLighting(world, changedPositions))
        requestRerenderChunk(chunk);

    if (!changes.isEmpty()) {
        Debug.log("Applied block-change confirmation: " + changedPositions.size() + "/"
                + changes.size() + " changed; " + pendingChunkUpdateCount()
                + " chunks queued for remeshing in "
                + ((System.nanoTime() - startedAt) / 1_000_000.0) + " ms.");
    }
}
```

This method is copied from `ClientWorldState`; imports are omitted in the excerpt.

Relevant source: [`BlockChangesConfirmCommand.execute`](../src/main/java/org/legendofvirelia/shared/commands/BlockChangesConfirmCommand.java), [`ClientWorldState.applyBlockChanges`](../src/main/java/org/legendofvirelia/shared/ClientWorldState.java).

## Breaking a block

Breaking uses the same round trip. This is the complete client method:

```java
private void breakBlock() {
    BlockPlacer.RaycastResult result = BlockPlacer.raycast(
            worldState.getCurrentWorld(), camera, 5f);

    if (result.hit && result.blockPosition != null) {
        BreakBlockCommand action = new BreakBlockCommand(result.blockPosition);
        worldState.sendCommand(action);
        camera.triggerImpact(0.45f);
    }
}
```

The complete server command method converts the request into a change with block ID `0` (air):

```java
@Override
public void execute(ServerWorldState world) {
    new BlockChangesCommand(List.of(
            new BlockChange(position.x, position.y, position.z, 0))).execute(world);
}
```

The local bridge batches it directly rather than calling this method, as noted above. After the server applies the air change and returns `BlockChangesConfirmCommand`, the client sets its corresponding block to air and queues render updates. Source: [`BreakBlockCommand`](../src/main/java/org/legendofvirelia/shared/commands/BreakBlockCommand.java).

## World-ready handshake

At client initialization, `ClientWorldState.init()` sends `ClientReady`. The server handles it by starting initial chunk generation. **This is a client-started signal, not a server “world is ready” response.** There is currently no `isWorldReady` flag or `WorldReady` command.

To gate player movement on the spawn chunk, check whether its chunk has arrived:

```java
int spawnChunkX = Math.floorDiv(spawnX, Chunk.SIZE_X);
int spawnChunkZ = Math.floorDiv(spawnZ, Chunk.SIZE_Z);
boolean spawnChunkLoaded = clientState.getCurrentWorld()
        .getChunk(spawnChunkX, spawnChunkZ) != null;
```

For a safe spawn region, check all required neighboring chunks too. Loaded data may not have a mesh yet; check `Chunk.isMeshBuilt()` if the screen must wait for visible geometry. Poll during updates and show a loading state instead of blocking the render thread. For a formal acknowledgment, add a server-to-client `WorldReady` command after the server has delivered the required spawn chunks.

## Reuse the pattern for another action

```text
Client sends ServerCommand request
→ server validates and updates authoritative state
→ server sends ClientCommand result
→ client applies result and refreshes derived state
```

`ServerCommand` executes on `ServerWorldState`; `ClientCommand` executes on `ClientWorldState`. Queue transfer is thread-safe, but direct concurrent world mutation is not. The current bridge passes Java objects in-process; a real network requires serialization and stable block identifiers.

## Usage example (pseudocode): change player stats

**Pseudocode only.** The state methods and `PlayerStatsChanged` response below are illustrative; add them to your game state/command classes.

```text
class ChangePlayerStatsCommand implements ServerCommand:
    playerId, requestedHealth

    execute(serverState):
        player = serverState.findPlayer(playerId)
        if player is missing: return
        acceptedHealth = clamp(requestedHealth, 0, player.maxHealth)
        player.health = acceptedHealth             // authoritative mutation
        serverState.sendCommand(
            PlayerStatsChanged(playerId, acceptedHealth))

class PlayerStatsChanged implements ClientCommand:
    playerId, health

    execute(clientState):
        clientState.updateDisplayedHealth(playerId, health)

Client input:
    clientState.sendCommand(ChangePlayerStatsCommand(id, requestedHealth))
```

The request should describe intent. The server validates it, updates its state, and responds with the accepted value for the client to display.

[← Previous: Architecture](architecture.md) · **2 / 10** · [Next: World generation →](world-generation.md)

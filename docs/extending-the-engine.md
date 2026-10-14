# 8. Extending the engine

[← Previous: API reference](api-reference.md) · **8 / 10** · [Next: UI and launcher →](ui-and-launcher.md)

## Add a world feature

Place rules next to the data they update: `World`, `Chunk`, `FluidBlock`, or a focused system. Keep world logic independent of OpenGL. `WorldGenerator` is data-only; `ChunkMesher` converts data into render geometry.

For a block type, follow [4. World and blocks](world-and-rendering.md) and [6. Shaders, textures, and models](shaders-textures-models.md). For procedural terrain, follow [3. World generation](world-generation.md).

## Add a command-backed action

```java
public final class HealRequest implements ServerCommand {
    public int amount;

    @Override
    public void execute(ServerWorldState state) {
        // validate request and change authoritative state
        // send a ClientCommand with the accepted result
    }
}
```

Complete the path:

1. Capture intent in client input logic.
2. Queue a directional command with `sendCommand`.
3. Validate and apply it on the server.
4. Send a result command back to the client.
5. Apply it to client state and invalidate dependent meshes/UI.
6. Update `Main.CommunicationBridge` if the new command needs batching or special transfer.

Typed command details: [2. Client/server](client-server-world-state.md).

## Add a player behavior

`PlayerPhysics` owns movement velocity and collision checks; `Camera` owns view direction/projection. Add movement rules to physics and camera effects to the camera. Do not duplicate collision/world lookup rules inside input handlers.

## Resource and thread rules

- Add models/textures under `assets/` and shaders under `shaders/`.
- Keep names and runtime paths consistent; see [6. Shaders and assets](shaders-textures-models.md).
- CPU workers may build chunk data. OpenGL calls require the client GL thread.
- Runtime block IDs are registration-order based; don’t persist/send them as stable identifiers without a mapping.

## Contributor loop

```text
find owner → make focused change → build → run affected feature → update guide
```

Build with `./gradlew.bat build`; run with `./gradlew.bat run`. Visual/input changes need a graphics-capable run to review.

## Start your own game: a practical plan

This repository is a game application with reusable engine source, not a ready-made engine dependency. Use it as a base project first; extract a standalone library only if you later need independent versioning or reuse across multiple games.

| Step | Change | Start here |
| --- | --- | --- |
| 1. Make it yours | Set the window title and game name; keep `Main` as the launcher while prototyping | `Main.java`, `build.gradle` |
| 2. Choose a game loop | Keep or replace the current client/server split based on whether your game needs an authoritative world | `Main.java`, `ClientGameLogic.java` |
| 3. Define the world | Change blocks, terrain, biomes, or replace voxel world rules | `Blocks.java`, `WorldGenerator.java`, `World.java` |
| 4. Add a player mechanic | Change movement/collision or introduce your own entities and owner/update path | `PlayerPhysics.java`, `GameObject.java` |
| 5. Set the visual style | Replace textures/models, then tune matching shaders and UI | `assets/`, `shaders/`, `ClientGameLogic.java` |
| 6. Add a game action | Implement it locally first; use request/response commands if the server must own the result | `shared/commands/`, client/server world states |
| 7. Iterate | Build, run, inspect the change, and update the matching guide | `./gradlew.bat build`, `./gradlew.bat run` |

Start with one small goal, such as a new biome or a block with a new interaction. Avoid changing terrain, networking, and rendering formats in the same first change; each has its own guide and source path.

## Usage example (pseudocode): add a placeable glowing block

**Pseudocode only.** This combines block registration, assets, and placement; wire the hotbar separately if players should select it.

```text
class GlowBlock extends Block:
    constructor:
        load model "glow"
        set light level = 12

Blocks.GLOW = supplier that creates GlowBlock
assets/models/glow.json + assets/textures/glow.png

Client startup, before chunks load:
    register "glow" with BlockRegistry
    optionally add its ID to the hotbar
```

Register the same stable block identity on client and server before generating or applying edits. For a networked version, don’t rely on incidental registration order for IDs.

[← API reference](api-reference.md) · **8 / 10** · [Next: UI and launcher →](ui-and-launcher.md)

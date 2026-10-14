# 1. Architecture and system flow

[← README](../README.md) · **1 / 10** · [Next: Client/server →](client-server-world-state.md)

## Project layers

| Package | Responsibility |
| --- | --- |
| `org.engine` | Window, input, loop, OpenGL helpers, resources, logging |
| `org.game` | Reusable world, blocks, meshes, camera, physics, lighting, UI |
| `org.legendofvirelia` | Game startup, client/server logic, world states, commands |

Entry point: `org.legendofvirelia.Main`.

## Runtime flow

```text
Main
├─ Server thread: transfer commands → server tick (20 TPS) → return updates
└─ Client thread: GameLoop
   ├─ input()         // keyboard and mouse
   ├─ update(1/60 s)  // client state, physics, HUD
   └─ render()        // opaque world → water → UI
```

`GameLoop` calls `ClientSide.init`, fixed updates, rendering, then cleanup. Rendering can run at a different rate from updates. `Main` sleeps 50 ms between server ticks.

## Core flow in one change

```text
input → command queue → server validates/applies → confirmation command
      → client updates its World → dirty chunks remesh → render
```

The current `Main.CommunicationBridge` moves Java objects between queues inside one process. It is not a socket multiplayer implementation. See [2. Client/server and world state](client-server-world-state.md).

## Where to look

- Frame lifecycle: `engine/loop/GameLoop.java`
- Game composition: `legendofvirelia/client/ClientGameLogic.java`
- World data: `game/world/World.java`, `Chunk.java`
- Render pipeline: [5. Rendering and meshes](rendering-pipeline.md)
- Reusable classes: [7. API and class reference](api-reference.md)

### Change checklist

- World rule? Change `World` or a focused world system.
- Input/render behavior? Change `ClientGameLogic` or the engine renderer.
- Authoritative multiplayer behavior? Add a directional command and server logic.

## Usage example (pseudocode): add a game mode

**Pseudocode only.** Connect the mode to the existing client lifecycle; `GameLoop` owns calling these methods.

```text
class SurvivalMode implements ClientSide:
    init(window): create camera, world state, renderer
    input(window): read controls and queue player actions
    update(delta): update world state and player
    render(): draw world, then HUD
    cleanup(): release meshes, textures, shaders

Main:
    mode = SurvivalMode()
    GameLoop(window, mode).run()
```

[Next: Client/server →](client-server-world-state.md)

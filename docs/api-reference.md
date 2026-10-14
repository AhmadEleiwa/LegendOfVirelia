# 7. API and reusable class reference

[← Previous: Shaders and assets](shaders-textures-models.md) · **7 / 10** · [Next: Extending the engine →](extending-the-engine.md)

This is a map of the main reusable classes. It is not a generated JavaDoc API: inspect the linked source for full signatures and current behavior.

## Engine layer

| Class/interface | Use it for | Source |
| --- | --- | --- |
| `GameLoop` | Fixed updates, rendering, and lifecycle | [`GameLoop.java`](../src/main/java/org/engine/loop/GameLoop.java) |
| `ClientSide` | Implement client `init/input/update/render/cleanup` | [`ClientSide.java`](../src/main/java/org/engine/loop/ClientSide.java) |
| `ServerSide` | Implement server initialization/update | [`ServerSide.java`](../src/main/java/org/engine/loop/ServerSide.java) |
| `Window`, `Input` | Window and keyboard/mouse access | [`Window.java`](../src/main/java/org/engine/io/Window.java) |
| `Renderer`, `ShaderProgram` | 3D draw submission and GLSL programs | [`Renderer.java`](../src/main/java/org/engine/rendering/Renderer.java) |

## Game layer

| Class | Use it for | Source |
| --- | --- | --- |
| `GameObject` | Base transform and update/render/delete contract | [`GameObject.java`](../src/main/java/org/game/core/GameObject.java) |
| `World`, `Chunk` | World and chunk data | [`World.java`](../src/main/java/org/game/world/World.java) |
| `Block`, `FluidBlock` | Block types and fluid behavior | [`Block.java`](../src/main/java/org/game/world/Block.java) |
| `BlockRegistry` | Runtime name ↔ ID registration | [`BlockRegistry.java`](../src/main/java/org/game/world/BlockRegistry.java) |
| `Camera`, `PlayerPhysics` | View and first-person collision/movement | [`Camera.java`](../src/main/java/org/game/entities/Camera.java) |
| `Mesh`, `LightedMesh`, `Model` | GPU geometry and mesh ownership | [`Mesh.java`](../src/main/java/org/game/meshes/Mesh.java) |
| `UIElement`, `Label`, `ColorRect`, `Container` | UI drawing/layout primitives | [`UIElement.java`](../src/main/java/org/game/ui/UIElement.java) |

## Extension examples

### Implement a game loop side

```java
public final class MyClient implements ClientSide {
    public void init(Window window) { /* create GL resources */ }
    public void input(Window window) { /* read Input */ }
    public void update(float delta) { /* update game state */ }
    public void render() { /* issue draw calls */ }
    public void cleanup() { /* release GL resources */ }
    public void placeBlock() { /* implement ClientSide contract */ }
}
```

Use `GameLoop` to own this lifecycle. Keep GL resource setup and cleanup on the thread with the active context.

### Extend a game object

```java
public final class Marker extends GameObject {
    public Marker() { type = "marker"; name = "marker"; }
    @Override public void update(float delta) { position.y += delta; }
    @Override public void render() { /* submit its model */ }
    @Override public void delete() { /* free owned resources */ }
}
```

The project does not yet have a general object manager that automatically registers and draws every `GameObject`; add an explicit owner/update/render path.

### Extend UI

Subclass `UIElement`, implement `draw(UIRenderer, width, height)`, add the element to an owner, and call `destroy()` when removing it. UI elements are visual primitives; mouse hit-testing/events are not implemented.

## Usage example (pseudocode): reusable entity and block

**Pseudocode only.** A `GameObject` needs an owner that calls its lifecycle methods; a block subclass still needs registry and asset setup.

```text
class Entity extends GameObject:
    update(delta): update movement/state
    render(): submit its model to a renderer
    delete(): release owned model resources

class EntityBlock extends Block:
    constructor: load the entity_block model
    onUse(world, position): apply this block's world behavior
```

Use `GameObject` for an independently updated object. Use `Block` when the behavior belongs to a voxel cell in `World`.

[Next: Extending the engine →](extending-the-engine.md)

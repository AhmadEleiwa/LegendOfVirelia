# 5. Rendering and meshes

[← Previous: World and blocks](world-and-rendering.md) · **5 / 10** · [Next: Shaders and assets →](shaders-textures-models.md)

## World-to-screen flow

```text
Chunk block IDs
   ↓ ChunkMesher: visible faces + UV + light + indices
LightedMesh: CPU arrays → VAO/VBO/EBO
   ↓
Model → WorldRenderer → Renderer → cube shaders → screen
                       └→ WaterRenderer (blend pass)
ClientGameLogic → UIRenderer (2D overlay)
```

## Build a chunk mesh

`ChunkMesher.buildModel(world, chunk, dayNightCycle)` scans each block. Air and transparent blocks are skipped in the opaque pass. For each solid block, it tests six neighbors and emits only visible faces. At chunk edges it checks adjacent loaded chunks. It writes position, atlas UV, voxel light, emissive value, and indices into CPU arrays.

```java
// Conceptual per-vertex data emitted by ChunkMesher
position = (x, y, z);
uv       = atlasUV(blockName, faceUV);
light    = sampleVoxelLight(faceCorner);
emissive = block.getLightLevel() > 0 ? 1f : 0f;
```

`LightedMesh` uploads these arrays to GPU buffers. `Model` owns meshes. `WorldRenderer` schedules missing/dirty chunk meshes and draws visible chunks; `FrustumCuller` helps skip off-screen geometry.

## Vertex contract

The current lighted mesh and `cube.vert` agree on these attributes:

| Location | Data | Components |
| --- | --- | --- |
| 0 | Position | `vec3` |
| 1 | Texture UV | `vec2` |
| 2 | Normal | `vec3` |
| 3 | Voxel lighting | `float` |
| 4 | Emissive | `float` |

When changing vertex data, update the Java buffer layout, `glVertexAttribPointer`, and GLSL inputs together. Ordinary `Mesh` uses a simpler layout; inspect its setup before sharing shaders.

## Pass order

1. Opaque chunk models render through `Renderer` with depth writes enabled.
2. Water chunks are sorted far-to-near, blended, drawn with depth writes disabled, then GL state is restored.
3. HUD elements draw afterward with `UIRenderer`.

Keep translucent geometry out of the opaque mesh unless its blending and ordering are also handled.

## Dirty mesh updates

Block confirmation queues chunks for remeshing. `ClientWorldState.render` rebuilds at most one queued chunk per frame; otherwise `WorldRenderer` builds visible missing meshes. Boundary edits invalidate neighbor chunks because face visibility depends on adjacent blocks. Avoid rebuilding meshes every frame.

Mesh creation/deletion uses OpenGL. Do it on the client/render thread with an active GL context; workers may generate CPU-side chunk data only.

### See the passes

![Terrain lighting](../screenshots/new%20lighting.png)

![Water rendering](../screenshots/Screenshot%20water%20logic.png)

Shader and asset details: [6. Shaders, textures, and models](shaders-textures-models.md).

## Usage example (pseudocode): render a custom mesh

**Pseudocode only.** Use the real `Mesh`/`Model` constructors and renderer calls for the chosen vertex layout.

```text
vertices = positions + UVs + normals
indices = triangle vertex indices
mesh = create GPU mesh(vertices, indices)       // client GL thread
model = Model(mesh)

each frame:
    renderer.render(model, camera, light, transform)

when removed:
    model.delete()                              // release GPU buffers
```

For a chunk mesh, build only exposed faces and enqueue a rebuild only when source world data changes.

[← World and blocks](world-and-rendering.md) · **5 / 10** · [Next: Shaders and assets →](shaders-textures-models.md)

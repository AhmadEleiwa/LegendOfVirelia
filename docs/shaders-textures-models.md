# 6. Shaders, textures, and models

[← Previous: Rendering](rendering-pipeline.md) · **6 / 10** · [Next: API reference →](api-reference.md)

## Shader pairs

| Pass | Vertex | Fragment | Renderer |
| --- | --- | --- | --- |
| Solid world | `cube.vert` | `cube.frag` | `Renderer` |
| Water | `water.vert` | `water.frag` | `WaterRenderer` |
| UI | `ui.vert` | `ui.frag` | `UIRenderer` |

`ClientGameLogic.init` reads shader files with `Resource.loadText` and constructs `ShaderProgram`. Renderer code sets uniforms. Editing a shader requires restarting; hot reload is not implemented.

### Safe shader edit

1. Match vertex attributes to the mesh layout in [5. Rendering](rendering-pipeline.md).
2. Match vertex outputs and fragment inputs by name and type.
3. For every uniform, find the renderer code that sets it.
4. Keep blend/depth behavior in the matching Java render pass.
5. Run and check compile/link messages in the log.

The solid fragment shader currently combines texture color, voxel light, directional light, emissive tint, and fog. Do not assume changing a GLSL uniform alone changes behavior; Java must supply its value.

## Add a texture

1. Put a `.png`, `.jpg`, or `.jpeg` in `assets/textures/`.
2. Use the block/model name as the filename. For `granite`, use `granite.png`.
3. Keep the image compatible with the atlas cell size configured in `ClientGameLogic.init` (currently 32 pixels).
4. Rebuild/restart and inspect the atlas UV debug output.

`AtlasBuilder` scans the texture folder, keys entries by filename without extension, packs them into a 1600 × 1600 atlas, and stores UV rectangles. Packing order follows filesystem enumeration, so UV coordinates are generated at startup and should not be saved as constants.

## Add a block model

`Block("granite")` loads `assets/models/granite.json`. `ModelLoader` deserializes JSON into `Model`; `MeshConverter` turns `elements`, face UVs, and texture references into meshes. Model coordinates use 0–16 units and are divided by 16 for a one-block model.

Minimal generic model (all faces use the `granite` texture):

```json
{
  "type": "generic",
  "textures": { "0": "granite" },
  "elements": [{
    "from": [0, 0, 0], "to": [16, 16, 16],
    "faces": {
      "north": { "uv": [0, 0, 16, 16], "texture": "#0" },
      "south": { "uv": [0, 0, 16, 16], "texture": "#0" },
      "east":  { "uv": [0, 0, 16, 16], "texture": "#0" },
      "west":  { "uv": [0, 0, 16, 16], "texture": "#0" },
      "up":    { "uv": [0, 0, 16, 16], "texture": "#0" },
      "down":  { "uv": [0, 0, 16, 16], "texture": "#0" }
    }
  }]
}
```

Current converter limitations: generic elements resolve `textures["0"]`; full external model texture indirection/rotation semantics are not supported. Chunk meshing also looks up atlas UV by block name and face UV from the first model element. Keep those names and definitions aligned. Compare with [`dirt.json`](../assets/models/dirt.json).

## Runtime paths

Paths are relative to the project directory (`assets/...`, `shaders/...`). Gradle copies `assets/` and `shaders/` into the build directory. Edit source assets, not generated copies under `build/`.

## Usage example (pseudocode): add a textured block

**Pseudocode only.** It shows the asset-to-runtime relationship, not a complete JSON file.

```text
assets/textures/moss_stone.png
assets/models/moss_stone.json:
    model texture key "0" → "moss_stone"
    each face → UV rectangle + texture reference "#0"

Blocks.MOSS_STONE = supplier for Block("moss_stone")
Startup:
    create texture atlas
    register MOSS_STONE before world generation
```

The block name, atlas filename, model texture key, and face UVs must agree with what `ChunkMesher` and `MeshConverter` look up.

[← Rendering](rendering-pipeline.md) · **6 / 10** · [Next: API reference →](api-reference.md)

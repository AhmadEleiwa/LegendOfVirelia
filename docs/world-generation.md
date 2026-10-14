# 3. World generation

[← Previous: Client/server](client-server-world-state.md) · **3 / 10** · [Next: World and blocks →](world-and-rendering.md)

## Chunk generation flow

```text
player position → target chunk → schedule nearby coordinates by distance
               → worker fills Chunk data → completion queue
               → server installs result → sends chunk → client builds mesh
```

`World` owns `WorldGenerator(4000)`, a worker pool capped at two threads, pending keys, and a completion queue. Workers create block/fluid data only. `collectGeneratedChunks(maximum)` installs results on the world owner thread. The server collects up to four chunks and sends up to two new chunks per tick.

The current view distance is 6 chunks in each X/Z direction. The first `ClientReady` command calls `generateInitialChunks()` around chunk (0,0); later `GenerateNewChunks` requests follow the player.

## Terrain algorithm

`WorldGenerator` combines seeded FastNoiseLite fields:

| Field | Effect |
| --- | --- |
| Terrain and hill | Broad land height and rolling variation |
| Biome | Plains, desert, mountain selection |
| Ridge/detail | Mountain peaks and small-scale variation |
| Warped river/lake | Lowers basins and selects water areas |
| 3D cave/tunnel | Carves chambers, passages, cave mouths |

For each world X/Z column, generation calculates terrain and water heights, then fills vertical layers: bedrock, biome surface, filler, and deep rock. Water above the terrain gets max level with `source=false`, so generated water is static. Player-placed fluid flow is handled later by `World`/`FluidBlock`.

Block IDs must be registered before generation. `BiomeConfig` stores IDs for surface/filler blocks; adding or changing registration order can change runtime IDs.

## Safely modify terrain

1. Change one noise scale, threshold, or height contribution at a time.
2. Base sampling on **world coordinates**, not local chunk coordinates, so adjacent chunks meet.
3. Keep returned heights within `0 .. Chunk.SIZE_Y - 1` (chunk height is 96).
4. Coordinate river/lake bed heights with `WATER_LEVEL`.
5. Inspect neighboring chunks, negative coordinates, biome edges, caves, and water boundaries.

Example: add a new layer in `generateChunk` after registering its block:

```java
if (y == BEDROCK_LEVEL)       blockId = bedrockId;
else if (y == surfaceY)       blockId = biome.surfaceBlock;
else if (y >= surfaceY - 6)   blockId = biome.fillerBlock;
else                          blockId = deepRockId;
```

Do not create meshes or call OpenGL from `WorldGenerator`; it should produce data only. Rendering is covered in [5. Rendering and meshes](rendering-pipeline.md).

### Configuration points

- Seed: `new WorldGenerator(4000)` in `World`.
- View distance: `World.viewDistance`.
- Worker count: `World` constructor.
- Install/send budgets: `ServerWorldState.update`.

There is no complete save/load system yet. Store the generator seed and player edits if persistence is added.

## Usage example (pseudocode): add a biome

**Pseudocode only.** This sketches the changes across biome selection and chunk filling; adapt names and noise thresholds to the existing `WorldGenerator`.

```text
1. Register the biome's surface and filler blocks before generation.
2. Add a BiomeConfig entry using those registered block IDs.
3. In WorldGenerator.getBiomeAt(worldX, worldZ):
       if crystalNoise(worldX, worldZ) > threshold:
           return CRYSTAL_FOREST
4. In generateChunk(...):
       biome = getBiomeAt(worldX, worldZ)
       top layer       = biome.surfaceBlock
       shallow layers  = biome.fillerBlock
       deeper layers   = rockBlockId
```

To customize a current biome, change its selection threshold or its surface/filler IDs. `BiomeConfig` is currently an enum, and its IDs are resolved from `BlockRegistry`; register blocks before the enum is first used. Keep selection based on world coordinates so biome borders line up across chunks.

[← Client/server](client-server-world-state.md) · **3 / 10** · [Next: World and blocks →](world-and-rendering.md)

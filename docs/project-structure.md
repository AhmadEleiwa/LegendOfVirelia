# 10. Project structure and workflow

[← Previous: UI and launcher](ui-and-launcher.md) · **10 / 10** · [Back to README ↑](../README.md)

## Folder map

```text
src/main/java/org/engine/       loop, input, window, render helpers
src/main/java/org/game/         world, entities, meshes, lighting, UI
src/main/java/org/legendofvirelia/ game startup, client/server, commands
src/main/resources/               Java resources (manifest metadata)
launcher/src/main/java/         desktop launcher
gradle/                          Gradle wrapper support
assets/models/                  JSON block models
assets/textures/                block and water textures
shaders/                        GLSL cube, water, and UI programs
fonts/                          UI font
screenshots/                    project screenshots
build.gradle                    dependencies and build/copy tasks
```

`build/`, `.gradle/`, and `bin/` are generated or local working folders. Edit source under `src/`, `launcher/src/`, `assets/`, and `shaders/` instead.

## Build and run

Use JDK 21 or newer for the game (the source uses Java 21 APIs). The launcher source targets Java 17.

```powershell
./gradlew.bat build
./gradlew.bat run
./gradlew.bat launcherJar
```

Gradle copies source assets and shaders into `build/`, runtime libraries into `build/lib`, and builds the launcher. Runtime paths are relative to the project directory; launch from the root or configure the launcher to use that directory.

## Where to edit

| Goal | Start here |
| --- | --- |
| Input, HUD, client behavior | `ClientGameLogic.java` |
| World/block behavior | `World.java`, `Chunk.java`, `Block.java` |
| Terrain | `WorldGenerator.java` |
| Chunk geometry/draw | `ChunkMesher.java`, `WorldRenderer.java`, `Renderer.java` |
| Server/client updates | `WorldState.java`, side-specific state, `commands/` |
| Models and textures | `assets/models/`, `assets/textures/` |
| Shader changes | `shaders/` and matching renderer |

For reusable class contracts, start with [7. API and class reference](api-reference.md). For the full learning sequence, return to the [README](../README.md).

## Ready-to-share change checklist

- [ ] Build succeeds with the documented JDK.
- [ ] Run and inspect visual/input changes.
- [ ] New resources load from the project root and packaged build.
- [ ] Related guide, controls, and screenshots are updated.
- [ ] Generated `build/` files are not used as source edits.

## Usage example (pseudocode): make a small engine fork

**Pseudocode only.** Use this as a first-pass map, then follow the guides for each subsystem.

```text
1. Keep engine loop/window/input code in org.engine.
2. Put reusable world, entity, mesh, and UI code in org.game.
3. Put game-specific startup and rules in your own application package.
4. Implement ClientSide and start it with GameLoop.
5. Add assets under assets/ and shaders under shaders/.
6. Add commands/world state only if your game needs authority or synchronization.
```

[← UI and launcher](ui-and-launcher.md) · **10 / 10** · [Back to README ↑](../README.md)

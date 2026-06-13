// org/legendofvirelia/client/ClientGameLogic.java
package org.legendofvirelia.client;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.util.List;

import org.engine.io.Input;
import org.engine.io.Window;
import org.engine.loop.ClientSide;
import org.engine.rendering.Renderer;
import org.engine.rendering.ShaderProgram;
import org.engine.rendering.UIRenderer;
import org.engine.utils.Debug;
import org.engine.utils.Logger;
import org.engine.utils.Resource;
import org.game.core.GameObject;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;

import org.game.ui.ColorRect;
import org.game.ui.Container;
import org.game.ui.Label;
import org.game.utils.AtlasBuilder;
import org.game.world.Block;
import org.game.world.BlockPlacer;
import org.game.world.BlockRegistry;
import org.game.world.Blocks;
import org.game.world.WorldRenderer;
import org.joml.Vector2f;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.commands.BreakBlockCommand;
import org.legendofvirelia.shared.commands.GenerateNewChunks;
import org.legendofvirelia.shared.commands.PlaceBlockCommand;

public class ClientGameLogic implements ClientSide {

    private final ClientWorldState worldState;
    private Renderer renderer;
    private UIRenderer uiRenderer;
    private Camera camera;
    public float timer = 0;
    public float cooldown = 0.1f; // Fast response for building
    private List<GameObject> objects;
    private Container ui;
    // Building system state
    private int selectedBlockId = 1;
    private boolean buildMode = true;
    private GameObject cube;
    private float time = 0;
    private Window window;
    ShaderProgram uishader;
    DirectionalLight sun;

    private Vector2f prev_pos = new Vector2f(0, 0);

    public ClientGameLogic(ClientWorldState state) {
        this.worldState = state;
    }

    @Override
    public void init(Window window) {
        this.window = window;
        camera = new Camera((float) (window.getWidth()) / (float) (window.getHeight()));
        prev_pos = new Vector2f(camera.position.x, camera.position.z);
        var vs = Resource.loadText("shaders/cube.vert");
        var fs = Resource.loadText("shaders/cube.frag");
        Debug.log("Client initialization");
        var uiv = Resource.loadText("shaders/ui.vert");
        var uif = Resource.loadText("shaders/ui.frag");
        try {
            AtlasBuilder atlas = AtlasBuilder.create("assets/textures", 1600, 32);
            Debug.log("Atlas UVs: " + atlas.getAllUVs().keySet());
        } catch (IOException e) {
            Logger.log("Failed to create texture atlas", e);
        }

        if (vs == null || fs == null) {
            return;
        }

        Block block = Blocks.DIRT.get();

        objects = List.of(block);
        ShaderProgram shader = new ShaderProgram(vs, fs);
        Debug.log("UI shaders loaded: " + (uiv != null && uif != null));
        uishader = new ShaderProgram(uiv, uif);

        // Create a semi-transparent red rectangle for UI
        // healthBar =

        ui = new Container(0, 80, 180, 600);
        ui.addChild(new ColorRect(10, 10, 200, 30, 1f, 0f, 0f, 1f));
        // Position and scale the UI rectangle
        Label label = new Label(10, 10, 200, 30, "Health: 100%", 16);
        ui.addChild(label);
        renderer = new Renderer(shader);
        uiRenderer = new UIRenderer(uishader);
        BlockRegistry.register("dirt", Blocks.DIRT.get());
        BlockRegistry.register("torch", Blocks.Torch.get());
        BlockRegistry.register("dirt2", Blocks.DIRT2.get());
        BlockRegistry.register("water", Blocks.WATER.get());

        sun = DirectionalLight.createSunlight();
        worldState.init();

    }

    @Override
    public void input(Window window) {

        if (Input.isKeyPressed(Input.KEY_F11)) {
            window.toggleFullScreen();
        }
        if (Input.isKeyPressed(Input.KEY_ESCAPE)) {
            window.setShouldClose(true);
        }
        if (Input.isKeyReleased(Input.KEY_Y)) {
            Debug.enable = !Debug.enable;
        }

        if (Input.isKeyPressed(Input.KEY_B)) {
            buildMode = !buildMode;
            Debug.log("Build mode: " + (buildMode ? "PLACE blocks" : "BREAK blocks"));
        }

        if (Input.isKeyPressed(Input.KEY_P)) {
            ui.setPosition(ui.getX() + 5, ui.getY());
        }
        if (Input.isKeyPressed(Input.KEY_O)) {
            ui.setSize(ui.getWidth() - 1, ui.getHeight());
        }
        if (Input.isKeyPressed(Input.KEY_L)) {
            ui.setSize(ui.getWidth() + 1, ui.getHeight());
        }
        handleBuildingInput();
    }

    private void handleBuildingInput() {
        // Cooldown to prevent spam clicking
        if (timer < cooldown) {
            return;
        }

        if (Input.isMouseButtonPressed(Input.MOUSE_RIGHT)) {
            if (buildMode) {
                placeBlock();
                timer = 0;
            } else {
                breakBlock();
                timer = 0;
            }
        }

        if (Input.isMouseButtonPressed(Input.MOUSE_LEFT)) {
            if (buildMode) {
                breakBlock();
                timer = 0;
            } else {
                placeBlock();
                timer = 0;
            }
        }
    }

    @Override
    public void placeBlock() {
        BlockPlacer.RaycastResult result = BlockPlacer.raycast(worldState.getCurrentWorld(), camera, 5f);

        if (result.hit && result.placePosition != null) {
            // Use the new client-side prediction system
            Debug.log(BlockRegistry.getId("dirt"));
            Debug.log(BlockRegistry.getId("torch"));

            PlaceBlockCommand action = new PlaceBlockCommand(result.placePosition, BlockRegistry.getId("torch"));
            worldState.sendCommand(action);
            Debug.log("Block placed immediately with client-side prediction: " + result.placePosition);
        } else {
            Debug.log("No valid placement position found");
        }
    }

    private void breakBlock() {
        BlockPlacer.RaycastResult result = BlockPlacer.raycast(worldState.getCurrentWorld(), camera, 5f);

        if (result.hit && result.blockPosition != null) {
            // Break block by placing air (blockId = 0)
            BreakBlockCommand action = new BreakBlockCommand(result.blockPosition);
            worldState.sendCommand(action);
            Debug.log("Block broken immediately with client-side prediction: " + result.blockPosition);
        }
    }

    @Override
    public void update(float delta) {
        camera.update(delta);
        worldState.update(delta); // Handles client-side prediction and server reconciliation
        timer += delta;
        // Update UI elements if needed
        time = time == 1 ? 0 : Math.clamp(time + delta / 100f, 0f, 1f);
        sun.updateForTimeOfDay(time);
        int currentChunkX = (int) Math.floor(camera.position.x / 16.0);
        int currentChunkZ = (int) Math.floor(camera.position.z / 16.0);
        
        int prevChunkX = (int) Math.floor(prev_pos.x / 16.0);
        int prevChunkZ = (int) Math.floor(prev_pos.y / 16.0); // Storing Z inside Vector2f.y

        // Trigger only if the player actually crosses a chunk border threshold on either axis!
        if (currentChunkX != prevChunkX || currentChunkZ != prevChunkZ) {
            worldState.sendCommand(new GenerateNewChunks(camera.position));
            
            // Save the exact chunk midpoint coordinate to prevent duplicate spam triggers
            prev_pos.x = camera.position.x;
            prev_pos.y = camera.position.z; 
        }

        // Run your staggered 1-to-2 mesh builder here per frame budget limits 
        // to keep rendering at a locked, buttery-smooth framerate!
        // WorldRenderer.generateVisibleMeshes(worldState.getCurrentWorld(), camera.position);

    }

    @Override
    public void render() {
        // 1. Clear the screen at the start of the frame
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 2. Render 3D world
        renderer.render(objects, camera, sun);
        worldState.render(renderer, camera, sun);
        // 3. Render 2D UI on top
        renderBuildingUI();
    }

    private void renderBuildingUI() {
        // Render UI elements
        // uiRenderer.render(healthBar, 1280, 720);
        ui.draw(uiRenderer, 1280, 720);
    }

    @Override
    public void cleanup() {
        // Cleanup resources

    }
}
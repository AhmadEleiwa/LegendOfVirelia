// org/legendofvirelia/client/ClientGameLogic.java
package org.legendofvirelia.client;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.engine.io.Input;
import org.engine.io.Window;
import org.engine.loop.ClientSide;
import org.engine.rendering.Renderer;
import org.engine.rendering.ShaderProgram;
import org.engine.rendering.UIRenderer;
import org.engine.rendering.WaterRenderer;
import org.engine.utils.Debug;
import org.engine.utils.Logger;
import org.engine.utils.Resource;
import org.game.core.GameObject;
import org.game.entities.Camera;
import org.game.entities.PlayerPhysics;
import org.game.lighting.DirectionalLight;

import org.game.ui.ColorRect;
import org.game.ui.Label;
import org.game.ui.UIElement;
import org.game.utils.AtlasBuilder;
import org.game.world.Block;
import org.game.world.BlockPlacer;
import org.game.world.BlockRegistry;
import org.game.world.Blocks;
import org.game.world.WorldRenderer;
import org.joml.Vector2f;
import org.joml.Vector3i;
import org.legendofvirelia.shared.ClientWorldState;
import org.legendofvirelia.shared.commands.BreakBlockCommand;
import org.legendofvirelia.shared.commands.GenerateNewChunks;
import org.legendofvirelia.shared.commands.PlaceBlockCommand;

public class ClientGameLogic implements ClientSide {

    private final ClientWorldState worldState;
    private Renderer renderer;
    private UIRenderer uiRenderer;
    private WaterRenderer waterRenderer;
    private Camera camera;
    private PlayerPhysics playerPhysics;
    public float timer = 0;
    public float cooldown = 0.1f; // Fast response for building
    private List<GameObject> objects;
    private final List<UIElement> hudElements = new ArrayList<>();
    private Label statsLabel;
    private Label hotbarLabel;
    private ColorRect statsBackground;
    private ColorRect crosshairHorizontal;
    private ColorRect crosshairVertical;
    private ColorRect crosshairHorizontalCore;
    private ColorRect crosshairVerticalCore;
    private ColorRect[] hotbarSlots;
    private ColorRect[] hotbarSwatches;
    private ColorRect[] selectedSlotBorder;
    private final int[] hotbarBlockIds = new int[4];
    private final String[] hotbarBlockNames = {"DIRT", "DIRT2", "TORCH", "WATER"};
    private final float[][] hotbarColors = {
            {0.45f, 0.29f, 0.16f},
            {0.42f, 0.43f, 0.46f},
            {0.95f, 0.58f, 0.12f},
            {0.10f, 0.58f, 0.78f}
    };
    private int selectedHotbarIndex;
    private int hudWidth;
    private int hudHeight;
    private float fpsElapsed;
    private int fpsFrames;
    private float hudRefreshElapsed;
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
        playerPhysics = new PlayerPhysics(worldState.getCurrentWorld());
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

        renderer = new Renderer(shader);
        uiRenderer = new UIRenderer(uishader);
        BlockRegistry.register("dirt", Blocks.DIRT.get());
        BlockRegistry.register("torch", Blocks.Torch.get());
        BlockRegistry.register("dirt2", Blocks.DIRT2.get());
        Block water = Blocks.WATER.get();
        water.setTransparent(true);
        BlockRegistry.register("water", water);
        hotbarBlockIds[0] = BlockRegistry.getId("dirt");
        hotbarBlockIds[1] = BlockRegistry.getId("dirt2");
        hotbarBlockIds[2] = BlockRegistry.getId("torch");
        hotbarBlockIds[3] = BlockRegistry.getId("water");
        selectedBlockId = hotbarBlockIds[selectedHotbarIndex];
        createHud();

        sun = DirectionalLight.createSunlight();
        waterRenderer = new WaterRenderer();
        WorldRenderer.setWaterRenderer(waterRenderer);
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
        if (Input.isKeyPressed(Input.KEY_R)) {
            camera.reset();
            playerPhysics.reset();
        }

        if (Input.isKeyPressed(Input.KEY_B)) {
            buildMode = !buildMode;
            Debug.log("Build mode: " + (buildMode ? "PLACE blocks" : "BREAK blocks"));
        }

        if (Input.isKeyPressed(Input.KEY_1)) selectHotbarSlot(0);
        if (Input.isKeyPressed(Input.KEY_2)) selectHotbarSlot(1);
        if (Input.isKeyPressed(Input.KEY_3)) selectHotbarSlot(2);
        if (Input.isKeyPressed(Input.KEY_4)) selectHotbarSlot(3);
        handleBuildingInput();
    }

    private void createHud() {
        statsBackground = new ColorRect(8, 8, 430, 30, 0.02f, 0.03f, 0.05f, 0.72f);
        statsLabel = new Label(8, 8, 430, 30, "", 14);
        crosshairHorizontal = new ColorRect(0, 0, 20, 4, 0f, 0f, 0f, 0.75f);
        crosshairVertical = new ColorRect(0, 0, 4, 20, 0f, 0f, 0f, 0.75f);
        crosshairHorizontalCore = new ColorRect(0, 0, 14, 2, 1f, 1f, 1f, 0.95f);
        crosshairVerticalCore = new ColorRect(0, 0, 2, 14, 1f, 1f, 1f, 0.95f);
        hudElements.add(statsBackground);
        hudElements.add(statsLabel);
        hudElements.add(crosshairHorizontal);
        hudElements.add(crosshairVertical);
        hudElements.add(crosshairHorizontalCore);
        hudElements.add(crosshairVerticalCore);

        hotbarSlots = new ColorRect[hotbarBlockIds.length];
        hotbarSwatches = new ColorRect[hotbarBlockIds.length];
        selectedSlotBorder = new ColorRect[4];
        for (int i = 0; i < hotbarBlockIds.length; i++) {
            hotbarSlots[i] = new ColorRect(0, 0, 48, 48, 0.04f, 0.05f, 0.07f, 0.88f);
            hotbarSwatches[i] = new ColorRect(0, 0, 28, 28,
                    hotbarColors[i][0], hotbarColors[i][1], hotbarColors[i][2], 1f);
            hudElements.add(hotbarSlots[i]);
            hudElements.add(hotbarSwatches[i]);
        }
        for (int i = 0; i < selectedSlotBorder.length; i++) {
            selectedSlotBorder[i] = new ColorRect(0, 0, 48, 3, 1f, 0.82f, 0.25f, 1f);
            hudElements.add(selectedSlotBorder[i]);
        }
        hotbarLabel = new Label(0, 0, 500, 22, "", 14);
        hudElements.add(hotbarLabel);
        layoutHud(window.getWidth(), window.getHeight());
        updateHotbarLabel();
    }

    private void selectHotbarSlot(int index) {
        if (index < 0 || index >= hotbarBlockIds.length || selectedHotbarIndex == index)
            return;
        selectedHotbarIndex = index;
        selectedBlockId = hotbarBlockIds[index];
        updateHotbarLabel();
        updateSelectedSlotBorder();
    }

    private void updateHotbarLabel() {
        if (hotbarLabel != null) {
            hotbarLabel.setText("SELECTED: " + hotbarBlockNames[selectedHotbarIndex]
                    + "     1 DIRT  2 DIRT2  3 TORCH  4 WATER");
        }
    }

    private void layoutHud(int width, int height) {
        if (width == hudWidth && height == hudHeight)
            return;
        hudWidth = width;
        hudHeight = height;

        int centerX = width / 2;
        int centerY = height / 2;
        crosshairHorizontal.setPosition(centerX - 10, centerY - 2);
        crosshairVertical.setPosition(centerX - 2, centerY - 10);
        crosshairHorizontalCore.setPosition(centerX - 7, centerY - 1);
        crosshairVerticalCore.setPosition(centerX - 1, centerY - 7);

        int slotWidth = 48;
        int slotGap = 6;
        int totalWidth = hotbarBlockIds.length * slotWidth
                + (hotbarBlockIds.length - 1) * slotGap;
        int firstSlotX = centerX - totalWidth / 2;
        int slotY = height - 68;
        for (int i = 0; i < hotbarBlockIds.length; i++) {
            int slotX = firstSlotX + i * (slotWidth + slotGap);
            hotbarSlots[i].setPosition(slotX, slotY);
            hotbarSwatches[i].setPosition(slotX + 10, slotY + 10);
        }

        updateSelectedSlotBorder(firstSlotX, slotY, slotWidth, slotGap);
        hotbarLabel.setPosition(centerX - 250, slotY - 25);
    }

    private void updateSelectedSlotBorder() {
        int slotWidth = 48;
        int slotGap = 6;
        int totalWidth = hotbarBlockIds.length * slotWidth
                + (hotbarBlockIds.length - 1) * slotGap;
        updateSelectedSlotBorder((hudWidth - totalWidth) / 2, hudHeight - 68, slotWidth, slotGap);
    }

    private void updateSelectedSlotBorder(int firstSlotX, int slotY, int slotWidth, int slotGap) {
        int selectedX = firstSlotX + selectedHotbarIndex * (slotWidth + slotGap);
        selectedSlotBorder[0].setPosition(selectedX, slotY);
        setSizeIfChanged(selectedSlotBorder[0], slotWidth, 3);
        selectedSlotBorder[1].setPosition(selectedX, slotY + slotWidth - 3);
        setSizeIfChanged(selectedSlotBorder[1], slotWidth, 3);
        selectedSlotBorder[2].setPosition(selectedX, slotY);
        setSizeIfChanged(selectedSlotBorder[2], 3, slotWidth);
        selectedSlotBorder[3].setPosition(selectedX + slotWidth - 3, slotY);
        setSizeIfChanged(selectedSlotBorder[3], 3, slotWidth);
    }

    private static void setSizeIfChanged(UIElement element, int width, int height) {
        if (element.getWidth() != width || element.getHeight() != height)
            element.setSize(width, height);
    }

    private void updateHud(float delta) {
        fpsElapsed += delta;
        fpsFrames++;
        hudRefreshElapsed += delta;
        if (hudRefreshElapsed < 0.25f)
            return;

        int fps = fpsElapsed > 0f ? Math.round(fpsFrames / fpsElapsed) : 0;
        fpsElapsed = 0f;
        fpsFrames = 0;
        hudRefreshElapsed = 0f;

        Runtime runtime = Runtime.getRuntime();
        long usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxRamMb = runtime.maxMemory() / (1024 * 1024);
        statsLabel.setText(String.format(Locale.ROOT,
                "FPS %d   XYZ %.1f, %.1f, %.1f   RAM %d/%d MB",
                fps, camera.position.x, camera.position.y, camera.position.z,
                usedRamMb, maxRamMb));
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
            // Use the new client-side prediction system
            PlaceBlockCommand action = new PlaceBlockCommand(targetPosition, selectedBlockId);
            worldState.sendCommand(action);
            camera.triggerImpact(0.65f);
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
            camera.triggerImpact(0.45f);
        }
    }

    @Override
    public void update(float delta) {
        camera.update(delta);
        worldState.update(delta); // Handles client-side prediction and server reconciliation
        playerPhysics.update(camera, delta);
        camera.smoothViewPosition(delta);
        updateHud(delta);
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
        // renderer.render(objects, camera, sun);
        worldState.render(renderer, camera, sun);
        // 3. Render 2D UI on top
        renderBuildingUI();

        // Debug.log(GL_2D);

    }

    private void renderBuildingUI() {
        layoutHud(window.getWidth(), window.getHeight());
        for (UIElement element : hudElements)
            element.draw(uiRenderer, window.getWidth(), window.getHeight());
    }

    @Override
    public void cleanup() {
        for (UIElement element : hudElements)
            element.destroy();
        if (waterRenderer != null)
            waterRenderer.delete();
    }
}
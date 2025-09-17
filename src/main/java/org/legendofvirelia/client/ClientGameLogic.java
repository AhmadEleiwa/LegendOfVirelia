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
import org.engine.utils.Debug;
import org.engine.utils.Logger;
import org.engine.utils.Resource;
import org.game.core.GameObject;
import org.game.entities.Camera;
import org.game.utils.AtlasBuilder;
import org.game.world.Block;
import org.game.world.BlockPlacer;
import org.game.world.BlockRegistry;
import org.game.world.Blocks;
import org.game.world.World;
import org.joml.Vector3i;

import org.legendofvirelia.shared.WorldState;

public class ClientGameLogic implements ClientSide {

    private final WorldState worldState;
    private List<GameObject> objects;
    private Renderer renderer;
    private Camera camera = new Camera(800f / 600f); // aspect ratio
    private World world;
    private BlockPlacer blockPlacer;
    public float timer = 0;
    public float cooldown = 10;
 
    // Building system state
    private int selectedBlockId = 1; // Default to dirt
    private boolean buildMode = true; // true = place blocks, false = break blocks
    public ClientGameLogic(WorldState state){
        this.worldState = state;
    }
    @Override
    public void init() {
        // Load resources, initialize objects

        var vs = Resource.loadText("shaders/cube.vert");
        var fs = Resource.loadText("shaders/cube.frag");
        Debug.log("initil;ization");
        try {
             AtlasBuilder atlas = AtlasBuilder.create("assets/textures",1600, 16);
             Debug.log("Atlas UVs: " + atlas.getAllUVs().keySet());
        } catch (IOException e) {
            Logger.log("Failed to create texture atlas", e);
        }
        
        if (vs == null || fs == null ) {
            return;
        }
        
        ShaderProgram shader = new ShaderProgram(vs, fs);
        renderer = new Renderer(shader);
        BlockRegistry.register("dirt", Blocks.DIRT.get());
        
        // Create the world and building system
        world = new World();
        blockPlacer = new BlockPlacer(world);
        
        Debug.log("World initialized with " + world.getChunks().size() + " chunks");
        Debug.log("Building system initialized - Left click: place, Right click: break");
        
        // Create some additional entities for testing (optional)
        Block obj1 = Blocks.DIRT.get();
        obj1.position.set(10, 5, 10);

        Block obj2 = Blocks.DIRT.get();
        obj2.position.set(15, 5, 10);
        
        objects = List.of(obj1, obj2);
    }

    @Override
    public void input(Window window) {
        // Basic controls
        if (Input.isKeyPressed(Input.KEY_ESCAPE)) {
            window.setShouldClose(true);
        }
        if (Input.isKeyReleased(Input.KEY_Y)) {
            Debug.enable = !Debug.enable;
        }
    
        
        // Building mode toggle
        if (Input.isKeyPressed(Input.KEY_B)) {
            buildMode = !buildMode;
            Debug.log("Build mode: " + (buildMode ? "PLACE blocks" : "BREAK blocks"));
        }
        

        // Mouse building controls
        handleBuildingInput();
    }
    
    private void handleBuildingInput() {
        // Left mouse button - primary action
        if(timer>cooldown){
            timer =0;
        }
        else{
            return;
        }
        if (Input.isMouseButtonPressed(Input.MOUSE_RIGHT) ) {
            
            if (buildMode) {
                placeBlock();
            } else {
    
                breakBlock();
            }
        }
        
        // Right mouse button - secondary action (opposite of current mode)
        if (Input.isMouseButtonPressed(Input.MOUSE_LEFT)) {
            if (buildMode) {
                breakBlock();
            } else {
                placeBlock();
            }
        }
    }
    
    private void placeBlock() {
        BlockPlacer.RaycastResult result = blockPlacer.raycast(camera);
    
        if (result.hit && result.placePosition != null) {
            boolean success = blockPlacer.placeBlock(result.placePosition, selectedBlockId);
            if (success) {
                Vector3i pos = result.placePosition;
                Debug.log("Placed " + getBlockName(selectedBlockId) + " at (" + 
                         pos.x + ", " + pos.y + ", " + pos.z + ")");
            }else{
                System.out.println("noo waay");
            }
        }
        else{
            System.out.println("idk");
        }
    }
    
    private void breakBlock() {
        BlockPlacer.RaycastResult result = blockPlacer.raycast(camera);
        
        if (result.hit && result.blockPosition != null) {
            boolean success = blockPlacer.breakBlock(result.blockPosition);
            if (success) {
                Vector3i pos = result.blockPosition;
                Debug.log("Broke block at (" + pos.x + ", " + pos.y + ", " + pos.z + ")");
            }
        }
    }
    
    private String getBlockName(int blockId) {
        // Add more block types as you register them
        switch (blockId) {
            case 1: return "Dirt";
            case 2: return "Stone";
            case 3: return "Wood";
            case 4: return "Grass";
            // Add more cases for other blocks
            default: return "Unknown";
        }
    }

    @Override
    public void update(float delta) {
        camera.update(delta);
        world.update(delta);
        timer+= delta;
    }

    @Override
    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        
        // Enable backface culling for better performance
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        if (renderer == null)
            return;
        
        renderer.render(objects, camera);
        world.render(renderer, camera);
        
        // Optional: Render crosshair or building UI here
        renderBuildingUI();
    }
    
    private void renderBuildingUI() {
        // This is where you'd render:
        // - Crosshair in center of screen
        // - Currently selected block indicator
        // - Build mode indicator
        // For now, we just use debug output
    }

    @Override
    public void cleanup() {
        if (world != null) {
            // Add cleanup logic for world if needed
        }
    }
}

package org.game.world;

import org.engine.utils.Logger;
import org.game.core.GameObject;
import org.game.meshes.Mesh;
import org.game.meshes.Model;
import org.game.meshes.ModelLoader;



public class Block extends GameObject {
    public static final float SIZE = 1.0f;
    protected Model model;
    protected boolean isTransparent;
    protected int lightLevel;
    private static int idCounter = 1;
     
    
    /*
     * Constructor for a Block object, which is a cube with predefined size.
     */
    public Block() {
        // model = new Model(Mesh.createCube(SIZE), new HashMap<>());
         // Use
    }

    /**
     * Constructor for a GameObject with a custom mesh.
     *
     * @param mesh The custom Mesh object.
     */
    public Block(Mesh mesh) {
        this.type = "custom";
        this.name = "custom_"+idCounter++;
        // Create a model with the custom mesh and no textures
        // this.model = new Model(mesh, new HashMap<>());
    }

    /**
     * Constructor for a GameObject loaded from a JSON file.
     *
     * @param type      The type of the object.
     * @param modelName The name of the model file (without extension).
     */
    public Block(String modelName) {
        this.type = modelName;
        this.name = modelName;
        // Load the JSON data
        Model model= ModelLoader.loadModel("assets/models/" + modelName + ".json");
        
        // Convert the JSON data into a MeshData object
        // ArrayList<Mesh> meshes = MeshConverter.processModel(modelJson);
        if(model==null){
            Logger.log("Failed to load model: " + modelName);
            return;
        }
        this.model = model;
        // Create a Model with the newly created mesh and the textures from the JSON
        // this.model = new Model(meshes, modelJson.textures);
    }
    public Block(String name, int lightLevel){
        this(name);
        this.lightLevel = lightLevel;
    }
    public String toString() {
        return "Block[type=" + type + "]";
    }

    public Model getModel() {
        return model;
    }

    @Override
    public void render() {
        if (model != null) {
            model.draw();
        }
    }

    @Override
    public void update(float delta) {
        // Blocks are static; no update logic needed

    }

    @Override
    public void delete() {
        if (model != null) {
            model.delete();
        }
    }
        public boolean isTransparent() {
        return isTransparent;
    }

    public void setTransparent(boolean isTransparent) {
        this.isTransparent = isTransparent;
    }
    public int getLightLevel(){
        return this.lightLevel;
    }
}

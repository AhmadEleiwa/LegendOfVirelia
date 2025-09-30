package org.game.meshes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.engine.utils.Debug;

import org.game.utils.AtlasBuilder;


public class Model {
    // A model can be composed of multiple meshes
    private ArrayList<Mesh> meshes = new ArrayList<>();
    // And it has a map of textures used by its meshes
    public String parent;
    public String type;
    public Map<String, String> textures;
    public List<Element> elements;
    public static class Element {
        public float[] from;
        public float[] to;
        public Map<String, Face> faces;
    }

    public static class Face {
        public float[] uv;
        public String texture;
    }
    public Model(Mesh mesh) {
        this.meshes.add(mesh);
       
    }

    public void initializeMeshes() {
        ArrayList<Mesh> mesh= MeshConverter.processModel(this);
        if(this.meshes == null){
            this.meshes = new ArrayList<>();
        }
        if(mesh!=null){
            this.meshes.addAll(mesh);
            Debug.log("Initialized model with " + mesh.size() + " meshes.");
        }

    }
    public void draw() {
        // Here, we would iterate through all meshes and draw them
        // For simplicity, we assume one mesh and bind the textures
        // The texture binding logic would be more complex in a real scenario
        // The shader should be bound and uniforms set before this call
        // For now, let's just draw the first mesh
        for (int i = 0; i < meshes.size(); i++) {
            // if (textures.containsKey(String.valueOf(i))) {
            //     textures.get(String.valueOf(i)).bind(); // Bind the correct texture
            // }
            AtlasBuilder.getDefault().getAtlasTexture().bind();
            meshes.get(i).draw();
            // textures.get(String.valueOf(i)).unbind();
        }
    }
    public void addMesh(Mesh mesh) {
        this.meshes.add(mesh);
    }
    
    public Map<String, String> getTextures() {
        return textures;
    }

    public void delete() {
        for (Mesh mesh : meshes) {
            mesh.delete();
        }
    }
    public List<Mesh> getMeshes() {
        return meshes;
    }

}
package org.game.lighting;

import org.joml.Vector3f;
import static org.lwjgl.opengl.GL20.*;

/**
 * Represents a directional light (like the sun) that illuminates everything
 * from a specific direction with parallel rays.
 */
public class PointLight {
    private Vector3f position;
    private Vector3f ambient;
    private Vector3f diffuse;

    
    /**
     * Creates a directional light with default sun-like properties.
     */
    public PointLight() {
        this.position = new Vector3f(0, 17f, 0);
        this.ambient = new Vector3f(0.2f, 0.2f, 0.2f);
        this.diffuse = new Vector3f(0.8f, 0.8f, 0.7f);
  
    }
    
    /**
     * Creates a directional light with custom properties.
     * 
     * @param direction The direction the light is pointing (will be normalized)
     * @param ambient   Ambient light color (base lighting level)
     * @param diffuse   Diffuse light color (main lighting)
     * @param specular  Specular light color (highlights)
     */
    public PointLight(Vector3f direction, Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.position = new Vector3f(direction).normalize();
        this.ambient = new Vector3f(ambient);
        this.diffuse = new Vector3f(diffuse);
    }
    
    /**
     * Uploads this directional light's properties to the shader.
     * 
     * @param shaderProgram The OpenGL shader program ID
     */
    public void uploadToShader(int shaderProgram) {
        
        // Get uniform locations
        int positionLoc = glGetUniformLocation(shaderProgram, "pointLight.position");
        int ambientLoc = glGetUniformLocation(shaderProgram, "pointLight.ambient");
        int diffuseLoc = glGetUniformLocation(shaderProgram, "pointLight.diffuse");
        
        // Upload values to shader (no specular needed for voxel games)
        glUniform3f(positionLoc, position.x, position.y, position.z);
        glUniform3f(ambientLoc, ambient.x, ambient.y, ambient.z);
        glUniform3f(diffuseLoc, diffuse.x, diffuse.y, diffuse.z);
    }
    
    // Getters
    public Vector3f getPoistion() {
        return new Vector3f(position);
    }
    
    public Vector3f getAmbient() {
        return new Vector3f(ambient);
    }
    
    public Vector3f getDiffuse() {
        return new Vector3f(diffuse);
    }
    

    // Setters
    public void setPosition(Vector3f direction) {
        this.position.set(direction).normalize();
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z).normalize();
    }
    
    public void setAmbient(Vector3f ambient) {
        this.ambient.set(ambient);
    }
    
    public void setAmbient(float r, float g, float b) {
        this.ambient.set(r, g, b);
    }
    
    public void setDiffuse(Vector3f diffuse) {
        this.diffuse.set(diffuse);
    }
    
    public void setDiffuse(float r, float g, float b) {
        this.diffuse.set(r, g, b);
    }
    
    
    /**
     * Updates the light direction based on time for day/night cycle.
     * 
     * @param timeOfDay Value between 0.0 (midnight) and 1.0 (next midnight)
     */
    
    @Override
    public String toString() {
        return String.format("DirectionalLight{poistion=%s, ambient=%s, diffuse=%s}",
                position, ambient, diffuse);
    }
}
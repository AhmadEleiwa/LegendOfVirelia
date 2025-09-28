package org.game.lighting;

import org.joml.Vector3f;
import static org.lwjgl.opengl.GL20.*;

/**
 * Represents a directional light (like the sun) that illuminates everything
 * from a specific direction with parallel rays.
 */
public class DirectionalLight {
    private Vector3f direction;
    private Vector3f ambient;
    private Vector3f diffuse;
    private Vector3f specular;
    
    /**
     * Creates a directional light with default sun-like properties.
     */
    public DirectionalLight() {
        this.direction = new Vector3f(-0.2f, -1.0f, -0.3f).normalize();
        this.ambient = new Vector3f(0.2f, 0.2f, 0.2f);
        this.diffuse = new Vector3f(0.8f, 0.8f, 0.7f);
        this.specular = new Vector3f(1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Creates a directional light with custom properties.
     * 
     * @param direction The direction the light is pointing (will be normalized)
     * @param ambient   Ambient light color (base lighting level)
     * @param diffuse   Diffuse light color (main lighting)
     * @param specular  Specular light color (highlights)
     */
    public DirectionalLight(Vector3f direction, Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.direction = new Vector3f(direction).normalize();
        this.ambient = new Vector3f(ambient);
        this.diffuse = new Vector3f(diffuse);
        this.specular = new Vector3f(specular);
    }
    
    /**
     * Uploads this directional light's properties to the shader.
     * 
     * @param shaderProgram The OpenGL shader program ID
     */
    public void uploadToShader(int shaderProgram) {
        
        // Get uniform locations
        int directionLoc = glGetUniformLocation(shaderProgram, "dirLight.direction");
        int ambientLoc = glGetUniformLocation(shaderProgram, "dirLight.ambient");
        int diffuseLoc = glGetUniformLocation(shaderProgram, "dirLight.diffuse");
        
        // Upload values to shader (no specular needed for voxel games)
        glUniform3f(directionLoc, direction.x, direction.y, direction.z);
        glUniform3f(ambientLoc, ambient.x, ambient.y, ambient.z);
        glUniform3f(diffuseLoc, diffuse.x, diffuse.y, diffuse.z);
    }
    
    // Getters
    public Vector3f getDirection() {
        return new Vector3f(direction);
    }
    
    public Vector3f getAmbient() {
        return new Vector3f(ambient);
    }
    
    public Vector3f getDiffuse() {
        return new Vector3f(diffuse);
    }
    
    public Vector3f getSpecular() {
        return new Vector3f(specular);
    }
    
    // Setters
    public void setDirection(Vector3f direction) {
        this.direction.set(direction).normalize();
    }
    
    public void setDirection(float x, float y, float z) {
        this.direction.set(x, y, z).normalize();
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
    
    public void setSpecular(Vector3f specular) {
        this.specular.set(specular);
    }
    
    public void setSpecular(float r, float g, float b) {
        this.specular.set(r, g, b);
    }
    
    /**
     * Creates a sun-like directional light.
     * 
     * @return A DirectionalLight configured like sunlight
     */
    public static DirectionalLight createSunlight() {
        return new DirectionalLight(
            new Vector3f(-0.2f, -1.0f, -0.3f),  // Coming from above and slightly angled
            new Vector3f(0.2f, 0.2f, 0.2f),     // Soft ambient light
            new Vector3f(0.8f, 0.8f, 0.7f),     // Warm white sunlight
            new Vector3f(1.0f, 1.0f, 1.0f)      // Bright white specular highlights
        );
    }
    
    /**
     * Creates a moonlight-like directional light.
     * 
     * @return A DirectionalLight configured like moonlight
     */
    public static DirectionalLight createMoonlight() {
        return new DirectionalLight(
            new Vector3f(0.3f, -0.8f, 0.5f),    // Coming from above at different angle
            new Vector3f(0.05f, 0.05f, 0.1f),   // Very dim blue ambient
            new Vector3f(0.3f, 0.3f, 0.4f),     // Cool blue moonlight
            new Vector3f(0.5f, 0.5f, 0.6f)      // Subtle blue specular
        );
    }
    
    /**
     * Updates the light direction based on time for day/night cycle.
     * 
     * @param timeOfDay Value between 0.0 (midnight) and 1.0 (next midnight)
     */
    public void updateForTimeOfDay(float timeOfDay) {
        // Convert time to angle (0 = midnight, 0.5 = noon)
        float angle = timeOfDay * 2.0f * (float) Math.PI;
        
        // Calculate sun position (sun moves in an arc)
        float sunHeight = (float) Math.sin(angle);
        float sunX = (float) Math.cos(angle) * 0.3f;
        
        // Update direction
        setDirection(sunX, -Math.abs(sunHeight), -0.3f);
        
        // Update colors based on time of day
        if (sunHeight > 0) {
            // Daytime
            float intensity = sunHeight;
            setAmbient(0.2f * intensity, 0.2f * intensity, 0.2f * intensity);
            setDiffuse(0.8f * intensity, 0.8f * intensity, 0.7f * intensity);
            setSpecular(intensity, intensity, intensity);
        } else {
            // Nighttime - moonlight
            float intensity = Math.abs(sunHeight) * 0.3f;
            setAmbient(0.05f * intensity, 0.05f * intensity, 0.1f * intensity);
            setDiffuse(0.3f * intensity, 0.3f * intensity, 0.4f * intensity);
            setSpecular(0.5f * intensity, 0.5f * intensity, 0.6f * intensity);
        }
    }
    
    @Override
    public String toString() {
        return String.format("DirectionalLight{direction=%s, ambient=%s, diffuse=%s, specular=%s}",
                direction, ambient, diffuse, specular);
    }
}
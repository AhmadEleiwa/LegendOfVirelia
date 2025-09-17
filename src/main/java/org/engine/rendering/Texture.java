package org.engine.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class Texture {
    private final int id;
    public final int width, height;

    public Texture(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    /**
     * Binds this texture to the active texture unit.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    /**
     * Binds this texture to a specific texture unit.
     * @param unit The texture unit to bind to (e.g., GL_TEXTURE0, GL_TEXTURE1).
     */
    public void bind(int unit) {
        glActiveTexture(unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }
    
    /**
     * Unbinds the currently bound texture from the active texture unit.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Deletes the texture from GPU memory.
     */
    public void delete() {
        glDeleteTextures(id);
    }

    public int getId() {
        return id;
    }

    /**
     * Static method to bind a texture to a specific texture unit.
     * This can be useful for external classes.
     * @param unit The texture unit to bind to (e.g., GL_TEXTURE0).
     * @param textureId The OpenGL ID of the texture to bind.
     */
    public static void bind(int unit, int textureId) {
        glActiveTexture(unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Static method to unbind all textures from a specific texture unit.
     * @param unit The texture unit to unbind.
     */
    public static void unbind(int unit) {
        glActiveTexture(unit);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
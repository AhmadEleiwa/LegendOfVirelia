package org.engine.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.nio.ByteBuffer;

import org.engine.utils.Resource;

public class TextureLoader {
    public static Texture loadTexture(String path) {
        int[] w = new int[1], h = new int[1], c = new int[1];
        ByteBuffer img = Resource.loadImage(path, w, h, c, 4);
        if (img == null) {
            return null;
        }

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        
        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w[0], h[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, img);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // Set texture parameters - important for proper rendering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST); // Pixelated look for Minecraft-style
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // No interpolation for crisp pixels
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // Unbind texture
        glBindTexture(GL_TEXTURE_2D, 0);
        
        Resource.freeImage(img);
        return new Texture(id, w[0], h[0]);
    }
}
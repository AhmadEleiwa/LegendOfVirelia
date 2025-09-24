package org.engine.rendering;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import java.nio.FloatBuffer;

import org.engine.utils.Debug;
import org.game.meshes.Mesh2D;
import org.game.ui.UIElement;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class UIRenderer {
    private final ShaderProgram shader;
    private final int uniModel, uniProj, uniColor, uniHasTexture, uniIsTransparent;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public UIRenderer(ShaderProgram shader) {
        this.shader = shader;
        shader.use();

        // Get uniform locations
        uniModel = glGetUniformLocation(shader.getId(), "model");
        uniProj = glGetUniformLocation(shader.getId(), "uProjection");
        uniColor = glGetUniformLocation(shader.getId(), "color");
        uniHasTexture = glGetUniformLocation(shader.getId(), "useTexture");
        uniIsTransparent = glGetUniformLocation(shader.getId(), "isTrasnparent");

        // Check if uniforms were found
        if (uniModel == -1)
            Debug.log("Warning: 'model' uniform not found in UI shader");
        if (uniProj == -1)
            Debug.log("Warning: 'uProjection' uniform not found in UI shader");
        if (uniColor == -1)
            Debug.log("Warning: 'color' uniform not found in UI shader");
        if (uniHasTexture == -1)
            Debug.log("Warning: 'hasTexture' uniform not found in UI shader");
        if (uniIsTransparent == -1)
            Debug.log("Warning: 'isTrasnparent' uniform not found in UI shader");
        shader.stop();
    }
    /**
     * Render a UI element.
     *
     * @param element       the UI element to render
     * @param windowWidth   current window width
     * @param windowHeight  current window height
     * @param r             red component (0-1)
     * @param g             green component (0-1)
     * @param b             blue component (0-1)
     * @param a             alpha component (0-1)
     * @param texture    whether the element has a texture
     * @param isTransparent whether the element is transparent
     */
    public void render(
            UIElement element,
            int windowWidth,
            int windowHeight,
            float r,
            float g,
            float b,
            float a,
            Texture texture,
            boolean isTransparent) {

        if (element == null || element.getMesh() == null) {
            Debug.log("Warning: UI element or mesh is null, cannot render.");
            return;
        }

        shader.use();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // projection & model
        new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0).get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        element.getModelMatrix().get(matrixBuffer);
        glUniformMatrix4fv(uniModel, false, matrixBuffer);

        // send uniforms
        glUniform1i(uniHasTexture, texture !=null ? 1 : 0);
        glUniform1i(uniIsTransparent, isTransparent ? 1 : 0);
        glUniform4f(uniColor,
                !isTransparent ? r : 0f,
                !isTransparent ? g : 0f,
                !isTransparent ? b : 0f,
                !isTransparent ? a : 0f);

        glDisable(GL_DEPTH_TEST);
        if (texture != null) {
            texture.bind();
        }
        element.getMesh().draw();
        glEnable(GL_DEPTH_TEST);

        glDisable(GL_BLEND);
        if (texture != null) {
            texture.unbind();
        }
        shader.stop();

    }
    public void render(Mesh2D mesh, Texture texture, int windowWidth, int windowHeight, boolean useColor, float r, float g, float b, float a, boolean hasTexture, boolean isTrasnparent) {
        if (mesh == null) {
            Debug.log("Warning: UI mesh is null, cannot render.");
            return;
        }

        shader.use();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // projection & model
        new Matrix4f().ortho2D(0, windowWidth, windowHeight, 0).get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        new Matrix4f().identity().get(matrixBuffer);
        glUniformMatrix4fv(uniModel, false, matrixBuffer);

        // send uniforms
        glUniform1i(uniHasTexture, hasTexture ? 1 : 0);
    
        glUniform1i(uniIsTransparent, isTrasnparent ? 1 : 0);
        glUniform4f(uniColor,
                useColor ? r : 0f,
                useColor ? g : 0f,
                useColor ? b : 0f,
                useColor ? a : 0f);
        texture.bind();
        glDisable(GL_DEPTH_TEST);
        mesh.draw();
        glEnable(GL_DEPTH_TEST);

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
        shader.stop();

    }

}

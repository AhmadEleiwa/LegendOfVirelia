package org.engine.rendering;

import java.nio.FloatBuffer;

import org.engine.utils.Resource;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.meshes.LightedMesh;
import org.game.meshes.Mesh;
import org.game.meshes.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public final class WaterRenderer {
    private final ShaderProgram shader;
    private final Texture waterTexture;
    private final Texture normalTexture;
    private final int projectionLocation;
    private final int viewLocation;
    private final int modelLocation;
    private final int timeLocation;
    private final int cameraLocation;
    private final int lightDirectionLocation;
    private final int waterColorLocation;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public WaterRenderer() {
        String vertexSource = Resource.loadText("shaders/water.vert");
        String fragmentSource = Resource.loadText("shaders/water.frag");
        if (vertexSource == null || fragmentSource == null)
            throw new IllegalStateException("Could not load water shader sources.");

        shader = new ShaderProgram(vertexSource, fragmentSource);
        if (glGetProgrami(shader.getId(), GL_LINK_STATUS) == 0) {
            shader.delete();
            throw new IllegalStateException("Water shader failed to link.");
        }

        Texture loadedWaterTexture = null;
        try {
            loadedWaterTexture = loadTexture("assets/textures/Water 0339.jpg");
            normalTexture = loadTexture("assets/textures/Water 0339normal.jpg");
            waterTexture = loadedWaterTexture;
        } catch (RuntimeException exception) {
            if (loadedWaterTexture != null)
                loadedWaterTexture.delete();
            shader.delete();
            throw exception;
        }

        shader.use();
        projectionLocation = glGetUniformLocation(shader.getId(), "uProjection");
        viewLocation = glGetUniformLocation(shader.getId(), "uView");
        modelLocation = glGetUniformLocation(shader.getId(), "uModel");
        timeLocation = glGetUniformLocation(shader.getId(), "uTime");
        cameraLocation = glGetUniformLocation(shader.getId(), "uCameraPosition");
        lightDirectionLocation = glGetUniformLocation(shader.getId(), "uLightDirection");
        waterColorLocation = glGetUniformLocation(shader.getId(), "uWaterColor");
        glUniform1i(glGetUniformLocation(shader.getId(), "uWaterTexture"), 0);
        glUniform1i(glGetUniformLocation(shader.getId(), "uNormalTexture"), 1);
        shader.stop();
    }

    public void render(Model model, Camera camera, DirectionalLight light,
            Vector3f position, Matrix4f projection, Matrix4f view, float animationTime) {
        if (model == null)
            return;

        shader.use();
        projection.get(matrixBuffer);
        glUniformMatrix4fv(projectionLocation, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(viewLocation, false, matrixBuffer);
        new Matrix4f().translation(position).get(matrixBuffer);
        glUniformMatrix4fv(modelLocation, false, matrixBuffer);
        glUniform1f(timeLocation, animationTime);
        glUniform3f(cameraLocation, camera.getViewPosition().x, camera.getViewPosition().y,
                camera.getViewPosition().z);
        var lightDirection = light.getDirection();
        glUniform3f(lightDirectionLocation, lightDirection.x, lightDirection.y, lightDirection.z);
        glUniform3f(waterColorLocation, 0.68f, 0.86f, 0.96f);

        waterTexture.bind(GL_TEXTURE0);
        normalTexture.bind(GL_TEXTURE1);
        for (Mesh mesh : model.getMeshes()) {
            if (mesh instanceof LightedMesh lightedMesh)
                lightedMesh.sortTranslucentFaces(camera.getViewPosition().x,
                        camera.getViewPosition().y, camera.getViewPosition().z,
                        position.x, position.y, position.z);
            mesh.draw();
        }
        Texture.unbind(GL_TEXTURE1);
        glActiveTexture(GL_TEXTURE0);
        Texture.unbind(GL_TEXTURE0);
        shader.stop();
    }

    public void delete() {
        waterTexture.delete();
        normalTexture.delete();
        shader.delete();
    }

    private static Texture loadTexture(String path) {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];
        var pixels = Resource.loadImage(path, width, height, channels, 4);
        if (pixels == null)
            throw new IllegalStateException("Could not load water texture: " + path);

        int textureId = glGenTextures();
        if (textureId == 0) {
            Resource.freeImage(pixels);
            throw new IllegalStateException("Could not allocate OpenGL texture for " + path);
        }
        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width[0], height[0], 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        Resource.freeImage(pixels);
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
        return new Texture(textureId, width[0], height[0]);
    }
}

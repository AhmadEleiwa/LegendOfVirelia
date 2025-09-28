package org.engine.rendering;

import org.engine.utils.Debug;
import org.game.core.GameObject;
import org.game.entities.Camera;
import org.game.lighting.DirectionalLight;
import org.game.meshes.Model;
import org.game.rendering.FrustumCuller;
import org.game.world.Block;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

import static org.lwjgl.opengl.GL20.*;

public class Renderer {
    private final ShaderProgram shader;
    private final int uniModel, uniView, uniProj, uniNormal;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer matrixNormalBuffer = BufferUtils.createFloatBuffer(9);

    private final FrustumCuller culler = new FrustumCuller();

    public Renderer(ShaderProgram shader) {
        this.shader = shader;
        shader.use();
        uniModel = glGetUniformLocation(shader.getId(), "model");
        uniView = glGetUniformLocation(shader.getId(), "view");
        uniProj = glGetUniformLocation(shader.getId(), "projection");
        uniNormal = glGetUniformLocation(shader.getId(), "normalMatrix");

        // Get the location of the texture sampler uniform
    }

    public void render(List<GameObject> objects, Camera camera, DirectionalLight light) {
        shader.use();

        Matrix4f view = camera.getView();
        Matrix4f proj = camera.getProjection();

        // Send projection & view once
        proj.get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(uniView, false, matrixBuffer);
        light.uploadToShader(shader.getId());
        int shininessLoc = glGetUniformLocation(shader.getId(), "shininess");
        glUniform1f(shininessLoc, 64.0f); // Material shininess

        for (GameObject obj : objects) {

            // if (!culler.isVisible(obj.position, 1.0f)) {
            // continue; // culled
            // }

            Matrix4f modelMatrix = obj.getModelMatrix();
            modelMatrix.get(matrixBuffer);
            glUniformMatrix4fv(uniModel, false, matrixBuffer);
            if (obj.getClass() == Block.class) {
                Model model = ((Block) obj).getModel();

                if (model != null)
                    model.draw();
                else {
                    Debug.log("hi");
                }
            }
            // Clean up by unbinding the texture (optional but good practice)
            glBindTexture(GL_TEXTURE_2D, 0);
            shader.stop();
        }

    }

    public void render(List<GameObject> objects, Camera camera) {
        shader.use();

        Matrix4f view = camera.getView();
        Matrix4f proj = camera.getProjection();

        // Send projection & view once
        proj.get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(uniView, false, matrixBuffer);

        for (GameObject obj : objects) {

            // if (!culler.isVisible(obj.position, 1.0f)) {
            // continue; // culled
            // }

            Matrix4f modelMatrix = obj.getModelMatrix();
            modelMatrix.get(matrixBuffer);
            glUniformMatrix4fv(uniModel, false, matrixBuffer);
            if (obj.getClass() == Block.class) {
                Model model = ((Block) obj).getModel();

                if (model != null)
                    model.draw();
                else {
                    Debug.log("hi");
                }
            }
            // Clean up by unbinding the texture (optional but good practice)
            glBindTexture(GL_TEXTURE_2D, 0);
            shader.stop();
        }

    }

    public void render(Model model, Camera camera, DirectionalLight light, Vector3f position, Vector3f center,
            float radius) {
        shader.use();

        Matrix4f view = camera.getView();
        Matrix4f proj = camera.getProjection();
        Matrix4f projView = new Matrix4f(proj).mul(view);
        culler.update(projView);
        // Send projection & view once
        proj.get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(uniView, false, matrixBuffer);

        light.uploadToShader(shader.getId());

        if (!culler.isVisible(center, radius)) {
            shader.stop();
            return;
        }

        // Send the model matrix
        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix = modelMatrix.translate(position);
        modelMatrix.get(matrixBuffer);

        Matrix3f normalMatrix = new Matrix3f(modelMatrix).invert().transpose();
        normalMatrix.get(matrixNormalBuffer);
        glUniformMatrix4fv(uniModel, false, matrixBuffer);
        glUniformMatrix3fv(uniNormal, false, matrixNormalBuffer);

        // Tell the Model to draw itself
        model.draw();

        // Clean up by unbinding the texture (optional but good practice)
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.stop();
    }

    public void render(Model model, Camera camera, Vector3f position, Vector3f center, float radius) {
        shader.use();

        Matrix4f view = camera.getView();
        Matrix4f proj = camera.getProjection();
        Matrix4f projView = new Matrix4f(proj).mul(view);
        culler.update(projView);
        // Send projection & view once
        proj.get(matrixBuffer);
        glUniformMatrix4fv(uniProj, false, matrixBuffer);
        view.get(matrixBuffer);
        glUniformMatrix4fv(uniView, false, matrixBuffer);

        if (!culler.isVisible(center, radius)) {
            shader.stop();
            return;
        }

        // Send the model matrix
        Matrix4f modelMatrix = new Matrix4f().identity();
        modelMatrix = modelMatrix.translate(position);
        modelMatrix.get(matrixBuffer);

        glUniformMatrix4fv(uniModel, false, matrixBuffer);

        // Tell the Model to draw itself
        model.draw();

        // Clean up by unbinding the texture (optional but good practice)
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.stop();
    }

}
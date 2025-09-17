package org.game.meshes;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int vertexCount;
 

    private final float[] positions;
    private final int[] indices;
    private final float[] texCoords;
    /**
     * Constructs a Mesh from a single interleaved float array for vertices and an int array for indices.
     * This is useful for pre-built geometry like the createCube method.
     * The vertex data is assumed to be in the format: [x, y, z, u, v, ...]
     *
     * @param vertices An interleaved array of vertex data (positions and texture coordinates).
     * @param indices  An array of indices defining the triangles.
     */
    public Mesh(float[] vertices, int[] indices) {
        this.vertexCount = indices.length;
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();
        this.indices = indices;
        this.positions = vertices;
        this.texCoords = new float[(vertices.length / 5) * 2];
        setupMesh(vertices, indices);
    }

    /**
     * Constructs a Mesh from separate arrays for positions and texture coordinates.
     * This is ideal for use with the MeshConverter class.
     *
     * @param positions An array of vertex positions (x, y, z).
     * @param texCoords An array of vertex texture coordinates (u, v).
     * @param indices   An array of indices defining the triangles.
     */
    public Mesh(float[] positions, float[] texCoords, int[] indices) {
        // We need to interleave the positions and texture coordinates into a single array
        float[] interleavedVertices = new float[positions.length + texCoords.length];
        int posIndex = 0;
        int texIndex = 0;
        for (int i = 0; i < interleavedVertices.length / 5; i++) {
            // Position
            interleavedVertices[i * 5] = positions[posIndex++];
            interleavedVertices[i * 5 + 1] = positions[posIndex++];
            interleavedVertices[i * 5 + 2] = positions[posIndex++];
            // Texture Coordinates
            interleavedVertices[i * 5 + 3] = texCoords[texIndex++];
            interleavedVertices[i * 5 + 4] = texCoords[texIndex++];
        }
        this.positions = positions;
        this.indices = indices;
        this.vertexCount = indices.length;
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();
        this.texCoords = texCoords;
        setupMesh(interleavedVertices, indices);
    }

    private void setupMesh(float[] vertices, int[] indices) {
        glBindVertexArray(vao);

        // Vertex buffer
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vb = BufferUtils.createFloatBuffer(vertices.length);
        vb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        // Index buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        // Define the vertex attributes
        int stride = 5 * Float.BYTES;
        // Position attribute (layout location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // Texture coordinate attribute (layout location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Unbind the VAO and buffers
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    public static Mesh createCube(float size) {
        float half = size / 2.0f;
        float[] vertices = {
            // positions             // texture coords
            // Front face (+Z)
            -half, -half,  half,     0.0f, 0.0f, // 0
             half, -half,  half,     1.0f, 0.0f, // 1
             half,  half,  half,     1.0f, 1.0f, // 2
            -half,  half,  half,     0.0f, 1.0f, // 3

            // Back face (-Z)
             half, -half, -half,     0.0f, 0.0f, // 4
            -half, -half, -half,     1.0f, 0.0f, // 5
            -half,  half, -half,     1.0f, 1.0f, // 6
             half,  half, -half,     0.0f, 1.0f, // 7

            // Left face (-X)
            -half, -half, -half,     0.0f, 0.0f, // 8
            -half, -half,  half,     1.0f, 0.0f, // 9
            -half,  half,  half,     1.0f, 1.0f, // 10
            -half,  half, -half,     0.0f, 1.0f, // 11

            // Right face (+X)
             half, -half,  half,     0.0f, 0.0f, // 12
             half, -half, -half,     1.0f, 0.0f, // 13
             half,  half, -half,     1.0f, 1.0f, // 14
             half,  half,  half,     0.0f, 1.0f, // 15

            // Top face (+Y)
            -half,  half,  half,     0.0f, 0.0f, // 16
             half,  half,  half,     1.0f, 0.0f, // 17
             half,  half, -half,     1.0f, 1.0f, // 18
            -half,  half, -half,     0.0f, 1.0f, // 19

            // Bottom face (-Y)
            -half, -half, -half,     0.0f, 0.0f, // 20
             half, -half, -half,     1.0f, 0.0f, // 21
             half, -half,  half,     1.0f, 1.0f, // 22
            -half, -half,  half,     0.0f, 1.0f  // 23
        };

        int[] indices = {
            // Front
            0, 1, 2,  2, 3, 0,
            // Back
            4, 5, 6,  6, 7, 4,
            // Left
            8, 9, 10, 10, 11, 8,
            // Right
            12, 13, 14, 14, 15, 12,
            // Top
            16, 17, 18, 18, 19, 16,
            // Bottom
            20, 21, 22, 22, 23, 20
        };

        return new Mesh(vertices, indices);
    }

    public float[] getPositions() {
    return positions;
}

    public float[] getTexCoords() {
        return texCoords;
    }

    public int[] getIndices() {
        return indices;
}

}
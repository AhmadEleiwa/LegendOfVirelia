package org.game.meshes;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

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
    private final float[] normals;

    /**
     * Constructs a Mesh from a single interleaved float array for vertices and an
     * int array for indices.
     * This is useful for pre-built geometry like the createCube method.
     * The vertex data is assumed to be in the format: [x, y, z, u, v, ...]
     * Normals will be automatically generated.
     *
     * @param vertices An interleaved array of vertex data (positions and texture
     *                 coordinates).
     * @param indices  An array of indices defining the triangles.
     */
    public Mesh(float[] vertices, int[] indices) {
        this.vertexCount = indices.length;
        this.indices = indices;

        // Extract separate arrays from interleaved vertices (assuming 5 floats per
        // vertex: pos(3) + tex(2))
        int vertexStride = 5;
        int numVertices = vertices.length / vertexStride;
        this.positions = new float[numVertices * 3];
        this.texCoords = new float[numVertices * 2];

        for (int i = 0; i < numVertices; i++) {
            int baseIndex = i * vertexStride;
            // Positions
            positions[i * 3] = vertices[baseIndex];
            positions[i * 3 + 1] = vertices[baseIndex + 1];
            positions[i * 3 + 2] = vertices[baseIndex + 2];
            // Texture coordinates
            texCoords[i * 2] = vertices[baseIndex + 3];
            texCoords[i * 2 + 1] = vertices[baseIndex + 4];
        }

        // Generate normals automatically
        this.normals = generateNormals(positions, indices);

        // Create interleaved array with normals
        float[] interleavedVertices = createInterleavedArray(positions, texCoords, normals);

        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();

        setupMesh(interleavedVertices, indices);
    }

    /**
     * Constructs a Mesh from separate arrays for positions and texture coordinates.
     * Normals will be automatically generated from the geometry.
     *
     * @param positions An array of vertex positions (x, y, z).
     * @param texCoords An array of vertex texture coordinates (u, v).
     * @param indices   An array of indices defining the triangles.
     */
    public Mesh(float[] positions, float[] texCoords, int[] indices) {
        this.positions = positions;
        this.indices = indices;
        this.texCoords = texCoords;
        this.vertexCount = indices.length;

        // Generate normals automatically
        this.normals = generateNormals(positions, indices);

        // Create interleaved array
        float[] interleavedVertices = createInterleavedArray(positions, texCoords, normals);

        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();

        setupMesh(interleavedVertices, indices);
    }

    /**
     * Generates smooth normals for the mesh based on vertex positions and indices.
     * This method calculates face normals and averages them for each vertex.
     */
    protected float[] generateNormals(float[] positions, int[] indices) {
        int numVertices = positions.length / 3;
        float[] normals = new float[positions.length]; // Same size as positions

        // Initialize normals to zero
        Arrays.fill(normals, 0.0f);

        // Calculate face normals and accumulate them for each vertex
        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i] * 3;
            int i1 = indices[i + 1] * 3;
            int i2 = indices[i + 2] * 3;

            // Get the three vertices of the triangle
            float[] v0 = { positions[i0], positions[i0 + 1], positions[i0 + 2] };
            float[] v1 = { positions[i1], positions[i1 + 1], positions[i1 + 2] };
            float[] v2 = { positions[i2], positions[i2 + 1], positions[i2 + 2] };

            // Calculate two edges of the triangle
            float[] edge1 = { v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2] };
            float[] edge2 = { v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2] };

            // Calculate the face normal using cross product
            float[] faceNormal = crossProduct(edge1, edge2);

            // Add this face normal to each vertex of the triangle
            for (int j = 0; j < 3; j++) {
                normals[i0 + j] += faceNormal[j];
                normals[i1 + j] += faceNormal[j];
                normals[i2 + j] += faceNormal[j];
            }
        }

        // Normalize all vertex normals
        for (int i = 0; i < numVertices; i++) {
            int baseIndex = i * 3;
            float length = (float) Math.sqrt(
                    normals[baseIndex] * normals[baseIndex] +
                            normals[baseIndex + 1] * normals[baseIndex + 1] +
                            normals[baseIndex + 2] * normals[baseIndex + 2]);

            if (length > 0.0f) {
                normals[baseIndex] /= length;
                normals[baseIndex + 1] /= length;
                normals[baseIndex + 2] /= length;
            }
        }

        return normals;
    }

    /**
     * Calculates the cross product of two 3D vectors.
     */
    private float[] crossProduct(float[] a, float[] b) {
        return new float[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    /**
     * Creates an interleaved array from separate position, texture coordinate, and
     * normal arrays.
     */
    private float[] createInterleavedArray(float[] positions, float[] texCoords, float[] normals) {
        int numVertices = positions.length / 3;
        float[] interleavedVertices = new float[numVertices * 8]; // pos(3) + tex(2) + normal(3)

        for (int i = 0; i < numVertices; i++) {
            int destIndex = i * 8;
            int posIndex = i * 3;
            int texIndex = i * 2;

            // Position
            interleavedVertices[destIndex] = positions[posIndex];
            interleavedVertices[destIndex + 1] = positions[posIndex + 1];
            interleavedVertices[destIndex + 2] = positions[posIndex + 2];

            // Texture coordinates
            interleavedVertices[destIndex + 3] = texCoords[texIndex];
            interleavedVertices[destIndex + 4] = texCoords[texIndex + 1];

            // Normals
            interleavedVertices[destIndex + 5] = normals[posIndex];
            interleavedVertices[destIndex + 6] = normals[posIndex + 1];
            interleavedVertices[destIndex + 7] = normals[posIndex + 2];
        }

        return interleavedVertices;
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

        // Define the vertex attributes (now with 8 floats per vertex)
        int stride = 8 * Float.BYTES;
        // Position attribute (layout location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // Texture coordinate attribute (layout location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // Normal attribute (layout location 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

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
                // positions // texture coords
                // Front face (+Z)
                -half, -half, half, 0.0f, 0.0f, // 0
                half, -half, half, 1.0f, 0.0f, // 1
                half, half, half, 1.0f, 1.0f, // 2
                -half, half, half, 0.0f, 1.0f, // 3

                // Back face (-Z)
                half, -half, -half, 0.0f, 0.0f, // 4
                -half, -half, -half, 1.0f, 0.0f, // 5
                -half, half, -half, 1.0f, 1.0f, // 6
                half, half, -half, 0.0f, 1.0f, // 7

                // Left face (-X)
                -half, -half, -half, 0.0f, 0.0f, // 8
                -half, -half, half, 1.0f, 0.0f, // 9
                -half, half, half, 1.0f, 1.0f, // 10
                -half, half, -half, 0.0f, 1.0f, // 11

                // Right face (+X)
                half, -half, half, 0.0f, 0.0f, // 12
                half, -half, -half, 1.0f, 0.0f, // 13
                half, half, -half, 1.0f, 1.0f, // 14
                half, half, half, 0.0f, 1.0f, // 15

                // Top face (+Y)
                -half, half, half, 0.0f, 0.0f, // 16
                half, half, half, 1.0f, 0.0f, // 17
                half, half, -half, 1.0f, 1.0f, // 18
                -half, half, -half, 0.0f, 1.0f, // 19

                // Bottom face (-Y)
                -half, -half, -half, 0.0f, 0.0f, // 20
                half, -half, -half, 1.0f, 0.0f, // 21
                half, -half, half, 1.0f, 1.0f, // 22
                -half, -half, half, 0.0f, 1.0f // 23
        };

        int[] indices = {
                // Front
                0, 1, 2, 2, 3, 0,
                // Back
                4, 5, 6, 6, 7, 4,
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

    public float[] getNormals() {
        return normals;
    }

    public int[] getIndices() {
        return indices;
    }

    protected int getVAO() {
        return vao;
    }

    protected int getVBO() {
        return vbo;
    }

    protected int getEBO() {
        return ebo;
    }
}
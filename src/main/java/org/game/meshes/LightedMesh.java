package org.game.meshes;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class LightedMesh extends Mesh {
    private final float[] lighting;
    private int[] translucentFaceIndices;
    private float[] translucentFaceCenters;
    private int[] lastSortedIndices;
    private float lastSortCameraX;
    private float lastSortCameraY;
    private float lastSortCameraZ;
    private boolean hasSortedForCamera;

    /**
     * Constructs a Mesh with lighting data from separate arrays.
     *
     * @param positions An array of vertex positions (x, y, z).
     * @param texCoords An array of vertex texture coordinates (u, v).
     * @param lighting  An array of vertex lighting values (0.0 - 1.0).
     * @param indices   An array of indices defining the triangles.
     */
    public LightedMesh(float[] positions, float[] texCoords, float[] lighting, int[] indices) {
        this(positions, texCoords, lighting, new float[lighting.length], indices);
    }

    public LightedMesh(float[] positions, float[] texCoords, float[] lighting,
                       float[] emissive, int[] indices) {
        super(positions, texCoords, indices);
        this.lighting = lighting;
        setupLightedMesh(positions, texCoords, lighting, emissive, indices);
    }

    private void setupLightedMesh(float[] positions, float[] texCoords, float[] lighting,
                                  float[] emissive, int[] indices) {
        int numVertices = positions.length / 3;
        float[] interleavedVertices = new float[numVertices * 10];
        float[] normals = generateNormals(positions, indices);
        
        for (int i = 0; i < numVertices; i++) {
            int destIndex = i * 10;
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
            
            // Lighting
            interleavedVertices[destIndex + 8] = lighting[i];
            interleavedVertices[destIndex + 9] = emissive[i];
        }
        
        // Setup the VAO with new layout
        setupLightedVAO(interleavedVertices, indices);
    }

    private void setupLightedVAO(float[] vertices, int[] indices) {
        glBindVertexArray(getVAO());

        // Vertex buffer
        glBindBuffer(GL_ARRAY_BUFFER, getVBO());
        FloatBuffer vb = BufferUtils.createFloatBuffer(vertices.length);
        vb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        // Index buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, getEBO());
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        int stride = 10 * Float.BYTES;
        
        // Position attribute (layout location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute (layout location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Normal attribute (layout location 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Lighting attribute (layout location 3)
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 9 * Float.BYTES);
        glEnableVertexAttribArray(4);

        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    // Helper method to access parent's protected fields (you may need to add getters to Mesh class)
    protected int getVAO() {
        // You'll need to add a getter in your parent Mesh class: public int getVAO() { return vao; }
        return super.getVAO(); // Assuming you add this method to Mesh
    }
    
    protected int getVBO() {
        return super.getVBO(); // Assuming you add this method to Mesh
    }
    
    protected int getEBO() {
        return super.getEBO(); // Assuming you add this method to Mesh
    }

    public float[] getLighting() {
        return lighting;
    }

    public void enableTranslucentFaceSorting(float[] positions, int[] indices) {
        translucentFaceIndices = indices.clone();
        int faceCount = indices.length / 6;
        translucentFaceCenters = new float[faceCount * 3];
        for (int face = 0; face < faceCount; face++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                int positionIndex = (face * 4 + vertex) * 3;
                translucentFaceCenters[face * 3] += positions[positionIndex] * 0.25f;
                translucentFaceCenters[face * 3 + 1] += positions[positionIndex + 1] * 0.25f;
                translucentFaceCenters[face * 3 + 2] += positions[positionIndex + 2] * 0.25f;
            }
        }
        lastSortedIndices = translucentFaceIndices.clone();
    }

    public void sortTranslucentFaces(float cameraX, float cameraY, float cameraZ,
            float offsetX, float offsetY, float offsetZ) {
        if (translucentFaceIndices == null || translucentFaceIndices.length == 0)
            return;
        float cameraDX = cameraX - lastSortCameraX;
        float cameraDY = cameraY - lastSortCameraY;
        float cameraDZ = cameraZ - lastSortCameraZ;
        if (hasSortedForCamera && cameraDX * cameraDX + cameraDY * cameraDY
                + cameraDZ * cameraDZ < 0.04f)
            return;

        Integer[] faceOrder = new Integer[translucentFaceCenters.length / 3];
        for (int face = 0; face < faceOrder.length; face++)
            faceOrder[face] = face;
        Arrays.sort(faceOrder, Comparator.comparingDouble((Integer face) -> {
            int center = face * 3;
            float dx = translucentFaceCenters[center] + offsetX - cameraX;
            float dy = translucentFaceCenters[center + 1] + offsetY - cameraY;
            float dz = translucentFaceCenters[center + 2] + offsetZ - cameraZ;
            return dx * dx + dy * dy + dz * dz;
        }).reversed());

        int[] sortedIndices = new int[translucentFaceIndices.length];
        for (int face = 0; face < faceOrder.length; face++)
            System.arraycopy(translucentFaceIndices, faceOrder[face] * 6,
                    sortedIndices, face * 6, 6);
        if (!Arrays.equals(lastSortedIndices, sortedIndices)) {
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(sortedIndices.length);
            indexBuffer.put(sortedIndices).flip();
            glBindVertexArray(getVAO());
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, getEBO());
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
            glBindVertexArray(0);
            lastSortedIndices = sortedIndices;
        }
        lastSortCameraX = cameraX;
        lastSortCameraY = cameraY;
        lastSortCameraZ = cameraZ;
        hasSortedForCamera = true;
    }

}
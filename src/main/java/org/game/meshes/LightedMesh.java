package org.game.meshes;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class LightedMesh extends Mesh {
    private final float[] lighting;

    /**
     * Constructs a Mesh with lighting data from separate arrays.
     *
     * @param positions An array of vertex positions (x, y, z).
     * @param texCoords An array of vertex texture coordinates (u, v).
     * @param lighting  An array of vertex lighting values (0.0 - 1.0).
     * @param indices   An array of indices defining the triangles.
     */
    public LightedMesh(float[] positions, float[] texCoords, float[] lighting, int[] indices) {
        super(positions, texCoords, indices);
        this.lighting = lighting;
        
        // Re-setup the mesh with lighting data
        setupLightedMesh(positions, texCoords, lighting, indices);
    }

    private void setupLightedMesh(float[] positions, float[] texCoords, float[] lighting, int[] indices) {
        // Create interleaved array: pos(3) + tex(2) + normal(3) + light(1) = 9 floats per vertex
        int numVertices = positions.length / 3;
        float[] interleavedVertices = new float[numVertices * 9];
        
        // Generate normals automatically (reuse from parent class)
        float[] normals = generateNormals(positions, indices);
        
        for (int i = 0; i < numVertices; i++) {
            int destIndex = i * 9;
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

        // Define the vertex attributes (9 floats per vertex)
        int stride = 9 * Float.BYTES;
        
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
}
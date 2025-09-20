package org.game.meshes;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh2D {
    private final int vao, vbo, ebo;
    private final int indexCount;

    public Mesh2D(float[] vertices, int[] indices) {
        indexCount = indices.length;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        // Vertex buffer
        FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.length);
        fb.put(vertices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        // Index buffer
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length);
        ib.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES; // 2 pos + 2 uv
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    public static Mesh2D createQuad(float x, float y, float w, float h) {
        // Vertex positions in screen coords
        float[] vertices = {
            x,     y,     0.0f, 0.0f,
            x + w, y,     1.0f, 0.0f,
            x + w, y + h, 1.0f, 1.0f,
            x,     y + h, 0.0f, 1.0f
        };
        int[] indices = {0,1,2, 2,3,0};
        return new Mesh2D(vertices, indices);
    }
}

package org.engine.rendering;

import org.engine.utils.Resource;
import org.game.meshes.Mesh2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_RGBA;
import static org.lwjgl.opengl.GL43C.GL_TEXTURE_SWIZZLE_RGBA;

import static org.lwjgl.stb.STBTruetype.*;

/**
 * Helper for generating Mesh2D text quads from a TTF font using stb_truetype.
 * Works with Mesh2D + UIRenderer pipeline.
 */
public class UIText {

    private static final int ATLAS_W = 512; // glyph atlas width
    private static final int ATLAS_H = 512; // glyph atlas height

    private Texture texture;
    private STBTTBakedChar.Buffer cdata;

    /**
     * Load and bake the font.
     *
     * @param fontPath    relative path to the TTF file (e.g.
     *                    "res/fonts/Roboto-Regular.ttf")
     * @param pixelHeight desired pixel height
     * @throws RuntimeException if loading or baking fails
     */
    public void loadFont(String fontPath, float pixelHeight) {
        ByteBuffer ttf = Resource.loadBinary(fontPath);
        if (ttf == null) {
            throw new RuntimeException("Could not load font at " + fontPath);
        }

        // allocate bitmap for glyph atlas
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_W * ATLAS_H);
        cdata = STBTTBakedChar.malloc(96); // ASCII 32..127

        int bakeResult = stbtt_BakeFontBitmap(ttf, pixelHeight, bitmap,
                ATLAS_W, ATLAS_H, 32, cdata);
        if (bakeResult <= 0) {
            throw new RuntimeException("stbtt_BakeFontBitmap failed for " + fontPath);
        }
        texture = new Texture(glGenTextures(), ATLAS_W, ATLAS_H);
        // upload to OpenGL

        texture.bind();
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1); // important for 1-byte pixels!
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, ATLAS_W, ATLAS_H, 0,
                GL_RED, GL_UNSIGNED_BYTE, bitmap);

        // swizzle so shader sees it in all channels
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer swizzle = stack.mallocInt(4);
            swizzle.put(GL_RED).put(GL_RED).put(GL_RED).put(GL_RED).flip();
            glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzle);
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Build a Mesh2D containing quads for the specified text string.
     *
     * @param text  ASCII text
     * @param x     start x in screen coordinates
     * @param y     start y in screen coordinates
     * @param scale scale multiplier (1.0 = baked pixelHeight)
     */
     public Mesh2D buildTextMesh(String text, float x, float y, float scale) {
        if (text == null || text.isEmpty()) return null;

        // each glyph 4 vertices, 6 indices
        int quads = text.length();
        float[] vertices = new float[quads * 4 * 4]; // x,y,u,v
        int[] indices = new int[quads * 6];

        int v = 0;
        int i = 0;
        int vertexIndex = 0;

        // STB call gives correct positions & UVs
        STBTTAlignedQuad q = STBTTAlignedQuad.malloc();
        float cx = x;
        float cy = y;

        for (int idx = 0; idx < text.length(); idx++) {
            char c = text.charAt(idx);
            if (c < 32 || c >= 128) continue;

            // STB will update cx,cy by reference, so use array wrappers
            float[] px = {cx};
            float[] py = {cy};
            stbtt_GetBakedQuad(cdata, ATLAS_W, ATLAS_H, c - 32, px, py, q, true);
            cx = px[0];
            cy = py[0];

            float x0 = q.x0() * scale;
            float y0 = q.y0() * scale;
            float x1 = q.x1() * scale;
            float y1 = q.y1() * scale;

            float s0 = q.s0();
            float t0 = q.t0();
            float s1 = q.s1();
            float t1 = q.t1();

            // 4 vertices x,y,u,v
            vertices[v++] = x0; vertices[v++] = y0; vertices[v++] = s0; vertices[v++] = t0;
            vertices[v++] = x1; vertices[v++] = y0; vertices[v++] = s1; vertices[v++] = t0;
            vertices[v++] = x1; vertices[v++] = y1; vertices[v++] = s1; vertices[v++] = t1;
            vertices[v++] = x0; vertices[v++] = y1; vertices[v++] = s0; vertices[v++] = t1;

            indices[i++] = vertexIndex;
            indices[i++] = vertexIndex + 1;
            indices[i++] = vertexIndex + 2;
            indices[i++] = vertexIndex + 2;
            indices[i++] = vertexIndex + 3;
            indices[i++] = vertexIndex;
            vertexIndex += 4;
        }
        q.free();

        return new Mesh2D(vertices, indices);
    }


    /** Get the OpenGL texture id of the baked glyph atlas. */
    public Texture getTexture() {
        return texture;
    }

    public void bindTexture() {
        if (texture != null) {
            texture.bind();
        }
    }

    public void stopTexture() {
        if (texture != null) {
            texture.unbind();
        }
    }

    /** Free GPU and native memory. */
    public void destroy() {
        if (texture != null) {
            texture.delete();
        }
        if (cdata != null) {
            cdata.free();
            cdata = null;
        }
    }

    /**
     * Utility: compute string width at the specified scale.
     */
    public float getTextWidth(String text, float scale) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c >= 128)
                continue;
            STBTTBakedChar g = cdata.get(c - 32);
            width += g.xadvance() * scale;
        }
        return width;
    }
}

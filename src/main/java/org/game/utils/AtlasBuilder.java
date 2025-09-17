package org.game.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.engine.rendering.Texture;
import org.engine.utils.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
public class AtlasBuilder {
    private Texture atlasTexture;
    private Map<String, float[]> uvMap = new HashMap<>();
    private static AtlasBuilder atlasBuilder;
    public static AtlasBuilder getDefault(){
        return atlasBuilder;
    }
    public static AtlasBuilder create(String folderPath, int atlasSize, int cellSize) throws IOException{
        if(atlasBuilder == null){
            atlasBuilder = new AtlasBuilder(folderPath, atlasSize, cellSize);
        }
        return atlasBuilder;
    }
    private AtlasBuilder(String folderPath, int atlasSize, int cellSize) throws IOException {
        // scan folder for PNGs
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(folderPath), "*.png")) {
            int x = 0, y = 0;
            ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasSize * atlasSize * 4);

            for (Path p : stream) {
                String fileName = p.getFileName().toString();
                String key = fileName.substring(0, fileName.length() - 4);

                // use your Resource helper to load image
                int[] w = new int[1], h = new int[1], c = new int[1];
                ByteBuffer image = Resource.loadImage(p.toString(), w, h, c, 4);
                if (image == null) continue;

                int width = w[0];
                int height = h[0];

                // copy pixels into atlas buffer
                for (int row = 0; row < height; row++) {
                    for (int col = 0; col < width; col++) {
                        int srcIndex = (row * width + col) * 4;
                        int destIndex = ((y + row) * atlasSize + (x + col)) * 4;
                        atlasBuffer.put(destIndex, image.get(srcIndex));
                        atlasBuffer.put(destIndex + 1, image.get(srcIndex + 1));
                        atlasBuffer.put(destIndex + 2, image.get(srcIndex + 2));
                        atlasBuffer.put(destIndex + 3, image.get(srcIndex + 3));
                    }
                }

                Resource.freeImage(image); // free STB buffer

                // store UVs
                float u0 = (float) x / atlasSize;
                float v0 = (float) y / atlasSize;
                float u1 = (float) (x + width) / atlasSize;
                float v1 = (float) (y + height) / atlasSize;
                uvMap.put(key, new float[]{u0, v0, u1, v1});

                // next cell
                x += cellSize;
                if (x + cellSize > atlasSize) { x = 0; y += cellSize; }
            }

            // upload atlasBuffer to GPU once
            int texId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, atlasSize, atlasSize, 0,
                              GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, atlasBuffer);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                                 GL11.GL_NEAREST_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            atlasTexture = new Texture(texId, atlasSize, atlasSize); // your Texture wrapper
        }
    }

    public Texture getAtlasTexture() { return atlasTexture; }
    public float[] getUV(String key) { return uvMap.get(key); }
    public Map<String, float[]> getAllUVs() { return uvMap;}
}

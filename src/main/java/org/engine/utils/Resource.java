package org.engine.utils;

import org.lwjgl.BufferUtils;

import com.google.gson.Gson;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.stb.STBImage.*;

public class Resource {

    /** Base project directory (can be overridden if needed) */
    private static final Path BASE_DIR = Paths.get("").toAbsolutePath();
    private static final Gson GSON = new Gson();

    /** Load text file from project directory */
    public static String loadText(String relativePath) {
        try {
            Path filePath = BASE_DIR.resolve(relativePath);
            if (!Files.exists(filePath)) {
                Logger.log("Text file not found: " + filePath);
                return null;
            }
            return Files.readString(filePath);
        } catch (Exception e) {
            Logger.log("Failed to load text file: " + relativePath, e);
            return null;
        }
    }

    /** Load binary file from project directory */
    public static ByteBuffer loadBinary(String relativePath) {
        try {
            Path filePath = BASE_DIR.resolve(relativePath);
            if (!Files.exists(filePath)) {
                Logger.log("Binary file not found: " + filePath);
                return null;
            }
            byte[] data = Files.readAllBytes(filePath);
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data).flip();
            return buffer;
        } catch (Exception e) {
            Logger.log("Failed to load binary file: " + relativePath, e);
            return null;
        }
    }

    /** Load image from project directory */
    public static ByteBuffer loadImage(String relativePath, int[] width, int[] height, int[] channels,
            int desiredChannels) {
        Path filePath = BASE_DIR.resolve(relativePath);
        if (!Files.exists(filePath)) {
            Logger.log("Image file not found: " + filePath);
            return null;
        }

        try (InputStream in = Files.newInputStream(filePath)) {
            ReadableByteChannel rbc = Channels.newChannel(in);
            ByteBuffer tmp = BufferUtils.createByteBuffer(8 * 1024);
            while (true) {
                int bytes = rbc.read(tmp);
                if (bytes == -1)
                    break;
                if (tmp.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(tmp.capacity() * 2);
                    tmp.flip();
                    newBuffer.put(tmp);
                    tmp = newBuffer;
                }
            }
            tmp.flip();
            ByteBuffer pixels = stbi_load_from_memory(tmp, width, height, channels, desiredChannels);
            if (pixels == null) {
                Logger.log("STB failed to decode image: " + relativePath + " reason: " + stbi_failure_reason());
                return null;
            }
            return pixels;
        } catch (Exception e) {
            Logger.log("Failed to load image: " + relativePath, e);
            return null;
        }
    }

    /** Free image memory */
    public static void freeImage(ByteBuffer image) {
        if (image != null)
            stbi_image_free(image);
    }

    public static <T> T loadJson(String path, Class<T> clazz) {
        try {
            String json = loadText(path);
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            Logger.log("Failed to load model: " + path, e);
            return null;
        }
    }

}

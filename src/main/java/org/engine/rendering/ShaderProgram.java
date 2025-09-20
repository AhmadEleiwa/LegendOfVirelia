package org.engine.rendering;
import static org.lwjgl.opengl.GL20.*;

import org.engine.utils.Logger;

public class ShaderProgram {
    private final int id;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, vertexSource);
        glCompileShader(vertex);
        if (glGetShaderi(vertex, GL_COMPILE_STATUS) == GL_FALSE){
            Logger.log("Vertex shader error: " + glGetShaderInfoLog(vertex));
        }
        int fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, fragmentSource);
        glCompileShader(fragment);
        if (glGetShaderi(fragment, GL_COMPILE_STATUS) == GL_FALSE)
            Logger.log("Fragment shader error: " + glGetShaderInfoLog(fragment));

        id = glCreateProgram();
        glAttachShader(id, vertex);
        glAttachShader(id, fragment);
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE)
            Logger.log("Shader link error: " + glGetProgramInfoLog(id));

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    public void use() {
        glUseProgram(id);
    }
    public void stop() {
        glUseProgram(0);
    }

    public int getId() {
        return id;
    }

    public void delete() {
        glDeleteProgram(id);
    }
}
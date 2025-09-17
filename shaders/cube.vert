#version 330 core
layout (location = 0) in vec3 inPos;
layout (location = 1) in vec2 inTex;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec2 fragTex;

void main() {
    fragTex = inTex;
    gl_Position = projection * view * model * vec4(inPos, 1.0);
}

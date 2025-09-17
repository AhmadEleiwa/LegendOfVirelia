#version 330 core
in vec2 fragTex;
uniform sampler2D texture_sampler;
out vec4 fragColor;

void main() {
    fragColor = texture(texture_sampler, fragTex);
}

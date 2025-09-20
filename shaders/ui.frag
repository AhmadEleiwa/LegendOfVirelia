#version 330 core
in vec2 vTexCoord;
out vec4 FragColor;

uniform vec4 color;
uniform sampler2D uTexture;
uniform bool useTexture;
uniform bool isTransparent;

void main() {
    if (isTransparent) {
        discard; // stops fragment completely
    }

    if (useTexture) {
        FragColor = texture(uTexture, vTexCoord) * color;
    } else {
        FragColor = color;
    }
}

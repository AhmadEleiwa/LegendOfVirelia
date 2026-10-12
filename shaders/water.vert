#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec3 aNormal;
layout(location = 3) in float aWaveWeight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform float uTime;

out vec3 vWorldPosition;
out vec3 vWorldNormal;
out vec2 vTexCoord;

void main() {
    vec4 worldPosition = uModel * vec4(aPosition, 1.0);
    float waveA = sin(worldPosition.x * 0.72 + worldPosition.z * 0.41 + uTime * 1.35);
    float waveB = cos(worldPosition.z * 0.58 - worldPosition.x * 0.27 - uTime * 1.05);
    worldPosition.y += (waveA * 0.018 + waveB * 0.010) * aWaveWeight;

    vWorldPosition = worldPosition.xyz;
    vWorldNormal = mat3(uModel) * aNormal;
    vTexCoord = aTexCoord;
    gl_Position = uProjection * uView * worldPosition;
}

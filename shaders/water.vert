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
out vec2 vFlowDirection;

void main() {
    vec4 worldPosition = uModel * vec4(aPosition, 1.0);
    vec2 flowDirection = normalize(aTexCoord);
    if (length(flowDirection) < 0.5)
        flowDirection = vec2(1.0, 0.0);
    vec2 crossDirection = vec2(-flowDirection.y, flowDirection.x);
    float alongFlow = dot(worldPosition.xz, flowDirection);
    float acrossFlow = dot(worldPosition.xz, crossDirection);
    float waveA = sin(alongFlow * 0.72 - uTime * 1.35);
    float waveB = cos(alongFlow * 0.27 + acrossFlow * 0.58 + uTime * 1.05);
    worldPosition.y += (waveA * 0.018 + waveB * 0.010) * aWaveWeight;

    vWorldPosition = worldPosition.xyz;
    vWorldNormal = mat3(uModel) * aNormal;
    vFlowDirection = flowDirection;
    gl_Position = uProjection * uView * worldPosition;
}

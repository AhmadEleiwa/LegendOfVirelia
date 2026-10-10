#version 330 core
layout (location = 0) in vec3 inPos;
layout (location = 1) in vec2 inTex;
layout (location = 2) in vec3 inNormal;
layout (location = 3) in float inLighting; // Per-vertex lighting from voxel lighting system
layout (location = 4) in float inEmissive;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMatrix;
out vec3 fragPos;
out vec2 fragTex;
out vec3 fragNormal;
smooth out float fragLighting;
out float fragEmissive;

void main() {
    fragPos = vec3(model * vec4(inPos, 1.0));
    fragTex = inTex;
    fragNormal = normalize(normalMatrix * inNormal);
    fragLighting = inLighting; // Pass through the voxel lighting
    fragEmissive = inEmissive;
    
    gl_Position = projection * view * vec4(fragPos, 1.0);
}
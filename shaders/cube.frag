#version 330 core
#define MAX_POINT_LIGHTS 32

struct DirectionalLight {
    vec3 direction;
    vec3 ambient;
    vec3 diffuse;
};

struct PointLight {
    vec3 position;
    vec3 ambient;
    vec3 diffuse;
    
    float constant;
    float linear;
    float quadratic;
};

in vec3 fragPos;
in vec2 fragTex;
in vec3 fragNormal;
in float fragLighting; // Voxel lighting from vertex shader

uniform sampler2D texture_sampler;

uniform DirectionalLight dirLight;
uniform int numPointLights;
uniform PointLight pointLights[MAX_POINT_LIGHTS];

out vec4 fragColor;

void main() {
    vec3 texColor = texture(texture_sampler, fragTex).rgb;
    vec3 norm = normalize(fragNormal);
    
    // Base voxel lighting (this should be the same for all vertices of a face)
    float voxelLight = fragLighting;
    
    // Add simple directional lighting for face orientation
    vec3 lightDir = normalize(-dirLight.direction);
    float faceLighting = dot(norm, lightDir);
    
    // Create face-based lighting multipliers based on normal direction
    float faceMultiplier = 1.0;
    
    // Top faces are brightest
    if (norm.y > 0.9) {
        faceMultiplier = 1.0;
    }
    // Bottom faces are darkest  
    else if (norm.y < -0.9) {
        faceMultiplier = 0.5;
    }
    // Side faces get medium lighting
    else {
        // North/South faces (Z direction)
        if (abs(norm.z) > abs(norm.x)) {
            faceMultiplier = 0.8;
        }
        // East/West faces (X direction)  
        else {
            faceMultiplier = 0.6;
        }
    }
    
    // Combine voxel lighting with face orientation
    vec3 result = texColor * voxelLight * faceMultiplier;
    
    // Add minimal ambient to prevent complete darkness
    vec3 ambient = texColor * 0.1;
    result = max(result, ambient);
    
    fragColor = vec4(result, 1.0);
}
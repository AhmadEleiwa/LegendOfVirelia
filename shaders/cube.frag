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

out vec4 fragColor;

// Function to calculate directional light contribution
vec3 CalcDirLight(DirectionalLight light, vec3 normal, vec3 texColor) {
    vec3 lightDir = normalize(-light.direction);
    
    // Diffuse shading
    float diff = max(dot(normal, lightDir), 0.0);
    
    // Combine results (no specular for voxel games)
    vec3 ambient = light.ambient * texColor;
    vec3 diffuse = light.diffuse * diff * texColor;
    
    return (ambient + diffuse);
}

void main() {
    // 1. Sample the full RGBA color to get the alpha (transparency) channel
    vec4 texColorFull = texture(texture_sampler, fragTex);
    
    // 2. Discard the fragment if it's transparent (alpha threshold)
    if (texColorFull.a < 0.1) {
        discard;
    }
    
    // Extract the RGB for the rest of your lighting calculations
    vec3 texColor = texColorFull.rgb;
    vec3 norm = normalize(fragNormal);
    
    // Calculate directional light (sun) - reduced influence for voxel games
    vec3 result = CalcDirLight(dirLight, norm, texColor) * 0.3; // Reduce sun influence
    

    // Apply voxel-based lighting (this is the main lighting for minecraft-style games)
    vec3 voxelLit = texColor * fragLighting;
    
    // Combine directional/point lights with voxel lighting
    // Voxel lighting is primary, directional/point lights add subtle enhancement
    result = mix(result, voxelLit, 0.8); // 80% voxel lighting, 20% traditional lighting
    
    // Ensure minimum brightness
    result = max(result, texColor * 0.1);
    
    fragColor = vec4(result, 1.0);
}
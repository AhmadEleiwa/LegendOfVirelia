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
smooth in float fragLighting;
in float fragEmissive;

uniform sampler2D texture_sampler;

uniform DirectionalLight dirLight;
uniform vec3 cameraPosition;
uniform vec3 fogColor;
uniform float fogNear;
uniform float fogFar;
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
    
    if (texColorFull.a < 0.1) {
        discard;
    }
    
    vec3 texColor = texColorFull.rgb;
    vec3 norm = normalize(fragNormal);
    
    float voxelLight = clamp(fragLighting, 0.0, 1.0);
    float diffuse = max(dot(norm, normalize(-dirLight.direction)), 0.0);
    vec3 directionalLight = dirLight.ambient + dirLight.diffuse * diffuse;
    float smoothVoxelLight = mix(voxelLight, smoothstep(0.0, 1.0, voxelLight), 0.35);
    vec3 illumination = mix(vec3(0.08), vec3(1.0), smoothVoxelLight);
    illumination *= 0.7 + 0.3 * directionalLight;

    vec3 litColor = texColor * illumination;
    float emissive = clamp(fragEmissive, 0.0, 1.0);
    vec3 warmEmission = vec3(1.0, 0.52, 0.16) * emissive;
    litColor += warmEmission * 1.25;

    float viewDistance = distance(fragPos, cameraPosition);
    float fogAmount = smoothstep(fogNear, fogFar, viewDistance);
    vec3 viewDirection = viewDistance > 0.001
            ? (fragPos - cameraPosition) / viewDistance
            : vec3(0.0, 0.0, 0.0);
    float sunAlignment = max(dot(viewDirection, normalize(-dirLight.direction)), 0.0);
    float scattering = pow(sunAlignment, 20.0) * fogAmount * 0.10;
    vec3 foggedColor = mix(litColor, fogColor, fogAmount);
    foggedColor += dirLight.diffuse * scattering;
    fragColor = vec4(foggedColor, 1.0);
}
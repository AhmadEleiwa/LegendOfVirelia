#version 330 core
#define MAX_POINT_LIGHTS 32

// --- STRUCTURES ---

struct DirectionalLight {
    vec3 direction; // Light direction (e.g., sun)
    vec3 ambient;
    vec3 diffuse;
};

struct PointLight {
    vec3 position; // Light position (e.g., torch)
    vec3 ambient;
    vec3 diffuse;
    
    // Attenuation factors
    float constant;
    float linear;
    float quadratic;
};

// --- INPUTS ---

in vec3 fragPos;
in vec2 fragTex;
in vec3 fragNormal;
in float fragLighting; // Voxel lighting from vertex shader (e.g., Sky Light or Block Light)

// --- UNIFORMS ---

uniform sampler2D texture_sampler;

uniform DirectionalLight dirLight;
uniform int numPointLights;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
// --- OUTPUTS ---

out vec4 fragColor;

// --- FUNCTIONS ---

// Calculates the lighting contribution from a single PointLight source
vec3 CalcPointLight(PointLight light, vec3 normal, vec3 texColor)
{
    vec3 lightDir = normalize(light.position - fragPos);
    
    // 1. Diffuse (Light Direction) Calculation
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 diffuseResult = light.diffuse * diff * texColor;
    
    // 2. Attenuation Calculation
    float distance    = length(light.position - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));  
    
    // 3. Ambient Calculation (scaled by ambient color)
    vec3 ambientResult = light.ambient * texColor;
    
    // Combine and apply attenuation
    return (ambientResult + diffuseResult) * attenuation;
}

// Calculates the lighting contribution from the DirectionalLight (Sun/Moon)
vec3 CalcDirLight(DirectionalLight light, vec3 normal, vec3 texColor)
{
    vec3 lightDir = normalize(-light.direction);
    
    // 1. Diffuse (Light Direction) Calculation
    // Use the maximum of the dot product and 0.0 to ensure dark faces don't wrap around to positive light
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 diffuseResult = light.diffuse * diff * texColor;
    
    // 2. Ambient Calculation (basic ambient light for non-illuminated areas)
    vec3 ambientResult = light.ambient * texColor;
    
    return ambientResult + diffuseResult;
}


// --- MAIN ---

void main() {
    vec3 texColor = texture(texture_sampler, fragTex).rgb;
    vec3 norm = normalize(fragNormal);
    
    // 1. BASE LIGHTING (Voxel & Directional)
    
    // Calculate DirLight (Sun/Moon)
    vec3 dirLighting = CalcDirLight(dirLight, norm, texColor);
    
    // 2. POINT LIGHTING (Torches, etc.)
    vec3 pointLighting = vec3(0.0);
    for(int i = 0; i < numPointLights; i++)
    {
        pointLighting += CalcPointLight(pointLights[i], norm, texColor);    
    }
    
    // 3. FINAL COMPOSITION
    
    // The fragLighting is your pre-calculated block/sky light value (0.0 to 1.0).
    // We scale the DirLighting and PointLighting by this factor. 
    // This is the core of mixing voxel and standard lighting.
    
    // We combine the directional lighting (dirLighting) with point lighting (pointLighting).
    // The voxel lighting (fragLighting) acts as a high-level intensity multiplier.
    vec3 finalLight = (dirLighting + pointLighting) * fragLighting;

    // Apply the final color, ensuring minimal ambient is present
    vec3 finalColor = max(finalLight, texColor * 0.1); // 0.1 is the hard-coded minimal ambient
    
    // 4. VOXEL FACE CORRECTION (Optional, but helps with classic look)
    // You can re-introduce a version of your face multiplier to enhance the blocky look.
    float faceMultiplier = 1.0;
    if (norm.y < -0.9) {
        faceMultiplier = 0.8; // Make bottom faces noticeably darker
    }
    // Apply a subtle darkening/brightening based on face orientation
    finalColor *= faceMultiplier;
    
    fragColor = vec4(finalColor, 1.0);
}
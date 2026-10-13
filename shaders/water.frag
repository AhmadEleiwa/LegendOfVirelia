#version 330 core

in vec3 vWorldPosition;
in vec3 vWorldNormal;
in vec2 vFlowDirection;

uniform sampler2D uWaterTexture;
uniform vec3 uCameraPosition;
uniform vec3 uLightDirection;
uniform vec3 uWaterColor;
uniform float uTime;

out vec4 fragColor;

void main() {
    vec3 geometricNormal = normalize(vWorldNormal);
    vec2 projection = abs(geometricNormal.y) > 0.5
            ? vWorldPosition.xz
            : abs(geometricNormal.x) > 0.5
                    ? vWorldPosition.zy
                    : vWorldPosition.xy;
    vec2 uv = projection * 0.22;
    vec2 flowDirection = normalize(vFlowDirection);
    vec2 crossDirection = vec2(-flowDirection.y, flowDirection.x);
    vec2 uvA = uv - flowDirection * uTime * 0.018;
    vec2 uvB = uv * 1.37 - (flowDirection * 0.011 + crossDirection * 0.008) * uTime;

    vec3 baseColor = mix(texture(uWaterTexture, uvA).rgb,
            texture(uWaterTexture, uvB).rgb, 0.5) * uWaterColor;

    vec3 lightDirection = normalize(-uLightDirection);
    float diffuse = max(dot(geometricNormal, lightDirection), 0.0);
    vec3 viewDirection = normalize(uCameraPosition - vWorldPosition);
    vec3 halfDirection = normalize(lightDirection + viewDirection);
    float specular = pow(max(dot(geometricNormal, halfDirection), 0.0), 48.0);
    float fresnel = pow(1.0 - max(dot(geometricNormal, viewDirection), 0.0), 3.0);
    vec3 color = baseColor * (0.42 + diffuse * 0.48);
    color += vec3(0.65, 0.82, 0.9) * (specular * 0.48 + fresnel * 0.14);
    fragColor = vec4(color, 0.78);
}

#version 330 core

in vec3 vWorldPosition;
in vec3 vWorldNormal;
in vec2 vTexCoord;

uniform sampler2D uWaterTexture;
uniform sampler2D uNormalTexture;
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
    vec2 uvA = uv + vec2(uTime * 0.018, uTime * 0.008);
    vec2 uvB = uv * 1.37 + vec2(-uTime * 0.011, uTime * 0.015);

    vec3 baseColor = mix(texture(uWaterTexture, uvA).rgb,
            texture(uWaterTexture, uvB).rgb, 0.5) * uWaterColor;
    vec3 tangentNormalA = texture(uNormalTexture, uvA).rgb * 2.0 - 1.0;
    vec3 tangentNormalB = texture(uNormalTexture, uvB).rgb * 2.0 - 1.0;
    vec3 tangentNormal = normalize(tangentNormalA + tangentNormalB);

    vec3 positionDx = dFdx(vWorldPosition);
    vec3 positionDy = dFdy(vWorldPosition);
    vec2 uvDx = dFdx(uv);
    vec2 uvDy = dFdy(uv);
    vec3 tangent = normalize(positionDx * uvDy.y - positionDy * uvDx.y);
    vec3 bitangent = normalize(-positionDx * uvDy.x + positionDy * uvDx.x);
    mat3 tangentToWorld = mat3(tangent, bitangent, geometricNormal);
    vec3 normal = normalize(tangentToWorld * tangentNormal);

    vec3 lightDirection = normalize(-uLightDirection);
    float diffuse = max(dot(normal, lightDirection), 0.0);
    vec3 viewDirection = normalize(uCameraPosition - vWorldPosition);
    vec3 halfDirection = normalize(lightDirection + viewDirection);
    float specular = pow(max(dot(normal, halfDirection), 0.0), 48.0);
    float fresnel = pow(1.0 - max(dot(normal, viewDirection), 0.0), 3.0);
    vec3 color = baseColor * (0.42 + diffuse * 0.48);
    color += vec3(0.65, 0.82, 0.9) * (specular * 0.48 + fresnel * 0.14);
    fragColor = vec4(color, 0.78);
}

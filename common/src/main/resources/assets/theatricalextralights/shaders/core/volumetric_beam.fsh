#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 goboTex = texture(Sampler0, texCoord0);

    float goboAlpha = pow(goboTex.a, 0.7);
    vec2 p = texCoord0 * 2.0 - 1.0;
    float radial = 1.0 - smoothstep(0.35, 1.0, length(p));
    float core = exp(-dot(p, p) * 2.2);

    float finalAlpha = goboAlpha * vertexColor.a * max(radial, core * 0.35);
    vec3 finalRGB = goboTex.rgb * vertexColor.rgb;

    fragColor = vec4(finalRGB, finalAlpha);
}
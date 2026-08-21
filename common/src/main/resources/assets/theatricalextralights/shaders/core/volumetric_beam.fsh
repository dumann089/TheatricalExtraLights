#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec4 VertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 uv0 = texCoord0;
    vec2 uv1 = vec2(texCoord0.x - 1.15, texCoord0.y);

    bool inBounds0 = (uv0.x >= 0.0 && uv0.x <= 1.0 && uv0.y >= 0.0 && uv0.y <= 1.0);
    bool inBounds1 = (uv1.x >= 0.0 && uv1.x <= 1.0 && uv1.y >= 0.0 && uv1.y <= 1.0);

    vec4 texColor0 = inBounds0 ? texture(Sampler0, uv0) : vec4(0.0);
    vec4 texColor1 = inBounds1 ? texture(Sampler1, uv1) : vec4(0.0);

    vec4 texColor = texColor0 + texColor1;

    if (texColor.a < 0.01) discard;

    fragColor = texColor * VertexColor;
}
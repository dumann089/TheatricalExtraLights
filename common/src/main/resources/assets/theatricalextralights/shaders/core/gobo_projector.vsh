#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vertexColor;
out vec2 vertexUV;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vertexColor = Color;
    vertexUV = UV0;
}
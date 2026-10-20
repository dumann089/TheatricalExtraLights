#version 150

// Le quad arrive deja en coordonnees normalisees d'ecran : le fragment shader reconstruit
// lui-meme le rayon de vue depuis la matrice de projection inverse.
in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
}

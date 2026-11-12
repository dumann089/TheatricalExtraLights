#version 150

// Reduction de la cible de lueur vers la demi-resolution : 4 prelevements bilineaires
// decales d'un demi-texel, soit une moyenne 4x4 douce, et un leger coude pour que seuls les
// pixels vraiment lumineux nourrissent le halo.
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 px = 1.0 / InSize;
    vec3 c = texture(DiffuseSampler, texCoord + px * vec2(-0.5, -0.5)).rgb
           + texture(DiffuseSampler, texCoord + px * vec2( 0.5, -0.5)).rgb
           + texture(DiffuseSampler, texCoord + px * vec2(-0.5,  0.5)).rgb
           + texture(DiffuseSampler, texCoord + px * vec2( 0.5,  0.5)).rgb;
    c *= 0.25;
    float lum = max(c.r, max(c.g, c.b));
    float knee = smoothstep(0.04, 0.35, lum);
    fragColor = vec4(c * knee, 1.0);
}

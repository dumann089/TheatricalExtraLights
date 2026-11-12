#version 150

// Flou gaussien separable, 9 prelevements le long de BlurDir espaces de Step texels.
// Deux allers-retours avec des pas differents donnent un halo large et sans bandes.
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Step;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 delta = BlurDir * Step / InSize;
    vec3 sum = texture(DiffuseSampler, texCoord).rgb * 0.2270270;
    sum += texture(DiffuseSampler, texCoord + delta * 1.0).rgb * 0.1945946;
    sum += texture(DiffuseSampler, texCoord - delta * 1.0).rgb * 0.1945946;
    sum += texture(DiffuseSampler, texCoord + delta * 2.0).rgb * 0.1216216;
    sum += texture(DiffuseSampler, texCoord - delta * 2.0).rgb * 0.1216216;
    sum += texture(DiffuseSampler, texCoord + delta * 3.0).rgb * 0.0540541;
    sum += texture(DiffuseSampler, texCoord - delta * 3.0).rgb * 0.0540541;
    sum += texture(DiffuseSampler, texCoord + delta * 4.0).rgb * 0.0162162;
    sum += texture(DiffuseSampler, texCoord - delta * 4.0).rgb * 0.0162162;
    fragColor = vec4(sum, 1.0);
}

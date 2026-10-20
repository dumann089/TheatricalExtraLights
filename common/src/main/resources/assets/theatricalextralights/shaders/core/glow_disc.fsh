#version 150

// ── Disque de lueur en un seul quad ──────────────────────────────────────────
// Les halos des strobes, l'eblouissement des blinders et les lueurs des LED etaient des
// disques de N anneaux x M segments (6 x 24, ou 3 x 12 en basse definition), soit jusqu'a
// 576 sommets par disque recalcules en Java a chaque image. Ce shader dessine le meme disque
// sur un seul quad : pour chaque pixel il retrouve le segment, l'anneau et le triangle du
// maillage d'origine (decoupe 0,1,2 / 2,3,0 de Minecraft), puis refait la meme interpolation
// et le meme arrondi par anneau (alpha = int(force x profil x 255) aux sommets).
//
// Sommet :
//   Color  = couleur de l'anneau (rgb) et octet fort de la force (a)
//   UV0    = position dans le disque, en rayons ; x + 4 en basse definition ; x > 6 : quad plein
//   UV2    = (coeur r | coeur g << 8, coeur b | octet faible de la force << 8)

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
flat in ivec2 packedCore;

out vec4 fragColor;

const float TWO_PI = 6.28318530718;

// (float) Math.pow(1f - k / rings, 2.4), dernier anneau a 0.
const float P6[7] = float[7](1.0, 0.6456021666526794, 0.3779034912586212, 0.18946456909179688,
                             0.0715993270277977, 0.013565541245043278, 0.0);
const float P3[4] = float[4](1.0, 0.3779034912586212, 0.0715993270277977, 0.0);

float ringProfile(bool lo, int k) {
    return lo ? P3[k] : P6[k];
}

float ringAlpha(float strength, float p) {
    return clamp(floor(strength * p * 255.0), 0.0, 255.0) / 255.0;
}

void main() {
    vec2 uv = texCoord0;
    if (uv.x > 6.0) {
        // Quad plein (face du flash, point de LED) : couleur du sommet telle quelle.
        vec4 c = vertexColor;
        if (c.a == 0.0) {
            discard;
        }
        fragColor = c * ColorModulator;
        return;
    }
    bool lo = uv.x > 2.0;
    if (lo) {
        uv.x -= 4.0;
    }
    int rings = lo ? 3 : 6;
    int segments = lo ? 12 : 24;
    float step = TWO_PI / float(segments);

    float ang = atan(uv.y, uv.x);
    if (ang < 0.0) {
        ang += TWO_PI;
    }
    int seg = clamp(int(floor(ang / step)), 0, segments - 1);
    vec2 d0 = vec2(cos(float(seg) * step), sin(float(seg) * step));
    vec2 d1 = vec2(cos(float(seg + 1) * step), sin(float(seg + 1) * step));

    // uv = a * d0 + b * d1 dans le secteur ; a + b est la position sur la corde des anneaux.
    float det = d0.x * d1.y - d0.y * d1.x;
    float a = (uv.x * d1.y - uv.y * d1.x) / det;
    float b = (d0.x * uv.y - d0.y * uv.x) / det;
    float s = a + b;
    if (s >= 1.0) {
        discard;
    }
    int k = clamp(int(floor(s * float(rings))), 0, rings - 1);
    float rk = float(k) / float(rings);
    float rk1 = float(k + 1) / float(rings);

    // Poids des sommets interieurs de l'anneau, triangle par triangle.
    float wIn;
    if (k == 0) {
        wIn = 1.0 - s / rk1;
    } else if (a / rk + b / rk1 >= 1.0) {
        float w2 = b / rk1;
        wIn = ((1.0 - w2) * rk1 - a) / (rk1 - rk);
    } else {
        float w0 = a / rk;
        float w2 = (b - (1.0 - w0) * rk) / (rk1 - rk);
        wIn = 1.0 - w2;
    }
    wIn = clamp(wIn, 0.0, 1.0);

    int sHi = int(vertexColor.a * 255.0 + 0.5);
    int packedX = packedCore.x & 0xFFFF;
    int packedY = packedCore.y & 0xFFFF;
    float strength = float((sHi << 8) | (packedY >> 8)) / 16383.0;

    float qIn = ringAlpha(strength, ringProfile(lo, k));
    float qOut = k + 1 == rings ? 0.0 : ringAlpha(strength, ringProfile(lo, k + 1));
    float alpha = qIn * wIn + qOut * (1.0 - wIn);
    if (alpha == 0.0) {
        discard;
    }

    vec3 ring = vertexColor.rgb;
    vec3 core = vec3(float(packedX & 0xFF), float(packedX >> 8), float(packedY & 0xFF)) / 255.0;
    vec3 colour = k == 0 ? core * wIn + ring * (1.0 - wIn) : ring;
    fragColor = vec4(colour, alpha) * ColorModulator;
}

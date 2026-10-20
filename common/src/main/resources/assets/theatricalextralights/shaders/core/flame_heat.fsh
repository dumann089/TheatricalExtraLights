#version 150

// ── Distorsion de chaleur ────────────────────────────────────────────────────
// L'air chaud au-dessus et autour d'une flamme refracte : l'image derriere tremble. Ce shader
// relit une copie de l'ecran (Sampler0) avec un decalage anime, fort pres de l'axe de la
// flamme et dans le panache qui monte au-dessus, nul ailleurs. Ce qui est devant la flamme
// (plus proche que le panache) n'est pas deforme.

uniform sampler2D Sampler0;   // copie de l'image
uniform sampler2D Sampler1;   // profondeur de la scene

uniform mat4 InvProjMat;
uniform vec2 ScreenSize;

uniform vec3 FlameOrigin;
uniform vec3 FlameDir;
uniform float Length;
uniform float Front;
uniform float Base;
uniform float Radius0;
uniform float RadiusEnd;
uniform float Fade;
uniform float Strength;
uniform float Seed;
uniform float Time;

out vec4 fragColor;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(
        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
        f.z);
}

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec3 rd = normalize(reconstructViewPos(uv, 1.0));

    // Point de l'axe (flamme puis panache au-dessus) le plus proche du rayon de vue.
    float zTop = Front + 0.6 * Length + 1.0;
    vec3 a = FlameOrigin + FlameDir * Base;
    vec3 seg = FlameDir * max(zTop - Base, 0.01);
    float b = dot(rd, seg);
    float c = dot(seg, seg);
    float denom = c - b * b;
    float sParam = denom > 1.0e-6 ? clamp((dot(rd, a) * b - dot(a, seg)) / denom, 0.0, 1.0) : 0.0;
    vec3 onAxis = a + seg * sParam;
    float tRay = max(dot(onAxis, rd), 0.0);
    float dist = length(rd * tRay - onAxis);
    float z = Base + sParam * (zTop - Base);

    // Largeur : celle de la flamme, puis le panache qui s'elargit en montant.
    float sFlame = clamp(z / max(Length, 0.1), 0.0, 1.0);
    float width = mix(Radius0, RadiusEnd, pow(sFlame, 0.6)) * 1.5
                + max(z - Front, 0.0) * 0.35 + 0.15;
    float w = exp(-pow(dist / width, 2.0));
    w *= smoothstep(Base - 0.1, Base + 0.4, z);
    w *= 1.0 - smoothstep(Front + 0.3 * Length, zTop, z);
    w *= Fade;
    if (w < 0.01) {
        discard;
    }

    // Rien devant la flamme ne tremble : la scene doit etre derriere le panache.
    float sceneT = length(reconstructViewPos(uv, texture(Sampler1, uv).r));
    if (sceneT < tRay - width) {
        discard;
    }

    // Decalage anime, cellules qui montent avec l'air chaud ; amplitude angulaire a peu pres
    // constante, plafonnee de pres pour ne pas casser l'image quand on a le nez dans la flamme.
    vec3 q = vec3(dist * 2.5, z * 2.2 - Time * 3.5, Seed * 11.0);
    vec2 off = vec2(vnoise(q), vnoise(q + vec3(5.2, 1.3, 7.7))) - 0.5;
    float px = Strength * 9.0 * w * clamp(6.0 / max(tRay, 0.5), 0.35, 1.6);
    vec2 sampleUV = uv + off * px / ScreenSize;
    fragColor = vec4(texture(Sampler0, sampleUV).rgb, 1.0);
}

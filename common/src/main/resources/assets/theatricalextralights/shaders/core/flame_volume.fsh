#version 150

// ── Flamme de scene en volume ────────────────────────────────────────────────
// Une flamme de propane n'est pas un sprite : c'est un volume de gaz qui brule, turbulent, qui
// s'ecoule depuis la buse. Ce shader le marche sur la longueur du rayon de vue :
//   - forme : un cone qui s'evase le long du jet, courbe vers le haut par la poussee
//     d'Archimede (jet incline), bord ronge par un bruit fractal qui s'ecoule avec le gaz ;
//   - temperature : chaude au coeur et pres de la buse, froide en peripherie et en bout ;
//   - couleur : rayonnement de corps noir (rouge sombre -> orange -> jaune -> blanc), pied
//     bleu a la buse ou le melange brule proprement ;
//   - suie : en bout de flamme le gaz refroidi absorbe, et assombrit un peu ce qui est derriere.
// Sortie en alpha premultiplie (ONE, ONE_MINUS_SRC_ALPHA) : emission ajoutee, suie qui couvre.
//
// Repere de la flamme (vue) : FlameDir le long du jet, FlameU le cote vers lequel elle se
// courbe (le haut projete), FlameV le troisieme axe. Longueurs en blocs.

uniform sampler2D Sampler1;   // profondeur de la scene

uniform mat4 InvProjMat;
uniform vec2 ScreenSize;
uniform int StepCount;
uniform int NoiseOctaves;

uniform vec3 FlameOrigin;
uniform vec3 FlameDir;
uniform vec3 FlameU;
uniform vec3 FlameV;
uniform vec3 FlameOriginW;    // repere monde : la turbulence suit le gaz, pas la camera

uniform float Length;         // longueur nominale du jet
uniform float Front;          // position du front de flamme (allumage), 0..Length
uniform float Base;           // pied de flamme, > 0 quand la flamme se detache a l'arret
uniform float Radius0;        // rayon a la buse
uniform float RadiusEnd;      // rayon en bout de jet
uniform float Bend;           // ecart lateral du bout de flamme (vers FlameU)
uniform float Bulb;           // gonflement en bout (style burst)
uniform float Turbulence;
uniform float FlowSpeed;      // vitesse d'ecoulement du gaz, blocs / s
uniform float Fade;           // extinction, 1 allume -> 0 eteint
uniform float Intensity;
uniform float Brightness;
uniform float Seed;
uniform float Time;

out vec4 fragColor;

// ── Bruit ────────────────────────────────────────────────────────────────────

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

float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int o = 0; o < 5; o++) {
        if (o >= NoiseOctaves) break;
        sum += amp * vnoise(p);
        norm += amp;
        p = p * 2.07 + vec3(17.1, 9.3, 4.7);
        amp *= 0.5;
    }
    return sum / max(norm, 1.0e-4);
}

float ign(vec2 px) {
    return fract(52.9829189 * fract(0.06711056 * px.x + 0.00583715 * px.y));
}

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

// Rayonnement de corps noir, temperature normalisee 0..1 (env. 800 K .. 2300 K).
vec3 blackbody(float t) {
    t = clamp(t, 0.0, 1.0);
    vec3 c0 = vec3(0.0, 0.0, 0.0);
    vec3 c1 = vec3(0.42, 0.035, 0.0);
    vec3 c2 = vec3(1.0, 0.30, 0.025);
    vec3 c3 = vec3(1.0, 0.62, 0.16);
    vec3 c4 = vec3(1.0, 0.90, 0.66);
    if (t < 0.25) return mix(c0, c1, t / 0.25);
    if (t < 0.5) return mix(c1, c2, (t - 0.25) / 0.25);
    if (t < 0.75) return mix(c2, c3, (t - 0.5) / 0.25);
    return mix(c3, c4, (t - 0.75) / 0.25);
}

float rmax() {
    return RadiusEnd * (1.0 + Bulb) + 0.35;
}

// Intersection du rayon avec la boite du volume, dans le repere de la flamme.
bool boxInterval(vec3 ro, vec3 rd, out float t0, out float t1) {
    vec3 o = ro - FlameOrigin;
    vec3 lo = vec3(dot(o, FlameU), dot(o, FlameV), dot(o, FlameDir));
    vec3 ld = vec3(dot(rd, FlameU), dot(rd, FlameV), dot(rd, FlameDir));
    float r = rmax();
    vec3 bmin = vec3(-r + min(0.0, Bend), -r, Base - 0.3);
    vec3 bmax = vec3(r + max(0.0, Bend), r, Front + r);
    vec3 dd = vec3(
        abs(ld.x) < 1.0e-6 ? 1.0e-6 : ld.x,
        abs(ld.y) < 1.0e-6 ? 1.0e-6 : ld.y,
        abs(ld.z) < 1.0e-6 ? 1.0e-6 : ld.z);
    vec3 a = (bmin - lo) / dd;
    vec3 b = (bmax - lo) / dd;
    t0 = max(max(min(a.x, b.x), min(a.y, b.y)), min(a.z, b.z));
    t1 = min(min(max(a.x, b.x), max(a.y, b.y)), max(a.z, b.z));
    t0 = max(t0, 0.0);
    return t1 > t0;
}

void main() {
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;
    float depth = texture(Sampler1, screenUV).r;
    float sceneT = length(reconstructViewPos(screenUV, depth));
    vec3 rd = normalize(reconstructViewPos(screenUV, 1.0));

    float tEnter;
    float tExit;
    if (!boxInterval(vec3(0.0), rd, tEnter, tExit)) {
        discard;
    }
    tExit = min(tExit, sceneT - 0.02);
    if (tExit <= tEnter) {
        discard;
    }

    int steps = clamp(StepCount, 6, 64);
    float dt = (tExit - tEnter) / float(steps);
    float t = tEnter + dt * ign(gl_FragCoord.xy);

    vec3 emitted = vec3(0.0);
    // Deux transmittances : la flamme est optiquement epaisse pour sa propre lumiere (on n'en voit
    // que la couche avant, d'ou les langues lisibles), mais elle ne masque l'arriere-plan que
    // par sa suie.
    float transE = 1.0;
    float trans = 1.0;
    float invLen = 1.0 / max(Length, 0.1);
    vec3 seedOff = vec3(Seed * 13.7, Seed * 7.3, Seed * 3.1);

    for (int i = 0; i < 64; i++) {
        if (i >= steps || transE < 0.02) break;
        vec3 p = rd * t - FlameOrigin;
        float z = dot(p, FlameDir);
        float s = clamp(z * invLen, 0.0, 1.2);
        float x = dot(p, FlameU) - Bend * s * s;
        float y = dot(p, FlameV);
        float r = length(vec2(x, y));

        // Profil : s'evase depuis la buse, gonfle vers les deux tiers, s'effile en bout ; le
        // style burst ajoute une boule au front de flamme.
        float sc = clamp(s, 0.0, 1.0);
        float radius = mix(Radius0, RadiusEnd, pow(sc, 0.6)) * (1.0 - 0.35 * smoothstep(0.72, 1.05, sc));
        radius *= 1.0 + Bulb * exp(-pow((z - Front * 0.82) / max(0.2 * Length, 0.2), 2.0));

        if (r < radius * 1.7 && z > Base - 0.2 && z < Front + radius) {
            // Turbulence qui s'ecoule avec le gaz : bruit a l'echelle du rayon local, etire dans le
            // sens du jet, decale avec le temps, deforme par un premier bruit (langues de flamme).
            float scale = 1.0 / max(radius, 0.05);
            // Langues de flamme : structures etirees le long du jet (frequence axiale ~4x plus
            // basse), qui ondulent en travers. Le gaz monte, le motif avec lui.
            // Echelle axiale constante : le decalage temporel (Time x FlowSpeed, des dizaines de
            // blocs) ne doit pas etre multiplie par une echelle qui varie avec z.
            float zf = (z - Time * FlowSpeed) * 0.62 / (Radius0 + RadiusEnd);
            vec3 q = vec3(x * scale * 1.25, y * scale * 1.25, zf) + seedOff;
            vec2 sway = vec2(vnoise(vec3(zf * 0.8, 1.7, Time * 0.9) + seedOff),
                             vnoise(vec3(zf * 0.8, 8.3, Time * 0.9) + seedOff)) - 0.5;
            q.xy += sway * 1.8 * Turbulence;
            float warp = vnoise(q * vec3(0.6, 0.6, 1.4) + vec3(0.0, 0.0, 5.3));
            q.xy += (warp - 0.5) * 1.4 * Turbulence;
            float n = fbm(q);

            // Champ de flamme : bruit moins distance a l'axe ; il s'amincit vers le bout.
            float rn = r / radius;
            float field = (n - 0.5) * 2.1 * Turbulence + (1.0 - rn * rn) * 0.95 - s * 0.32;
            // Bout de flamme : des langues depassent ou restent en deca du front au gre du bruit.
            float tongue = (n - 0.5) * radius * 3.2;
            field -= smoothstep(Front - 0.9 * radius, Front + 0.6 * radius, z - tongue) * 1.4;
            field -= (1.0 - smoothstep(Base, Base + 0.15 + 0.35 * radius, z)) * 1.2;
            float d = smoothstep(0.0, 0.35, field);

            if (d > 0.002) {
                // Temperature : coeur chaud pres de la buse, langues et bout plus froids.
                // Le champ fait office de temperature : bords froids (rouge sombre), coeur chaud
                // (jaune-blanc) ; le gaz refroidit en s'eloignant de la buse.
                float temp = smoothstep(0.0, 0.62, field) * (1.28 - 0.5 * s);
                temp *= Fade;
                vec3 col = blackbody(temp);
                float glow = pow(temp, 1.5) * 9.0 + temp * 1.2;

                // Pied bleu : le melange brule proprement sur les premiers decimetres.
                float blue = (1.0 - smoothstep(0.02, 0.35, z - Base)) * (1.0 - smoothstep(0.3, 0.9, rn)) * d;
                vec3 e = col * glow + vec3(0.12, 0.25, 1.0) * blue * 2.0 * Fade;

                // Suie : gaz froid en bout de flamme, il absorbe un peu.
                float soot = d * (1.0 - clamp(temp * 1.8, 0.0, 1.0)) * smoothstep(0.5, 1.0, s);
                float a = 1.0 - exp(-(d * 4.5 + soot * 2.0) * dt);

                emitted += transE * e * Intensity * a;
                transE *= 1.0 - a;
                trans *= 1.0 - a * (0.05 + 0.6 * soot / max(d, 1.0e-3)) * Fade;
            }
        }
        t += dt;
    }

    vec3 c = emitted * Brightness;
    float lum = max(c.r, max(c.g, c.b));
    float alpha = clamp(1.0 - trans, 0.0, 1.0);
    if (lum < 0.002 && alpha < 0.004) {
        discard;
    }
    // Compression douce qui garde la teinte : le coeur crame vers le jaune-blanc sans saturer.
    vec3 mapped = lum > 0.0 ? c * ((1.0 - exp(-lum)) / lum) : c;
    // Le coeur surexpose vire au blanc chaud, comme a la camera devant un jet de propane.
    mapped = mix(mapped, vec3(1.0, 0.96, 0.86), smoothstep(2.5, 9.0, lum) * 0.65);
    fragColor = vec4(max(mapped, 0.0), alpha);
}

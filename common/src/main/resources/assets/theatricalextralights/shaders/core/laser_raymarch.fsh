#version 150

// ── Laser de spectacle physiquement plausible ────────────────────────────────
// Un vrai laser est un faisceau collimate de quelques millimetres : il n'existe a l'oeil
// que par la lumiere que la brume renvoie (diffusion de Mie, tres directive vers l'avant)
// et par le point brulant qu'il laisse sur la surface qu'il frappe. Un motif balaye vite
// par les galvos se lit comme une nappe (cone, plan) dont la brillance decroit en 1/r.
//
// Chaque pixel integre analytiquement, sans marche a pas :
//   - faisceaux : gaussienne de la distance rayon/droite, integree le long du rayon,
//   - nappes    : traversee du plan du secteur balaye, ponderee par 1/(r * angle total),
//   - impacts   : tache et trait sur la geometrie, lus dans la profondeur de la scene.
// La figure (jusqu'a 512 segments) arrive dans Sampler2, une texture flottante 8 x N.

uniform sampler2D Sampler1;   // profondeur de la scene
uniform sampler2D Sampler2;   // donnees de la figure (RGBA32F, 8 texels par segment)

uniform mat4 InvProjMat;
uniform vec2 ScreenSize;
uniform int SegCount;

uniform vec3 OriginV;        // sortie du laser, repere vue
uniform vec3 OriginW;        // sortie du laser, repere monde (brume fixe dans la salle)
uniform float PixelAngle;    // angle couvert par un pixel, radians
uniform float BeamRadius;    // rayon physique du faisceau a la sortie, metres
uniform float Divergence;    // tangente de la demi-divergence
uniform float HazeDensity;   // 0 = air limpide (faisceaux invisibles), 1 = brume dense
uniform float Brightness;
uniform float Intensity;
uniform float Anisotropy;
uniform float Extinction;    // attenuation par metre dans la brume a densite 1
uniform float TotalSpan;     // angle total balaye par les nappes, radians
uniform float ScanHead;      // position de la tete de balayage sur la figure, 0..1
uniform float ScanTrail;     // amplitude de la surbrillance derriere la tete
uniform float ImpactEnabled;
uniform float Striation;     // striations radiales des nappes, 0 (scan rapide) .. 1
uniform int NoiseOctaves;
uniform float Time;
uniform float Ambient;

out vec4 fragColor;

const float PI = 3.14159265;
const float INV_SQRT_2PI = 0.39894228;
const float SHEET_GAIN = 55.0;
const float IMPACT_GAIN = 0.55;
const float IMPACT_LINE_GAIN = 6.0;

const int FLAG_HIT0 = 1;
const int FLAG_HIT1 = 2;
const int FLAG_SHEET = 4;
const int FLAG_BEAM0 = 8;
const int FLAG_BEAM1 = 16;

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

// Densite locale de brume en un point du monde. Les volutes derivent lentement, comme la
// sortie d'une machine a brouillard brassee par la ventilation.
float hazeAt(vec3 wp, float contrast) {
    if (HazeDensity <= 0.0001) {
        return 0.0;
    }
    vec3 wind = vec3(Time * 0.015, Time * 0.007, Time * 0.011);
    vec3 p = wp * 0.32 + wind;
    float sum = 0.0;
    float amp = 0.55;
    float norm = 0.0;
    for (int o = 0; o < 3; o++) {
        if (o >= NoiseOctaves) break;
        sum += amp * vnoise(p);
        norm += amp;
        p = p * 2.3 + vec3(9.1, 4.7, 6.3) - wind * 0.8;
        amp *= 0.5;
    }
    float n = norm > 0.0 ? sum / norm : 0.5;
    // Jamais totalement vide : une salle enfumee garde un voile de fond. Contraste modere :
    // des volutes trop marquees se lisent comme des taches blanches dans une nappe.
    float billow = mix(1.0, 0.25 + 1.5 * n * n, contrast);
    return HazeDensity * billow;
}

// Poussieres en suspension qui traversent le faisceau : points brillants brefs, fins, qui
// derivent avec la brume. C'est ce scintillement qui trahit un faisceau reel.
float motes(vec3 wp, float t) {
    if (NoiseOctaves < 2) return 0.0;
    vec3 drift = vec3(Time * 0.11, -Time * 0.05, Time * 0.08);
    // Grains fins (quelques millimetres) : de pres ils restent des points, jamais des taches.
    float a = vnoise(wp * 55.0 + drift * 4.0);
    float b = vnoise(wp * 140.0 - drift * 7.0 + 4.2);
    float m = a * b;
    // A moins de deux metres un grain couvrirait plusieurs pixels : on le fond. Au-dela de
    // vingt metres un grain fait moins d'un pixel et ne serait plus qu'un crepitement : on
    // l'eteint aussi.
    float nearFade = smoothstep(1.5, 5.0, t);
    float farFade = 1.0 - smoothstep(18.0, 32.0, t);
    return pow(max(m - 0.42, 0.0) * 3.4, 4.0) * nearFade * farFade;
}

float henyeyGreenstein(float cosTheta, float g) {
    float g2 = g * g;
    float denom = max(1.0e-4, 1.0 + g2 - 2.0 * g * cosTheta);
    return (1.0 - g2) / (4.0 * PI * pow(denom, 1.5));
}

// Diffusion de Mie simplifiee : un socle isotrope et un lobe avant marque. C'est ce lobe qui
// rend un faisceau eblouissant quand on regarde vers la source et discret de dos.
float phaseFn(vec3 rd, vec3 toLight) {
    float cosTheta = dot(-rd, toLight);
    // La brume de scene diffuse surtout vers l'avant : plancher a 0.6 quel que soit le reglage.
    float g = clamp(max(Anisotropy, 0.6), 0.0, 0.92);
    return 0.18 + henyeyGreenstein(cosTheta, g);
}

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

// Surbrillance qui court le long de la figure derriere la tete de balayage. Nulle quand la
// persistance DMX est a fond : l'oeil integre alors un trait continu.
float scanMod(float path) {
    if (ScanTrail <= 0.0001) {
        return 1.0;
    }
    float behind = fract(ScanHead - path);
    return 1.0 + ScanTrail * exp(-behind * 9.0);
}

// Fondu net mais sans crenelage la ou le faisceau passe derriere la geometrie.
float depthFade(float t, float sceneT) {
    return clamp((sceneT + 0.12 - t) / 0.28, 0.0, 1.0);
}

// ── Faisceau : droite fine, integrale analytique ─────────────────────────────
// La puissance P du faisceau est etalee sur pi*sigma^2. Elargir sigma jusqu'a l'empreinte
// d'un pixel pour l'antialiasing garde l'energie constante : le pic baisse d'autant.

vec3 beamScatter(vec3 rd, vec3 dirV, float len, vec3 dirW, vec3 color, float weight,
                 float path, float sceneT, bool hit) {
    if (weight <= 0.0) {
        return vec3(0.0);
    }
    vec3 O = OriginV;
    float b = dot(rd, dirV);
    float d = dot(rd, -O);
    float e = dot(dirV, -O);
    float denom = 1.0 - b * b;

    float s;
    float t;
    if (denom < 1.0e-5) {
        t = max(dot(O, rd), 0.0);
        s = clamp(dot(rd * t - O, dirV), 0.0, len);
    } else {
        s = clamp((e - b * d) / denom, 0.0, len);
        t = dot(O + dirV * s, rd);
    }
    if (t < 0.03) {
        return vec3(0.0);
    }
    vec3 pr = rd * t;
    vec3 pb = O + dirV * s;
    float dist = distance(pr, pb);

    float sigmaPhys = BeamRadius + s * Divergence;
    float sigma = max(sigmaPhys, PixelAngle * t * 1.1);
    // Halo de diffusion : lobe doux autour du trait, un quart de l'energie. Reference photo :
    // un trait fin et brulant dans un halo qui s'etend sur plusieurs largeurs de faisceau.
    // Sa largeur suit le faisceau physique, pas l'empreinte du pixel : de loin il reste de
    // quelques pixels, sinon les faisceaux qui se croisent a la lentille font une boule.
    float glowSigma = max(sigmaPhys * 7.0, PixelAngle * t * 3.0);
    if (dist > 3.2 * glowSigma) {
        return vec3(0.0);
    }
    float fade = depthFade(t, sceneT);
    if (fade <= 0.0) {
        return vec3(0.0);
    }

    // Vu dans l'axe, l'integrale explose : plafond a ~9 degres pour garder de la couleur.
    float sinTheta = max(sqrt(max(denom, 0.0)), 0.15);
    float core = exp(-0.5 * dist * dist / (sigma * sigma)) / sigma;
    float glow = exp(-0.5 * dist * dist / (glowSigma * glowSigma)) / glowSigma;
    float lineIntegral = INV_SQRT_2PI * (0.75 * core + 0.25 * glow) / sinTheta;

    vec3 wp = OriginW + dirW * s;
    float haze = hazeAt(wp, 0.75);
    if (haze <= 0.0) {
        return vec3(0.0);
    }
    // Les poussieres ne brillent que dans le coeur du faisceau.
    haze += motes(wp, t) * 1.6 * exp(-0.5 * dist * dist / (sigma * sigma));
    // Extinction le long du faisceau, puis entre le point et l'oeil : un faisceau lointain
    // est voile par la brume qui le separe de la camera, un faisceau proche reste net.
    float ext = exp(-Extinction * HazeDensity * (s + 0.7 * t));
    vec3 toLight = normalize(O - pb);
    float phase = phaseFn(rd, toLight);
    float camFade = smoothstep(0.0, 0.8, t);
    // Tout pres de la sortie les faisceaux d'un motif se superposent : court fondu pour
    // ne pas empiler leur energie en un point brulant.
    float apertureFade = smoothstep(0.05, 1.2, s);
    // Un faisceau qui ne touche rien se perd dans l'air au lieu de se couper net.
    float endFade = hit ? 1.0 : 1.0 - smoothstep(len * 0.55, len, s);

    return color * (weight * Intensity * phase * haze * ext * fade * camFade * apertureFade
            * endFade * lineIntegral * scanMod(path));
}

// ── Nappe : secteur balaye entre deux directions ─────────────────────────────
// Le scanner passe un temps proportionnel a l'angle parcouru : la brillance d'une nappe en
// un point vaut P / (r * angle total), quel que soit le decoupage en segments.

vec3 sheetScatter(vec3 rd, vec3 dir0, vec3 dir1, float len0, float len1,
                  vec3 dirW0, vec3 dirW1, float span, vec3 color, float weight,
                  float path0, float path1, float sceneT, bool hit0, bool hit1) {
    if (weight <= 0.0 || span < 1.0e-4) {
        return vec3(0.0);
    }
    vec3 n = cross(dir0, dir1);
    float nl = length(n);
    if (nl < 1.0e-5) {
        return vec3(0.0);
    }
    n /= nl;
    float dn = dot(rd, n);
    if (abs(dn) < 1.0e-5) {
        return vec3(0.0);
    }
    vec3 O = OriginV;
    float t = dot(O, n) / dn;
    if (t < 0.03) {
        return vec3(0.0);
    }
    vec3 q = rd * t - O;
    float r = length(q);
    if (r < 0.02) {
        return vec3(0.0);
    }
    vec3 qn = q / r;
    // Jamais derriere la lentille : pour une nappe tres fine (deux directions presque
    // confondues) les produits vectoriels des bords s'annulent aussi sur l'axe oppose et la
    // nappe ressortirait en ligne droite a travers le projecteur.
    vec3 mid = normalize(dir0 + dir1);
    if (dot(qn, mid) <= 0.0) {
        return vec3(0.0);
    }

    // Bords du secteur adoucis sur l'empreinte d'un pixel ou l'epaisseur du faisceau.
    float s0 = dot(cross(dir0, qn), n);
    float s1 = dot(cross(qn, dir1), n);
    // Angle apparent du bord, borne : pres de la lentille le rapport rayon / distance
    // exploserait et le halo envelopperait la source dans toutes les directions.
    float edgeSoft = clamp((BeamRadius + r * Divergence) / r, PixelAngle * 1.5, 0.02);
    float mask = smoothstep(-edgeSoft, edgeSoft, s0) * smoothstep(-edgeSoft, edgeSoft, s1);
    // Halo au-dela des bords : la nappe ne se coupe pas au rasoir, elle s'eteint sur
    // quelques degres comme la diffusion autour d'un trait. Jamais vers l'arriere.
    float forward = smoothstep(0.55, 0.85, dot(qn, mid));
    float glowSoft = min(edgeSoft * 10.0, 0.05);
    float glow = smoothstep(-glowSoft, glowSoft * 0.5, s0) * smoothstep(-glowSoft, glowSoft * 0.5, s1);
    mask = max(mask, glow * 0.15 * forward);
    if (mask < 0.002) {
        return vec3(0.0);
    }
    float frac = clamp(acos(clamp(dot(dir0, qn), -1.0, 1.0)) / span, 0.0, 1.0);
    float lenAt = mix(len0, len1, frac);
    float endMask = 1.0 - smoothstep(lenAt - 0.35, lenAt + 0.05, r);
    // Bord libre : la nappe s'eteint progressivement la ou aucun des deux rayons ne touche.
    float openness = mix(hit0 ? 0.0 : 1.0, hit1 ? 0.0 : 1.0, frac);
    endMask *= mix(1.0, 1.0 - smoothstep(lenAt * 0.55, lenAt, r), openness);
    if (endMask < 0.002) {
        return vec3(0.0);
    }
    float fade = depthFade(t, sceneT);
    if (fade <= 0.0) {
        return vec3(0.0);
    }

    float cosPhi = max(abs(dn), 0.06);
    float sheetIntegral = SHEET_GAIN / (max(r, 0.8) * TotalSpan * cosPhi);

    vec3 dirW = normalize(mix(dirW0, dirW1, frac));
    float haze = hazeAt(OriginW + dirW * r, 0.4);
    if (haze <= 0.0) {
        return vec3(0.0);
    }
    // Striations radiales : positions successives du scanner, visibles quand il balaie
    // lentement, fondues quand la persistance est a fond. Bruit doux plutot qu'une sinusoide
    // (des raies nettes se lisaient comme des faisceaux), et eteintes des que leur pas fait
    // moins de trois pixels a l'ecran : sinon elles crenelent en anneaux de moire.
    if (Striation > 0.001) {
        float angle = frac * span;
        float period = 2.0 * PI / 160.0;
        float periodPx = period * r / max(PixelAngle * t, 1.0e-6);
        float visible = smoothstep(3.0, 8.0, periodPx);
        float stri = vnoise(vec3(angle * 160.0 / (2.0 * PI), path0 * 6.0, Time * 0.02));
        haze *= 1.0 + Striation * 0.3 * visible * (stri - 0.5);
    }
    float ext = exp(-Extinction * HazeDensity * (r + 0.7 * t));
    float phase = phaseFn(rd, -qn);
    float camFade = smoothstep(0.0, 0.8, t);
    float apertureFade = smoothstep(0.1, 0.8, r);
    float path = mix(path0, path1, frac);

    return color * (weight * Intensity * phase * haze * ext * fade * camFade * apertureFade
            * mask * endMask * sheetIntegral * scanMod(path));
}

// ── Eblouissement a la sortie ────────────────────────────────────────────────
// Vue de face, la lentille d'un laser est un point aveuglant : la diffusion de la brume
// juste devant elle et la lumiere parasite de l'optique. Tres directif : rien de dos.

vec3 apertureGlare(vec3 rd, vec3 meanDir, vec3 color, float sceneT) {
    float t = length(OriginV);
    if (t < 0.3) {
        return vec3(0.0);
    }
    vec3 toO = OriginV / t;
    // Derriere un mur : pas d'eblouissement.
    if (sceneT < t - 0.2) {
        return vec3(0.0);
    }
    float cosA = dot(rd, toO);
    float ang = acos(clamp(cosA, -1.0, 1.0));
    // Uniquement quand le laser pointe vers la camera.
    float facing = smoothstep(0.55, 0.95, dot(-toO, meanDir));
    if (facing <= 0.0) {
        return vec3(0.0);
    }
    float sigma = max(PixelAngle * 2.5, 0.004);
    float core = exp(-0.5 * ang * ang / (sigma * sigma));
    float halo = exp(-ang / (sigma * 8.0)) * 0.08;
    float haze = 0.4 + 0.6 * hazeAt(OriginW, 0.5);
    return color * ((core + halo) * facing * haze * Intensity * 3.0);
}

// ── Impacts sur la geometrie ─────────────────────────────────────────────────
// Le point d'impact d'un vrai laser est aveuglant et sature vers le blanc, avec un halo de
// diffusion sur la surface. Il reste visible sans brume : c'est lui qui trahit un laser
// dans un air limpide.

float impactProfile(float dist, float sigma) {
    float core = exp(-0.5 * dist * dist / (sigma * sigma));
    float halo = exp(-dist / (sigma * 6.0)) * 0.10;
    return core + halo;
}

vec3 impactPoint(vec3 scenePos, float sceneT, vec3 hit, float len, vec3 color, float weight) {
    if (weight <= 0.0) {
        return vec3(0.0);
    }
    float dist = distance(scenePos, hit);
    float sigma = max(BeamRadius * 1.6 + len * Divergence, PixelAngle * sceneT * 1.1);
    if (dist > sigma * 14.0) {
        return vec3(0.0);
    }
    // Puissance etalee sur la tache : P / (2 pi sigma^2).
    float areal = IMPACT_GAIN / (2.0 * PI * sigma * sigma);
    return color * (weight * Intensity * areal * impactProfile(dist, sigma));
}

vec3 impactLine(vec3 scenePos, float sceneT, vec3 h0, vec3 h1, float len0, float len1,
                vec3 color, float weight) {
    if (weight <= 0.0) {
        return vec3(0.0);
    }
    vec3 ab = h1 - h0;
    float abl2 = dot(ab, ab);
    float u = abl2 > 1.0e-8 ? clamp(dot(scenePos - h0, ab) / abl2, 0.0, 1.0) : 0.0;
    vec3 closest = h0 + ab * u;
    float dist = distance(scenePos, closest);
    float lenAt = mix(len0, len1, u);
    float sigma = max(BeamRadius * 1.6 + lenAt * Divergence, PixelAngle * sceneT * 1.1);
    if (dist > sigma * 14.0) {
        return vec3(0.0);
    }
    // Trait : P / (r * angle total) etale sur l'epaisseur sigma.
    float lineal = IMPACT_LINE_GAIN / (max(lenAt, 0.3) * TotalSpan) * INV_SQRT_2PI / sigma;
    return color * (weight * Intensity * lineal * impactProfile(dist, sigma));
}

// ── Main ─────────────────────────────────────────────────────────────────────

void main() {
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;
    float depth = texture(Sampler1, screenUV).r;
    vec3 scenePos = reconstructViewPos(screenUV, depth);
    float sceneT = length(scenePos);
    bool hasSurface = depth < 0.99999;
    vec3 rd = normalize(reconstructViewPos(screenUV, 1.0));

    vec3 accum = vec3(0.0);
    vec3 meanDir = vec3(0.0);
    vec3 meanColor = vec3(0.0);

    for (int i = 0; i < 512; i++) {
        if (i >= SegCount) break;

        vec4 a = texelFetch(Sampler2, ivec2(0, i), 0);   // dir0 vue, len0
        vec4 b = texelFetch(Sampler2, ivec2(1, i), 0);   // dir1 vue, len1
        vec4 f = texelFetch(Sampler2, ivec2(2, i), 0);   // dir0 monde, flags
        vec4 g = texelFetch(Sampler2, ivec2(3, i), 0);   // dir1 monde, span
        vec4 c = texelFetch(Sampler2, ivec2(4, i), 0);   // couleur, poids nappe
        vec4 w = texelFetch(Sampler2, ivec2(5, i), 0);   // poids faisceau 0/1, chemin 0/1
        int flags = int(f.w + 0.5);
        meanDir += a.xyz;
        meanColor += c.rgb;

        if ((flags & FLAG_BEAM0) != 0) {
            accum += beamScatter(rd, a.xyz, a.w, f.xyz, c.rgb, w.x, w.z, sceneT,
                    (flags & FLAG_HIT0) != 0);
        }
        if ((flags & FLAG_BEAM1) != 0) {
            accum += beamScatter(rd, b.xyz, b.w, g.xyz, c.rgb, w.y, w.w, sceneT,
                    (flags & FLAG_HIT1) != 0);
        }
        if ((flags & FLAG_SHEET) != 0) {
            accum += sheetScatter(rd, a.xyz, b.xyz, a.w, b.w, f.xyz, g.xyz, g.w,
                    c.rgb, c.w, w.z, w.w, sceneT,
                    (flags & FLAG_HIT0) != 0, (flags & FLAG_HIT1) != 0);
        }

        if (ImpactEnabled > 0.5 && hasSurface) {
            bool hit0 = (flags & FLAG_HIT0) != 0;
            bool hit1 = (flags & FLAG_HIT1) != 0;
            vec3 h0 = OriginV + a.xyz * a.w;
            vec3 h1 = OriginV + b.xyz * b.w;
            if (hit0 && (flags & FLAG_BEAM0) != 0) {
                accum += impactPoint(scenePos, sceneT, h0, a.w, c.rgb, w.x);
            }
            if (hit1 && (flags & FLAG_BEAM1) != 0) {
                accum += impactPoint(scenePos, sceneT, h1, b.w, c.rgb, w.y);
            }
            if ((flags & FLAG_SHEET) != 0 && hit0 && hit1) {
                accum += impactLine(scenePos, sceneT, h0, h1, a.w, b.w, c.rgb, c.w);
            }
        }
    }

    if (SegCount > 0 && dot(meanDir, meanDir) > 1.0e-6) {
        accum += apertureGlare(rd, normalize(meanDir), meanColor / float(SegCount), sceneT);
    }

    accum *= Brightness;
    accum *= mix(1.0, 0.6, clamp(Ambient, 0.0, 1.0));

    float lum = max(accum.r, max(accum.g, accum.b));
    if (lum < 0.0015) {
        discard;
    }

    // Compression douce qui garde la teinte. La derive vers le blanc est reservee aux coeurs
    // vraiment brulants (faisceau vu de face, impact) et reste partielle, pour que les nappes
    // et la brume gardent la couleur du laser.
    vec3 mapped = accum * ((1.0 - exp(-lum)) / lum);
    float hot = smoothstep(6.0, 24.0, lum) * 0.45;
    mapped = mix(mapped, vec3(1.0), hot);

    fragColor = vec4(max(mapped, 0.0), 1.0);
}

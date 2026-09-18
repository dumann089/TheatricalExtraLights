#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform mat4 InvProjMat;
uniform vec3 BeamOrigin;
uniform vec3 BeamDir;
uniform vec3 AxisU;
uniform vec3 AxisV;

uniform vec3 BeamOriginW;
uniform vec3 BeamDirW;
uniform vec3 AxisUW;
uniform vec3 AxisVW;

uniform sampler2D Sampler2;
uniform float WheelTransition;

uniform vec3 BeamColor;
uniform float TanHalfAngle;
uniform float BeamLength;
uniform float BaseRadius;
uniform float WidthScale;
uniform float HeightScale;
uniform float Intensity;
uniform float Density;
uniform float MaxAlpha;
uniform float Brightness;
uniform float Anisotropy;
uniform float FadeLength;
uniform float DustAmount;
uniform float GoboRotation;
uniform float Time;
uniform float Ambient;
uniform vec2 ScreenSize;
uniform int StepCount;

// Module de couteaux (framing shutters) : 4 lames haut / droite / bas / gauche.
uniform float ShutterEnabled;
uniform vec4 BladeA;         // insertion du coin A de chaque lame, 0 (sorti) .. 1 (rentre a fond)
uniform vec4 BladeB;         // insertion du coin B de chaque lame
uniform float FrameRotation; // rotation du module complet, radians

// ── Animation wheel ──────────────────────────────────────────────────────────
// Texture d'effet (flammes, eau, nuages...) qui defile devant la porte, dans le repere
// (u,v) normalise au rayon du faisceau. Une tuile couvre le diametre de la porte.
uniform sampler2D Sampler3;
uniform float AnimEnabled;
uniform float AnimAngle;   // orientation, radians
uniform float AnimOffset;  // defilement, en tuiles

float animationMask(float u, float v, float radius) {
    if (AnimEnabled < 0.5) return 1.0;
    float ca = cos(AnimAngle);
    float sa = sin(AnimAngle);
    vec2 p = vec2(u, v) / max(radius, 0.0001);
    vec2 r = vec2(p.x * ca - p.y * sa, p.x * sa + p.y * ca);
    vec2 uv = fract(vec2(r.x * 0.5 + AnimOffset, r.y * 0.5));
    float a = dot(texture(Sampler3, uv).rgb, vec3(0.299, 0.587, 0.114));
    return clamp(a, 0.0, 1.0);
}

// ── Ombres portees ───────────────────────────────────────────────────────────
// Grille d'occupation des blocs (Sampler4 : N tranches empilees, N x N*N, rouge = solide) et
// jusqu'a 8 boites d'entites. Un point est ombre si le segment point→source traverse un
// solide.
uniform sampler2D Sampler4;
uniform float ShadowEnabled;
uniform vec3 VoxelOrigin;   // coin min de la grille, monde
uniform float VoxelCell;    // taille d'une cellule, blocs
uniform float VoxelSize;    // cellules par axe
uniform float OccCount;
uniform vec3 OccMin0;
uniform vec3 OccMax0;
uniform vec3 OccMin1;
uniform vec3 OccMax1;
uniform vec3 OccMin2;
uniform vec3 OccMax2;
uniform vec3 OccMin3;
uniform vec3 OccMax3;
uniform vec3 OccMin4;
uniform vec3 OccMax4;
uniform vec3 OccMin5;
uniform vec3 OccMax5;
uniform vec3 OccMin6;
uniform vec3 OccMax6;
uniform vec3 OccMin7;
uniform vec3 OccMax7;

float voxelSolid(vec3 wp) {
    vec3 c = (wp - VoxelOrigin) / VoxelCell;
    if (c.x < 0.0 || c.y < 0.0 || c.z < 0.0 || c.x >= VoxelSize || c.y >= VoxelSize || c.z >= VoxelSize) return 0.0;
    vec3 i = floor(c);
    vec2 uv = vec2((i.x + 0.5) / VoxelSize, (i.z * VoxelSize + i.y + 0.5) / (VoxelSize * VoxelSize));
    return texture(Sampler4, uv).r;
}

bool segmentHitsBox(vec3 a, vec3 b, vec3 bmin, vec3 bmax) {
    vec3 d = b - a;
    vec3 dd = vec3(
        abs(d.x) < 1.0e-6 ? 1.0e-6 : d.x,
        abs(d.y) < 1.0e-6 ? 1.0e-6 : d.y,
        abs(d.z) < 1.0e-6 ? 1.0e-6 : d.z);
    vec3 t0 = (bmin - a) / dd;
    vec3 t1 = (bmax - a) / dd;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    float tn = max(max(tmin.x, tmin.y), tmin.z);
    float tf = min(min(tmax.x, tmax.y), tmax.z);
    return tf >= max(tn, 0.0) && tn <= 1.0;
}

// startOffset : distance (blocs) a partir du point avant de tester les blocs, pour ne pas
// se faire ombrer par la surface sur laquelle on est.
float shadowFactor(vec3 wp, vec3 lightW, float startOffset) {
    if (ShadowEnabled < 0.5) return 1.0;
    vec3 toL = lightW - wp;
    float len = length(toL);
    if (len < 0.75) return 1.0;
    if (OccCount > 0.5 && segmentHitsBox(wp, lightW, OccMin0, OccMax0)) return 0.0;
    if (OccCount > 1.5 && segmentHitsBox(wp, lightW, OccMin1, OccMax1)) return 0.0;
    if (OccCount > 2.5 && segmentHitsBox(wp, lightW, OccMin2, OccMax2)) return 0.0;
    if (OccCount > 3.5 && segmentHitsBox(wp, lightW, OccMin3, OccMax3)) return 0.0;
    if (OccCount > 4.5 && segmentHitsBox(wp, lightW, OccMin4, OccMax4)) return 0.0;
    if (OccCount > 5.5 && segmentHitsBox(wp, lightW, OccMin5, OccMax5)) return 0.0;
    if (OccCount > 6.5 && segmentHitsBox(wp, lightW, OccMin6, OccMax6)) return 0.0;
    if (OccCount > 7.5 && segmentHitsBox(wp, lightW, OccMin7, OccMax7)) return 0.0;
    vec3 dirL = toL / len;
    float start = min(startOffset, len * 0.5);
    float span = len - start - VoxelCell * 0.75;   // on s'arrete avant la cellule de la source
    if (span <= 0.0) return 1.0;
    float shadowCap = StepCount <= 8 ? 12.0 : 32.0;
    int steps = int(clamp(ceil(span / (VoxelCell * 0.9)), 2.0, shadowCap));
    float ds = span / float(steps);
    vec3 p = wp + dirL * (start + ds * 0.5);
    for (int i = 0; i < 32; i++) {
        if (i >= steps) break;
        if (voxelSolid(p) > 0.5) return 0.0;
        p += dirL * ds;
    }
    return 1.0;
}

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

const float PI = 3.14159265;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// Smooth trilinear value noise — raw white noise per sample looks blotchy.
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
        f.z
    );
}

// Interleaved gradient noise: stable per-pixel dither without temporal flicker.
float ign(vec2 px) {
    return fract(
        52.9829189 * fract(
            0.06711056 * px.x + 0.00583715 * px.y
        )
    );
}

// 3-octave fractal noise: soft billows with finer detail on top.
float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;

    for (int i = 0; i < 3; i++) {
        sum += amp * vnoise(p);
        p = p * 2.17 + vec3(11.3, 7.1, 5.9);
        amp *= 0.5;
    }

    return sum;
}

float henyeyGreenstein(float cosTheta, float g) {
    float g2 = g * g;
    float denom = max(
        1.0e-4,
        1.0 + g2 - 2.0 * g * cosTheta
    );

    return (1.0 - g2) / (4.0 * PI * pow(denom, 1.5));
}

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(
        uv * 2.0 - 1.0,
        depth * 2.0 - 1.0,
        1.0
    );

    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

// Tight ray/cone intersection so the march interval hugs the actual beam.
bool intersectBounds(
    vec3 ro,
    vec3 rd,
    out float tEnter,
    out float tExit
) {
    vec3 axis = normalize(BeamDir);
    vec3 uA = normalize(AxisU);
    vec3 vA = normalize(AxisV);

    float k = max(TanHalfAngle, 1.0e-4) * 1.15;
    float wS = max(WidthScale, 0.05);
    float hS = max(HeightScale, 0.05);

    float z0 = max(BaseRadius, 1.0e-3) / k;
    vec3 apex = BeamOrigin - axis * z0;
    float zMax = z0 + BeamLength;

    vec3 o = ro - apex;

    float ou = dot(o, uA) / wS;
    float ov = dot(o, vA) / hS;
    float ozc = dot(o, axis);

    float du = dot(rd, uA) / wS;
    float dv = dot(rd, vA) / hS;
    float dzc = dot(rd, axis);

    float ts0;
    float ts1;

    if (abs(dzc) < 1.0e-6) {
        if (ozc < 0.0 || ozc > zMax) {
            return false;
        }

        ts0 = 0.0;
        ts1 = 1.0e6;
    } else {
        ts0 = -ozc / dzc;
        ts1 = (zMax - ozc) / dzc;

        if (ts0 > ts1) {
            float tmp = ts0;
            ts0 = ts1;
            ts1 = tmp;
        }
    }

    ts0 = max(ts0, 0.0);

    if (ts1 <= ts0) {
        return false;
    }

    float a = du * du + dv * dv - k * k * dzc * dzc;
    float b = 2.0 * (
        ou * du +
        ov * dv -
        k * k * ozc * dzc
    );
    float c = ou * ou + ov * ov - k * k * ozc * ozc;

    float q0 = ts0;
    float q1 = ts1;

    if (abs(a) > 1.0e-7) {
        float disc = b * b - 4.0 * a * c;

        if (disc < 0.0) {
            if (a > 0.0) {
                return false;
            }
        } else {
            float s = sqrt(disc);
            float r0 = (-b - s) / (2.0 * a);
            float r1 = (-b + s) / (2.0 * a);

            if (r0 > r1) {
                float tmp = r0;
                r0 = r1;
                r1 = tmp;
            }

            if (a > 0.0) {
                q0 = max(ts0, r0);
                q1 = min(ts1, r1);
            } else {
                float l1 = min(ts1, r0);
                float u0 = max(ts0, r1);

                bool lower = l1 > ts0;
                bool upper = ts1 > u0;

                if (lower && (!upper || ts0 <= u0)) {
                    q0 = ts0;
                    q1 = l1;
                } else if (upper) {
                    q0 = u0;
                    q1 = ts1;
                } else {
                    return false;
                }
            }
        }
    } else if (abs(b) > 1.0e-7) {
        float r = -c / b;

        if (b > 0.0) {
            q1 = min(ts1, r);
        } else {
            q0 = max(ts0, r);
        }
    } else if (c > 0.0) {
        return false;
    }

    if (q1 <= q0) {
        return false;
    }

    tEnter = q0;
    tExit = q1;

    return true;
}

// Une lame, convention A/B (grandMA, Ayrton, Clay Paky) : deux coins qui s'enfoncent
// independamment, le bord de la lame est la droite qui les relie. n = normale de base de
// la lame (vers l'exterieur du faisceau), p = point dans le disque unite du faisceau
// (deja ramene dans le repere du module).
float bladeMask(vec2 p, vec2 n, float insA, float insB, float soft) {
    if (insA <= 0.0005 && insB <= 0.0005) {
        return 1.0;
    }
    // Tangente : A est a gauche du faisceau quand on regarde la lame depuis le centre.
    vec2 t = vec2(n.y, -n.x);
    const float R = 1.15; // a insertion 0 le bord est hors du cercle ; a 1 il couvre tout.
    vec2 cornerA = n * (R - 2.0 * R * insA) - t * R;
    vec2 cornerB = n * (R - 2.0 * R * insB) + t * R;
    vec2 edge = cornerB - cornerA;
    vec2 m = normalize(vec2(edge.y, -edge.x));
    if (dot(m, n) < 0.0) {
        m = -m;
    }
    float sd = dot(p - cornerA, m);
    // sd > 0 : derriere la lame (occulte). Bord legerement doux comme un couteau reel.
    return 1.0 - smoothstep(-soft, soft, sd);
}

// Masque combine des 4 couteaux pour un point (u,v) a un rayon de faisceau donne.
float shutterMask(float u, float v, float radius, float soft) {
    if (ShutterEnabled < 0.5) {
        return 1.0;
    }
    vec2 p = vec2(u, v) / max(radius, 1.0e-4);
    float cr = cos(-FrameRotation);
    float sr = sin(-FrameRotation);
    vec2 q = vec2(p.x * cr - p.y * sr, p.x * sr + p.y * cr);

    float m = 1.0;
    m *= bladeMask(q, vec2(0.0, 1.0),  BladeA.x, BladeB.x, soft);
    m *= bladeMask(q, vec2(1.0, 0.0),  BladeA.y, BladeB.y, soft);
    m *= bladeMask(q, vec2(0.0, -1.0), BladeA.z, BladeB.z, soft);
    m *= bladeMask(q, vec2(-1.0, 0.0), BladeA.w, BladeB.w, soft);
    return m;
}

// Tu sistema de proyección de Gobo.
// El resto del shader permanece igual al del repositorio.
float sampleGobo(vec3 worldOffset, float zDist) {
    vec3 uAxis = normalize(AxisU);
    vec3 vAxis = normalize(AxisV);

    float z = max(zDist, 0.001);
    float u = dot(worldOffset, uAxis);
    float v = dot(worldOffset, vAxis);

    float angle = radians(GoboRotation);
    float ca = cos(angle);
    float sa = sin(angle);

    float projectionRadius = max(
        z * max(TanHalfAngle, 0.0001),
        BaseRadius
    );

    // Mismo cálculo que en projector
    float slotDistance = projectionRadius * 2.0;

    float localU_A = u + WheelTransition * slotDistance;
    float localV_A = v;
    float ruA = localU_A * ca - localV_A * sa;
    float rvA = localU_A * sa + localV_A * ca;
    vec2 uvA = vec2(ruA / projectionRadius * 0.5 + 0.5, 1.0 - (rvA / projectionRadius * 0.5 + 0.5));

    float localU_B = u - (1.0 - WheelTransition) * slotDistance;
    float localV_B = v;
    float ruB = localU_B * ca - localV_B * sa;
    float rvB = localU_B * sa + localV_B * ca;
    vec2 uvB = vec2(ruB / projectionRadius * 0.5 + 0.5, 1.0 - (rvB / projectionRadius * 0.5 + 0.5));

    float valA = 0.0;
    if (uvA.x >= 0.0 && uvA.x <= 1.0 && uvA.y >= 0.0 && uvA.y <= 1.0) {
        vec3 texA = texture(Sampler0, uvA).rgb;
        valA = clamp(dot(texA, vec3(0.299, 0.587, 0.114)), 0.0, 1.0);
    }

    float valB = 0.0;
    if (uvB.x >= 0.0 && uvB.x <= 1.0 && uvB.y >= 0.0 && uvB.y <= 1.0) {
        vec3 texB = texture(Sampler2, uvB).rgb;
        valB = clamp(dot(texB, vec3(0.299, 0.587, 0.114)), 0.0, 1.0);
    }

    return clamp(valA + valB, 0.0, 1.0) * animationMask(u, v, projectionRadius);
}

void main() {
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;

    float sceneDepthSample = texture(Sampler1, screenUV).r;
    vec3 sceneViewPos = reconstructViewPos(
        screenUV,
        sceneDepthSample
    );

    float sceneT = length(sceneViewPos);

    vec3 rayOrigin = vec3(0.0);
    vec3 rayDir = normalize(
        reconstructViewPos(screenUV, 1.0)
    );

    float tEnter;
    float tExit;

    if (!intersectBounds(
        rayOrigin,
        rayDir,
        tEnter,
        tExit
    )) {
        discard;
    }

    tExit = min(
        tExit,
        sceneT - 0.02
    );

    if (tExit <= tEnter) {
        discard;
    }

    float marchLen = tExit - tEnter;

    int steps = clamp(StepCount, 4, 48);
    // Close-up LOD: if the cone starts near the camera it fills the view.
    // Fewer samples, larger dt — single-scatter energy stays the same.
    float closeLod = mix(0.28, 1.0, smoothstep(0.75, 14.0, tEnter));
    float longLod = mix(1.0, 0.55, smoothstep(8.0, 32.0, marchLen));
    float lod = min(closeLod, longLod);
    int minSteps = tEnter < 1.5 ? 3 : 4;
    steps = max(minSteps, int(float(steps) * lod + 0.5));

    float dt = marchLen / float(steps);
    float t = tEnter + dt * ign(gl_FragCoord.xy);

    vec3 axis = normalize(BeamDir);
    vec3 uAxis = normalize(AxisU);
    vec3 vAxis = normalize(AxisV);

    float g = clamp(
        Anisotropy,
        -0.9,
        0.9
    );

    float wScale = max(
        WidthScale,
        0.05
    );

    float hScale = max(
        HeightScale,
        0.05
    );

    float k = max(
        TanHalfAngle,
        1.0e-5
    );

    // Slow lateral drift with a gentle upward rise, like machine haze.
    vec3 wind = vec3(
        Time * 0.016,
        Time * 0.009,
        Time * 0.013
    );

    vec3 tint = BeamColor * vertexColor.rgb;

    // Reference distance for the inverse-square falloff.
    float dRef = max(
        BeamLength * 0.35,
        1.0
    );

    float sigmaS = Density * 2.5;
    float sigmaT = sigmaS * 0.5;

    // Physically-based single scattering.
    vec3 scattered = vec3(0.0);
    float T = 1.0;

    for (int i = 0; i < 48; i++) {
        if (
            i >= steps ||
            t > tExit ||
            T < 0.01
        ) {
            break;
        }

        vec3 pos = rayOrigin + rayDir * t;
        vec3 toPos = pos - BeamOrigin;

        float zDist = dot(
            toPos,
            axis
        );

        if (zDist < 0.0 || zDist > BeamLength) {
            t += dt;
            continue;
        }

        float radius = max(
            BaseRadius,
            zDist * k
        );

        float u = dot(
            toPos,
            uAxis
        ) / wScale;

        float v = dot(
            toPos,
            vAxis
        ) / hScale;

        float radial01 = length(
            vec2(u, v)
        ) / radius;

        if (radial01 > 1.0) {
            t += dt;
            continue;
        }

        // Bright core with a gaussian shoulder and fully soft rim.
        float profile =
            exp(-radial01 * radial01 * 2.5) *
            (1.0 - smoothstep(
                0.75,
                1.0,
                radial01
            ));

        // Inverse-square falloff from the source.
        float dn = zDist / dRef;
        float falloff =
            1.0 / (1.0 + dn * dn);

        float endFade = 1.0;

        if (FadeLength > 0.0) {
            endFade = 1.0 - smoothstep(
                BeamLength - FadeLength,
                BeamLength,
                zDist
            );
        }

        // Soft contact with geometry.
        float depthFade = clamp(
            (sceneT - t) * 2.0,
            0.0,
            1.0
        );

        // Ease-in when the camera is inside the volume.
        float camFade = smoothstep(
            0.0,
            1.5,
            t
        );

        float gobo = sampleGobo(
            toPos,
            zDist
        );

        // Couteaux : coupe nette du volume, dans le meme repere (u,v) que le gobo.
        gobo *= shutterMask(u, v, radius, 0.035);

        if (gobo < 0.01) {
            t += dt;
            continue;
        }

        // Volumetric haze.
        float haze = 1.0;

        if (DustAmount > 0.0) {
            vec3 wpos =
                BeamOriginW
                + zDist * BeamDirW
                + (u * wScale) * AxisUW
                + (v * hScale) * AxisVW;

            vec3 hp =
                wpos * 0.8 +
                wind;

            float warp = vnoise(
                hp * 1.9 -
                wind * 1.6
            );

            hp += (warp - 0.5) * 0.9;

            float billow = steps <= 6 ? vnoise(hp) : fbm(hp);

            float wisp = steps <= 6 ? 0.5 : vnoise(
                wpos * 3.1 +
                wind * 2.4
            );

            float h =
                billow *
                (0.65 + 0.7 * wisp);

            h = h * h * 1.8;

            haze = mix(
                1.0,
                0.25 + h,
                clamp(
                    DustAmount,
                    0.0,
                    1.0
                )
            );
        }

        // Isotropic base + Henyey-Greenstein forward boost.
        vec3 dl = BeamOrigin - pos;

        vec3 toLight =
            dl / max(
                length(dl),
                1.0e-4
            );

        float cosTheta = dot(
            -rayDir,
            toLight
        );

        float phase =
            0.3 +
            henyeyGreenstein(
                cosTheta,
                g
            );

        float density =
            sigmaS *
            profile *
            haze;

        // Ombre portee : un bloc ou une entite entre ce point et la source coupe la lumiere.
        vec3 wposS =
            BeamOriginW
            + zDist * BeamDirW
            + (u * wScale) * AxisUW
            + (v * hScale) * AxisVW;
        float shadow = shadowFactor(wposS, BeamOriginW, VoxelCell * 0.5);

        vec3 radiance =
            tint *
            (
                Intensity *
                falloff *
                gobo *
                endFade *
                shadow
            );

        scattered +=
            T *
            density *
            phase *
            radiance *
            (
                depthFade *
                camFade *
                dt
            );

        T *= exp(
            -sigmaT *
            profile *
            haze *
            dt
        );

        t += dt;
    }

    vec3 accum =
        scattered *
        (Brightness * MaxAlpha * 80.0);

    accum *= mix(
        1.0,
        0.55,
        clamp(
            Ambient,
            0.0,
            1.0
        )
    );

    float lum = max(
        accum.r,
        max(accum.g, accum.b)
    );

    if (lum < 0.002) {
        discard;
    }

    vec3 mapped =
        accum *
        (
            (1.0 - exp(-lum * 1.4)) /
            lum
        );

    fragColor = vec4(
        max(mapped, 0.0),
        1.0
    );
}
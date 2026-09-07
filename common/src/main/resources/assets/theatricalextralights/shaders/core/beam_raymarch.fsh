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

    return clamp(valA + valB, 0.0, 1.0);
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

    int steps = clamp(
        StepCount,
        4,
        48
    );

    float marchLen = tExit - tEnter;
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

            float billow = fbm(hp);

            float wisp = vnoise(
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

        vec3 radiance =
            tint *
            (
                Intensity *
                falloff *
                gobo *
                endFade
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
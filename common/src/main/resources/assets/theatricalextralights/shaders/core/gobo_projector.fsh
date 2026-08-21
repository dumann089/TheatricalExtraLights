#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec2 ScreenSize;
uniform mat4 InvProjMat;

uniform vec3 CameraPos;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform vec3 CameraLook;

uniform vec3 LightPos;
uniform vec3 LightDir;
uniform vec3 AxisU;
uniform vec3 AxisV;

uniform float TanHalfAngle;
uniform float MaxLen;

uniform float WheelProgress;
uniform float Time;

uniform float RaymarchingIntensity;
uniform float SmokeNoiseAmount;

in vec4 vertexColor;
in vec2 vertexUV;

out vec4 fragColor;

const float PI = 3.14159265359;
const float EXPOSURE = 1.5;

const int MIN_STEPS = 8;
const int MAX_STEPS = 48;

const float STEPS_PER_BLOCK = 6.0;

float hash12(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1));
    p *= 17.0;
    return fract(
        p.x *
        p.y *
        p.z *
        (p.x + p.y + p.z)
    );
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float n000 =
        hash13(i + vec3(0.0, 0.0, 0.0));

    float n100 =
        hash13(i + vec3(1.0, 0.0, 0.0));

    float n010 =
        hash13(i + vec3(0.0, 1.0, 0.0));

    float n110 =
        hash13(i + vec3(1.0, 1.0, 0.0));

    float n001 =
        hash13(i + vec3(0.0, 0.0, 1.0));

    float n101 =
        hash13(i + vec3(1.0, 0.0, 1.0));

    float n011 =
        hash13(i + vec3(0.0, 1.0, 1.0));

    float n111 =
        hash13(i + vec3(1.0, 1.0, 1.0));

    float nx00 =
        mix(n000, n100, f.x);

    float nx10 =
        mix(n010, n110, f.x);

    float nx01 =
        mix(n001, n101, f.x);

    float nx11 =
        mix(n011, n111, f.x);

    float nxy0 =
        mix(nx00, nx10, f.y);

    float nxy1 =
        mix(nx01, nx11, f.y);

    return mix(
        nxy0,
        nxy1,
        f.z
    );
}

float smokeDensity(vec3 worldPos) {
    float amount =
        max(
            SmokeNoiseAmount,
            0.0
        );

    if (amount <= 0.0001) {
        return 1.0;
    }

    vec3 wind =
        vec3(
            Time * 0.18,
            Time * 0.035,
            Time * 0.11
        );

    vec3 p =
        worldPos * 0.42 +
        wind;

    float n1 =
        valueNoise(p);

    float n2 =
        valueNoise(
            p * 2.15 +
            vec3(7.1, 3.4, 9.2)
        );

    float n3 =
        valueNoise(
            p * 4.4 +
            vec3(2.7, 8.3, 1.4)
        );

    float n =
        n1 * 0.55 +
        n2 * 0.30 +
        n3 * 0.15;

    float density =
        smoothstep(
            0.30,
            0.78,
            n
        );

    return mix(
        1.0,
        density,
        clamp(
            amount,
            0.0,
            1.0
        )
    );
}

vec3 reconstructViewPosition(
        vec2 uv,
        float depth) {

    vec4 clip =
        vec4(
            uv * 2.0 - 1.0,
            depth * 2.0 - 1.0,
            1.0
        );

    vec4 view =
        InvProjMat *
        clip;

    return view.xyz /
        max(
            abs(view.w),
            0.000001
        );
}

vec3 viewToWorldDirection(
        vec3 viewDir) {

    vec3 worldDir =
        CameraRight * viewDir.x +
        CameraUp * viewDir.y -
        CameraLook * viewDir.z;

    return normalize(
        worldDir
    );
}

vec3 worldToCone(
        vec3 worldPos) {

    vec3 p =
        worldPos -
        LightPos;

    return vec3(
        dot(p, AxisU),
        dot(p, AxisV),
        dot(p, LightDir)
    );
}

vec4 sampleGobo(vec2 uv) {
    if (uv.x < 0.0 ||
        uv.x > 1.0 ||
        uv.y < 0.0 ||
        uv.y > 1.0) {

        return vec4(0.0);
    }

    float progress =
        clamp(
            WheelProgress,
            0.0,
            1.0
        );

    vec2 uv0 =
        vec2(
            uv.x +
            progress * 1.05,
            uv.y
        );

    vec2 uv1 =
        vec2(
            uv.x -
            (1.0 - progress) * 1.05,
            uv.y
        );

    vec4 g0 =
        vec4(0.0);

    vec4 g1 =
        vec4(0.0);

    if (uv0.x >= 0.0 &&
        uv0.x <= 1.0) {

        g0 =
            texture(
                Sampler0,
                uv0
            );
    }

    if (uv1.x >= 0.0 &&
        uv1.x <= 1.0) {

        g1 =
            texture(
                Sampler1,
                uv1
            );
    }

    return g0 + g1;
}

vec4 evaluateCone(
        vec3 worldPos) {

    vec3 p =
        worldToCone(
            worldPos
        );

    float z =
        p.z;

    if (z <= 0.001 ||
        z >= MaxLen) {

        return vec4(0.0);
    }

    float radius =
        z *
        TanHalfAngle;

    if (radius <= 0.00001) {
        return vec4(0.0);
    }

    vec2 projected =
        p.xy /
        radius;

    float radial =
        dot(
            projected,
            projected
        );

    if (radial > 1.0) {
        return vec4(0.0);
    }

    float r =
        sqrt(
            max(
                radial,
                0.0
            )
        );

    float edge =
        1.0 -
        smoothstep(
            0.88,
            1.0,
            r
        );

    vec2 uv =
        projected *
        0.5 +
        0.5;

    vec4 gobo =
        sampleGobo(
            uv
        );

    float distanceAttenuation =
        1.0 /
        (
            1.0 +
            z * 0.055
        );

    float endFade =
        1.0 -
        smoothstep(
            MaxLen * 0.82,
            MaxLen,
            z
        );

    gobo.rgb *=
        edge *
        distanceAttenuation *
        endFade *
        EXPOSURE;

    gobo.a *=
        edge *
        endFade;

    return gobo;
}

bool intersectCone(
        vec3 rayOrigin,
        vec3 rayDir,
        float maxDistance,
        out float tEnter,
        out float tExit) {

    vec3 ro =
        rayOrigin -
        LightPos;

    float ox =
        dot(
            ro,
            AxisU
        );

    float oy =
        dot(
            ro,
            AxisV
        );

    float oz =
        dot(
            ro,
            LightDir
        );

    float dx =
        dot(
            rayDir,
            AxisU
        );

    float dy =
        dot(
            rayDir,
            AxisV
        );

    float dz =
        dot(
            rayDir,
            LightDir
        );

    float k =
        TanHalfAngle;

    float k2 =
        k * k;

    float zEnter;
    float zExit;

    if (abs(dz) < 0.000001) {

        if (oz < 0.0 ||
            oz > MaxLen) {

            return false;
        }

        zEnter = 0.0;
        zExit = maxDistance;

    } else {

        float tz0 =
            (0.0 - oz) /
            dz;

        float tz1 =
            (MaxLen - oz) /
            dz;

        zEnter =
            min(
                tz0,
                tz1
            );

        zExit =
            max(
                tz0,
                tz1
            );

        zEnter =
            max(
                zEnter,
                0.0
            );

        zExit =
            min(
                zExit,
                maxDistance
            );

        if (zExit <= zEnter) {
            return false;
        }
    }

    float A =
        dx * dx +
        dy * dy -
        k2 * dz * dz;

    float B =
        2.0 * (
            ox * dx +
            oy * dy -
            k2 * oz * dz
        );

    float C =
        ox * ox +
        oy * oy -
        k2 * oz * oz;

    float enter =
        zEnter;

    float exit =
        zExit;

    if (abs(A) < 0.000001) {

        if (abs(B) < 0.000001) {

            if (C > 0.0) {
                return false;
            }

        } else {

            float root =
                -C /
                B;

            if (B > 0.0) {

                exit =
                    min(
                        exit,
                        root
                    );

            } else {

                enter =
                    max(
                        enter,
                        root
                    );
            }
        }

    } else {

        float discriminant =
            B * B -
            4.0 * A * C;

        if (discriminant < 0.0) {
            return false;
        }

        float sqrtD =
            sqrt(
                discriminant
            );

        float r0 =
            (-B - sqrtD) /
            (2.0 * A);

        float r1 =
            (-B + sqrtD) /
            (2.0 * A);

        float coneEnter =
            min(
                r0,
                r1
            );

        float coneExit =
            max(
                r0,
                r1
            );

        float testT =
            0.5 *
            (
                coneEnter +
                coneExit
            );

        float testX =
            ox +
            dx * testT;

        float testY =
            oy +
            dy * testT;

        float testZ =
            oz +
            dz * testT;

        float testF =
            testX * testX +
            testY * testY -
            k2 * testZ * testZ;

        if (testF <= 0.0) {

            enter =
                max(
                    enter,
                    coneEnter
                );

            exit =
                min(
                    exit,
                    coneExit
                );

        } else {
            return false;
        }
    }

    if (exit <= enter) {
        return false;
    }

    tEnter =
        max(
            enter,
            0.0
        );

    tExit =
        min(
            exit,
            maxDistance
        );

    return tExit >
        tEnter + 0.0001;
}

void main() {
    vec2 screenUV =
        gl_FragCoord.xy /
        ScreenSize;

    float depth =
        texture(
            Sampler2,
            screenUV
        ).r;

    bool sky =
        depth >= 0.9999;

    vec3 viewPosition =
        reconstructViewPosition(
            screenUV,
            depth
        );

    vec3 viewRay =
        normalize(
            viewPosition
        );

    vec3 worldRay =
        viewToWorldDirection(
            viewRay
        );

    float sceneDistance =
        length(
            viewPosition
        );

    float maxRayDistance =
        sky
            ? MaxLen
            : min(
                sceneDistance,
                MaxLen
            );

    if (maxRayDistance <= 0.01) {
        discard;
    }

    float tEnter;
    float tExit;

    if (!intersectCone(
            CameraPos,
            worldRay,
            maxRayDistance,
            tEnter,
            tExit)) {

        discard;
    }

    float segmentLength =
        tExit -
        tEnter;

    if (segmentLength <= 0.01) {
        discard;
    }

    int steps =
        clamp(
            int(
                segmentLength *
                STEPS_PER_BLOCK
            ),
            MIN_STEPS,
            MAX_STEPS
        );

    float stepSize =
        segmentLength /
        float(steps);

    /*
     * SIN JITTER POR PIXEL.
     *
     * El sample empieza siempre
     * en el centro del primer paso.
     * Esto elimina la estática/granulación.
     */
    float t =
        tEnter +
        stepSize * 0.5;

    vec3 volumeColor =
        vec3(0.0);

    float volumeAlpha =
        0.0;

    float finalIntensity =
        max(
            RaymarchingIntensity,
            0.0
        );

    for (int i = 0;
         i < MAX_STEPS;
         i++) {

        if (i >= steps) {
            break;
        }

        vec3 worldPos =
            CameraPos +
            worldRay *
            t;

        vec4 gobo =
            evaluateCone(
                worldPos
            );

        if (gobo.a > 0.001) {

            float smoke =
                smokeDensity(
                    worldPos
                );

            float density =
                smoke *
                gobo.a *
                finalIntensity *
                stepSize;

            density =
                1.0 -
                exp(
                    -density
                );

            volumeColor +=
                (1.0 - volumeAlpha) *
                gobo.rgb *
                density;

            volumeAlpha +=
                (1.0 - volumeAlpha) *
                density;

            if (volumeAlpha > 0.995) {
                break;
            }
        }

        t += stepSize;
    }

    vec3 finalColor =
        volumeColor *
        vertexColor.rgb;

    float finalAlpha =
        volumeAlpha *
        vertexColor.a;

    finalColor *=
        1.0 +
        finalIntensity *
        0.35;

    if (finalAlpha <= 0.002) {
        discard;
    }

    fragColor =
        vec4(
            finalColor,
            clamp(
                finalAlpha,
                0.0,
                1.0
            )
        );
}
#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 InvProjMat;

uniform vec3 LightPos;
uniform vec3 LightDir;
uniform vec3 AxisU;
uniform vec3 AxisV;

uniform vec3 OcclusionPos;
uniform vec3 OcclusionNormal;

uniform float TanHalfAngle;
uniform float BaseRadius;
uniform float MaxLen;
uniform float MaxGoboDist;
uniform float GoboRotation;
uniform float WheelTransition;
uniform float Intensity;
uniform float Focus;
uniform float GoboBrightness;
uniform float SurfaceTintStrength;

uniform vec3 LightColor;
uniform vec2 ScreenSize;
uniform float OcclusionEnabled;

uniform float ShutterEnabled;
uniform vec4 BladeA;
uniform vec4 BladeB;
uniform float FrameRotation;

uniform sampler2D Sampler3;
uniform float AnimEnabled;
uniform float AnimAngle;
uniform float AnimOffset;

uniform vec3 LightPosW;
uniform vec3 LightDirW;
uniform vec3 AxisUW;
uniform vec3 AxisVW;

uniform sampler2D Sampler4;
uniform sampler2D Sampler5;

uniform float ShadowEnabled;
uniform vec3 VoxelOrigin;
uniform float VoxelCell;
uniform float VoxelSize;
uniform float OccCount;

uniform float SolidAny;
uniform vec3 SolidMin;
uniform vec3 SolidMax;

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

in vec4 VertexColor;
out vec4 fragColor;


// -----------------------------------------------------------------------------
// Utilidades de color
// -----------------------------------------------------------------------------

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

vec3 sampleSceneColor() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    return max(texture(Sampler5, uv).rgb, vec3(0.0));
}

// SurfaceTintStrength controla la influencia de la luminosidad de la superficie
// sobre la intensidad. No modifica el tono RGB de LightColor ni del gobo.
float calculateSurfaceTint(vec3 sceneColor) {
    float strength = clamp(SurfaceTintStrength, 0.0, 1.0);
    float surfaceLuminance = clamp(luminance(sceneColor), 0.0, 1.0);

    // Factor escalar limitado: una superficie oscura nunca elimina
    // completamente la luz proyectada.
    float response = mix(0.75, 1.25, surfaceLuminance);

    return mix(1.0, response, strength);
}


// -----------------------------------------------------------------------------
// Muestreo del gobo con alpha premultiplicado
// -----------------------------------------------------------------------------
//
// El filtrado de una textura RGBA debe interpolar RGB * alpha y alpha.
// Interpolar RGB de forma independiente produce halos negros alrededor
// de texeles transparentes. Aquí se realiza el filtrado en espacio
// premultiplicado y se recupera RGB recto al terminar.

vec4 toPremultiplied(vec4 color) {
    return vec4(color.rgb * color.a, color.a);
}

vec4 toStraightColor(vec4 color) {
    if (color.a <= 0.00001) {
        return vec4(0.0);
    }

    return vec4(
        color.rgb / color.a,
        color.a
    );
}

vec4 readPremultipliedTexel(
    sampler2D samp,
    ivec2 pixel,
    ivec2 dimensions
) {
    ivec2 coord = clamp(
        pixel,
        ivec2(0),
        dimensions - ivec2(1)
    );

    return toPremultiplied(
        texelFetch(samp, coord, 0)
    );
}

vec4 sampleGoboLinear(sampler2D samp, vec2 uv) {
    ivec2 dimensions = textureSize(samp, 0);

    vec2 pixelPosition = uv * vec2(dimensions) - 0.5;
    ivec2 basePixel = ivec2(floor(pixelPosition));
    vec2 f = fract(pixelPosition);

    vec4 c00 = readPremultipliedTexel(
        samp, basePixel, dimensions
    );

    vec4 c10 = readPremultipliedTexel(
        samp, basePixel + ivec2(1, 0), dimensions
    );

    vec4 c01 = readPremultipliedTexel(
        samp, basePixel + ivec2(0, 1), dimensions
    );

    vec4 c11 = readPremultipliedTexel(
        samp, basePixel + ivec2(1, 1), dimensions
    );

    vec4 top = mix(c00, c10, f.x);
    vec4 bottom = mix(c01, c11, f.x);
    vec4 filtered = mix(top, bottom, f.y);

    return toStraightColor(filtered);
}

void accumulateGoboSample(
    inout vec4 accumulated,
    sampler2D samp,
    vec2 uv,
    float weight
) {
    vec4 sampleColor = sampleGoboLinear(samp, uv);
    accumulated += toPremultiplied(sampleColor) * weight;
}

vec4 sampleGoboBlur(sampler2D samp, vec2 uv, float blur) {
    if (blur <= 0.00001) {
        return sampleGoboLinear(samp, uv);
    }

    float b = blur;
    float s = b * 0.70710678;

    vec4 accumulated = vec4(0.0);

    accumulateGoboSample(accumulated, samp, uv, 0.20);

    accumulateGoboSample(
        accumulated, samp, uv + vec2(b, 0.0), 0.10
    );

    accumulateGoboSample(
        accumulated, samp, uv - vec2(b, 0.0), 0.10
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(0.0, b), 0.10
    );

    accumulateGoboSample(
        accumulated, samp, uv - vec2(0.0, b), 0.10
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(s, s), 0.075
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(-s, s), 0.075
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(s, -s), 0.075
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(-s, -s), 0.075
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(b * 1.5, 0.0), 0.025
    );

    accumulateGoboSample(
        accumulated, samp, uv - vec2(b * 1.5, 0.0), 0.025
    );

    accumulateGoboSample(
        accumulated, samp, uv + vec2(0.0, b * 1.5), 0.025
    );

    accumulateGoboSample(
        accumulated, samp, uv - vec2(0.0, b * 1.5), 0.025
    );

    return toStraightColor(accumulated);
}


// -----------------------------------------------------------------------------
// Separación entre la máscara y el color del gobo
// -----------------------------------------------------------------------------
//
// La intensidad del patrón se extrae una sola vez.
// Después se normaliza su color para evitar multiplicar dos veces
// los niveles de gris de la textura.
//
// Esto evita que los píxeles grises de los bordes se conviertan en
// contribuciones oscuras al combinarlos con el alpha.

float goboMask(vec4 sampleColor) {
    float maxChannel = max(
        sampleColor.r,
        max(sampleColor.g, sampleColor.b)
    );

    return clamp(sampleColor.a * maxChannel, 0.0, 1.0);
}

vec3 goboChromaticColor(vec3 color) {
    float maxChannel = max(
        color.r,
        max(color.g, color.b)
    );

    if (maxChannel <= 0.00001) {
        return vec3(0.0);
    }

    return color / maxChannel;
}


// -----------------------------------------------------------------------------
// Animación de la rueda de efectos
// -----------------------------------------------------------------------------

float animationMask(float u, float v, float radius) {
    if (AnimEnabled < 0.5) {
        return 1.0;
    }

    float ca = cos(AnimAngle);
    float sa = sin(AnimAngle);

    vec2 p = vec2(u, v) / max(radius, 0.0001);

    vec2 rotated = vec2(
        p.x * ca - p.y * sa,
        p.x * sa + p.y * ca
    );

    vec2 uv = fract(vec2(
        rotated.x * 0.5 + AnimOffset,
        rotated.y * 0.5
    ));

    return clamp(
        luminance(texture(Sampler3, uv).rgb),
        0.0,
        1.0
    );
}


// -----------------------------------------------------------------------------
// Intersección de segmentos y cajas
// -----------------------------------------------------------------------------

bool segmentHitsBox(vec3 a, vec3 b, vec3 bmin, vec3 bmax) {
    vec3 d = b - a;

    vec3 dd = vec3(
        abs(d.x) < 1.0e-6 ? 1.0e-6 : d.x,
        abs(d.y) < 1.0e-6 ? 1.0e-6 : d.y,
        abs(d.z) < 1.0e-6 ? 1.0e-6 : d.z
    );

    vec3 t0 = (bmin - a) / dd;
    vec3 t1 = (bmax - a) / dd;

    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);

    float tn = max(max(tmin.x, tmin.y), tmin.z);
    float tf = min(min(tmax.x, tmax.y), tmax.z);

    return tf >= max(tn, 0.0) && tn <= 1.0;
}


// -----------------------------------------------------------------------------
// Consulta de la grilla de bloques
// -----------------------------------------------------------------------------

float voxelSolid(vec3 wp) {
    if (VoxelCell <= 0.00001 || VoxelSize <= 0.0) {
        return 0.0;
    }

    vec3 c = (wp - VoxelOrigin) / VoxelCell;

    if (
        c.x < 0.0 ||
        c.y < 0.0 ||
        c.z < 0.0 ||
        c.x >= VoxelSize ||
        c.y >= VoxelSize ||
        c.z >= VoxelSize
    ) {
        return 0.0;
    }

    vec3 i = floor(c);

    vec2 uv = vec2(
        (i.x + 0.5) / VoxelSize,
        (i.z * VoxelSize + i.y + 0.5) /
            (VoxelSize * VoxelSize)
    );

    return texture(Sampler4, uv).r;
}


// -----------------------------------------------------------------------------
// Sombras
// -----------------------------------------------------------------------------

float shadowFactor(vec3 wp, vec3 lightW, float startOffset) {
    if (ShadowEnabled < 0.5) {
        return 1.0;
    }

    vec3 toL = lightW - wp;
    float len = length(toL);

    if (len < 0.75) {
        return 1.0;
    }

    if (OccCount > 0.5 && segmentHitsBox(wp, lightW, OccMin0, OccMax0)) {
        return 0.0;
    }

    if (OccCount > 1.5 && segmentHitsBox(wp, lightW, OccMin1, OccMax1)) {
        return 0.0;
    }

    if (OccCount > 2.5 && segmentHitsBox(wp, lightW, OccMin2, OccMax2)) {
        return 0.0;
    }

    if (OccCount > 3.5 && segmentHitsBox(wp, lightW, OccMin3, OccMax3)) {
        return 0.0;
    }

    if (OccCount > 4.5 && segmentHitsBox(wp, lightW, OccMin4, OccMax4)) {
        return 0.0;
    }

    if (OccCount > 5.5 && segmentHitsBox(wp, lightW, OccMin5, OccMax5)) {
        return 0.0;
    }

    if (OccCount > 6.5 && segmentHitsBox(wp, lightW, OccMin6, OccMax6)) {
        return 0.0;
    }

    if (OccCount > 7.5 && segmentHitsBox(wp, lightW, OccMin7, OccMax7)) {
        return 0.0;
    }

    if (SolidAny < 0.5 || VoxelCell <= 0.00001) {
        return 1.0;
    }

    vec3 dirL = toL / len;

    float start = min(startOffset, len * 0.5);
    float span = len - start - VoxelCell * 0.75;

    if (span <= 0.0) {
        return 1.0;
    }

    int steps = int(clamp(
        ceil(span / (VoxelCell * 0.9)),
        2.0,
        32.0
    ));

    float ds = span / float(steps);

    vec3 dd = vec3(
        abs(dirL.x) < 1.0e-6 ? 1.0e-6 : dirL.x,
        abs(dirL.y) < 1.0e-6 ? 1.0e-6 : dirL.y,
        abs(dirL.z) < 1.0e-6 ? 1.0e-6 : dirL.z
    );

    vec3 b0 = (SolidMin - 0.01 - wp) / dd;
    vec3 b1 = (SolidMax + 0.01 - wp) / dd;

    float sIn = max(
        max(min(b0.x, b1.x), min(b0.y, b1.y)),
        min(b0.z, b1.z)
    );

    float sOut = min(
        min(max(b0.x, b1.x), max(b0.y, b1.y)),
        max(b0.z, b1.z)
    );

    if (sOut < sIn) {
        return 1.0;
    }

    int i0 = max(
        0,
        int(floor((sIn - start) / ds - 0.5))
    );

    int i1 = min(
        steps - 1,
        int(ceil((sOut - start) / ds - 0.5))
    );

    for (int i = 0; i < 32; i++) {
        if (i + i0 > i1) {
            break;
        }

        vec3 p = wp + dirL * (
            start + ds * (float(i + i0) + 0.5)
        );

        if (voxelSolid(p) > 0.5) {
            return 0.0;
        }
    }

    return 1.0;
}


// -----------------------------------------------------------------------------
// Shutters
// -----------------------------------------------------------------------------

float bladeMask(
    vec2 p,
    vec2 n,
    float insA,
    float insB,
    float soft
) {
    if (insA <= 0.0005 && insB <= 0.0005) {
        return 1.0;
    }

    vec2 t = vec2(n.y, -n.x);

    const float R = 1.15;

    vec2 cornerA = n * (R - 2.0 * R * insA) - t * R;
    vec2 cornerB = n * (R - 2.0 * R * insB) + t * R;

    vec2 edge = cornerB - cornerA;
    float edgeLength = length(edge);

    if (edgeLength <= 0.00001) {
        return 1.0;
    }

    vec2 m = normalize(vec2(edge.y, -edge.x));

    if (dot(m, n) < 0.0) {
        m = -m;
    }

    float sd = dot(p - cornerA, m);

    return 1.0 - smoothstep(-soft, soft, sd);
}

float shutterMask(float u, float v, float radius, float soft) {
    if (ShutterEnabled < 0.5) {
        return 1.0;
    }

    vec2 p = vec2(u, v) / max(radius, 0.0001);

    float cr = cos(-FrameRotation);
    float sr = sin(-FrameRotation);

    vec2 q = vec2(
        p.x * cr - p.y * sr,
        p.x * sr + p.y * cr
    );

    float m = 1.0;

    m *= bladeMask(q, vec2(0.0, 1.0), BladeA.x, BladeB.x, soft);
    m *= bladeMask(q, vec2(1.0, 0.0), BladeA.y, BladeB.y, soft);
    m *= bladeMask(q, vec2(0.0, -1.0), BladeA.z, BladeB.z, soft);
    m *= bladeMask(q, vec2(-1.0, 0.0), BladeA.w, BladeB.w, soft);

    return m;
}


// -----------------------------------------------------------------------------
// Reconstrucción de posición desde profundidad
// -----------------------------------------------------------------------------

vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(
        uv * 2.0 - 1.0,
        depth * 2.0 - 1.0,
        1.0
    );

    vec4 view = InvProjMat * clip;

    return view.xyz / max(view.w, 0.000001);
}


// -----------------------------------------------------------------------------
// Atenuación por distancia
// -----------------------------------------------------------------------------

float calculateDistanceFade(float distance, float maxDistance) {
    if (maxDistance <= 0.0001) {
        return 0.0;
    }

    // La transición comienza al 30 % y llega a cero en el límite configurado.
    float fadeStart = maxDistance * 0.30;

    float rangeFade = 1.0 - smoothstep(
        fadeStart,
        maxDistance,
        distance
    );

    // Caída adicional y gradual de intensidad por distancia.
    float physicalFalloff = 1.0 / (
        1.0 + distance * distance * 0.0015
    );

    return clamp(
        rangeFade * physicalFalloff,
        0.0,
        1.0
    );
}


// -----------------------------------------------------------------------------
// Shader principal
// -----------------------------------------------------------------------------

void main() {
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;

    float depth = texture(Sampler1, screenUV).r;

    if (depth >= 0.99999) {
        discard;
    }

    vec3 surfacePos = reconstructViewPos(screenUV, depth);
    vec3 toSurface = surfacePos - LightPos;
    vec3 dir = normalize(LightDir);

    float zDist = dot(toSurface, dir);

    float maxDist = min(MaxLen, MaxGoboDist);

    if (maxDist <= 0.0001 || zDist <= 0.0 || zDist >= maxDist) {
        discard;
    }

    // -------------------------------------------------------------------------
    // Oclusión
    // -------------------------------------------------------------------------

    if (OcclusionEnabled > 0.5) {
        float surfaceDistance = length(toSurface);

        if (surfaceDistance > 0.0001) {
            vec3 surfaceRayDir = toSurface / surfaceDistance;

            float denominator = dot(
                surfaceRayDir,
                OcclusionNormal
            );

            if (abs(denominator) > 0.00001) {
                float hitT = dot(
                    OcclusionPos - LightPos,
                    OcclusionNormal
                ) / denominator;

                float occlusionBias = max(
                    0.08,
                    surfaceDistance * 0.03
                );

                if (
                    hitT > 0.001 &&
                    hitT < surfaceDistance - occlusionBias
                ) {
                    discard;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Coordenadas del haz
    // -------------------------------------------------------------------------

    vec3 uAxis = normalize(AxisU);
    vec3 vAxis = normalize(AxisV);

    float u = dot(toSurface, uAxis);
    float v = dot(toSurface, vAxis);

    float angle = radians(GoboRotation);
    float ca = cos(angle);
    float sa = sin(angle);

    float radius = max(
        BaseRadius + zDist * max(TanHalfAngle, 0.0001),
        0.0001
    );

    float slotDistance = radius * 2.0;

    float localU_A = u + WheelTransition * slotDistance;

    float ruA = localU_A * ca - v * sa;
    float rvA = localU_A * sa + v * ca;

    vec2 goboUVA = vec2(
        ruA / radius * 0.5 + 0.5,
        1.0 - (rvA / radius * 0.5 + 0.5)
    );

    float localU_B = u - (1.0 - WheelTransition) * slotDistance;

    float ruB = localU_B * ca - v * sa;
    float rvB = localU_B * sa + v * ca;

    vec2 goboUVB = vec2(
        ruB / radius * 0.5 + 0.5,
        1.0 - (rvB / radius * 0.5 + 0.5)
    );

    float radial = length(vec2(u, v)) / radius;

    if (radial >= 1.0) {
        discard;
    }

    // -------------------------------------------------------------------------
    // Focus
    // -------------------------------------------------------------------------

    float focusNorm = clamp(Focus, 0.0, 1.0);
    float blurAmount = focusNorm * focusNorm * 0.045;

    // -------------------------------------------------------------------------
    // Gobo A: máscara y color separados
    // -------------------------------------------------------------------------

    float goboAlphaA = 0.0;
    vec3 goboColorA = vec3(0.0);

    if (
        goboUVA.x >= 0.0 && goboUVA.x <= 1.0 &&
        goboUVA.y >= 0.0 && goboUVA.y <= 1.0
    ) {
        vec4 sampleA = sampleGoboBlur(
            Sampler0,
            goboUVA,
            blurAmount
        );

        goboAlphaA = goboMask(sampleA);
        goboColorA = goboChromaticColor(sampleA.rgb);
    }

    // -------------------------------------------------------------------------
    // Gobo B: máscara y color separados
    // -------------------------------------------------------------------------

    float goboAlphaB = 0.0;
    vec3 goboColorB = vec3(0.0);

    if (
        goboUVB.x >= 0.0 && goboUVB.x <= 1.0 &&
        goboUVB.y >= 0.0 && goboUVB.y <= 1.0
    ) {
        vec4 sampleB = sampleGoboBlur(
            Sampler2,
            goboUVB,
            blurAmount
        );

        goboAlphaB = goboMask(sampleB);
        goboColorB = goboChromaticColor(sampleB.rgb);
    }

    float goboAlpha = goboAlphaA + goboAlphaB;

    if (goboAlpha <= 0.001) {
        discard;
    }

    vec3 goboColor = (
        goboColorA * goboAlphaA +
        goboColorB * goboAlphaB
    ) / goboAlpha;

    // -------------------------------------------------------------------------
    // Shutters, animación y sombras
    // -------------------------------------------------------------------------

    float shutterSoft = 0.012 + focusNorm * 0.06;

    goboAlpha *= shutterMask(
        u,
        v,
        radius,
        shutterSoft
    );

    goboAlpha *= animationMask(
        u,
        v,
        radius
    );

    vec3 wposS = (
        LightPosW +
        zDist * LightDirW +
        u * AxisUW +
        v * AxisVW
    );

    goboAlpha *= shadowFactor(
        wposS,
        LightPosW,
        VoxelCell * 1.05
    );

    if (goboAlpha <= 0.001) {
        discard;
    }

    // -------------------------------------------------------------------------
    // Borde suave del haz
    // -------------------------------------------------------------------------

    float edgeStart = mix(
        0.88,
        0.72,
        focusNorm
    );

    float edgeFade = 1.0 - smoothstep(
        edgeStart,
        1.0,
        radial
    );

    // -------------------------------------------------------------------------
    // Distancia: empieza a atenuar al 30 %
    // -------------------------------------------------------------------------

    float distanceFade = calculateDistanceFade(
        zDist,
        maxDist
    );

    if (distanceFade <= 0.001) {
        discard;
    }

    // -------------------------------------------------------------------------
    // SurfaceTint: afecta la intensidad, nunca el tono RGB
    // -------------------------------------------------------------------------

    float surfaceFactor = calculateSurfaceTint(
        sampleSceneColor()
    );

    // -------------------------------------------------------------------------
    // Resultado
    // -------------------------------------------------------------------------

    float finalFactor = clamp(
        goboAlpha *
        edgeFade *
        Intensity *
        GoboBrightness *
        VertexColor.a *
        distanceFade *
        surfaceFactor,
        0.0,
        1.0
    );

    if (finalFactor <= 0.001) {
        discard;
    }

    vec3 finalColor = LightColor * goboColor;

    // ADITIVE_TRANSPARENCY usa SRC_ALPHA, ONE.
    // No premultiplicar finalColor por finalFactor: el blending aplica alpha.
    fragColor = vec4(finalColor, finalFactor);
}
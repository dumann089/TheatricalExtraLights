#version 150

uniform sampler2D Sampler0;   // Gobo A
uniform sampler2D Sampler1;   // Depth buffer
uniform sampler2D Sampler2;   // Gobo B (wheel transition)
uniform mat4      InvProjMat;
uniform vec3      LightPos;
uniform vec3      LightDir;
uniform vec3      AxisU;
uniform vec3      AxisV;
uniform vec3      OcclusionPos;
uniform vec3      OcclusionNormal;
uniform float     TanHalfAngle;
uniform float     BaseRadius;
uniform float     MaxLen;
uniform float     MaxGoboDist;
uniform float     GoboRotation;
uniform float     WheelTransition;
uniform float     Intensity;
uniform float     Focus;
uniform vec3      LightColor;
uniform vec2      ScreenSize;
uniform float     OcclusionEnabled;

// Framing shutters
uniform float ShutterEnabled;
uniform vec4  BladeA;
uniform vec4  BladeB;
uniform float FrameRotation;

// Animation wheel
uniform sampler2D Sampler3;
uniform float     AnimEnabled;
uniform float     AnimAngle;
uniform float     AnimOffset;

// World-space para sombras
uniform vec3 LightPosW;
uniform vec3 LightDirW;
uniform vec3 AxisUW;
uniform vec3 AxisVW;

// Shadow voxel
uniform sampler2D Sampler4;
uniform float     ShadowEnabled;
uniform vec3      VoxelOrigin;
uniform float     VoxelCell;
uniform float     VoxelSize;
uniform float     OccCount;
uniform vec3 OccMin0; uniform vec3 OccMax0;
uniform vec3 OccMin1; uniform vec3 OccMax1;
uniform vec3 OccMin2; uniform vec3 OccMax2;
uniform vec3 OccMin3; uniform vec3 OccMax3;
uniform vec3 OccMin4; uniform vec3 OccMax4;
uniform vec3 OccMin5; uniform vec3 OccMax5;
uniform vec3 OccMin6; uniform vec3 OccMax6;
uniform vec3 OccMin7; uniform vec3 OccMax7;

in  vec4 VertexColor;
out vec4 fragColor;

// ─────────────────────────────────────────────
//  Constantes de calibración
// ─────────────────────────────────────────────
const float PI              = 3.14159265358979;

// Cuánto "pesa" la componente difusa vs especular en el resultado final.
// Subir SPECULAR_STRENGTH da brillo tipo piso encerado / metal.
// Bajar SPECULAR_STRENGTH da aspecto mate suave.
const float SPECULAR_STRENGTH = 0.35;

// Dureza del lóbulo especular (Blinn-Phong exponent).
// 32 → superficie semimate; 128 → plástico/vidrio; 8 → matte difuso
const float SPECULAR_SHININESS = 48.0;

// Fresnel: oscurece los bordes grazing (evita que la luz "flote" en bordes)
const float FRESNEL_BIAS  = 0.04;
const float FRESNEL_POWER = 4.0;

// Brillo global del gobo (equivale al "Multiplier" de UE4 Spotlight)
const float GOBO_MASTER   = 1.15;

// ─────────────────────────────────────────────
//  Reconstrucción de posición en view-space
// ─────────────────────────────────────────────
vec3 reconstructViewPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    return view.xyz / max(view.w, 0.000001);
}

// ─────────────────────────────────────────────
//  Normal geométrica reconstruida de pantalla
//  (igual que el G-Buffer normal de UE4 deferred)
// ─────────────────────────────────────────────
vec3 reconstructNormal(vec3 pos) {
    vec3 dx = dFdx(pos);
    vec3 dy = dFdy(pos);
    vec3 n  = cross(dx, dy);
    float len = length(n);
    return len > 1e-5 ? n / len : vec3(0.0, 1.0, 0.0);
}

// ─────────────────────────────────────────────
//  Fresnel Schlick aproximado
// ─────────────────────────────────────────────
float fresnelSchlick(float cosTheta) {
    float f0 = FRESNEL_BIAS;
    return f0 + (1.0 - f0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), FRESNEL_POWER);
}

// ─────────────────────────────────────────────
//  BRDF simplificado (Diffuse Burley + Blinn-Phong spec)
//  Retorna vec2(diffuse, specular) sin multiplicar por color todavía
// ─────────────────────────────────────────────
vec2 brdf(vec3 N, vec3 L, vec3 V, float roughness) {
    float NdotL = max(dot(N, L), 0.0);

    // ── Difusa Burley (simplificada, sin el factor de rugosidad completo)
    //    Idéntica a la de UE4 para roughness medio
    float FL = pow(clamp(1.0 - NdotL, 0.0, 1.0), 5.0);
    float diffuse = (1.0 - 0.5 * FL) * NdotL;

    // Half-Lambert respaldo: evita zonas totalmente negras en superficie
    // (igual que lo usa Valve en Half-Life 2 y UE4 en su skin shader)
    float halfLambert = (NdotL * 0.5 + 0.5);
    halfLambert      *= halfLambert;

    // Mezcla: 70 % Burley puro + 30 % Half-Lambert para conservar detalle
    diffuse = mix(halfLambert, diffuse, 0.7);

    // ── Especular Blinn-Phong (barato y convincente)
    vec3 H      = normalize(L + V);
    float NdotH = max(dot(N, H), 0.0);
    float spec  = pow(NdotH, SPECULAR_SHININESS) * NdotL * SPECULAR_STRENGTH;

    return vec2(diffuse, spec);
}

// ─────────────────────────────────────────────
//  Cone falloff "IES soft" (idéntico al de UE4 Spotlight)
//  innerRadial: fracción radial donde empieza el soft edge
// ─────────────────────────────────────────────
float coneFalloff(float radial, float focusNorm) {
    // Focus alto = borde más duro (como abrir el iris de un Fresnel real)
    float innerFraction = mix(0.50, 0.92, focusNorm);
    // Curva suave desde innerFraction → 1.0
    float edgeFactor = 1.0 - smoothstep(innerFraction, 1.0, radial);
    // Potencia extra para dar forma de "campana" en vez de plano uniforme
    // UE4 usa un cos^2 sobre la apertura de cono:
    float cosBell = 1.0 - radial * radial;
    cosBell = max(cosBell, 0.0);
    return edgeFactor * sqrt(cosBell);
}

// ─────────────────────────────────────────────
//  Atenuación física cuadrática (1/d²) suavizada
// ─────────────────────────────────────────────
float distanceAttenuation(float dist, float maxDist) {
    // UE4 usa: saturate(1 - (d/dmax)^4)^2 / (d^2 + 1)
    float ratio  = dist / max(maxDist, 0.0001);
    float window = clamp(1.0 - ratio * ratio * ratio * ratio, 0.0, 1.0);
    window       = window * window;
    return window / (dist * 0.01 + 1.0);
}

// ─────────────────────────────────────────────
//  Shadow / Occlusion (sin cambios respecto al original)
// ─────────────────────────────────────────────
float voxelSolid(vec3 wp) {
    vec3 c = (wp - VoxelOrigin) / VoxelCell;
    if (c.x < 0.0 || c.y < 0.0 || c.z < 0.0 ||
        c.x >= VoxelSize || c.y >= VoxelSize || c.z >= VoxelSize) return 0.0;
    vec3 i  = floor(c);
    vec2 uv = vec2((i.x + 0.5) / VoxelSize,
                   (i.z * VoxelSize + i.y + 0.5) / (VoxelSize * VoxelSize));
    return texture(Sampler4, uv).r;
}

bool segmentHitsBox(vec3 a, vec3 b, vec3 bmin, vec3 bmax) {
    vec3 d  = b - a;
    vec3 dd = vec3(abs(d.x) < 1e-6 ? 1e-6 : d.x,
                   abs(d.y) < 1e-6 ? 1e-6 : d.y,
                   abs(d.z) < 1e-6 ? 1e-6 : d.z);
    vec3 t0   = (bmin - a) / dd;
    vec3 t1   = (bmax - a) / dd;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    float tn  = max(max(tmin.x, tmin.y), tmin.z);
    float tf  = min(min(tmax.x, tmax.y), tmax.z);
    return tf >= max(tn, 0.0) && tn <= 1.0;
}

float shadowFactor(vec3 wp, vec3 lightW, float startOffset) {
    if (ShadowEnabled < 0.5) return 1.0;
    vec3  toL = lightW - wp;
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
    vec3  dirL = toL / len;
    float start = min(startOffset, len * 0.5);
    float span  = len - start - VoxelCell * 0.75;
    if (span <= 0.0) return 1.0;
    int steps   = int(clamp(ceil(span / (VoxelCell * 0.9)), 2.0, 32.0));
    float ds    = span / float(steps);
    vec3  p     = wp + dirL * (start + ds * 0.5);
    for (int i = 0; i < 32; i++) {
        if (i >= steps) break;
        if (voxelSolid(p) > 0.5) return 0.0;
        p += dirL * ds;
    }
    return 1.0;
}

// ─────────────────────────────────────────────
//  Gobo blur (sin cambios)
// ─────────────────────────────────────────────
vec4 sampleGoboBlur(sampler2D samp, vec2 uv, float blur) {
    if (blur <= 0.00001) return texture(samp, uv);
    float b = blur;
    float s = b * 0.70710678;
    vec4 c  = texture(samp, uv) * 0.20;
    c += texture(samp, uv + vec2( b,  0)) * 0.10;
    c += texture(samp, uv + vec2(-b,  0)) * 0.10;
    c += texture(samp, uv + vec2( 0,  b)) * 0.10;
    c += texture(samp, uv + vec2( 0, -b)) * 0.10;
    c += texture(samp, uv + vec2( s,  s)) * 0.075;
    c += texture(samp, uv + vec2(-s,  s)) * 0.075;
    c += texture(samp, uv + vec2( s, -s)) * 0.075;
    c += texture(samp, uv + vec2(-s, -s)) * 0.075;
    c += texture(samp, uv + vec2( b * 1.5,  0))   * 0.025;
    c += texture(samp, uv - vec2( b * 1.5,  0))   * 0.025;
    c += texture(samp, uv + vec2( 0,  b * 1.5))   * 0.025;
    c += texture(samp, uv - vec2( 0,  b * 1.5))   * 0.025;
    return c;
}

// ─────────────────────────────────────────────
//  Blade / Shutter mask (sin cambios)
// ─────────────────────────────────────────────
float bladeMask(vec2 p, vec2 n, float insA, float insB, float soft) {
    if (insA <= 0.0005 && insB <= 0.0005) return 1.0;
    vec2 t = vec2(n.y, -n.x);
    const float R = 1.15;
    vec2 cornerA = n * (R - 2.0 * R * insA) - t * R;
    vec2 cornerB = n * (R - 2.0 * R * insB) + t * R;
    vec2 edge    = cornerB - cornerA;
    vec2 m       = normalize(vec2(edge.y, -edge.x));
    if (dot(m, n) < 0.0) m = -m;
    float sd = dot(p - cornerA, m);
    return 1.0 - smoothstep(-soft, soft, sd);
}

float shutterMask(float u, float v, float radius, float soft) {
    if (ShutterEnabled < 0.5) return 1.0;
    vec2  p  = vec2(u, v) / max(radius, 0.0001);
    float cr = cos(-FrameRotation);
    float sr = sin(-FrameRotation);
    vec2  q  = vec2(p.x * cr - p.y * sr, p.x * sr + p.y * cr);
    float m  = 1.0;
    m *= bladeMask(q, vec2( 0,  1), BladeA.x, BladeB.x, soft);
    m *= bladeMask(q, vec2( 1,  0), BladeA.y, BladeB.y, soft);
    m *= bladeMask(q, vec2( 0, -1), BladeA.z, BladeB.z, soft);
    m *= bladeMask(q, vec2(-1,  0), BladeA.w, BladeB.w, soft);
    return m;
}

float animationMask(float u, float v, float radius) {
    if (AnimEnabled < 0.5) return 1.0;
    float ca = cos(AnimAngle);
    float sa = sin(AnimAngle);
    vec2  p  = vec2(u, v) / max(radius, 0.0001);
    vec2  r  = vec2(p.x * ca - p.y * sa, p.x * sa + p.y * ca);
    vec2  uv = fract(vec2(r.x * 0.5 + AnimOffset, r.y * 0.5));
    float a  = dot(texture(Sampler3, uv).rgb, vec3(0.299, 0.587, 0.114));
    return clamp(a, 0.0, 1.0);
}

// ══════════════════════════════════════════════════════════════════════════════
//  MAIN
// ══════════════════════════════════════════════════════════════════════════════
void main() {

    // ── 0. Reconstrucción de geometría ──────────────────────────────────────
    vec2  screenUV  = gl_FragCoord.xy / ScreenSize;
    float depth     = texture(Sampler1, screenUV).r;
    if (depth >= 0.99999) discard;

    vec3 surfacePos = reconstructViewPos(screenUV, depth);
    vec3 toSurface  = surfacePos - LightPos;          // vector luz → superficie
    vec3 lightDir   = normalize(LightDir);

    float zDist  = dot(toSurface, lightDir);
    float maxDist = min(MaxLen, MaxGoboDist);
    if (zDist <= 0.0 || zDist > maxDist) discard;

    // ── 1. Ejes del cono ────────────────────────────────────────────────────
    vec3  uAxis  = normalize(AxisU);
    vec3  vAxis  = normalize(AxisV);
    float u      = dot(toSurface, uAxis);
    float v      = dot(toSurface, vAxis);
    float radius = max(BaseRadius + zDist * max(TanHalfAngle, 0.0001), 0.0001);
    float radial = length(vec2(u, v)) / radius;
    if (radial >= 1.0) discard;

    // ── 2. Normal geométrica de pantalla ────────────────────────────────────
    vec3 N = reconstructNormal(surfacePos);
    // Aseguramos que la normal apunte hacia la cámara (hacia origen en view-space)
    if (dot(N, -surfacePos) < 0.0) N = -N;

    // ── 3. Vectores de iluminación ──────────────────────────────────────────
    vec3  L    = normalize(-toSurface);       // superficie → luz
    vec3  V    = normalize(-surfacePos);      // superficie → cámara (view-space)

    // ── 4. BRDF: difusa + especular ─────────────────────────────────────────
    float NdotV      = max(dot(N, V), 0.0);
    vec2  lighting   = brdf(N, L, V, 1.0 - Focus);

    // Fresnel edge darkening: reduce contribución en ángulos grazing
    float fresnel    = fresnelSchlick(NdotV);
    float edgeDim    = 1.0 - fresnel * 0.65;   // 0.35 mínimo en grazing

    // ── 5. Atenuaciones ─────────────────────────────────────────────────────
    float focusNorm  = clamp(Focus, 0.0, 1.0);
    float cone       = coneFalloff(radial, focusNorm);
    float distAtten  = distanceAttenuation(zDist, maxDist);

    // ── 6. Oclusión geométrica (plano de corte) ──────────────────────────────
    if (OcclusionEnabled > 0.5) {
        vec3  ray = toSurface;
        float d   = length(ray);
        if (d > 0.0001) {
            vec3  rd    = ray / d;
            float denom = dot(rd, OcclusionNormal);
            if (abs(denom) > 0.00001) {
                float hitT  = dot(OcclusionPos - LightPos, OcclusionNormal) / denom;
                float bias  = max(0.08, d * 0.03);
                if (hitT > 0.001 && hitT < d - bias) discard;
            }
        }
    }

    // ── 7. UV del Gobo ───────────────────────────────────────────────────────
    float angle       = radians(GoboRotation);
    float ca = cos(angle), sa = sin(angle);
    float slotDistance = radius * 2.0;
    float blurAmount   = focusNorm * focusNorm * 0.045;

    // Slot A
    float uA = u + WheelTransition * slotDistance;
    float ruA = uA * ca - v * sa;
    float rvA = uA * sa + v * ca;
    vec2 uvA  = vec2(ruA / radius * 0.5 + 0.5, 1.0 - (rvA / radius * 0.5 + 0.5));

    // Slot B
    float uB  = u - (1.0 - WheelTransition) * slotDistance;
    float ruB = uB * ca - v * sa;
    float rvB = uB * sa + v * ca;
    vec2 uvB  = vec2(ruB / radius * 0.5 + 0.5, 1.0 - (rvB / radius * 0.5 + 0.5));

    // Muestras
    vec4  sA = vec4(0.0), sB = vec4(0.0);
    if (all(greaterThanEqual(uvA, vec2(0.0))) && all(lessThanEqual(uvA, vec2(1.0))))
        sA = sampleGoboBlur(Sampler0, uvA, blurAmount);
    if (all(greaterThanEqual(uvB, vec2(0.0))) && all(lessThanEqual(uvB, vec2(1.0))))
        sB = sampleGoboBlur(Sampler2, uvB, blurAmount);

    // Luminancia de cada slot como máscara de transmisión
    float lumA     = dot(sA.rgb, vec3(0.299, 0.587, 0.114));
    float lumB     = dot(sB.rgb, vec3(0.299, 0.587, 0.114));
    float maskA    = sA.a * lumA;
    float maskB    = sB.a * lumB;
    float totalMask = maskA + maskB;
    if (totalMask <= 0.001) discard;

    // Color transmitido del gobo (mezcla ponderada)
    vec3 goboColor = (sA.rgb * maskA + sB.rgb * maskB) / totalMask;

    // ── 8. Máscaras de shutters y animación ─────────────────────────────────
    float shutterSoft = 0.012 + focusNorm * 0.06;
    float shutterM    = shutterMask(u, v, radius, shutterSoft);
    float animM       = animationMask(u, v, radius);
    totalMask        *= shutterM * animM;
    if (totalMask <= 0.001) discard;

    // ── 9. Sombra voxel ──────────────────────────────────────────────────────
    vec3  wposS  = LightPosW + zDist * LightDirW + u * AxisUW + v * AxisVW;
    float shadow = shadowFactor(wposS, LightPosW, VoxelCell * 1.05);
    if (shadow < 0.001) discard;

    // ── 10. Ensamble final ───────────────────────────────────────────────────

    float diffuse  = lighting.x;
    float specular = lighting.y;

    float finalFactor = totalMask
                      * cone
                      * distAtten
                      * edgeDim
                      * shadow
                      * Intensity
                      * VertexColor.a;

    // Difusa teñida por el gobo, especular neutra (blanca) como UE4
    vec3 diffuseContrib  = LightColor * goboColor * diffuse  * finalFactor;
    vec3 specularContrib = LightColor             * specular * finalFactor * 0.3;

    vec3 finalColor = diffuseContrib + specularContrib;

    // Escala a rango bajo para blend DST_COLOR, ONE:
    // Con este blend: output = sceneColor × finalColor + sceneColor
    // Si finalColor = 0.0 → escena sin cambio
    // Si finalColor = 0.4 → escena × 1.4 → iluminada, no saturada
    // NUNCA superar 0.5 o se convierte en calcomanía brillante
    finalColor = clamp(finalColor * 0.38, 0.0, 0.48);

    fragColor = vec4(finalColor, 1.0);
}

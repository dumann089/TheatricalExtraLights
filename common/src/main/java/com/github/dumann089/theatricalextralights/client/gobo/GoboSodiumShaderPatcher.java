
package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GoboSodiumShaderPatcher {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static final String VERTEX_COLOR_MARKER = "out vec4 v_Color;";
    private static final String VERTEX_POSITION_MARKER =
            "vec3 position = _vert_position + translation;";

    private static final String FRAGMENT_TEXCOORD_MARKER = "in vec2 v_TexCoord;";

    private static final String FRAGMENT_OUTPUT_MARKER =
            "fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);";

    private static final String FRAGMENT_SAMPLE_MARKER =
            "vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, v_MaterialMipBias);";

    private static final String GLSL_DECLARATIONS = """

            uniform sampler2D GoboData;
            uniform sampler2D GoboTileIndices;
            uniform sampler2DArray GoboAtlas;

            uniform int GoboProjectorCount;
            uniform int GoboTileSize;
            uniform int GoboTilesX;
            uniform int GoboTilesY;
            uniform int GoboMaxPerTile;

            uniform vec2 GoboScreenSize;
            uniform float GoboBrightness;

            vec4 goboDecodeLayer(vec4 sampleColor) {
                float peak = max(
                        sampleColor.r,
                        max(sampleColor.g, sampleColor.b)
                );

                if (peak <= 0.0001) {
                    return vec4(0.0);
                }

                return vec4(
                        sampleColor.rgb / peak,
                        sampleColor.a * peak
                );
            }

            vec2 goboRotateUv(vec2 uv, float angle) {
                vec2 p = uv - vec2(0.5);

                float c = cos(angle);
                float s = sin(angle);

                return vec2(
                        c * p.x - s * p.y,
                        s * p.x + c * p.y
                ) + vec2(0.5);
            }

            vec3 goboDirectLighting(vec3 viewPosition, vec3 albedo) {
                if (GoboProjectorCount <= 0
                        || GoboTileSize <= 0
                        || GoboTilesX <= 0
                        || GoboTilesY <= 0) {
                    return vec3(0.0);
                }

                vec3 geometricNormal = cross(
                        dFdx(viewPosition),
                        dFdy(viewPosition)
                );

                float normalLengthSquared = dot(
                        geometricNormal,
                        geometricNormal
                );

                if (normalLengthSquared <= 1.0e-20) {
                    return vec3(0.0);
                }

                geometricNormal *= inversesqrt(normalLengthSquared);

                if (dot(geometricNormal, -viewPosition) < 0.0) {
                    geometricNormal = -geometricNormal;
                }

                ivec2 pixel = ivec2(gl_FragCoord.xy);

                pixel = clamp(
                        pixel,
                        ivec2(0),
                        ivec2(
                                max(int(GoboScreenSize.x) - 1, 0),
                                max(int(GoboScreenSize.y) - 1, 0)
                        )
                );

                ivec2 tile = pixel / GoboTileSize;

                tile = clamp(
                        tile,
                        ivec2(0),
                        ivec2(GoboTilesX - 1, GoboTilesY - 1)
                );

                int tileIndex = tile.y * GoboTilesX + tile.x;

                vec3 accumulatedLight = vec3(0.0);

                for (int slot = 0; slot < GoboMaxPerTile; slot++) {
                    float storedIndex = texelFetch(
                            GoboTileIndices,
                            ivec2(slot, tileIndex),
                            0
                    ).r;

                    if (storedIndex < 0.0) {
                        break;
                    }

                    int projectorIndex = int(floor(storedIndex + 0.5));

                    if (projectorIndex >= GoboProjectorCount) {
                        continue;
                    }

                    // Load geometric data first.
                    vec4 p0 = texelFetch(GoboData, ivec2(0, projectorIndex), 0);
                    vec4 p1 = texelFetch(GoboData, ivec2(1, projectorIndex), 0);
                    vec4 p2 = texelFetch(GoboData, ivec2(2, projectorIndex), 0);
                    vec4 p3 = texelFetch(GoboData, ivec2(3, projectorIndex), 0);

                    vec3 lightPosition = p0.xyz;

                    // The manager already normalizes projector directions.
                    vec3 lightDirection = p1.xyz;

                    float maxDistance = p2.w;
                    float intensity = p3.w;

                    if (maxDistance <= 0.001 || intensity <= 0.001) {
                        continue;
                    }

                    vec3 fromLight = viewPosition - lightPosition;

                    float forwardDistance = dot(
                            fromLight,
                            lightDirection
                    );

                    if (forwardDistance <= 0.001
                            || forwardDistance >= maxDistance) {
                        continue;
                    }

                    float radius = max(
                            p0.w + forwardDistance * p1.w,
                            0.0001
                    );

                    float u = dot(fromLight, p2.xyz);
                    float v = dot(fromLight, p3.xyz);

                    // Reject outside the beam before expensive operations.
                    float radialSquared = (u * u + v * v) / (radius * radius);

                    if (radialSquared >= 1.0) {
                        continue;
                    }

                    float radial = sqrt(max(radialSquared, 0.0));

                    float distanceSquared = dot(fromLight, fromLight);

                    if (distanceSquared <= 0.00000001) {
                        continue;
                    }

                    float inverseDistance = inversesqrt(distanceSquared);
                    float surfaceDistance = 1.0 / inverseDistance;

                    vec3 surfaceRayDirection = fromLight * inverseDistance;
                    vec3 toLight = -surfaceRayDirection;

                    // Occlusion is evaluated before sampling the gobo textures.
                    vec4 p9 = texelFetch(GoboData, ivec2(9, projectorIndex), 0);

                    if (p9.y > 0.5) {
                        vec3 p10 = texelFetch(
                                GoboData,
                                ivec2(10, projectorIndex),
                                0
                        ).xyz;

                        vec3 p11 = texelFetch(
                                GoboData,
                                ivec2(11, projectorIndex),
                                0
                        ).xyz;

                        float denominator = dot(
                                surfaceRayDirection,
                                p11
                        );

                        if (abs(denominator) > 0.00001) {
                            float hitDistance = dot(
                                    p10 - lightPosition,
                                    p11
                            ) / denominator;

                            float occlusionBias = max(
                                    0.08,
                                    surfaceDistance * 0.03
                            );

                            if (hitDistance > 0.001
                                    && hitDistance < surfaceDistance - occlusionBias) {
                                continue;
                            }
                        }
                    }

                    // Reject surfaces facing away before atlas sampling.
                    float ndotl = dot(
                            geometricNormal,
                            toLight
                    );

                    if (ndotl <= 0.001) {
                        continue;
                    }

                    float angleAttenuation = smoothstep(
                            0.02,
                            0.25,
                            ndotl
                    );

                    // Load optical data only after geometric rejection.
                    vec4 p4 = texelFetch(GoboData, ivec2(4, projectorIndex), 0);
                    vec4 p5 = texelFetch(GoboData, ivec2(5, projectorIndex), 0);
                    vec4 p6 = texelFetch(GoboData, ivec2(6, projectorIndex), 0);

                    float wheelProgress = clamp(p6.x, 0.0, 1.0);
                    float slotDistance = radius * 2.0;

                    float angle = p4.w;
                    float ca = cos(angle);
                    float sa = sin(angle);

                    float localUA = u + wheelProgress * slotDistance;

                    float ruA = localUA * ca - v * sa;
                    float rvA = localUA * sa + v * ca;

                    vec2 uvA = vec2(
                            ruA / radius * 0.5 + 0.5,
                            1.0 - (rvA / radius * 0.5 + 0.5)
                    );

                    float localUB = u - (1.0 - wheelProgress) * slotDistance;

                    float ruB = localUB * ca - v * sa;
                    float rvB = localUB * sa + v * ca;

                    vec2 uvB = vec2(
                            ruB / radius * 0.5 + 0.5,
                            1.0 - (rvB / radius * 0.5 + 0.5)
                    );

                    float focusLod = clamp(p5.w * 4.0, 0.0, 5.0);

                    int outgoingLayer = int(floor(p5.x + 0.5));
                    int incomingLayer = int(floor(p5.y + 0.5));

                    vec4 goboA = vec4(0.0);
                    vec4 goboB = vec4(0.0);

                    if (uvA.x >= 0.0 && uvA.x <= 1.0
                            && uvA.y >= 0.0 && uvA.y <= 1.0) {
                        goboA = goboDecodeLayer(
                                textureLod(
                                        GoboAtlas,
                                        vec3(uvA, float(outgoingLayer)),
                                        focusLod
                                )
                        );
                    }

                    if (uvB.x >= 0.0 && uvB.x <= 1.0
                            && uvB.y >= 0.0 && uvB.y <= 1.0) {
                        goboB = goboDecodeLayer(
                                textureLod(
                                        GoboAtlas,
                                        vec3(uvB, float(incomingLayer)),
                                        focusLod
                                )
                        );
                    }

                    float goboAlpha = goboA.a + goboB.a;

                    if (goboAlpha <= 0.001) {
                        continue;
                    }

                    vec3 goboColor = (
                            goboA.rgb * goboA.a
                            + goboB.rgb * goboB.a
                    ) / max(goboAlpha, 0.0001);

                    if (p6.w > 0.5 && p5.z > 0.5) {
                        vec2 animationPosition = vec2(u, v) / radius;

                        float animationAngle = p6.y;
                        float animationCos = cos(animationAngle);
                        float animationSin = sin(animationAngle);

                        vec2 rotatedAnimation = vec2(
                                animationPosition.x * animationCos
                                        - animationPosition.y * animationSin,
                                animationPosition.x * animationSin
                                        + animationPosition.y * animationCos
                        );

                        vec2 animationUv = fract(vec2(
                                rotatedAnimation.x * 0.5 + p6.z,
                                rotatedAnimation.y * 0.5
                        ));

                        vec4 animationSample = textureLod(
                                GoboAtlas,
                                vec3(animationUv, p5.z),
                                focusLod
                        );

                        float animationMask = dot(
                                animationSample.rgb,
                                vec3(0.299, 0.587, 0.114)
                        );

                        goboAlpha *= clamp(animationMask, 0.0, 1.0);
                    }

                    if (goboAlpha <= 0.001) {
                        continue;
                    }

                    float edgeFade = 1.0 - smoothstep(
                            0.88,
                            1.0,
                            radial
                    );

                    float distanceFade = 1.0 - smoothstep(
                            maxDistance * 0.30,
                            maxDistance,
                            forwardDistance
                    );

                    float physicalFalloff = 1.0 / (
                            1.0
                            + forwardDistance * forwardDistance * 0.0015
                    );

                    float radialCore = 1.0 - radialSquared;
                    float opticalHotspot = exp(-radialSquared * 12.0);

                    float beamProfile =
                            mix(0.78, 1.12, radialCore)
                            + opticalHotspot * 0.13;

                    vec3 lightSpectrum = p4.rgb * goboColor;

                    float illumination =
                            goboAlpha
                            * edgeFade
                            * distanceFade
                            * physicalFalloff
                            * angleAttenuation
                            * beamProfile
                            * intensity
                            * GoboBrightness;

                    accumulatedLight +=
                            albedo
                            * lightSpectrum
                            * illumination
                            * 1.85;
                }

                return accumulatedLight;
            }

            """;

    private GoboSodiumShaderPatcher() {
    }

    public static String patch(String identifier, String source) {
        if (identifier == null || source == null) {
            return source;
        }

        int separator = identifier.indexOf(':');

        if (separator <= 0 || separator >= identifier.length() - 1) {
            return source;
        }

        String namespace = identifier.substring(0, separator);
        String path = identifier.substring(separator + 1);

        if (!namespace.equals("sodium") && !namespace.equals("embeddium")) {
            return source;
        }

        return switch (path) {
            case "blocks/block_layer_opaque.vsh" -> patchVertex(identifier, source);
            case "blocks/block_layer_opaque.fsh" -> patchFragment(identifier, source);
            default -> source;
        };
    }

    private static String patchVertex(String identifier, String source) {
        if (source.contains("goboViewPosition")) {
            return source;
        }

        if (!source.contains(VERTEX_COLOR_MARKER)
                || !source.contains(VERTEX_POSITION_MARKER)) {
            warnOnce(
                    identifier,
                    "Could not find the expected Sodium vertex shader markers"
            );
            return source;
        }

        String patched = source.replace(
                VERTEX_COLOR_MARKER,
                VERTEX_COLOR_MARKER + """


                        out vec3 goboViewPosition;
                        out vec4 goboVertexColor;
                        """
        );

        patched = patched.replace(
                VERTEX_POSITION_MARKER,
                VERTEX_POSITION_MARKER + """


                            goboViewPosition = (
                                    u_ModelViewMatrix * vec4(position, 1.0)
                            ).xyz;

                            goboVertexColor = _vert_color;
                        """
        );

        LOGGER.info(
                "[TheatricalExtraLights/Gobo] Patched terrain vertex shader: {}",
                identifier
        );

        return patched;
    }

    private static String patchFragment(String identifier, String source) {
        if (source.contains("goboDirectLighting")) {
            return source;
        }

        if (!source.contains(FRAGMENT_TEXCOORD_MARKER)
                || !source.contains(FRAGMENT_SAMPLE_MARKER)
                || !source.contains(FRAGMENT_OUTPUT_MARKER)
                || !source.contains("void main() {")) {
            warnOnce(
                    identifier,
                    "Could not find the expected terrain fragment shader markers"
            );
            return source;
        }

        source = source.replace(
                FRAGMENT_TEXCOORD_MARKER,
                """
                in vec2 v_TexCoord;
                in vec3 goboViewPosition;
                in vec4 goboVertexColor;
                """
        );

        source = source.replace(
                "void main() {",
                GLSL_DECLARATIONS + "\nvoid main() {"
        );

        source = source.replace(
                FRAGMENT_SAMPLE_MARKER,
                FRAGMENT_SAMPLE_MARKER + """


                    vec3 goboBaseAlbedo =
                            diffuseColor.rgb * goboVertexColor.rgb;
                    """
        );

        source = source.replace(
                FRAGMENT_OUTPUT_MARKER,
                """
                    diffuseColor.rgb += goboDirectLighting(
                            goboViewPosition,
                            goboBaseAlbedo
                    );

                    """ + FRAGMENT_OUTPUT_MARKER
        );

        LOGGER.info(
                "[TheatricalExtraLights/Gobo] Patched terrain fragment shader: {}",
                identifier
        );

        return source;
    }

    private static void warnOnce(String identifier, String message) {
        if (WARNED.add(identifier)) {
            LOGGER.warn(
                    "[TheatricalExtraLights/Gobo] {}: {}",
                    message,
                    identifier
            );
        }
    }
}
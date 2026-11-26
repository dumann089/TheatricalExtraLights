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
                float peak = max(sampleColor.r, max(sampleColor.g, sampleColor.b));
                if (peak <= 0.0001) {
                    return vec4(0.0);
                }
                return vec4(sampleColor.rgb / peak, sampleColor.a * peak);
            }

            float goboBladeMask(vec2 p, vec2 n, float insA, float insB, float soft) {
                if (insA <= 0.0005 && insB <= 0.0005) {
                    return 1.0;
                }
                vec2 tangent = vec2(n.y, -n.x);
                const float R = 1.15;
                vec2 cornerA = n * (R - 2.0 * R * insA) - tangent * R;
                vec2 cornerB = n * (R - 2.0 * R * insB) + tangent * R;
                vec2 edge = cornerB - cornerA;
                if (dot(edge, edge) <= 0.0000001) {
                    return 1.0;
                }
                vec2 normal = normalize(vec2(edge.y, -edge.x));
                if (dot(normal, n) < 0.0) {
                    normal = -normal;
                }
                return 1.0 - smoothstep(-soft, soft, dot(p - cornerA, normal));
            }

            float goboShutterMask(
                    float u, float v, float inverseRadius,
                    vec4 bladeA, vec4 bladeB,
                    float frameRotation, float soft
            ) {
                vec2 p = vec2(u, v) * inverseRadius;
                float cr = cos(-frameRotation);
                float sr = sin(-frameRotation);
                vec2 q = vec2(p.x * cr - p.y * sr, p.x * sr + p.y * cr);
                float mask = 1.0;
                mask *= goboBladeMask(q, vec2(0.0, 1.0), bladeA.x, bladeB.x, soft);
                mask *= goboBladeMask(q, vec2(1.0, 0.0), bladeA.y, bladeB.y, soft);
                mask *= goboBladeMask(q, vec2(0.0, -1.0), bladeA.z, bladeB.z, soft);
                mask *= goboBladeMask(q, vec2(-1.0, 0.0), bladeA.w, bladeB.w, soft);
                return mask;
            }

            float goboAnimationMask(
                    float u, float v, float inverseRadius, float layer,
                    float angle, float offset, float lod
            ) {
                if (layer < 0.5) {
                    return 1.0;
                }
                float ca = cos(angle);
                float sa = sin(angle);
                vec2 p = vec2(u, v) * inverseRadius;
                vec2 rotated = vec2(p.x * ca - p.y * sa, p.x * sa + p.y * ca);
                vec2 uv = fract(vec2(rotated.x * 0.5 + offset, rotated.y * 0.5));
                vec3 animationColor = textureLod(
                        GoboAtlas, vec3(uv, layer), lod
                ).rgb;
                return clamp(dot(animationColor, vec3(0.299, 0.587, 0.114)), 0.0, 1.0);
            }

            vec3 goboProjectorLighting(
                    int projectorIndex,
                    vec3 viewPosition,
                    vec3 geometricNormal,
                    vec3 albedo,
                    float vertexAlpha
            ) {
                vec4 p0 = texelFetch(GoboData, ivec2(0, projectorIndex), 0);
                vec4 p1 = texelFetch(GoboData, ivec2(1, projectorIndex), 0);
                vec4 p2 = texelFetch(GoboData, ivec2(2, projectorIndex), 0);
                vec4 p3 = texelFetch(GoboData, ivec2(3, projectorIndex), 0);

                float intensity = p3.w;
                float maxDistance = p2.w;
                if (intensity <= 0.001 || maxDistance <= 0.001) {
                    return vec3(0.0);
                }

                vec3 fromLight = viewPosition - p0.xyz;
                float forwardDistance = dot(fromLight, p1.xyz);
                if (forwardDistance <= 0.001 || forwardDistance >= maxDistance) {
                    return vec3(0.0);
                }

                float radius = max(p0.w + forwardDistance * p1.w, 0.0001);
                float inverseRadius = 1.0 / radius;
                float u = dot(fromLight, p2.xyz);
                float v = dot(fromLight, p3.xyz);
                float radialSquared = (u * u + v * v) * inverseRadius * inverseRadius;
                if (radialSquared >= 1.0) {
                    return vec3(0.0);
                }

                vec4 p4 = texelFetch(GoboData, ivec2(4, projectorIndex), 0);
                vec4 p5 = texelFetch(GoboData, ivec2(5, projectorIndex), 0);
                if (p5.x < 0.5) {
                    return vec3(0.0);
                }

                vec4 featureData = texelFetch(GoboData, ivec2(12, projectorIndex), 0);
                int featureFlags = int(featureData.x + 0.5);
                bool hasOcclusion = (featureFlags & 1) != 0;
                bool hasWheel = (featureFlags & 2) != 0;
                bool hasShutters = (featureFlags & 4) != 0;
                bool hasAnimation = (featureFlags & 8) != 0;
                float blurLod = clamp(featureData.y, 0.0, 4.0);

                float distanceSquared = dot(fromLight, fromLight);
                if (distanceSquared <= 0.00000001) {
                    return vec3(0.0);
                }
                float inverseDistance = inversesqrt(distanceSquared);
                vec3 toLight = -fromLight * inverseDistance;
                float ndotl = abs(dot(geometricNormal, toLight));
                if (ndotl <= 0.001) {
                    return vec3(0.0);
                }

                if (hasOcclusion) {
                    vec3 occlusionPosition = texelFetch(
                            GoboData, ivec2(10, projectorIndex), 0
                    ).xyz;
                    vec3 occlusionNormal = texelFetch(
                            GoboData, ivec2(11, projectorIndex), 0
                    ).xyz;
                    float surfaceDistance = 1.0 / inverseDistance;
                    float denominator = dot(fromLight * inverseDistance, occlusionNormal);
                    if (abs(denominator) > 0.00001) {
                        float hitDistance = dot(
                                occlusionPosition - p0.xyz,
                                occlusionNormal
                        ) / denominator;
                        float bias = max(0.08, surfaceDistance * 0.03);
                        if (hitDistance > 0.001 && hitDistance < surfaceDistance - bias) {
                            return vec3(0.0);
                        }
                    }
                }

                float wheelTransition = 0.0;
                float animationAngle = 0.0;
                float animationOffset = 0.0;
                if (hasWheel || hasAnimation) {
                    vec4 p6 = texelFetch(GoboData, ivec2(6, projectorIndex), 0);
                    if (hasWheel) {
                        wheelTransition = clamp(p6.x, 0.0, 1.0);
                    }
                    if (hasAnimation) {
                        animationAngle = p6.y;
                        animationOffset = p6.z;
                    }
                }

                float focus = clamp(p5.w, 0.0, 1.0);
                float slotDistance = radius * 2.0;
                float angle = p4.w;
                float ca = cos(angle);
                float sa = sin(angle);
                float localA = u + wheelTransition * slotDistance;
                float rotatedAX = localA * ca - v * sa;
                float rotatedAY = localA * sa + v * ca;
                vec2 uvA = vec2(
                        rotatedAX * inverseRadius * 0.5 + 0.5,
                        1.0 - (rotatedAY * inverseRadius * 0.5 + 0.5)
                );

                vec4 goboA = vec4(0.0);
                vec4 goboB = vec4(0.0);
                if (uvA.x >= 0.0 && uvA.x <= 1.0 && uvA.y >= 0.0 && uvA.y <= 1.0) {
                    goboA = goboDecodeLayer(
                            textureLod(GoboAtlas, vec3(uvA, p5.x), blurLod)
                    );
                }

                if (hasWheel && wheelTransition > 0.0001 && p5.y >= 0.5) {
                    float localB = u - (1.0 - wheelTransition) * slotDistance;
                    float rotatedBX = localB * ca - v * sa;
                    float rotatedBY = localB * sa + v * ca;
                    vec2 uvB = vec2(
                            rotatedBX * inverseRadius * 0.5 + 0.5,
                            1.0 - (rotatedBY * inverseRadius * 0.5 + 0.5)
                    );
                    if (uvB.x >= 0.0 && uvB.x <= 1.0 && uvB.y >= 0.0 && uvB.y <= 1.0) {
                        goboB = goboDecodeLayer(
                                textureLod(GoboAtlas, vec3(uvB, p5.y), blurLod)
                        );
                    }
                }

                float goboAlpha = goboA.a + goboB.a;
                if (goboAlpha <= 0.001) {
                    return vec3(0.0);
                }
                vec3 goboColor = (
                        goboA.rgb * goboA.a + goboB.rgb * goboB.a
                ) / max(goboAlpha, 0.0001);

                if (hasShutters) {
                    vec4 p7 = texelFetch(GoboData, ivec2(7, projectorIndex), 0);
                    vec4 p8 = texelFetch(GoboData, ivec2(8, projectorIndex), 0);
                    goboAlpha *= goboShutterMask(
                            u, v, inverseRadius, p7, p8, featureData.z,
                            0.012 + focus * 0.06
                    );
                    if (goboAlpha <= 0.001) {
                        return vec3(0.0);
                    }
                }

                if (hasAnimation) {
                    goboAlpha *= goboAnimationMask(
                            u, v, inverseRadius, p5.z,
                            animationAngle, animationOffset, blurLod
                    );
                    if (goboAlpha <= 0.001) {
                        return vec3(0.0);
                    }
                }

                float radial = sqrt(max(radialSquared, 0.0));
                float angleAttenuation = mix(0.90, 1.0, smoothstep(0.0, 0.25, ndotl));
                float edgeStart = mix(0.88, 0.72, focus);
                float edgeFade = 1.0 - smoothstep(edgeStart, 1.0, radial);
                float radialCore = max(1.0 - radialSquared, 0.0);
                float radialCore2 = radialCore * radialCore;
                float opticalHotspot = radialCore2 * radialCore2;
                float beamProfile = mix(0.78, 1.12, radialCore) + opticalHotspot * 0.13;

                float angularSpread = clamp(p1.w * 2.0, 0.0, 1.0);
                float fadeStartRatio = mix(0.90, 0.30, angularSpread);

                float distanceFade = 1.0 - smoothstep(
                        maxDistance * fadeStartRatio,
                        maxDistance,
                        forwardDistance
                );

                float physicalFalloff = 1.0 / (
                        1.0 + forwardDistance * forwardDistance * 0.00015
                );

                float spreadRadius = forwardDistance * max(p1.w, 0.0);
                float spreadFalloff = 1.0 / (
                        1.0 + spreadRadius * spreadRadius * 0.00075
                );

                float illumination = clamp(
                        goboAlpha * edgeFade * distanceFade
                        * physicalFalloff * spreadFalloff
                        * angleAttenuation * beamProfile * intensity
                        * GoboBrightness * vertexAlpha,
                        0.0,
                        1.0
                );

                vec3 effectiveAlbedo = max(albedo, vec3(0.012));
                vec3 lightSpectrum = max(p4.rgb, vec3(0.0)) * max(goboColor, vec3(0.0));
                return effectiveAlbedo * lightSpectrum * beamProfile * 1.85 * illumination;
            }

            vec3 goboDirectLighting(vec3 viewPosition, vec3 albedo, float vertexAlpha) {
                if (GoboProjectorCount <= 0 || GoboTileSize <= 0
                        || GoboTilesX <= 0 || GoboTilesY <= 0 || GoboMaxPerTile <= 0) {
                    return vec3(0.0);
                }

                ivec2 pixel = clamp(
                        ivec2(gl_FragCoord.xy),
                        ivec2(0),
                        ivec2(max(int(GoboScreenSize.x) - 1, 0),
                                max(int(GoboScreenSize.y) - 1, 0))
                );
                ivec2 tile = pixel / GoboTileSize;
                tile = clamp(tile, ivec2(0), ivec2(GoboTilesX - 1, GoboTilesY - 1));
                int tileIndex = tile.y * GoboTilesX + tile.x;

                vec4 firstIndices = texelFetch(GoboTileIndices, ivec2(0, tileIndex), 0);
                if (firstIndices.x < 0.0) {
                    return vec3(0.0);
                }

                vec3 geometricNormal = cross(dFdx(viewPosition), dFdy(viewPosition));
                float normalLengthSquared = dot(geometricNormal, geometricNormal);
                if (normalLengthSquared <= 1.0e-20) {
                    return vec3(0.0);
                }
                geometricNormal *= inversesqrt(normalLengthSquared);
                if (dot(geometricNormal, -viewPosition) < 0.0) {
                    geometricNormal = -geometricNormal;
                }

                int groupCount = (GoboMaxPerTile + 3) / 4;
                vec3 accumulatedLight = vec3(0.0);

                for (int group = 0; group < groupCount; group++) {
                    vec4 packedIndices = group == 0
                            ? firstIndices
                            : texelFetch(GoboTileIndices, ivec2(group, tileIndex), 0);

                    float storedIndex = packedIndices.x;
                    if (storedIndex < 0.0) break;
                    int projectorIndex = int(storedIndex + 0.5);
                    if (projectorIndex < GoboProjectorCount) {
                        accumulatedLight += goboProjectorLighting(
                                projectorIndex, viewPosition, geometricNormal,
                                albedo, vertexAlpha
                        );
                    }

                    storedIndex = packedIndices.y;
                    if (storedIndex < 0.0) break;
                    projectorIndex = int(storedIndex + 0.5);
                    if (projectorIndex < GoboProjectorCount) {
                        accumulatedLight += goboProjectorLighting(
                                projectorIndex, viewPosition, geometricNormal,
                                albedo, vertexAlpha
                        );
                    }

                    storedIndex = packedIndices.z;
                    if (storedIndex < 0.0) break;
                    projectorIndex = int(storedIndex + 0.5);
                    if (projectorIndex < GoboProjectorCount) {
                        accumulatedLight += goboProjectorLighting(
                                projectorIndex, viewPosition, geometricNormal,
                                albedo, vertexAlpha
                        );
                    }

                    storedIndex = packedIndices.w;
                    if (storedIndex < 0.0) break;
                    projectorIndex = int(storedIndex + 0.5);
                    if (projectorIndex < GoboProjectorCount) {
                        accumulatedLight += goboProjectorLighting(
                                projectorIndex, viewPosition, geometricNormal,
                                albedo, vertexAlpha
                        );
                    }
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
            warnOnce(identifier, "Could not find the expected Sodium vertex shader markers");
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

                        goboViewPosition = (u_ModelViewMatrix * vec4(position, 1.0)).xyz;
                        goboVertexColor = _vert_color;
                    """
        );

        LOGGER.info("[TheatricalExtraLights/Gobo] Patched terrain vertex shader: {}", identifier);
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
            warnOnce(identifier, "Could not find the expected terrain fragment shader markers");
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
        source = source.replace("void main() {", GLSL_DECLARATIONS + "\nvoid main() {");
        source = source.replace(
                FRAGMENT_SAMPLE_MARKER,
                FRAGMENT_SAMPLE_MARKER + """

                    vec3 goboBaseAlbedo = diffuseColor.rgb * goboVertexColor.rgb;
                    """
        );
        source = source.replace(
                FRAGMENT_OUTPUT_MARKER,
                """
                    if (GoboProjectorCount > 0) {
                        diffuseColor.rgb += goboDirectLighting(
                                goboViewPosition,
                                goboBaseAlbedo,
                                goboVertexColor.a
                        );
                    }

                    """ + FRAGMENT_OUTPUT_MARKER
        );

        LOGGER.info("[TheatricalExtraLights/Gobo] Patched terrain fragment shader: {}", identifier);
        return source;
    }

    private static void warnOnce(String identifier, String message) {
        if (WARNED.add(identifier)) {
            LOGGER.warn("[TheatricalExtraLights/Gobo] {}: {}", message, identifier);
        }
    }
}
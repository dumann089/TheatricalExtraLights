package com.github.dumann089.theatricalextralights.client.gobo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoboShaderResourceProvider implements ResourceProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Set<String> SUPPORTED_RENDER_TYPES = Set.of(
            "rendertype_entity_cutout",
            "rendertype_entity_cutout_no_cull",
            "rendertype_entity_solid",
            "rendertype_entity_translucent",
            "rendertype_armor_cutout_no_cull",
            "rendertype_solid",
            "rendertype_cutout",
            "rendertype_cutout_mipped",
            "rendertype_translucent",
            "rendertype_tripwire"
    );

    private static final ConcurrentHashMap<String, Set<String>> PATCHED_PARTS =
            new ConcurrentHashMap<>();

    private static final Pattern GL_POSITION_PATTERN = Pattern.compile(
            "gl_Position\\s*=\\s*ProjMat\\s*\\*\\s*ModelViewMat\\s*\\*\\s*vec4\\s*\\(\\s*(Position|pos)\\s*,\\s*1\\.0\\s*\\)\\s*;"
    );

    private static final Pattern FOG_OUTPUT_PATTERN = Pattern.compile(
            "fragColor\\s*=\\s*linear_fog\\s*\\(\\s*color\\s*,\\s*vertexDistance\\s*,\\s*FogStart\\s*,\\s*FogEnd\\s*,\\s*FogColor\\s*\\)\\s*;"
    );

    private final ResourceProvider parent;

    public GoboShaderResourceProvider(ResourceProvider parent) {
        this.parent = parent;
    }

    public static boolean isReady() {
        for (Set<String> parts : PATCHED_PARTS.values()) {
            if (parts.contains("json") && parts.contains("vsh") && parts.contains("fsh")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        Optional<Resource> optional = parent.getResource(location);
        if (optional.isEmpty()) {
            return optional;
        }

        String renderType = getRenderType(location);
        if (renderType == null) {
            return optional;
        }

        String extension = getExtension(location);
        if (!extension.equals("json") && !extension.equals("vsh") && !extension.equals("fsh")) {
            return optional;
        }

        Resource original = optional.get();
        try {
            String source;
            try (InputStream input = original.open()) {
                source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            String patched = switch (extension) {
                case "json" -> patchJson(source);
                case "vsh" -> patchVertex(source);
                case "fsh" -> patchFragment(source);
                default -> source;
            };

            PATCHED_PARTS.computeIfAbsent(renderType, key -> ConcurrentHashMap.newKeySet())
                    .add(extension);

            byte[] output = patched.getBytes(StandardCharsets.UTF_8);
            return Optional.of(new Resource(
                    original.source(),
                    () -> new ByteArrayInputStream(output),
                    original::metadata
            ));
        } catch (Exception exception) {
            LOGGER.error("[TheatricalExtraLights/Gobo] Could not patch {}", location, exception);
            return optional;
        }
    }

    private static String getRenderType(ResourceLocation location) {
        if (!location.getNamespace().equals("minecraft")) {
            return null;
        }

        String path = location.getPath();
        String prefix = "shaders/core/";
        if (!path.startsWith(prefix)) {
            return null;
        }

        String file = path.substring(prefix.length());
        int extensionIndex = file.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return null;
        }

        String name = file.substring(0, extensionIndex);
        return SUPPORTED_RENDER_TYPES.contains(name) ? name : null;
    }

    private static String getExtension(ResourceLocation location) {
        String path = location.getPath();
        int index = path.lastIndexOf('.');
        return index < 0 ? "" : path.substring(index + 1);
    }

    private static String patchJson(String source) {
        JsonObject root = JsonParser.parseString(source).getAsJsonObject();
        JsonArray samplers = root.has("samplers") && root.get("samplers").isJsonArray()
                ? root.getAsJsonArray("samplers") : new JsonArray();
        JsonArray uniforms = root.has("uniforms") && root.get("uniforms").isJsonArray()
                ? root.getAsJsonArray("uniforms") : new JsonArray();

        root.add("samplers", samplers);
        root.add("uniforms", uniforms);
        addSampler(samplers, "GoboData");
        addSampler(samplers, "GoboTileIndices");
        addSampler(samplers, "GoboAtlas");
        addUniform(uniforms, "GoboProjectorCount", 1);
        addUniform(uniforms, "GoboTileSize", 1);
        addUniform(uniforms, "GoboTilesX", 1);
        addUniform(uniforms, "GoboTilesY", 1);
        addUniform(uniforms, "GoboMaxPerTile", 1);
        addUniform(uniforms, "GoboBrightness", 1);
        addUniform(uniforms, "GoboScreenSize", 2);
        return GSON.toJson(root);
    }

    private static void addSampler(JsonArray samplers, String name) {
        for (JsonElement element : samplers) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("name") && name.equals(object.get("name").getAsString())) {
                    return;
                }
            }
        }

        JsonObject sampler = new JsonObject();
        sampler.addProperty("name", name);
        samplers.add(sampler);
    }

    private static void addUniform(JsonArray uniforms, String name, int count) {
        for (JsonElement element : uniforms) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("name") && name.equals(object.get("name").getAsString())) {
                    return;
                }
            }
        }

        JsonObject uniform = new JsonObject();
        uniform.addProperty("name", name);
        uniform.addProperty("type", "float");
        uniform.addProperty("count", count);
        JsonArray values = new JsonArray();
        for (int i = 0; i < count; i++) {
            values.add(0.0f);
        }
        uniform.add("values", values);
        uniforms.add(uniform);
    }

    private static String patchVertex(String source) {
        if (source.contains("GoboViewPosition")) {
            return source;
        }

        if (!source.contains("in vec3 Normal;")
                || !source.contains("in vec4 Color;")
                || !source.contains("uniform mat4 ModelViewMat;")) {
            throw new IllegalStateException("Required vertex attributes are missing");
        }

        String normalMarker = "out vec4 normal;";
        if (!source.contains(normalMarker)) {
            throw new IllegalStateException("Missing normal output");
        }

        source = source.replace(
                normalMarker,
                normalMarker + """

                out vec3 GoboViewPosition;
                out vec3 GoboViewNormal;
                out vec4 GoboVertexTint;
                """
        );

        Matcher matcher = GL_POSITION_PATTERN.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not locate entity or terrain position transform");
        }

        String positionVariable = matcher.group(1);
        String injected = matcher.group() + """

                    GoboViewPosition = (ModelViewMat * vec4(%s, 1.0)).xyz;
                    GoboViewNormal = normalize(mat3(ModelViewMat) * Normal);
                    GoboVertexTint = Color;
                """.formatted(positionVariable);

        return matcher.replaceFirst(Matcher.quoteReplacement(injected));
    }

    private static String patchFragment(String source) {
        if (source.contains("goboApplyTileLighting")) {
            return source;
        }

        if (!source.contains("in vec4 normal;")
                || !source.contains("uniform sampler2D Sampler0;")
                || !source.contains("out vec4 fragColor;")
                || !source.contains("texCoord0")
                || !source.contains("ColorModulator")) {
            throw new IllegalStateException("Required fragment shader declarations are missing");
        }

        source = source.replace(
                "out vec4 fragColor;",
                "out vec4 fragColor;\n" + buildFragmentDeclarations()
        );

        Matcher matcher = FOG_OUTPUT_PATTERN.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not locate final fragment output");
        }

        StringBuilder lighting = new StringBuilder();
        lighting.append("""

                    if (GoboProjectorCount > 0.5) {
                        vec3 goboAlbedo = texture(Sampler0, texCoord0).rgb
                                * GoboVertexTint.rgb * ColorModulator.rgb;
                """);

        if (source.contains("in vec4 overlayColor;")) {
            lighting.append("""

                        goboAlbedo = mix(overlayColor.rgb, goboAlbedo, overlayColor.a);
                    """);
        }

        lighting.append("""

                        color.rgb += goboApplyTileLighting(
                                GoboViewPosition,
                                GoboViewNormal,
                                goboAlbedo,
                                GoboVertexTint.a
                        );
                    }
                """);

        String replacement = lighting + "\n    " + matcher.group();
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private static String buildFragmentDeclarations() {
        return """

            in vec3 GoboViewPosition;
            in vec3 GoboViewNormal;
            in vec4 GoboVertexTint;

            uniform sampler2D GoboData;
            uniform sampler2D GoboTileIndices;
            uniform sampler2DArray GoboAtlas;

            uniform float GoboProjectorCount;
            uniform float GoboTileSize;
            uniform float GoboTilesX;
            uniform float GoboTilesY;
            uniform float GoboMaxPerTile;
            uniform float GoboBrightness;
            uniform vec2 GoboScreenSize;

            float goboLuminance(vec3 color) {
                return dot(color, vec3(0.299, 0.587, 0.114));
            }

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
                return clamp(
                        goboLuminance(textureLod(GoboAtlas, vec3(uv, layer), lod).rgb),
                        0.0,
                        1.0
                );
            }

            vec3 goboProjectorContribution(
                    int projectorIndex,
                    vec3 viewPosition,
                    vec3 surfaceNormal,
                    bool surfaceNormalValid,
                    vec3 albedo,
                    float vertexAlpha
            ) {
                vec4 p0 = texelFetch(GoboData, ivec2(0, projectorIndex), 0);
                vec4 p1 = texelFetch(GoboData, ivec2(1, projectorIndex), 0);
                vec4 p2 = texelFetch(GoboData, ivec2(2, projectorIndex), 0);
                vec4 p3 = texelFetch(GoboData, ivec2(3, projectorIndex), 0);

                float intensity = p3.w;
                float maxDistance = p2.w;
                if (intensity <= 0.0001 || maxDistance <= 0.0001) {
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
                float inverseDistance = inversesqrt(max(distanceSquared, 0.00000001));
                vec3 toLight = -fromLight * inverseDistance;
                float ndotl = surfaceNormalValid ? abs(dot(surfaceNormal, toLight)) : 1.0;
                if (surfaceNormalValid && ndotl <= 0.001) {
                    return vec3(0.0);
                }

                if (hasOcclusion && distanceSquared > 0.00000001) {
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

                float alphaA = 0.0;
                float alphaB = 0.0;
                vec3 chromaticA = vec3(0.0);
                vec3 chromaticB = vec3(0.0);

                if (uvA.x >= 0.0 && uvA.x <= 1.0 && uvA.y >= 0.0 && uvA.y <= 1.0) {
                    vec4 sampleA = textureLod(GoboAtlas, vec3(uvA, p5.x), blurLod);
                    float peakA = max(sampleA.r, max(sampleA.g, sampleA.b));
                    alphaA = clamp(sampleA.a * peakA, 0.0, 1.0);
                    if (peakA > 0.00001) {
                        chromaticA = sampleA.rgb / peakA;
                    }
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
                        vec4 sampleB = textureLod(GoboAtlas, vec3(uvB, p5.y), blurLod);
                        float peakB = max(sampleB.r, max(sampleB.g, sampleB.b));
                        alphaB = clamp(sampleB.a * peakB, 0.0, 1.0);
                        if (peakB > 0.00001) {
                            chromaticB = sampleB.rgb / peakB;
                        }
                    }
                }

                float goboAlpha = alphaA + alphaB;
                if (goboAlpha <= 0.001) {
                    return vec3(0.0);
                }
                vec3 goboColor = (
                        chromaticA * alphaA + chromaticB * alphaB
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
                float distanceFade = 1.0 - smoothstep(
                        maxDistance * 0.30, maxDistance, forwardDistance
                );
                float physicalFalloff = 1.0 / (
                        1.0 + forwardDistance * forwardDistance * 0.0015
                );
                float finalFactor = clamp(
                        goboAlpha * edgeFade * intensity * GoboBrightness
                        * vertexAlpha * distanceFade * physicalFalloff * angleAttenuation,
                        0.0,
                        1.0
                );
                if (finalFactor <= 0.001) {
                    return vec3(0.0);
                }

                vec3 effectiveAlbedo = max(albedo, vec3(0.012));
                vec3 lightSpectrum = max(p4.rgb, vec3(0.0)) * max(goboColor, vec3(0.0));
                return effectiveAlbedo * lightSpectrum * beamProfile * 1.85 * finalFactor;
            }

            vec3 goboApplyTileLighting(
                    vec3 viewPosition,
                    vec3 viewNormal,
                    vec3 albedo,
                    float vertexAlpha
            ) {
                if (GoboProjectorCount <= 0.0 || GoboTileSize <= 0.0
                        || GoboTilesX <= 0.0 || GoboTilesY <= 0.0
                        || GoboMaxPerTile <= 0.0) {
                    return vec3(0.0);
                }

                ivec2 pixel = clamp(
                        ivec2(gl_FragCoord.xy),
                        ivec2(0),
                        ivec2(max(int(GoboScreenSize.x) - 1, 0),
                                max(int(GoboScreenSize.y) - 1, 0))
                );
                ivec2 tile = pixel / int(GoboTileSize);
                tile = clamp(tile, ivec2(0), ivec2(int(GoboTilesX) - 1, int(GoboTilesY) - 1));
                int tileIndex = tile.y * int(GoboTilesX) + tile.x;

                vec4 firstIndices = texelFetch(GoboTileIndices, ivec2(0, tileIndex), 0);
                if (firstIndices.x < 0.0) {
                    return vec3(0.0);
                }

                float normalLengthSquared = dot(viewNormal, viewNormal);
                bool normalValid = normalLengthSquared > 0.00000001;
                vec3 surfaceNormal = normalValid
                        ? viewNormal * inversesqrt(normalLengthSquared)
                        : vec3(0.0);

                int groupCount = (int(GoboMaxPerTile + 0.5) + 3) / 4;
                vec3 totalLighting = vec3(0.0);

                for (int group = 0; group < groupCount; group++) {
                    vec4 packedIndices = group == 0
                            ? firstIndices
                            : texelFetch(GoboTileIndices, ivec2(group, tileIndex), 0);

                    float encodedIndex = packedIndices.x;
                    if (encodedIndex < 0.0) break;
                    int projectorIndex = int(encodedIndex + 0.5);
                    if (projectorIndex < int(GoboProjectorCount + 0.5)) {
                        totalLighting += goboProjectorContribution(
                                projectorIndex, viewPosition, surfaceNormal,
                                normalValid, albedo, vertexAlpha
                        );
                    }

                    encodedIndex = packedIndices.y;
                    if (encodedIndex < 0.0) break;
                    projectorIndex = int(encodedIndex + 0.5);
                    if (projectorIndex < int(GoboProjectorCount + 0.5)) {
                        totalLighting += goboProjectorContribution(
                                projectorIndex, viewPosition, surfaceNormal,
                                normalValid, albedo, vertexAlpha
                        );
                    }

                    encodedIndex = packedIndices.z;
                    if (encodedIndex < 0.0) break;
                    projectorIndex = int(encodedIndex + 0.5);
                    if (projectorIndex < int(GoboProjectorCount + 0.5)) {
                        totalLighting += goboProjectorContribution(
                                projectorIndex, viewPosition, surfaceNormal,
                                normalValid, albedo, vertexAlpha
                        );
                    }

                    encodedIndex = packedIndices.w;
                    if (encodedIndex < 0.0) break;
                    projectorIndex = int(encodedIndex + 0.5);
                    if (projectorIndex < int(GoboProjectorCount + 0.5)) {
                        totalLighting += goboProjectorContribution(
                                projectorIndex, viewPosition, surfaceNormal,
                                normalValid, albedo, vertexAlpha
                        );
                    }
                }

                return totalLighting;
            }
            """;
    }

}
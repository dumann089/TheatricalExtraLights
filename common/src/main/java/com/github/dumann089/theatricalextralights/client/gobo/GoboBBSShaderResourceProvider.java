package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public final class GoboBBSShaderResourceProvider implements ResourceProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, ResourceLocation> BBS_RESOURCES = Map.of(
            "json", new ResourceLocation("bbs", "shaders/core/model.json"),
            "vsh", new ResourceLocation("bbs", "shaders/core/model.vsh"),
            "fsh", new ResourceLocation("bbs", "shaders/core/model.fsh")
    );

    private static final Map<String, ResourceLocation> PATCH_RESOURCES = Map.of(
            "json", new ResourceLocation("minecraft", "shaders/core/rendertype_entity_cutout.json"),
            "vsh", new ResourceLocation("minecraft", "shaders/core/rendertype_entity_cutout.vsh"),
            "fsh", new ResourceLocation("minecraft", "shaders/core/rendertype_entity_cutout.fsh")
    );

    private static final Map<ResourceLocation, ResourceLocation> RESOURCE_ALIASES = Map.of(
            PATCH_RESOURCES.get("json"), BBS_RESOURCES.get("json"),
            PATCH_RESOURCES.get("vsh"), BBS_RESOURCES.get("vsh"),
            PATCH_RESOURCES.get("fsh"), BBS_RESOURCES.get("fsh")
    );

    private static final ResourceLocation BBS_SHIMMER_INCLUDE =
            new ResourceLocation("bbs", "shaders/include/shimmer.glsl");

    private static final ResourceLocation MINECRAFT_SHIMMER_INCLUDE =
            new ResourceLocation("minecraft", "shaders/include/shimmer.glsl");

    private final ResourceProvider parent;
    private final GoboShaderResourceProvider shaderPatcher;

    public GoboBBSShaderResourceProvider(ResourceProvider parent) {
        this.parent = parent;

        ResourceProvider aliasProvider = new ResourceProvider() {
            @Override
            public Optional<Resource> getResource(ResourceLocation location) {
                ResourceLocation original = RESOURCE_ALIASES.get(location);

                if (original != null) {
                    return GoboBBSShaderResourceProvider.this.parent.getResource(original);
                }

                return GoboBBSShaderResourceProvider.this.parent.getResource(location);
            }
        };

        this.shaderPatcher = new GoboShaderResourceProvider(aliasProvider);

        ShimmerBBSCompat.initialize();
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        if (BBS_SHIMMER_INCLUDE.equals(location) && ShimmerBBSCompat.isAvailable()) {
            Optional<Resource> include = parent.getResource(MINECRAFT_SHIMMER_INCLUDE);

            if (include.isPresent()) {
                return include;
            }
        }

        String extension = getBbsExtension(location);

        if (extension == null) {
            return parent.getResource(location);
        }

        ResourceLocation alias = PATCH_RESOURCES.get(extension);
        Optional<Resource> patched = shaderPatcher.getResource(alias);

        if (patched.isEmpty()) {
            return parent.getResource(location);
        }

        if (!extension.equals("vsh")) {
            return patched;
        }

        return patchBbsVertex(patched.get());
    }

    private static String getBbsExtension(ResourceLocation location) {
        if (!location.getNamespace().equals("bbs")) {
            return null;
        }

        return switch (location.getPath()) {
            case "shaders/core/model.json" -> "json";
            case "shaders/core/model.vsh" -> "vsh";
            case "shaders/core/model.fsh" -> "fsh";
            default -> null;
        };
    }

    private static Optional<Resource> patchBbsVertex(Resource resource) {
        try {
            String source;

            try (InputStream input = resource.open()) {
                source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            String normalOriginal =
                    "GoboViewNormal = normalize(mat3(ModelViewMat) * Normal);";

            String normalCorrected =
                    "GoboViewNormal = normalize(NormalMat * Normal);";

            if (source.contains("uniform mat3 NormalMat;")
                    && source.contains(normalOriginal)) {
                source = source.replace(normalOriginal, normalCorrected);
            }

            source = ShimmerBBSCompat.patchVertex(source);

            byte[] output = source.getBytes(StandardCharsets.UTF_8);

            return Optional.of(new Resource(
                    resource.source(),
                    () -> new ByteArrayInputStream(output),
                    resource::metadata
            ));
        } catch (Exception exception) {
            LOGGER.error(
                    "[TheatricalExtraLights/GoboBBS] Could not patch BBS vertex shader",
                    exception
            );

            return Optional.of(resource);
        }
    }
}
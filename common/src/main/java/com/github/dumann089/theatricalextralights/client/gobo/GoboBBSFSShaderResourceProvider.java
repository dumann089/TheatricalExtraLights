package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class GoboBBSFSShaderResourceProvider implements ResourceProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BBS_FACTORY_CLASS =
            "mchorse.bbs_mod.client.BBSShaders$ProxyResourceFactory";

    private static final String MODEL_JSON = "shaders/core/model.json";
    private static final String MODEL_VSH = "shaders/core/model.vsh";
    private static final String MODEL_FSH = "shaders/core/model.fsh";

    private final ResourceProvider parent;

    public GoboBBSFSShaderResourceProvider(ResourceProvider parent) {
        this.parent = parent;
    }

    public static boolean isBbsFsFactory(ResourceProvider provider) {
        return provider != null
                && provider.getClass().getName().equals(BBS_FACTORY_CLASS);
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        Optional<Resource> original = parent.getResource(location);

        if (original.isEmpty()) {
            return original;
        }

        String path = location.getPath();
        String extension;

        if (MODEL_JSON.equals(path)) {
            extension = "json";
        } else if (MODEL_VSH.equals(path)) {
            extension = "vsh";
        } else if (MODEL_FSH.equals(path)) {
            extension = "fsh";
        } else {
            return original;
        }

        try {
            Resource patched = patchGobo(original.get(), extension);

            if (extension.equals("vsh")) {
                patched = patchVertexExtras(patched);
            }

            return Optional.of(patched);
        } catch (Exception exception) {
            LOGGER.error(
                    "[TheatricalExtraLights/BBSFS] Could not patch BBS model shader {}",
                    location,
                    exception
            );

            return original;
        }
    }

    private static Resource patchGobo(Resource original, String extension) {
        ResourceLocation alias = new ResourceLocation(
                "minecraft",
                "shaders/core/rendertype_entity_cutout." + extension
        );

        ResourceProvider aliasProvider = requested ->
                alias.equals(requested)
                        ? Optional.of(original)
                        : Optional.empty();

        GoboShaderResourceProvider patcher =
                new GoboShaderResourceProvider(aliasProvider);

        return patcher.getResource(alias).orElse(original);
    }

    private static Resource patchVertexExtras(Resource resource) throws Exception {
        String source;

        try (InputStream input = resource.open()) {
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        source = source.replace(
                "GoboViewNormal = normalize(mat3(ModelViewMat) * Normal);",
                "GoboViewNormal = normalize(NormalMat * Normal);"
        );

        source = ShimmerBBSFSCompat.patchVertexShader(source);

        byte[] output = source.getBytes(StandardCharsets.UTF_8);

        return new Resource(
                resource.source(),
                () -> new ByteArrayInputStream(output),
                resource::metadata
        );
    }
}
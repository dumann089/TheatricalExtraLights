package com.github.dumann089.theatricalextralights.compat.shimmer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

/**
 * During Forge's first shader reload, mod resource packs are not mounted yet.
 * Shimmer injects {@code #moj_import <shimmer.glsl>} into vanilla shaders;
 * this provider only supplies bundled fallbacks when the parent pack cannot resolve them.
 */
public final class ClasspathGlslResourceProvider implements ResourceProvider {
    private static final ResourceLocation SHIMMER_GLSL =
            new ResourceLocation("minecraft", "shaders/include/shimmer.glsl");
    private static final ResourceLocation SHIMMER_FOG_GLSL =
            new ResourceLocation("minecraft", "shaders/include/shimmer_fog.glsl");

    private final ResourceProvider parent;

    public ClasspathGlslResourceProvider(ResourceProvider parent) {
        this.parent = parent;
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        Optional<Resource> found = parent.getResource(location);
        if (found.isPresent()) {
            return found;
        }
        if (isBundledInclude(location)) {
            return classpathResource(location);
        }
        return Optional.empty();
    }

    private static boolean isBundledInclude(ResourceLocation location) {
        return SHIMMER_GLSL.equals(location) || SHIMMER_FOG_GLSL.equals(location);
    }

    private static Optional<Resource> classpathResource(ResourceLocation location) {
        String relative = "assets/" + location.getNamespace() + "/" + location.getPath();
        InputStream stream = openBundledStream(relative);
        if (stream == null) {
            return Optional.empty();
        }
        IoSupplier<InputStream> supplier = () -> {
            InputStream s = openBundledStream(relative);
            if (s == null) {
                throw new java.io.FileNotFoundException(relative);
            }
            return s;
        };
        return Optional.of(new Resource(FallbackGlslPackResources.INSTANCE, supplier));
    }

    private static InputStream openBundledStream(String relative) {
        String withSlash = "/" + relative;
        ClassLoader[] loaders = new ClassLoader[] {
                ClasspathGlslResourceProvider.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            URL url = loader.getResource(relative);
            if (url == null) {
                url = loader.getResource(withSlash);
            }
            if (url != null) {
                try {
                    return url.openStream();
                } catch (java.io.IOException ignored) {
                }
            }
        }
        InputStream fromClass = ClasspathGlslResourceProvider.class.getResourceAsStream(withSlash);
        if (fromClass != null) {
            return fromClass;
        }
        return ClasspathGlslResourceProvider.class.getResourceAsStream(relative);
    }
}

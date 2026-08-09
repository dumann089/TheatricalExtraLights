package com.github.dumann089.theatricalextralights.compat.shimmer;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.Set;

final class FallbackGlslPackResources implements PackResources {
    static final FallbackGlslPackResources INSTANCE = new FallbackGlslPackResources();

    private FallbackGlslPackResources() {
    }

    @Override
    public String packId() {
        return "theatricalextralights:shimmer_glsl_fallback";
    }

    @Override
    public void close() {
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of("minecraft");
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return null;
    }
}

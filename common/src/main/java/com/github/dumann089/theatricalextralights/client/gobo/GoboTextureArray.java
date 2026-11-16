package com.github.dumann089.theatricalextralights.client.gobo;

import com.github.dumann089.theatricalextralights.client.CustomGoboLoader;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GoboTextureArray {
    public static final int SIZE = 256;
    public static final int MAX_LAYERS = 256;

    private static final Map<ResourceLocation, Integer> LAYERS = new HashMap<>();

    private static int textureId;
    private static int nextLayer = 1;
    private static boolean mipmapsDirty;
    private static boolean capacityWarning;

    private GoboTextureArray() {
    }

    public static int getTextureId() {
        return textureId;
    }

    public static int getLayer(ResourceLocation location) {
        if (location == null) {
            return 0;
        }

        RenderSystem.assertOnRenderThreadOrInit();

        Integer cached = LAYERS.get(location);
        if (cached != null) {
            return cached;
        }

        if (nextLayer >= MAX_LAYERS) {
            if (!capacityWarning) {
                capacityWarning = true;
                System.err.println("[TheatricalExtraLights/Gobo] Gobo atlas capacity reached.");
            }
            return 0;
        }

        ensureCreated();

        NativeImage customImage = CustomGoboLoader.isCustomGobo(location)
                ? CustomGoboLoader.getImage(location)
                : null;

        if (customImage != null) {
            return upload(location, customImage);
        }

        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(location);

        if (resource.isEmpty()) {
            System.err.println("[TheatricalExtraLights/Gobo] Missing texture: " + location);
            return 0;
        }

        try (InputStream stream = resource.get().open();
             NativeImage image = NativeImage.read(stream)) {
            return upload(location, image);
        } catch (Exception exception) {
            System.err.println("[TheatricalExtraLights/Gobo] Could not load texture: " + location);
            exception.printStackTrace();
            return 0;
        }
    }

    public static int finishUploads() {
        RenderSystem.assertOnRenderThreadOrInit();
        ensureCreated();

        if (mipmapsDirty) {
            int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);

            int previousBinding = GL11.glGetInteger(GL30.GL_TEXTURE_BINDING_2D_ARRAY);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureId);
            GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, previousBinding);

            RenderSystem.activeTexture(previousActive);
            mipmapsDirty = false;
        }

        return textureId;
    }

    private static int upload(ResourceLocation location, NativeImage image) {
        ensureCreated();

        int layer = nextLayer++;
        ByteBuffer pixels = MemoryUtil.memAlloc(SIZE * SIZE * 4);

        try {
            int sourceWidth = image.getWidth();
            int sourceHeight = image.getHeight();

            for (int y = 0; y < SIZE; y++) {
                int sourceY = Math.min(sourceHeight - 1, y * sourceHeight / SIZE);

                for (int x = 0; x < SIZE; x++) {
                    int sourceX = Math.min(sourceWidth - 1, x * sourceWidth / SIZE);
                    int pixel = image.getPixelRGBA(sourceX, sourceY);

                    pixels.put((byte) (pixel & 0xFF));
                    pixels.put((byte) ((pixel >>> 8) & 0xFF));
                    pixels.put((byte) ((pixel >>> 16) & 0xFF));
                    pixels.put((byte) ((pixel >>> 24) & 0xFF));
                }
            }

            pixels.flip();

            int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);

            int previousBinding = GL11.glGetInteger(GL30.GL_TEXTURE_BINDING_2D_ARRAY);

            try (GoboPixelUnpackState unpackState = new GoboPixelUnpackState()) {
                GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureId);

                GL30.glTexSubImage3D(
                        GL30.GL_TEXTURE_2D_ARRAY,
                        0,
                        0,
                        0,
                        layer,
                        SIZE,
                        SIZE,
                        1,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        pixels
                );

                int error = GL11.glGetError();

                if (error != GL11.GL_NO_ERROR) {
                    throw new IllegalStateException(
                            "[TheatricalExtraLights/Gobo] Atlas upload error: 0x"
                                    + Integer.toHexString(error)
                    );
                }
            } finally {
                GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, previousBinding);
                RenderSystem.activeTexture(previousActive);
            }

            LAYERS.put(location, layer);
            mipmapsDirty = true;
            return layer;

        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private static void ensureCreated() {
        if (textureId != 0) {
            return;
        }

        int previousActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        int previousBinding = GL11.glGetInteger(GL30.GL_TEXTURE_BINDING_2D_ARRAY);

        try (GoboPixelUnpackState unpackState = new GoboPixelUnpackState()) {
            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureId);

            GL30.glTexImage3D(
                    GL30.GL_TEXTURE_2D_ARRAY,
                    0,
                    GL11.GL_RGBA8,
                    SIZE,
                    SIZE,
                    MAX_LAYERS,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null
            );

            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);

            ByteBuffer emptyLayer = MemoryUtil.memCalloc(SIZE * SIZE * 4);

            try {
                GL30.glTexSubImage3D(
                        GL30.GL_TEXTURE_2D_ARRAY,
                        0,
                        0,
                        0,
                        0,
                        SIZE,
                        SIZE,
                        1,
                        GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE,
                        emptyLayer
                );
            } finally {
                MemoryUtil.memFree(emptyLayer);
            }

            int error = GL11.glGetError();

            if (error != GL11.GL_NO_ERROR) {
                throw new IllegalStateException(
                        "[TheatricalExtraLights/Gobo] Atlas allocation error: 0x"
                                + Integer.toHexString(error)
                );
            }

        } finally {
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, previousBinding);
            RenderSystem.activeTexture(previousActive);
        }
    }
}
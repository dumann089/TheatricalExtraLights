package com.github.dumann089.theatricalextralights.client.gobo;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GobosAtlasManager implements ResourceManagerReloadListener {

    public static final ResourceLocation SHADER_ATLAS_TARGET = new ResourceLocation("theatricalextralights", "textures/gobo_atlas.png");
    private static final Map<ResourceLocation, Integer> GOBO_TO_INDEX = new HashMap<>();

    // CORREGIDO: Resolución exacta de tus gobos
    private static final int GOBO_RESOLUTION = 128;

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        GOBO_TO_INDEX.clear();
        Map<ResourceLocation, Resource> allGoboFiles = manager.listResources("textures/gobos", path -> path.getPath().endsWith(".png"));

        List<NativeImage> imagesToUpload = new ArrayList<>();
        int currentIndex = 0;

        for (Map.Entry<ResourceLocation, Resource> entry : allGoboFiles.entrySet()) {
            ResourceLocation goboLoc = entry.getKey();
            if (goboLoc.getPath().endsWith("open.png")) continue;

            try (InputStream is = entry.getValue().open()) {
                NativeImage singleGobo = NativeImage.read(is);
                imagesToUpload.add(singleGobo);
                GOBO_TO_INDEX.put(goboLoc, currentIndex);
                currentIndex++;
            } catch (Exception e) {
                System.err.println("[TEL] Error leyendo el gobo: " + goboLoc);
            }
        }

        final int totalGobos = Math.max(1, currentIndex);

        Minecraft.getInstance().execute(() -> {
            AbstractTexture arrayTexture = new AbstractTexture() {
                @Override
                public void load(ResourceManager resourceManager) {
                    int texId = this.getId();

                    GL30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, texId);

                    GL30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                    // Reserva 3D con 128x128
                    GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA8, GOBO_RESOLUTION, GOBO_RESOLUTION, totalGobos, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);

                    for (int i = 0; i < imagesToUpload.size(); i++) {
                        NativeImage img = imagesToUpload.get(i);

                        // Buffer nativo para 128x128
                        java.nio.ByteBuffer buffer = org.lwjgl.system.MemoryUtil.memAlloc(GOBO_RESOLUTION * GOBO_RESOLUTION * 4);

                        for (int y = 0; y < GOBO_RESOLUTION; y++) {
                            for (int x = 0; x < GOBO_RESOLUTION; x++) {
                                if (x < img.getWidth() && y < img.getHeight()) {
                                    int color = img.getPixelRGBA(x, y);
                                    buffer.put((byte) (color & 0xFF));         // R
                                    buffer.put((byte) ((color >> 8) & 0xFF));  // G
                                    buffer.put((byte) ((color >> 16) & 0xFF)); // B
                                    buffer.put((byte) ((color >> 24) & 0xFF)); // A
                                } else {
                                    buffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                                }
                            }
                        }
                        buffer.flip();

                        // Subida a la GPU en formato 128x128
                        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,
                                0, 0, i,
                                GOBO_RESOLUTION, GOBO_RESOLUTION, 1,
                                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

                        org.lwjgl.system.MemoryUtil.memFree(buffer);
                        img.close();
                    }
                }
            };

            Minecraft.getInstance().getTextureManager().register(SHADER_ATLAS_TARGET, arrayTexture);
            System.out.println("[TEL] Texture Array 3D (128x128) inyectado. " + totalGobos + " gobos listos para IRLights.");
        });
    }
}
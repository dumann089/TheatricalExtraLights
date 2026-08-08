package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.net.ModNetworking;
import com.github.dumann089.theatricalextralights.util.GoboFileManager;
import com.mojang.blaze3d.platform.NativeImage;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomGoboLoader {
    private static final Map<String, ResourceLocation> LOADED_GOBOS = new HashMap<>();
    private static final Set<String> REQUESTED_GOBOS = new HashSet<>();

    public static ResourceLocation getOrCreateCustomGobo(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        // 1. Si ya está en memoria RAM, la devolvemos
        if (LOADED_GOBOS.containsKey(fileName)) {
            return LOADED_GOBOS.get(fileName);
        }

        // 2. Si el Admin/Dueño tiene la imagen en config/gobos, la cargamos (Esto arregla las miniaturas)
        File localFile = new File(GoboFileManager.getLocalConfigDir(), fileName);
        if (localFile.exists()) {
            try (InputStream stream = new FileInputStream(localFile)) {
                return registerImageToRAM(fileName, NativeImage.read(stream));
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 3. Si no la tiene (es un jugador normal), se la pide al Servidor
        if (!REQUESTED_GOBOS.contains(fileName)) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(fileName);
            NetworkManager.sendToServer(ModNetworking.REQUEST_GOBO, buf);
            REQUESTED_GOBOS.add(fileName);
        }

        return null;
    }

    // 4. El jugador recibe la imagen del servidor y va a la RAM (Sin guardarse en disco)
    public static void receiveBytesFromServer(String fileName, byte[] data) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            registerImageToRAM(fileName, NativeImage.read(stream));
        } catch (Exception e) {
            e.printStackTrace();
            LOADED_GOBOS.put(fileName, null);
        }
    }

    private static ResourceLocation registerImageToRAM(String fileName, NativeImage img) {
        DynamicTexture tex = new DynamicTexture(img);
        String cleanName = fileName.toLowerCase().replace(".png", "").replaceAll("[^a-z0-9_.-]", "_");
        ResourceLocation rl = new ResourceLocation("theatricalextralights", "custom_gobo_" + cleanName);
        Minecraft.getInstance().getTextureManager().register(rl, tex);
        LOADED_GOBOS.put(fileName, rl);
        return rl;
    }

    public static void clearCache() {
        LOADED_GOBOS.clear();
        REQUESTED_GOBOS.clear();
    }
}
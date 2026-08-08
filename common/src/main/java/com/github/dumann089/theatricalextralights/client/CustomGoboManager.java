package com.github.dumann089.theatricalextralights.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomGoboManager {
    // Carpeta donde estarán los gobos: .minecraft/config/gobos
    private static final File GOBOS_DIR = new File(Minecraft.getInstance().gameDirectory, "config/gobos");

    // Caché para no recargar la imagen 60 veces por segundo
    private static final Map<String, ResourceLocation> LOADED_GOBOS = new HashMap<>();

    public static ResourceLocation getOrCreateCustomGobo(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        // Si ya lo cargamos en memoria, devolvemos la textura cacheada
        if (LOADED_GOBOS.containsKey(fileName)) {
            return LOADED_GOBOS.get(fileName);
        }

        // Asegurar que la carpeta existe
        if (!GOBOS_DIR.exists()) {
            GOBOS_DIR.mkdirs();
        }

        File imageFile = new File(GOBOS_DIR, fileName);
        if (!imageFile.exists()) {
            System.err.println("Gobo no encontrado en config/gobos: " + fileName);
            LOADED_GOBOS.put(fileName, null); // Marcar como nulo para no intentar cargarlo de nuevo
            return null;
        }

        try (InputStream stream = new FileInputStream(imageFile)) {
            // Leer la imagen PNG
            NativeImage nativeImage = NativeImage.read(stream);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

            // Crear un ID único para Minecraft y registrarlo
            ResourceLocation rl = new ResourceLocation("theatricalextralights", "custom_gobo_" + fileName.toLowerCase().replace(".png", ""));
            Minecraft.getInstance().getTextureManager().register(rl, dynamicTexture);

            LOADED_GOBOS.put(fileName, rl);
            return rl;
        } catch (Exception e) {
            System.err.println("Error al cargar el gobo custom: " + fileName);
            e.printStackTrace();
            LOADED_GOBOS.put(fileName, null);
            return null;
        }
    }
}
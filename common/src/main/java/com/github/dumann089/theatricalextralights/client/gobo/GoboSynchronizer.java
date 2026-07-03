package com.github.dumann089.theatricalextralights.client.gobo;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class GoboSynchronizer {

    public static void sync() {
        try {
            Path targetDir = FabricLoader.getInstance().getConfigDir().resolve("irl-redactor").resolve("cookies");
            if (!Files.exists(targetDir)) Files.createDirectories(targetDir);

            // 1. Obtenemos la lista EXACTA de GoboLibrary
            List<ResourceLocation> allGobos = GoboLibrary.getAllUniqueGobos();

            // 2. Copiamos con un prefijo numérico para forzar el orden alfabético de IRLights
            for (int i = 0; i < allGobos.size(); i++) {
                ResourceLocation loc = allGobos.get(i);

                // Creamos un nombre estilo "000_generic_1_open.png"
                String indexPrefix = String.format("%03d", i);
                String fileName = indexPrefix + "_" + loc.getPath().replace("/", "_");
                Path targetFile = targetDir.resolve(fileName);

                // Copiamos desde el InputStream del Resource del juego
                try (InputStream is = Minecraft.getInstance().getResourceManager().getResource(loc).get().open()) {
                    Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("[TEL] Sincronización forzada con orden de índice completada.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
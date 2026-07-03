package com.github.dumann089.theatricalextralights.client.gobo;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class GlobalGoboRegistry {

    private static final Map<String, Integer> NAME_TO_ID = new HashMap<>();

    public static void initializeRegistry() {
        NAME_TO_ID.clear();
        Path cookieDir = FabricLoader.getInstance().getConfigDir().resolve("irl-redactor").resolve("cookies");

        if (!Files.exists(cookieDir)) return;

        try (Stream<Path> paths = Files.list(cookieDir)) {
            paths.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                String fileName = p.getFileName().toString().replace(".png", "");

                // Formato: 006_textures_gobos_...
                String[] parts = fileName.split("_", 2);
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        // Guardamos la clave SIN .png
                        NAME_TO_ID.put(parts[1], id);
                        System.out.println("[TEL-MATCH] Mapeado: " + parts[1] + " a ID: " + id);
                    } catch (NumberFormatException e) {
                        System.err.println("[TEL-ERROR] Prefijo no numérico: " + fileName);
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static int getCookieLayer(ResourceLocation goboTex) {
        if (goboTex.getPath().endsWith("open.png")) return -1;

        // Convertimos la ruta a la misma clave que guardamos arriba
        // 1. Quitamos namespace
        // 2. Reemplazamos / por _
        // 3. QUITAMOS EL .png (¡Este era el error!)
        String path = goboTex.getPath()
                .replace("textures/gobos/", "textures_gobos_") // Ajuste para que coincida con tu formato de archivo
                .replace(".png", "")
                .replace("/", "_");

        if (NAME_TO_ID.containsKey(path)) {
            int id = NAME_TO_ID.get(path);
            System.out.println("[TEL-OK] ¡MATCH! Enviando Layer " + id + " para " + path);
            return id;
        } else {
            System.err.println("[TEL-ERROR] No hay ID para: " + path);
            return -1;
        }
    }
}
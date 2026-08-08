package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GlobalGoboManager {
    private static final Map<String, Map<String, String>> MAPPINGS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String getCustomGobo(GoboLibrary library, int slot) {
        if (library == null) return null;
        Map<String, String> libMap = MAPPINGS.get(library.name());
        return libMap != null ? libMap.get(String.valueOf(slot)) : null;
    }

    public static void setMapping(String libraryName, int slot, String fileName) {
        MAPPINGS.computeIfAbsent(libraryName, k -> new HashMap<>()).put(String.valueOf(slot), fileName);
    }

    public static void load(MinecraftServer server) {
        MAPPINGS.clear();
        File dir = GoboFileManager.getServerWorldDir(server);
        if (dir != null) {
            File file = new File(dir, "gobo_mappings.json");
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
                    Map<String, Map<String, String>> data = GSON.fromJson(reader, type);
                    if (data != null) MAPPINGS.putAll(data);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    public static void save(MinecraftServer server) {
        File dir = GoboFileManager.getServerWorldDir(server);
        if (dir != null) {
            try (FileWriter writer = new FileWriter(new File(dir, "gobo_mappings.json"))) {
                GSON.toJson(MAPPINGS, writer);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static Map<String, Map<String, String>> getAllMappings() {
        return MAPPINGS;
    }
}
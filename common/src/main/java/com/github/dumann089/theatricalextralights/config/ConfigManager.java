package com.github.dumann089.theatricalextralights.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/theatricalextralights.json");

    private static final Map<String, List<Runnable>> listeners = new HashMap<>();
    private static TheatricalExtraLightsConfig instance = new TheatricalExtraLightsConfig();

    public static TheatricalExtraLightsConfig getInstance() {
        return instance;
    }

    public static void load() {
        File parent = FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                TheatricalExtraLightsConfig loaded = GSON.fromJson(reader, TheatricalExtraLightsConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reload() {
        load();
        listeners.values().forEach(list -> list.forEach(Runnable::run));
    }

    public static void resetToDefaults() {
        instance = new TheatricalExtraLightsConfig();
        save();
        listeners.values().forEach(list -> list.forEach(Runnable::run));
    }
    public static void registerListener(String fieldName, Runnable onUpdate) {
        listeners.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(onUpdate);
    }
    public static void notifyChange(String fieldName) {
        List<Runnable> fieldListeners = listeners.get(fieldName);
        if (fieldListeners != null) {
            fieldListeners.forEach(Runnable::run);
        }
    }
}
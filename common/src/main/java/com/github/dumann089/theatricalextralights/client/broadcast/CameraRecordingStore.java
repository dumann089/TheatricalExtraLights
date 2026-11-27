package com.github.dumann089.theatricalextralights.client.broadcast;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Gravações de câmera no disco: {@code config/theatricalextralights/camera_recordings/*.json}. */
public final class CameraRecordingStore {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private CameraRecordingStore() {
    }

    private static Path directory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("theatricalextralights").resolve("camera_recordings");
    }

    /** Gravações da variante pedida (a broadcast e a jib não misturam), em ordem alfabética. */
    public static List<CameraRecording> list(boolean jib) {
        List<CameraRecording> result = new ArrayList<>();
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!file.getFileName().toString().endsWith(".json")) {
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    CameraRecording recording = GSON.fromJson(reader, CameraRecording.class);
                    if (recording != null && recording.isValid() && recording.jib == jib) {
                        recording.fileName = file.getFileName().toString();
                        if (recording.name == null || recording.name.isBlank()) {
                            recording.name = recording.fileName.replace(".json", "");
                        }
                        result.add(recording);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not read camera recording {}", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not list camera recordings", e);
        }
        result.sort(Comparator.comparing(r -> r.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static void save(CameraRecording recording) throws IOException {
        Path dir = directory();
        Files.createDirectories(dir);
        String base = sanitize(recording.name);
        String fileName = base + ".json";
        int n = 2;
        while (Files.exists(dir.resolve(fileName))) {
            fileName = base + "-" + n++ + ".json";
        }
        recording.fileName = fileName;
        try (Writer writer = Files.newBufferedWriter(dir.resolve(fileName), StandardCharsets.UTF_8)) {
            GSON.toJson(recording, writer);
        }
    }

    public static void delete(CameraRecording recording) {
        if (recording.fileName == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory().resolve(recording.fileName));
        } catch (IOException e) {
            LOGGER.warn("Could not delete camera recording {}", recording.fileName, e);
        }
    }

    /** Nome informado (ou "Take N") sem repetir o de outra gravação da mesma variante. */
    public static String uniqueName(String requested, boolean jib) {
        List<CameraRecording> existing = list(jib);
        String base = requested == null || requested.isBlank() ? "Take" : requested.trim();
        boolean auto = requested == null || requested.isBlank();
        String candidate = auto ? base + " 1" : base;
        int n = 1;
        while (nameTaken(existing, candidate)) {
            n++;
            candidate = base + " " + n;
        }
        return candidate;
    }

    private static boolean nameTaken(List<CameraRecording> existing, String name) {
        for (CameraRecording recording : existing) {
            if (recording.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9 _.-]", "_");
        return clean.isBlank() ? "recording" : clean;
    }
}

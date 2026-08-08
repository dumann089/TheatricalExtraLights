package com.github.dumann089.theatricalextralights.util;

import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GoboFileManager {

    // --- CARPETA LOCAL (config/gobos) ---
    public static File getLocalConfigDir() {
        Path configPath = Platform.getConfigFolder();
        File dir = configPath.resolve("gobos").toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static byte[] readLocalGobo(String fileName) {
        if (fileName == null || fileName.isEmpty()) return new byte[0];
        File file = new File(getLocalConfigDir(), fileName);
        if (file.exists()) {
            try { return Files.readAllBytes(file.toPath()); } catch (IOException e) { e.printStackTrace(); }
        }
        return new byte[0];
    }

    // --- CARPETA DEL MUNDO (Aislada por servidor) ---
    public static File getServerWorldDir(MinecraftServer server) {
        if (server == null) return null;
        Path worldPath = server.getWorldPath(LevelResource.ROOT);
        File dir = worldPath.resolve("theatrical_gobos").toFile();
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void saveToServerWorld(MinecraftServer server, String fileName, byte[] data) {
        File dir = getServerWorldDir(server);
        if (dir == null || data == null || data.length == 0) return;
        try { Files.write(new File(dir, fileName).toPath(), data); } catch (IOException e) { e.printStackTrace(); }
    }

    public static byte[] readFromServerWorld(MinecraftServer server, String fileName) {
        File dir = getServerWorldDir(server);
        if (dir == null || fileName == null || fileName.isEmpty()) return new byte[0];
        File file = new File(dir, fileName);
        if (file.exists()) {
            try { return Files.readAllBytes(file.toPath()); } catch (IOException e) { e.printStackTrace(); }
        }
        return new byte[0];
    }
}
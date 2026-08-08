package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.util.GlobalGoboManager;
import com.github.dumann089.theatricalextralights.util.GoboFileManager;
import dev.architectury.networking.NetworkManager;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.Unpooled;
import com.google.gson.Gson;
import net.minecraft.server.MinecraftServer;
import dev.architectury.event.events.common.LifecycleEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModNetworking {
    public static final ResourceLocation UPLOAD_GOBO = new ResourceLocation("theatricalextralights", "upload_gobo");
    public static final ResourceLocation REQUEST_GOBO = new ResourceLocation("theatricalextralights", "request_gobo");
    public static final ResourceLocation SEND_GOBO = new ResourceLocation("theatricalextralights", "send_gobo");
    public static final ResourceLocation SYNC_MAPPINGS = new ResourceLocation("theatricalextralights", "sync_mappings");

    private static final Map<UUID, Map<String, byte[][]>> uploadBuffers = new HashMap<>();

    public static void register() {
        LifecycleEvent.SERVER_STARTED.register(server -> GlobalGoboManager.load(server));
        PlayerEvent.PLAYER_JOIN.register(player -> sendMappingsToPlayer((ServerPlayer) player));

        // USAMOS EnvExecutor. No hace comparaciones, así que no da error de "incomparable types"
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            com.github.dumann089.theatricalextralights.client.ClientEventsHandler.register();
        });

        // C2S: seguro registrarlo en común, corre en el server
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UPLOAD_GOBO, (buf, context) -> {
            String libraryName = buf.readUtf();
            int slot = buf.readInt();
            String fileName = buf.readUtf();
            int totalChunks = buf.readInt();
            int chunkIndex = buf.readInt();
            byte[] chunkData = buf.readByteArray();

            context.queue(() -> {
                MinecraftServer server = context.getPlayer().getServer();
                UUID playerId = context.getPlayer().getUUID();

                if (fileName.isEmpty()) {
                    Map<String, String> libMap = GlobalGoboManager.getAllMappings().get(libraryName);
                    if (libMap != null) libMap.remove(String.valueOf(slot));
                    GlobalGoboManager.save(server);
                    syncMappingsToAll(server);
                    return;
                }

                uploadBuffers.putIfAbsent(playerId, new HashMap<>());
                Map<String, byte[][]> playerBuffer = uploadBuffers.get(playerId);
                playerBuffer.putIfAbsent(fileName, new byte[totalChunks][]);
                playerBuffer.get(fileName)[chunkIndex] = chunkData;

                boolean complete = true;
                int totalSize = 0;
                for (byte[] c : playerBuffer.get(fileName)) { if (c == null) { complete = false; break; } totalSize += c.length; }

                if (complete) {
                    byte[] fullData = new byte[totalSize];
                    int offset = 0;
                    for (byte[] c : playerBuffer.get(fileName)) { System.arraycopy(c, 0, fullData, offset, c.length); offset += c.length; }

                    GoboFileManager.saveToServerWorld(server, fileName, fullData);
                    GlobalGoboManager.setMapping(libraryName, slot, fileName);
                    GlobalGoboManager.save(server);
                    syncMappingsToAll(server);
                    playerBuffer.remove(fileName);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_GOBO, (buf, context) -> {
            String fileName = buf.readUtf();
            context.queue(() -> {
                MinecraftServer server = context.getPlayer().getServer();
                byte[] data = GoboFileManager.readFromServerWorld(server, fileName);
                if (data.length > 0) {
                    int chunkSize = 20000;
                    int totalChunks = (int) Math.ceil((double) data.length / chunkSize);
                    for (int i = 0; i < totalChunks; i++) {
                        int start = i * chunkSize;
                        int length = Math.min(chunkSize, data.length - start);
                        byte[] chunk = new byte[length];
                        System.arraycopy(data, start, chunk, 0, length);
                        FriendlyByteBuf outBuf = new FriendlyByteBuf(Unpooled.buffer());
                        outBuf.writeUtf(fileName);
                        outBuf.writeInt(totalChunks);
                        outBuf.writeInt(i);
                        outBuf.writeByteArray(chunk);
                        NetworkManager.sendToPlayer((ServerPlayer) context.getPlayer(), SEND_GOBO, outBuf);
                    }
                }
            });
        });

        // S2C: SOLO se registran del lado cliente. Esto es lo que rompía el server.
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> registerClientReceivers());
    }

    private static void registerClientReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_MAPPINGS, (buf, context) -> {
            String jsonStr = buf.readUtf();
            context.queue(() -> {
                Gson gson = new Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Map<String, String>>>(){}.getType();
                Map<String, Map<String, String>> data = gson.fromJson(jsonStr, type);
                GlobalGoboManager.getAllMappings().clear();
                if (data != null) GlobalGoboManager.getAllMappings().putAll(data);
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SEND_GOBO, (buf, context) -> {
            String fileName = buf.readUtf();
            int totalChunks = buf.readInt();
            int chunkIndex = buf.readInt();
            byte[] chunkData = buf.readByteArray();
            context.queue(() -> {
                com.github.dumann089.theatricalextralights.client.CustomGoboLoader.receiveBytesFromServer(fileName, chunkData);
            });
        });
    }

    private static void syncMappingsToAll(MinecraftServer server) {
        if (server == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(new Gson().toJson(GlobalGoboManager.getAllMappings()));
        NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), SYNC_MAPPINGS, buf);
    }

    private static void sendMappingsToPlayer(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(new Gson().toJson(GlobalGoboManager.getAllMappings()));
        NetworkManager.sendToPlayer(player, SYNC_MAPPINGS, buf);
    }
}
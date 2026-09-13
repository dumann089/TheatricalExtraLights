package com.github.dumann089.theatricalextralights.util;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Envoie les donnees d'un block entity aux clients qui suivent son chunk, sans passer par
 * {@code Level.sendBlockUpdated}.
 *
 * <p>{@code sendBlockUpdated} marque aussi le bloc comme modifie : chaque client reconstruit
 * la section de chunk, et le rendu du projecteur saute une frame a chaque trame DMX qui
 * change (clignotement visible avec Sodium / Embeddium). L'etat du bloc ne change jamais
 * lors d'une mise a jour DMX, seul le paquet de donnees est necessaire.
 */
public final class BlockEntitySync {

    private BlockEntitySync() {
    }

    public static void sendData(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Packet<?> packet = blockEntity.getUpdatePacket();
        if (packet == null) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(blockEntity.getBlockPos());
        for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            player.connection.send(packet);
        }
    }
}

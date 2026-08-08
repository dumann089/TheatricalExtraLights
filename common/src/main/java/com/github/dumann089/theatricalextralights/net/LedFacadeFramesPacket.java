package com.github.dumann089.theatricalextralights.net;

import com.github.dumann089.theatricalextralights.client.LedFacadeClient;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C : diffuse les trames DMX brutes (512 o par univers couvert) d'une façade LED aux clients
 * proches, qui reconstruisent la texture localement. Seuls les univers modifiés sont envoyés
 * (plus un renvoi complet périodique pour les arrivants tardifs).
 */
public class LedFacadeFramesPacket extends BaseS2CMessage {

    private final BlockPos pos;
    private final int[] offsets;
    private final byte[][] frames;

    public LedFacadeFramesPacket(BlockPos pos, int[] offsets, byte[][] frames) {
        this.pos = pos;
        this.offsets = offsets;
        this.frames = frames;
    }

    public LedFacadeFramesPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int n = buf.readVarInt();
        this.offsets = new int[n];
        this.frames = new byte[n][];
        for (int i = 0; i < n; i++) {
            this.offsets[i] = buf.readVarInt();
            this.frames[i] = buf.readByteArray();
        }
    }

    @Override
    public MessageType getType() {
        return ExtraLightsNet.LED_FACADE_FRAMES;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            buf.writeVarInt(offsets[i]);
            buf.writeByteArray(frames[i]);
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> LedFacadeClient.applyFrames(pos, offsets, frames));
    }
}

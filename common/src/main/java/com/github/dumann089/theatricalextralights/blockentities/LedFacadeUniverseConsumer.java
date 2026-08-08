package com.github.dumann089.theatricalextralights.blockentities;

import ch.bildspur.artnet.rdm.RDMDeviceId;
import dev.imabad.theatrical.Constants;
import dev.imabad.theatrical.api.dmx.DMXConsumer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Consommateur DMX pour un univers additionnel d'une façade LED (offset ≥ 1). Un
 * {@link DMXConsumer} du mod de base est lié à un seul univers ; une façade qui s'étale sur
 * plusieurs univers en enregistre un par univers au même {@code BlockPos}. Chaque instance
 * recopie sa trame reçue dans le tampon de la façade via {@link LedFacadeBlockEntity#storeFrame}.
 */
public class LedFacadeUniverseConsumer implements DMXConsumer {

    private final LedFacadeBlockEntity facade;
    private final int universeOffset;
    private final RDMDeviceId deviceId;

    public LedFacadeUniverseConsumer(LedFacadeBlockEntity facade, int universeOffset) {
        this.facade = facade;
        this.universeOffset = universeOffset;
        this.deviceId = new RDMDeviceId(Constants.MANUFACTURER_ID, deviceBytes(facade, universeOffset));
    }

    private static byte[] deviceBytes(LedFacadeBlockEntity facade, int offset) {
        long h = facade.getBlockPos().asLong() * 31 + offset * 0x9E3779B1L;
        return new byte[]{(byte) (h >> 24), (byte) (h >> 16), (byte) (h >> 8), (byte) h};
    }

    public int getUniverseOffset() {
        return universeOffset;
    }

    @Override
    public int getChannelCount() {
        return facade.channelsInUniverse(universeOffset);
    }

    @Override
    public int getChannelStart() {
        return 1;
    }

    @Override
    public int getUniverse() {
        return facade.getUniverse() + universeOffset;
    }

    @Override
    public void consume(byte[] dmxValues) {
        facade.storeFrame(universeOffset, dmxValues);
    }

    @Override
    public RDMDeviceId getDeviceId() {
        return deviceId;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "LED Facade (+" + universeOffset + ")";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return facade.getFixtureId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public UUID getNetworkId() {
        return facade.getNetworkId();
    }

    @Override
    public void setNetworkId(UUID newNetworkId) {
        // Le réseau est piloté par la façade ; rien à faire ici.
    }

    @Override
    public String getTranslationKey() {
        return facade.getTranslationKey();
    }
}

package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.DWTPanelBlock;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DmxShutterStrobeHelper;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.lighting.LightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class DWTPanelBlockEntity extends ExtraLightsLightBlockEntity {

    public static final int CHANNEL_COUNT = 6;
    public static final int SECTION_COUNT = 4;

    private final int[] sections = new int[SECTION_COUNT];
    private int warmSection = 0;

    private AtomicZoneLight[] zoneLights;

    public DWTPanelBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.DWT_PANEL.get(), pos, state);
        setChannelCount(CHANNEL_COUNT);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.DWT_PANEL.get();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] v = Arrays.copyOfRange(dmxValues, start, start + CHANNEL_COUNT);
        if (v.length < CHANNEL_COUNT) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();

        for (int i = 0; i < SECTION_COUNT; i++) {
            sections[i] = u(v[i]);
        }

        warmSection = u(v[4]);

        pan = (int) (u(v[5]) * (360.0f / 255.0f));

        int peak = warmSection;
        long rTotal = warmSection * 255L;
        long gTotal = warmSection * 160L;
        long bTotal = warmSection * 60L;
        long weight = warmSection;

        for (int sec : sections) {
            if (sec > peak) peak = sec;
            rTotal += sec * 255L;
            gTotal += sec * 255L;
            bTotal += sec * 255L;
            weight += sec;
        }

        intensity = peak;
        if (weight > 0) {
            red   = (int) Math.min(255, rTotal / weight);
            green = (int) Math.min(255, gTotal / weight);
            blue  = (int) Math.min(255, bTotal / weight);
        } else {
            red = green = blue = 0;
        }

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    protected long getGameTimeForStrobe() {
        return level != null ? level.getGameTime() : 0L;
    }

    public int getSectionIntensity(int index) {
        if (index < 0 || index >= SECTION_COUNT) return 0;
        return (int) DmxShutterStrobeHelper.computeEffectiveIntensity(sections[index], sections[index], getGameTimeForStrobe());
    }

    public int getWarmSectionIntensity() {
        return warmSection;
    }

    private static int u(byte b) {
        return Byte.toUnsignedInt(b);
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putIntArray("Sections", sections);
        tag.putInt("WarmSection", warmSection);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        setChannelCount(CHANNEL_COUNT);
        int[] savedSections = tag.getIntArray("Sections");
        if (savedSections.length == sections.length) {
            System.arraycopy(savedSections, 0, sections, 0, sections.length);
        }
        warmSection = tag.getInt("WarmSection");
    }

    @Override
    public void lightTick() {
        super.lightTick();
        if (level != null && level.isClientSide) {
            boolean isStrobing = false;
            for (int sec : sections) {
                if (DmxShutterStrobeHelper.isStrobing(sec)) {
                    isStrobing = true;
                    break;
                }
            }
            if (isStrobing) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putIntArray("Sections", sections);
        tag.putInt("WarmSection", warmSection);
        return tag;
    }

    @Override
    public int getDeviceTypeId() { return 0x03; }
    @Override
    public String getModelName() { return "DWT Panel"; }
    @Override
    public ResourceLocation getFixtureId() { return Fixtures.DWT_PANEL.getId(); }
    @Override
    public int getActivePersonality() { return 0; }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.dwt_panel";
    }

    @Override
    public float getLightSpread() {
        return 25.0f;
    }
}
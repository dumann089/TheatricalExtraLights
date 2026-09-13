package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
import com.github.dumann089.theatricalextralights.client.particle.JetVariant;
import com.github.dumann089.theatricalextralights.client.particle.WaterJetParticleOptions;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

import java.util.Arrays;

public class WaterJetBigBlockEntity extends ExtraLightsLightBlockEntity
        implements HasJetHeight, HasJetThickness {

    public double smoothedHeight = 0.0;
    private float jetHeight = 5.0f;
    private float jetThickness = 0.7f;
    private int tickCounter = 0;

    public static final float MIN_THICKNESS = 0.05f;
    public static final float MAX_THICKNESS = 55.5f;
    public static final float MIN_JET_HEIGHT = 0.1f;
    public static final float MAX_JET_HEIGHT = 99.0f;

    public WaterJetBigBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.WATER_JET_BIG.get(), pos, state);
        setChannelCount(1);
    }

    // -------------------
    // GETTERS / SETTERS
    // -------------------
    @Override
    public float getJetThickness() {
        return jetThickness;
    }

    @Override
    public void setJetThickness(float thickness) {
        this.jetThickness = Mth.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    @Override
    public float getJetHeight() {
        return jetHeight;
    }

    @Override
    public void setJetHeight(float h) {
        this.jetHeight = Mth.clamp(h, MIN_JET_HEIGHT, MAX_JET_HEIGHT);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    // -------------------
    // DMX
    // -------------------
    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (ourValues.length < 1) return;

        intensity = convertByteToInt(ourValues[0]);
        if (storePrev()) {
            BlockEntitySync.sendData(this);
        }
        setChanged();
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }


    // -------------------
    // FIXTURE OVERRIDES
    // -------------------
    @Override
    public Fixture getFixture() {
        return Fixtures.WATER_JET_BIG.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Water Jet Big";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.WATER_JET_BIG.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    // -------------------
    // NBT
    // -------------------
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("JetHeight")) jetHeight = tag.getFloat("JetHeight");
        if (tag.contains("JetThickness")) jetThickness = tag.getFloat("JetThickness");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        return tag;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.water_jet_big";
    }
}

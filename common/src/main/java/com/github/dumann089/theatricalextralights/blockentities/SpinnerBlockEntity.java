package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
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

public class SpinnerBlockEntity extends ExtraLightsLightBlockEntity
        implements HasJetHeight, HasJetThickness {

    public double smoothedHeight = 0.0;

    private float jetHeight = 9.0f;
    private float jetThickness = 0.1f;
    private float nozzleAngle = 4.0f;

    private int tickCounter = 0;

    private float spinAccumulator = 0.0f;
    private float prevSpinAngle = 0.0f;

    public static final float MIN_THICKNESS = 0.05f;
    public static final float MAX_THICKNESS = 9.5f;
    public static final float MIN_JET_HEIGHT = 0.1f;
    public static final float MAX_JET_HEIGHT = 99.0f;
    public static final float MIN_NOZZLE_ANGLE = 0.0f;
    public static final float MAX_NOZZLE_ANGLE = 45.0f;

    public SpinnerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.SPINNER.get(), pos, state);
        setChannelCount(1);
    }

    @Override
    public float getJetThickness() {
        return jetThickness;
    }

    @Override
    public void setJetThickness(float thickness) {
        this.jetThickness = Mth.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
        setChanged();
        sync();
    }

    @Override
    public float getJetHeight() {
        return jetHeight;
    }

    @Override
    public void setJetHeight(float h) {
        this.jetHeight = Mth.clamp(h, MIN_JET_HEIGHT, MAX_JET_HEIGHT);
        setChanged();
        sync();
    }

    public float getNozzleAngle() {
        return nozzleAngle;
    }

    public void setNozzleAngle(float angle) {
        this.nozzleAngle = Mth.clamp(angle, MIN_NOZZLE_ANGLE, MAX_NOZZLE_ANGLE);
        setChanged();
        sync();
    }

    public float getSpinAngle() {
        return spinAccumulator;
    }

    public float getPrevSpinAngle() {
        return prevSpinAngle;
    }

    public float getSpinSpeed() {
        return intensity / 255.0f * 4.5f;
    }

    public double getSmoothedHeight() {
        return smoothedHeight;
    }

    public void updateSpin() {
        float intensityNorm = getIntensity() / 255.0f;

        prevSpinAngle = spinAccumulator;
        spinAccumulator += intensityNorm * 3.3f;

        if (spinAccumulator > 36000.0f) {
            float reduction = 36000.0f;
            spinAccumulator -= reduction;
            prevSpinAngle -= reduction;
        }
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + 1);
        if (ourValues.length < 1) return;
        intensity = Byte.toUnsignedInt(ourValues[0]);
        if (storePrev()) {
            sync();
        }
        setChanged();
    }


    public static void tick(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state, SpinnerBlockEntity blockEntity) {
        if (level.isClientSide()) {
            com.github.dumann089.theatricalextralights.client.WaterJetClientEffects.updateSpinnerClient(blockEntity);
        }
    }

    public float getSpinAngleFloat() {
        return spinAccumulator;
    }

    public double getSmoothedHeightValue() {
        return smoothedHeight;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.SPINNER.get();
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
        return "Spinner";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.SPINNER.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        tag.putFloat("NozzleAngle", nozzleAngle);
        tag.putInt("Intensity", intensity);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("JetHeight")) jetHeight = tag.getFloat("JetHeight");
        if (tag.contains("JetThickness")) jetThickness = tag.getFloat("JetThickness");
        if (tag.contains("NozzleAngle")) nozzleAngle = tag.getFloat("NozzleAngle");
        if (tag.contains("Intensity")) intensity = tag.getInt("Intensity");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        tag.putFloat("NozzleAngle", nozzleAngle);
        tag.putInt("Intensity", intensity);
        return tag;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.spinner";
    }
}
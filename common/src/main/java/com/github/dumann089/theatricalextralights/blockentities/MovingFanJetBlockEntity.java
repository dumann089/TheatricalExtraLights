package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetHeight;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasJetThickness;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class MovingFanJetBlockEntity extends ExtraLightsLightBlockEntity implements HasJetHeight, HasJetThickness {
    public double smoothedHeight = 0.0;

    private float jetHeight = 13.0F;
    private float jetThickness = 0.4F;
    private int yaw;
    private int prevYaw;

    public static final float MIN_THICKNESS = 0.05F;
    public static final float MAX_THICKNESS = 9.5F;
    public static final float MIN_JET_HEIGHT = 0.1F;
    public static final float MAX_JET_HEIGHT = 99.0F;

    public MovingFanJetBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.MOVING_FAN_JET.get(), pos, state);
        setChannelCount(4); // Intensity + Pan + Tilt + Yaw
    }

    @Override
    public float getJetThickness() {
        return jetThickness;
    }

    @Override
    public void setJetThickness(float thickness) {
        jetThickness = Mth.clamp(thickness, MIN_THICKNESS, MAX_THICKNESS);
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
    public void setJetHeight(float height) {
        jetHeight = Mth.clamp(height, MIN_JET_HEIGHT, MAX_JET_HEIGHT);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
        }
    }

    public int getYaw() {
        return yaw;
    }

    public float getInterpolatedYaw(float partialTick) {
        return prevYaw + Mth.wrapDegrees((float) (yaw - prevYaw)) * partialTick;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] values = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (values.length < 4) {
            return;
        }

        int previousYaw = yaw;
        prevYaw = yaw;
        intensity = Byte.toUnsignedInt(values[0]);
        pan = (int) ((Byte.toUnsignedInt(values[1]) * 360.0F) / 255.0F) - 180;
        tilt = (int) ((Byte.toUnsignedInt(values[2]) / 255.0F) * 180.0F - 90.0F);
        yaw = (int) ((Byte.toUnsignedInt(values[3]) * 360.0F) / 255.0F) - 180;

        boolean baseValuesChanged = storePrev();
        if (baseValuesChanged || previousYaw != yaw) {
            BlockEntitySync.sendData(this);
        }
        setChanged();
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.MOVING_FAN_JET.get();
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
        return "Moving Fan Jet";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.MOVING_FAN_JET.getId();
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
        tag.putInt("Yaw", yaw);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("JetHeight")) {
            jetHeight = tag.getFloat("JetHeight");
        }
        if (tag.contains("JetThickness")) {
            jetThickness = tag.getFloat("JetThickness");
        }
        if (tag.contains("Yaw")) {
            prevYaw = yaw;
            yaw = tag.getInt("Yaw");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("JetHeight", jetHeight);
        tag.putFloat("JetThickness", jetThickness);
        tag.putInt("Yaw", yaw);
        return tag;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.moving_fan_jet";
    }
}
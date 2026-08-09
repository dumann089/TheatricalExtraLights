package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.client.FlameThrowerClientEffects;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.DirectionOffset;
import com.github.dumann089.theatricalextralights.util.DmxFrameFlamePanSync;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class FlameThrowerBlockEntity extends ExtraLightsLightBlockEntity implements DmxFrameFlamePanSync {
    private static final int FLAME_HOT_COLOR = 0xFF8434;

    private boolean clientWasActive;

    public FlameThrowerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.FLAME_THROWER.get(), pos, state);
        setChannelCount(2);
        intensity = 0;
        pan = 128;
        tilt = 0;
    }

    public static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            T blockEntity
    ) {
        if (blockEntity instanceof FlameThrowerBlockEntity flameThrower) {
            if (level.isClientSide) {
                flameThrower.tickClient();
            }
        }
    }

    public float getHeadRenderAngle(float partialTick) {
        float current = DirectionOffset.panToHeadRenderAngle(getPan());
        float previous = DirectionOffset.panToHeadRenderAngle(getPrevPan());
        return Mth.lerp(partialTick, previous, current);
    }

    public float getHeadAngle(float partialTick) {
        float current = DirectionOffset.panToAngle(getPan());
        float previous = DirectionOffset.panToAngle(getPrevPan());
        return Mth.lerp(partialTick, previous, current);
    }

    private void tickClient() {
        lightTick();
        FlameThrowerClientEffects.tick(this);
    }

    /** Returns true when the client active state changed. */
    public boolean updateClientActiveState(boolean active) {
        if (clientWasActive == active) {
            return false;
        }
        clientWasActive = active;
        return true;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.FLAME_THROWER.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getSyncPan() {
        return pan;
    }

    @Override
    public int getSyncPrevPan() {
        return prevPan;
    }

    @Override
    public void setSyncPan(int value) {
        pan = value;
    }

    @Override
    public void setSyncPrevPan(int value) {
        prevPan = value;
    }

    @Override
    public BlockPos getSyncBlockPos() {
        return getBlockPos();
    }

    @Override
    public Level getSyncLevel() {
        return level;
    }

    @Override
    public void applyDmxFrameBase(int intensity, int red, int green, int blue,
                                  int prevIntensity, int prevRed, int prevGreen, int prevBlue) {
        super.applyDmxFrameBase(intensity, red, green, blue, prevIntensity, prevRed, prevGreen, prevBlue);
        markFlamePanFrameApplied();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (ourValues.length < 2) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int newIntensity = Byte.toUnsignedInt(ourValues[0]);
        int newPan = Byte.toUnsignedInt(ourValues[1]);
        boolean valuesChanged = intensity != newIntensity || pan != newPan;

        intensity = newIntensity;
        pan = newPan;
        tilt = 0;
        focus = 255;
        red = (FLAME_HOT_COLOR >> 16) & 0xFF;
        green = (FLAME_HOT_COLOR >> 8) & 0xFF;
        blue = FLAME_HOT_COLOR & 0xFF;

        finishDmxUpdate(valuesChanged, prevAdvanced);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x06;
    }

    @Override
    public String getModelName() {
        return "Flame Thrower";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.FLAME_THROWER.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return getBlockState().getBlock().getDescriptionId();
    }

    @Override
    protected boolean needsContinuousClientRender() {
        return intensity > 0;
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            FlameThrowerClientEffects.stop(worldPosition);
        }
        super.setRemoved();
    }
}

package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blocks.ParScrollerBlock;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class ParScrollerBlockEntity extends ManualParGoboBlockEntity {

    public static final long COLOR_WHEEL_STEP_DURATION_MS = 1100L;

    private int colorWheelPosition;

    public ParScrollerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.PARSCROLLER.get(), pos, state);
        setChannelCount(2);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.PARSCROLLER.get();
    }

    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.PARSCROLLER_COLORWHEEL;
    }

    /** DMX channel 2 (0-255) selects a position along the linear color-gel wheel. */
    public int getColorWheelPosition() {
        return colorWheelPosition;
    }

    /** Kept as an alias for old integrations that used the former gel-scroller name. */
    public int getScrollerPosition() {
        return colorWheelPosition;
    }

    @Override
    public int getGobo() {
        int slotCount = getGoboLibrary().getSlotCount();
        if (slotCount <= 1) {
            return 0;
        }
        return Mth.clamp(
                Math.round((colorWheelPosition / 255.0F) * (slotCount - 1)),
                0,
                slotCount - 1
        );
    }

    @Override
    public long getGoboWheelStepDurationMs() {
        return COLOR_WHEEL_STEP_DURATION_MS;
    }

    @Override
    public boolean isGoboWheelLinear() {
        return true;
    }

    public int getAnimatedColorWheelColor() {
        GoboLibrary library = getGoboLibrary();
        var animator = getGoboAnimator();
        animator.updateTarget(library, getGobo(), getGoboWheelStepDurationMs(), false);

        float virtualSlot = animator.snapshotVirtualSlot();
        int outgoing = animator.getOutgoingSlot(library, virtualSlot);
        int incoming = animator.getIncomingSlot(library, virtualSlot);
        float progress = animator.getShaderProgress(library, virtualSlot);

        return blendRgb(library.getColor(outgoing), library.getColor(incoming), progress);
    }

    private static int blendRgb(int from, int to, float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        int red = Mth.floor(Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF) + 0.5F);
        int green = Mth.floor(Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF) + 0.5F);
        int blue = Mth.floor(Mth.lerp(t, from & 0xFF, to & 0xFF) + 0.5F);
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public void consume(byte[] dmxValues) {
        if (dmxValues == null) {
            return;
        }

        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        if (start < 0 || start + 2 > dmxValues.length) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();
        int oldIntensity = intensity;
        int oldPosition = colorWheelPosition;

        int newIntensity = Byte.toUnsignedInt(dmxValues[start]);
        int newPosition = Byte.toUnsignedInt(dmxValues[start + 1]);
        intensity = newIntensity;

        // Ignore an all-zero frame so a temporarily lost DMX frame does not reset the wheel.
        if (newIntensity != 0 || newPosition != 0) {
            colorWheelPosition = newPosition;
        }

        finishDmxUpdate(
                intensity != oldIntensity || colorWheelPosition != oldPosition,
                prevAdvanced
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("ColorWheelPosition", colorWheelPosition);
        return tag;
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("ColorWheelPosition", colorWheelPosition);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        if (tag.contains("ColorWheelPosition")) {
            colorWheelPosition = Mth.clamp(tag.getInt("ColorWheelPosition"), 0, 255);
        } else if (tag.contains("ScrollerPosition")) {
            colorWheelPosition = Mth.clamp(tag.getInt("ScrollerPosition"), 0, 255);
        } else if (tag.contains("GoboScrollerPosition")) {
            float oldPosition = tag.getFloat("GoboScrollerPosition");
            float oldMaxSlot = Math.max(1.0F, GoboLibrary.VL2C.getSlotCount() - 1.0F);
            colorWheelPosition = Mth.clamp(Math.round(oldPosition / oldMaxSlot * 255.0F), 0, 255);
        } else {
            colorWheelPosition = 0;
        }
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Par Scroller";
    }

    @Override
    public boolean isUpsideDown() {
        return getBlockState().getValue(ParScrollerBlock.HANGING)
                && getBlockState().getValue(ParScrollerBlock.HANG_DIRECTION) == Direction.UP;
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.PARSCROLLER.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.parscroller";
    }
}

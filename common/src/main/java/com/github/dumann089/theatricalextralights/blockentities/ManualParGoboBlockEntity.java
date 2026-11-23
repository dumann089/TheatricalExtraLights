package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;
import com.github.dumann089.theatricalextralights.client.gobo.GoboWheelAnimator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ManualParGoboBlockEntity extends ExtraLightsLightBlockEntity implements HasGobo {

    private final GoboWheelAnimator goboAnimator = new GoboWheelAnimator();

    protected ManualParGoboBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.WASH;
    }

    @Override
    public GoboWheelAnimator getGoboAnimator() {
        return goboAnimator;
    }

    @Override
    public int getGobo() {
        return 0;
    }

    @Override
    public int getGoboSpin() {
        return 0;
    }

    @Override
    public float getGoboRotation() {
        return 0.0F;
    }

    @Override
    public int getZoom() {
        return 0;
    }

    @Override
    public float getPartialZoom(float partialTicks) {
        return 0.0F;
    }

    @Override
    public float getPartialIntensity(float partialTicks) {
        return getDisplayIntensity(partialTicks);
    }

    @Override
    public int getPartialColour(float partialTicks) {
        return getDisplayColour(partialTicks);
    }

    @Override
    public float getPartialPanDeg(float partialTicks) {
        return Mth.lerp(partialTicks, getPrevPan(), getPan());
    }

    @Override
    public float getPartialTiltDeg(float partialTicks) {
        return Mth.lerp(partialTicks, getPrevTilt(), getTilt());
    }

    @Override
    public int getLightLuminance() {
        return 0;
    }

}

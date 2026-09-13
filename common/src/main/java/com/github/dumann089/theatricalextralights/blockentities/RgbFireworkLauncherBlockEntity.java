package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.entities.FireworkRocketEntity;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.firework.FireworkPreset;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class RgbFireworkLauncherBlockEntity extends FireworkLauncherBlockEntity {
    private int effectDmx = 0;
    private FireworkPreset selectedPreset = FireworkPreset.RED_COMET;

    public RgbFireworkLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.RGB_FIREWORK_LAUNCHER.get(), pos, state);
        setChannelCount(7);
    }

    @Override
    public FireworkPreset getPreset() {
        return selectedPreset;
    }

    @Override
    protected FireworkRocketEntity createRocket(ServerLevel serverLevel) {
        FireworkRocketEntity rocket = new FireworkRocketEntity(serverLevel, getPreset(), worldPosition);
        rocket.setCustomColors(red, green, blue);
        return rocket;
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.FIREWORK_RGB_LAUNCHER.get();
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.FIREWORK_RGB_LAUNCHER.getId();
    }

    @Override
    public String getModelName() {
        return "RGB Firework (" + selectedPreset.getDisplayName() + ")";
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = getChannelStart() > 0 ? getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start, start + getChannelCount());
        if (ourValues.length < 2) {
            return;
        }

        if (storePrev() && level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        int newIntensity = safetyArmed ? Byte.toUnsignedInt(ourValues[0]) : 0;
        if (prevIntensity == 0 && newIntensity > 0) {
            pendingOneShot = true;
        }
        prevIntensity = newIntensity;

        intensity = newIntensity;
        tilt = Byte.toUnsignedInt(ourValues[1]);
        focus = ourValues.length >= 3 ? Byte.toUnsignedInt(ourValues[2]) : 0;
        red = ourValues.length >= 4 ? Byte.toUnsignedInt(ourValues[3]) : 255;
        green = ourValues.length >= 5 ? Byte.toUnsignedInt(ourValues[4]) : 255;
        blue = ourValues.length >= 6 ? Byte.toUnsignedInt(ourValues[5]) : 255;
        effectDmx = ourValues.length >= 7 ? Byte.toUnsignedInt(ourValues[6]) : 0;
        selectedPreset = FireworkPreset.byDmxIndex(effectDmx);
        pan = 0;

        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("EffectDmx", effectDmx);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        setChannelCount(7);
        effectDmx = tag.getInt("EffectDmx");
        selectedPreset = FireworkPreset.byDmxIndex(effectDmx);
    }
}

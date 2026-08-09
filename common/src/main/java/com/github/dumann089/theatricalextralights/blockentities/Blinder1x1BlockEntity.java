package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Blinder1x1BlockEntity extends BlinderBaseBlockEntity {

    public Blinder1x1BlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BLINDER1X1.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Blinder1x1BlockEntity be) {
        BlinderBaseBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.BLINDER1X1.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "Blinder 1x1";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.BLINDER1X1.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.blinder1x1";
    }
}

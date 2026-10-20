package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Blinder a deux lampes : meme DMX que le 1x1 (dimmer, RGB, strobe), deux faces allumees. */
public class Blinder1x2BlockEntity extends BlinderBaseBlockEntity {

    public Blinder1x2BlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BLINDER1X2.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Blinder1x2BlockEntity be) {
        BlinderBaseBlockEntity.tick(level, pos, state, be);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.BLINDER1X2.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "Blinder 1x2";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.BLINDER1X2.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.blinder1x2";
    }
}

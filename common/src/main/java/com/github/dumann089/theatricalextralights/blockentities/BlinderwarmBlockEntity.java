package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Blinder Warm : 8 lampes tungstene, mode 1 canal ou un canal par lampe. */
public class BlinderwarmBlockEntity extends WarmBlinderBlockEntity {

    public static final int LAMPS = 8;

    public BlinderwarmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BLINDER_WARM.get(), pos, state, LAMPS);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.BLINDER_WARM.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Blinder Warm";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.BLINDER_WARM.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.blinder_warm";
    }
}

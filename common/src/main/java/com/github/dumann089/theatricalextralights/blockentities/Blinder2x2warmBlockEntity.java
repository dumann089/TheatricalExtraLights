package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Blinder 2x2 Warm : 4 lampes tungstene, mode 1 canal ou un canal par lampe. */
public class Blinder2x2warmBlockEntity extends WarmBlinderBlockEntity {

    public static final int LAMPS = 4;

    public Blinder2x2warmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BLINDER2X2WARM.get(), pos, state, LAMPS);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.BLINDER2X2WARM.get();
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Blinder 2x2 Warm";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.BLINDER2X2WARM.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.blinder2x2warm";
    }
}

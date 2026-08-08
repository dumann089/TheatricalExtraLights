package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Façade LED : matrice de pixels adressée en DMX. Chaque pixel dessiné consomme 4 canaux
 * (dimmer / R / G / B). La personnalité 4ch décrite ici sert de métadonnée RDM / patch pour
 * un pixel ; la logique multi-pixels / multi-univers réelle est portée par le BlockEntity.
 */
public class LedFacadeFixture extends Fixture {

    private static final List<DMXPersonality> PERSONALITIES = Collections.singletonList(
            new DMXPersonality(4, "4-Channel Pixel (Dimmer/R/G/B)")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
    );

    private static final ResourceLocation STATIC_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/ledfacade/led_facade_whole");

    @Override
    public ResourceLocation getTiltModel() {
        return null;
    }

    @Override
    public ResourceLocation getPanModel() {
        return null;
    }

    @Override
    public ResourceLocation getStaticModel() {
        return STATIC_MODEL;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return new float[3];
    }

    @Override
    public float[] getPanRotationPosition() {
        return new float[3];
    }

    @Override
    public float[] getBeamStartPosition() {
        return new float[3];
    }

    @Override
    public float getDefaultRotation() {
        return 0;
    }

    @Override
    public float getBeamWidth() {
        return 0;
    }

    @Override
    public float getRayTraceRotation() {
        return 0;
    }

    @Override
    public HangType getHangType() {
        return HangType.BRACE_BAR;
    }

    @Override
    public boolean hasBeam() {
        return false;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        return new float[]{0, 0, 0};
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }

    @Override
    public double getLightRadius() {
        return 8.0;
    }
}

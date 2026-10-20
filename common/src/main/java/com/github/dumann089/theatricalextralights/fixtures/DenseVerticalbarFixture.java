package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.blockentities.DenseVerticalbarBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.PixelBarBlockEntity;
import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Vertical RGB Bar dense : le meme appareil, 46 pixels au lieu de 9 en mode pixel (184 canaux). */
public class DenseVerticalbarFixture extends VerticalbarFixture {

    public static final int PIXEL_CHANNEL_COUNT = PixelBarBlockEntity.pixelModeChannelCount(0, DenseVerticalbarBlockEntity.PIXEL_COUNT);

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(4, "4-Channel Mode")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE),
            PixelBarPersonalities.addPixels(new DMXPersonality(PIXEL_CHANNEL_COUNT,
                    PixelBarPersonalities.pixelModeName(PIXEL_CHANNEL_COUNT, DenseVerticalbarBlockEntity.PIXEL_COUNT, "")),
                    DenseVerticalbarBlockEntity.PIXEL_COUNT)
    );

    /** Tete dense : une LED par pixel au lieu des lentilles de la barre d'origine (tools/models/gen_dense_bars.py). */
    private static final ResourceLocation DENSE_TILT_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/barvertical/barvertical_dense_tilt");

    @Override
    public ResourceLocation getTiltModel() {
        return DENSE_TILT_MODEL;
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }
}

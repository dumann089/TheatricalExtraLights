package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.blockentities.DenseMovingbarBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.PixelBarBlockEntity;
import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.fixtures.SharedSlots;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Moving Bar dense : le meme appareil, 24 pixels au lieu de 8 en mode pixel (2 + 96 = 98 canaux). */
public class DenseMovingbarFixture extends MovingbarFixture {

    public static final int PIXEL_CHANNEL_COUNT = PixelBarBlockEntity.pixelModeChannelCount(
            DenseMovingbarBlockEntity.PIXEL_HEADER_CHANNELS, DenseMovingbarBlockEntity.PIXEL_COUNT);

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            new DMXPersonality(7, "7-Channel Mode")
                    .addSlot(SharedSlots.INTENSITY)
                    .addSlot(SharedSlots.RED)
                    .addSlot(SharedSlots.GREEN)
                    .addSlot(SharedSlots.BLUE)
                    .addSlot(SharedSlots.FOCUS)
                    .addSlot(SharedSlots.PAN)
                    .addSlot(SharedSlots.TILT),
            PixelBarPersonalities.addPixels(new DMXPersonality(PIXEL_CHANNEL_COUNT,
                    PixelBarPersonalities.pixelModeName(PIXEL_CHANNEL_COUNT, DenseMovingbarBlockEntity.PIXEL_COUNT, "Pan, Tilt"))
                    .addSlot(SharedSlots.PAN)
                    .addSlot(SharedSlots.TILT),
                    DenseMovingbarBlockEntity.PIXEL_COUNT)
    );

    /** Tete dense : une LED par pixel au lieu des lentilles de la barre d'origine (tools/models/gen_dense_bars.py). */
    private static final ResourceLocation DENSE_TILT_MODEL =
            new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/movingbar/movingbar_dense_tilt");

    @Override
    public ResourceLocation getTiltModel() {
        return DENSE_TILT_MODEL;
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }
}

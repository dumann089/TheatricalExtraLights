package com.github.dumann089.theatricalextralights.fixtures;

import com.github.dumann089.theatricalextralights.TheatricalExtraLights;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.HangType;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import dev.imabad.theatrical.blocks.light.BaseLightBlock;
import dev.imabad.theatrical.fixtures.SharedSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Strobe LED type Atomic : un tube strobe blanc de neuf segments entre deux plaques de quatre
 * zones RGB, sur une lyre en U posee au sol ou accrochee.
 *
 * <p>Personnalites, dans l'ordre (l'index est sauve dans le bloc, le 34 canaux historique reste
 * en premier pour les mondes existants) :
 * <ol start="0">
 * <li>34 canaux pixel historique : 8 zones RGB, 9 segments, focus, tout en continu.</li>
 * <li>1 canal : strobe seul (0 eteint, 1-254 cadence, 255 allume).</li>
 * <li>4 canaux Atomic : intensite, duree de flash, cadence, effets.</li>
 * <li>8 canaux Atomic + Aura : les plaques RGB comme retro-eclairage d'une seule couleur.</li>
 * <li>10 canaux compresse : tube (4) + plaque comme un seul pixel avec son propre strobe (6).</li>
 * <li>40 canaux pixel : tube (4) + 9 segments + plaque (3) + 8 zones RGB.</li>
 * <li>400 canaux pixel map : tube (4) + 9 segments + plaque (3) + 96 pixels Dim/RGB (12 x 4 par plaque).</li>
 * </ol>
 */
public class AtomicStrobeFixture extends Fixture {

    private static DMXPersonality legacy() {
        DMXPersonality p = new DMXPersonality(34, "34-Channel Pixel (Legacy)");
        for (int zone = 0; zone < 8; zone++) {
            p.addSlot(SharedSlots.RED).addSlot(SharedSlots.GREEN).addSlot(SharedSlots.BLUE);
        }
        for (int i = 0; i < 9; i++) {
            p.addSlot(SharedSlots.INTENSITY);
        }
        p.addSlot(SharedSlots.FOCUS);
        return p;
    }

    private static DMXPersonality bar(DMXPersonality p) {
        return p.addSlot(AtomicSlots.INTENSITY).addSlot(AtomicSlots.DURATION).addSlot(AtomicSlots.RATE).addSlot(AtomicSlots.EFFECTS);
    }

    private static DMXPersonality pixel() {
        DMXPersonality p = bar(new DMXPersonality(40, "40-Channel Pixel (Bar + Plate)"));
        for (int i = 1; i <= 9; i++) {
            p.addSlot(AtomicSlots.segment(i));
        }
        p.addSlot(AtomicSlots.PLATE_INTENSITY).addSlot(AtomicSlots.PLATE_DURATION).addSlot(AtomicSlots.PLATE_RATE);
        for (int zone = 1; zone <= 8; zone++) {
            p.addSlot(AtomicSlots.zoneRed(zone)).addSlot(AtomicSlots.zoneGreen(zone)).addSlot(AtomicSlots.zoneBlue(zone));
        }
        return p;
    }

    private static DMXPersonality pixelMap() {
        DMXPersonality p = bar(new DMXPersonality(400, "400-Channel Pixel Map (Bar + 96 Pixels)"));
        for (int i = 1; i <= 9; i++) {
            p.addSlot(AtomicSlots.segment(i));
        }
        p.addSlot(AtomicSlots.PLATE_INTENSITY).addSlot(AtomicSlots.PLATE_DURATION).addSlot(AtomicSlots.PLATE_RATE);
        for (int pixel = 1; pixel <= 96; pixel++) {
            p.addSlot(AtomicSlots.pixelDim(pixel)).addSlot(AtomicSlots.pixelRed(pixel))
                    .addSlot(AtomicSlots.pixelGreen(pixel)).addSlot(AtomicSlots.pixelBlue(pixel));
        }
        return p;
    }

    private static final List<DMXPersonality> PERSONALITIES = List.of(
            legacy(),
            new DMXPersonality(1, "1-Channel Strobe").addSlot(AtomicSlots.STROBE),
            bar(new DMXPersonality(4, "4-Channel Atomic")),
            bar(new DMXPersonality(8, "8-Channel Atomic + Aura"))
                    .addSlot(AtomicSlots.AURA_INTENSITY).addSlot(AtomicSlots.AURA_RED)
                    .addSlot(AtomicSlots.AURA_GREEN).addSlot(AtomicSlots.AURA_BLUE),
            bar(new DMXPersonality(10, "10-Channel Compressed (Bar + Plate)"))
                    .addSlot(AtomicSlots.PLATE_INTENSITY).addSlot(AtomicSlots.PLATE_DURATION).addSlot(AtomicSlots.PLATE_RATE)
                    .addSlot(AtomicSlots.PLATE_RED).addSlot(AtomicSlots.PLATE_GREEN).addSlot(AtomicSlots.PLATE_BLUE),
            pixel(),
            pixelMap()
    );

    private static final ResourceLocation TILT_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomic_strobe/atomic_strobe_tilt");
    private static final ResourceLocation PAN_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomic_strobe/atomic_strobe_pan");
    private static final ResourceLocation STATIC_MODEL = new ResourceLocation(TheatricalExtraLights.MOD_ID, "block/atomic_strobe/atomic_strobe_static");

    // Corps x 1..15, y 3..11, z 7.5..11.5, face au sud (z = 11.5). Lyre en U : bras aux flancs,
    // axes a y = 7 ; le pan tourne le socle entier autour du centre de la plaque de sol.
    private final float[] tiltRotation = new float[]{0.5F, 7F / 16F, 9.5F / 16F};
    private final float[] panRotation = new float[]{0.5F, 0.0F, 9.5F / 16F};
    private final float[] beamStartPosition = new float[]{0.5F, 7F / 16F, 11.6F / 16F};

    @Override
    public ResourceLocation getTiltModel() {
        return TILT_MODEL;
    }

    @Override
    public ResourceLocation getPanModel() {
        return PAN_MODEL;
    }

    @Override
    public ResourceLocation getStaticModel() {
        return STATIC_MODEL;
    }

    @Override
    public float[] getTiltRotationPosition() {
        return tiltRotation;
    }

    @Override
    public float[] getPanRotationPosition() {
        return panRotation;
    }

    @Override
    public float[] getBeamStartPosition() {
        return beamStartPosition;
    }

    @Override
    public float getDefaultRotation() {
        return 90;
    }

    @Override
    public float getBeamWidth() {
        return 0.0f;
    }

    @Override
    public float getRayTraceRotation() {
        return 180f;
    }

    @Override
    public HangType getHangType() {
        return HangType.HOOK_BAR;
    }

    @Override
    public float[] getTransforms(BlockState fixtureBlockState, BlockState supportBlockState) {
        if (fixtureBlockState.getValue(BaseLightBlock.HANG_DIRECTION) == Direction.UP) {
            return new float[]{0, .510f, 0};
        }
        return new float[]{0, -0.365F, 0};
    }

    @Override
    public List<DMXPersonality> getDMXPersonalities() {
        return PERSONALITIES;
    }

    @Override
    public boolean invertTilt() {
        return false;
    }

    @Override
    public boolean invertPan() {
        return false;
    }

    @Override
    public double getLightRadius() {
        return 14.5;
    }
}

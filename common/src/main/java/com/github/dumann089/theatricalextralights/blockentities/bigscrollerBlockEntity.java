
package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.GelScrollerConfigurable;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.client.blockentities.bigscrollerRenderer;
import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.api.dmx.DMXPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class bigscrollerBlockEntity extends ExtraLightsLightBlockEntity implements HasPersonality, GelScrollerConfigurable {
    public static final int MIN_GELS = 2;
    public static final int MAX_GELS = 24;
    public static final float MIN_GEL_HEIGHT = 0.25F;
    public static final float MAX_GEL_HEIGHT = 4.0F;
    public static final float MIN_SCROLLER_SPEED = 0.05F;
    public static final float MAX_SCROLLER_SPEED = 3.0F;

    private static final int GEL_CONFIG_VERSION = 1;

    private static final int[] DEFAULT_GEL_COLORS = {
            0xFF0000, 0xFF5000, 0xFFB400, 0xFFFF00,
            0x00FF00, 0x00FFB4, 0x00B4FF, 0x0000FF,
            0x6400FF, 0xB400FF, 0xFF0078, 0xFFFFFF
    };

    private int activePersonalityIndex = 0;
    private final int[] sectionIntensities = {255, 255, 255};
    private int scrollerPosition = 0;
    private int[] gelColors = DEFAULT_GEL_COLORS.clone();

    /*
     * GelHeight es el alto de cada franja de color.
     * El área visible completa de la tira sigue siendo 2 x 3 bloques
     * y se define por separado en el renderer.
     */
    private float gelHeight = 3.0F;
    private float scrollerSpeed = 0.75F;

    public bigscrollerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BIGSCROLLER.get(), pos, state);
        setChannelCount(4);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.BIGSCROLLER.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();

        if (index < 0 || index >= personalities.size()
                || activePersonalityIndex == index) {
            return;
        }

        activePersonalityIndex = index;
        setChannelCount(personalities.get(index).getChannelCount());

        /*
         * Elimina/activa la luz original de Theatrical al cambiar de modo.
         * En modo scroller los colores provienen de las tres fuentes nuevas.
         */
        syncOriginalShimmerLight();

        setChanged();

        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncOriginalShimmerLight() {
        if (level == null || !level.isClientSide) {
            return;
        }

        setLightEnabled(!isScrollerMode() && intensity > 0);
    }

    /*
     * La fuente dinámica heredada no debe emitir ni hacer raycast
     * cuando el renderer usa las tres fuentes Shimmer por zona.
     */
    @Override
    public int getLightLuminance() {
        return isScrollerMode() ? 0 : super.getLightLuminance();
    }

    @Override
    public boolean emitsLight() {
        return !isScrollerMode() && super.emitsLight();
    }

    @Override
    public boolean shouldTrace() {
        return !isScrollerMode() && super.shouldTrace();
    }


    @Override
    public void consume(byte[] dmxValues) {
        if (dmxValues == null) return;

        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        if (start < 0 || start + 4 > dmxValues.length) return;

        int[] ourValues = new int[4];
        for (int i = 0; i < 4; i++) {
            ourValues[i] = Byte.toUnsignedInt(dmxValues[start + i]);
        }

        boolean prevAdvanced = beginDmxUpdate();

        int oldIntensity = intensity;
        int oldRed = red;
        int oldGreen = green;
        int oldBlue = blue;
        int oldScrollerPosition = scrollerPosition;
        int[] oldSectionIntensities = sectionIntensities.clone();

        if (getActivePersonality() == 1) {
            // Personalidad 3-Section Gel Scroller: CH1-CH3 intensidades, CH4 posición.
            sectionIntensities[0] = ourValues[0];
            sectionIntensities[1] = ourValues[1];
            sectionIntensities[2] = ourValues[2];

            boolean allChannelsZero =
                    ourValues[0] == 0 &&
                            ourValues[1] == 0 &&
                            ourValues[2] == 0 &&
                            ourValues[3] == 0;

            // No reiniciar la posición cuando la señal DMX vuelve a cero.
            if (!allChannelsZero) {
                scrollerPosition = ourValues[3];
            }
        } else {
            // Personalidad iRGB de 4 canales.
            intensity = ourValues[0];
            red = ourValues[1];
            green = ourValues[2];
            blue = ourValues[3];
        }

        boolean changed =
                intensity != oldIntensity ||
                        red != oldRed ||
                        green != oldGreen ||
                        blue != oldBlue ||
                        scrollerPosition != oldScrollerPosition ||
                        !Arrays.equals(sectionIntensities, oldSectionIntensities);

        finishDmxUpdate(changed, prevAdvanced);
    }

    @Override
    protected boolean hasExtraDmxChannelsBeyondBatch() {
        return true;
    }

    public int getSectionIntensity(int section) {
        if (section < 0 || section >= sectionIntensities.length) {
            return 0;
        }
        return sectionIntensities[section];
    }

    public int getScrollerPosition() {
        return scrollerPosition;
    }

    public boolean isScrollerMode() {
        return activePersonalityIndex == 1;
    }

    @Override
    public float getGelSize() {
        return getGelHeight();
    }

    @Override
    public boolean isHorizontalGelScroller() {
        return false;
    }

    public int[] getGelColors() {
        return gelColors.clone();
    }

    public int getGelCount() {
        return gelColors.length;
    }

    public int[] getGelColorAtIndex(int index) {
        int color = gelColors[Math.floorMod(index, gelColors.length)];

        return new int[] {
                (color >> 16) & 255,
                (color >> 8) & 255,
                color & 255
        };
    }

    public float getGelHeight() {
        return gelHeight;
    }

    public float getScrollerSpeed() {
        return scrollerSpeed;
    }

    public void setGelConfiguration(int[] colors, float height, float speed) {
        if (colors == null || colors.length < MIN_GELS
                || colors.length > MAX_GELS) {
            return;
        }

        int[] normalized = new int[colors.length];

        for (int i = 0; i < colors.length; i++) {
            normalized[i] = colors[i] & 0xFFFFFF;
        }

        gelColors = normalized;
        gelHeight = Math.max(
                MIN_GEL_HEIGHT, Math.min(MAX_GEL_HEIGHT, height));
        scrollerSpeed = Math.max(
                MIN_SCROLLER_SPEED, Math.min(MAX_SCROLLER_SPEED, speed));

        setChanged();

        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int convertByteToInt(byte value) {
        return Byte.toUnsignedInt(value);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x02;
    }

    @Override
    public String getModelName() {
        return "Big Scroller";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.BIGSCROLLER.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.bigscroller";
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);

        tag.putInt("GelConfigVersion", GEL_CONFIG_VERSION);
        tag.putInt("ActivePersonality", activePersonalityIndex);
        tag.putInt("ScrollerPosition", scrollerPosition);
        tag.putIntArray("SectionIntensities", sectionIntensities);
        tag.putIntArray("GelColors", gelColors);
        tag.putFloat("GelHeight", gelHeight);
        tag.putFloat("ScrollerSpeed", scrollerSpeed);
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);

        int count = getFixture().getDMXPersonalities().size();
        activePersonalityIndex = Math.max(
                0, Math.min(tag.getInt("ActivePersonality"), count - 1));

        setChannelCount(
                getFixture().getDMXPersonalities()
                        .get(activePersonalityIndex)
                        .getChannelCount());

        scrollerPosition = Math.max(
                0, Math.min(255, tag.getInt("ScrollerPosition")));

        int[] sections = tag.getIntArray("SectionIntensities");
        if (sections.length >= 3) {
            for (int i = 0; i < 3; i++) {
                sectionIntensities[i] = Math.max(
                        0, Math.min(255, sections[i]));
            }
        }

        int[] savedColors = tag.getIntArray("GelColors");
        if (savedColors.length >= MIN_GELS && savedColors.length <= MAX_GELS) {
            gelColors = new int[savedColors.length];

            for (int i = 0; i < savedColors.length; i++) {
                gelColors[i] = savedColors[i] & 0xFFFFFF;
            }
        } else {
            gelColors = DEFAULT_GEL_COLORS.clone();
        }

        int version = tag.getInt("GelConfigVersion");
        float savedHeight = tag.contains("GelHeight")
                ? tag.getFloat("GelHeight")
                : 0.4375F;

        /*
         * Migración de mundos creados con la versión anterior del menú:
         * el alto 2.8 representaba la apertura completa y hacía que las
         * tres zonas muestrearan la misma franja durante casi todo el recorrido.
         */
        if (version < GEL_CONFIG_VERSION && savedHeight > 1.0F) {
            savedHeight = 0.4375F;
        }

        gelHeight = Math.max(
                MIN_GEL_HEIGHT, Math.min(MAX_GEL_HEIGHT, savedHeight));

        scrollerSpeed = Math.max(
                MIN_SCROLLER_SPEED,
                Math.min(
                        MAX_SCROLLER_SPEED,
                        tag.contains("ScrollerSpeed")
                                ? tag.getFloat("ScrollerSpeed")
                                : 0.75F
                )
        );

        // Reacciona cuando el modo se recibe del servidor o al cargar el mundo.
        syncOriginalShimmerLight();
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            setLightEnabled(false);
            bigscrollerRenderer.removeSectionShimmerLights(level, worldPosition);
        }

        super.setRemoved();
    }
}
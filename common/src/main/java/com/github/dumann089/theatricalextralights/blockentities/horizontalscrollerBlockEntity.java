
package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.blockentities.interfaces.GelScrollerConfigurable;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasPersonality;
import com.github.dumann089.theatricalextralights.client.blockentities.horizontalscrollerRenderer;
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

public class horizontalscrollerBlockEntity extends ExtraLightsLightBlockEntity
        implements HasPersonality, GelScrollerConfigurable {

    public static final int MIN_GELS = 2;
    public static final int MAX_GELS = 24;
    public static final float MIN_GEL_SIZE = 0.10F;
    public static final float MAX_GEL_SIZE = 2.0F;
    public static final float MIN_SCROLLER_SPEED = 0.05F;
    public static final float MAX_SCROLLER_SPEED = 3.0F;

    private static final int[] DEFAULT_GEL_COLORS = {
            0xFF0000, 0xFF5000, 0xFFB400, 0xFFFF00,
            0x00FF00, 0x00FFB4, 0x00B4FF, 0x0000FF,
            0x6400FF, 0xB400FF, 0xFF0078, 0xFFFFFF
    };

    private int activePersonalityIndex = 0;
    private int scrollerPosition = 0;

    private int[] gelColors = DEFAULT_GEL_COLORS.clone();

    // Ancho de cada franja del rollo horizontal, no el alto de la ventana.
    private float gelWidth = 2.0F;
    private float scrollerSpeed = 0.75F;

    public horizontalscrollerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.HORIZONTALSCROLLER.get(), pos, state);
        setChannelCount(4);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.HORIZONTALSCROLLER.get();
    }

    @Override
    public int getFocus() {
        return 255;
    }

    @Override
    public int getActivePersonality() {
        return activePersonalityIndex;
    }

    public boolean isScrollerMode() {
        return activePersonalityIndex == 1;
    }

    @Override
    public void setActivePersonality(int index) {
        List<DMXPersonality> personalities = getFixture().getDMXPersonalities();

        if (index < 0 || index >= personalities.size()
                || index == activePersonalityIndex) {
            return;
        }

        activePersonalityIndex = index;
        setChannelCount(personalities.get(index).getChannelCount());
        syncOriginalShimmerLight();
        setChanged();

        if (level != null && !level.isClientSide) {
            BlockEntitySync.sendData(this);
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncOriginalShimmerLight() {
        if (level != null && level.isClientSide) {
            setLightEnabled(!isScrollerMode() && intensity > 0);
        }
    }

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
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        boolean gelMode = getActivePersonality() != 0;
        int requiredChannels = gelMode ? 2 : 4;

        if (start < 0 || start + requiredChannels > dmxValues.length) {
            return;
        }

        boolean prevAdvanced = beginDmxUpdate();

        int oldIntensity = intensity;
        int oldRed = red;
        int oldGreen = green;
        int oldBlue = blue;
        int oldFocus = focus;
        int oldPan = pan;
        int oldTilt = tilt;
        int oldScrollerPosition = scrollerPosition;

        int newIntensity = Byte.toUnsignedInt(dmxValues[start]);

        intensity = newIntensity;

        if (gelMode) {
            int newPosition = Byte.toUnsignedInt(dmxValues[start + 1]);

            // Si ambos canales llegan a cero, se conserva la última posición.
            // Esto evita que el scroller vuelva al inicio cuando se pierde la señal DMX.
            if (newIntensity != 0 || newPosition != 0) {
                scrollerPosition = newPosition;
            }
        } else {
            red = Byte.toUnsignedInt(dmxValues[start + 1]);
            green = Byte.toUnsignedInt(dmxValues[start + 2]);
            blue = Byte.toUnsignedInt(dmxValues[start + 3]);
        }

        boolean changed =
                intensity != oldIntensity
                        || red != oldRed
                        || green != oldGreen
                        || blue != oldBlue
                        || focus != oldFocus
                        || pan != oldPan
                        || tilt != oldTilt
                        || scrollerPosition != oldScrollerPosition;

        finishDmxUpdate(changed, prevAdvanced);
    }

    public int getScrollerPosition() {
        return scrollerPosition;
    }

    @Override
    public int[] getGelColors() {
        return gelColors.clone();
    }

    @Override
    public int getGelCount() {
        return gelColors.length;
    }

    @Override
    public int[] getGelColorAtIndex(int index) {
        int color = gelColors[Math.floorMod(index, gelColors.length)];
        return new int[] {
                (color >> 16) & 255,
                (color >> 8) & 255,
                color & 255
        };
    }

    public float getGelWidth() {
        return gelWidth;
    }

    @Override
    public float getGelSize() {
        return gelWidth;
    }

    @Override
    public float getScrollerSpeed() {
        return scrollerSpeed;
    }

    @Override
    public boolean isHorizontalGelScroller() {
        return true;
    }

    @Override
    public void setGelConfiguration(int[] colors, float size, float speed) {
        if (colors == null || colors.length < MIN_GELS
                || colors.length > MAX_GELS) {
            return;
        }

        gelColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            gelColors[i] = colors[i] & 0xFFFFFF;
        }

        gelWidth = Math.max(
                MIN_GEL_SIZE, Math.min(MAX_GEL_SIZE, size));
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
        return "Horizontal Scroller";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.HORIZONTALSCROLLER.getId();
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.horizontalscroller";
    }

    @Override
    public void write(CompoundTag tag) {
        super.write(tag);

        tag.putInt("ActivePersonality", activePersonalityIndex);
        tag.putInt("ScrollerPosition", scrollerPosition);
        tag.putIntArray("GelColors", gelColors);
        tag.putFloat("GelWidth", gelWidth);
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
                        .getChannelCount()
        );

        scrollerPosition = Math.max(
                0, Math.min(255, tag.getInt("ScrollerPosition")));

        int[] savedColors = tag.getIntArray("GelColors");
        if (savedColors.length >= MIN_GELS && savedColors.length <= MAX_GELS) {
            gelColors = new int[savedColors.length];
            for (int i = 0; i < savedColors.length; i++) {
                gelColors[i] = savedColors[i] & 0xFFFFFF;
            }
        } else {
            gelColors = DEFAULT_GEL_COLORS.clone();
        }

        gelWidth = Math.max(
                MIN_GEL_SIZE,
                Math.min(
                        MAX_GEL_SIZE,
                        tag.contains("GelWidth") ? tag.getFloat("GelWidth") : 2.0F
                )
        );

        scrollerSpeed = Math.max(
                MIN_SCROLLER_SPEED,
                Math.min(
                        MAX_SCROLLER_SPEED,
                        tag.contains("ScrollerSpeed")
                                ? tag.getFloat("ScrollerSpeed")
                                : 0.75F
                )
        );

        syncOriginalShimmerLight();
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            setLightEnabled(false);
            horizontalscrollerRenderer.removeShimmerLights(level, worldPosition);
        }

        super.setRemoved();
    }
}
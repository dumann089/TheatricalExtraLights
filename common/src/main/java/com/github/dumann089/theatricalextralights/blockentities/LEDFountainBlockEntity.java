package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.blockentities.interfaces.HasGobo;
import com.github.dumann089.theatricalextralights.client.gobo.GoboLibrary;

import java.util.Arrays;

public class LEDFountainBlockEntity extends ExtraLightsLightBlockEntity implements HasGobo {

    // Valores hardcodeados para el sistema de gobo y zoom
    private final int fixedGobo = 0;
    private final int fixedZoom = 255; // Equivale a un zoom intermedio (~50%)
    private final float fixedGoboRotation = 0f;

    public LEDFountainBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.LED_FOUNTAIN.get(), pos, state);
        setChannelCount(3); // Solo utiliza RGB
    }

    @Override
    public GoboLibrary getGoboLibrary() {
        return GoboLibrary.WASH;
    }

    @Override
    public int getGobo() { return fixedGobo; }

    public int getPrevGobo() { return fixedGobo; }

    public int getZoom() { return fixedZoom; }

    public int getPrevZoom() { return fixedZoom; }

    public float getPartialZoom(float partialTicks) { return (float) fixedZoom; }

    public int getGoboSpin() { return 0; }

    public float getGoboRotation() { return fixedGoboRotation; }

    @Override
    public Fixture getFixture() {
        return Fixtures.LED_FOUNTAIN.get();
    }

    @Override
    public int getFocus() {
        return 1;
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start,
                start+ this.getChannelCount());
        if(ourValues.length < 3){
            return;
        }
        boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        red = convertByteToInt(ourValues[0]);
        green = convertByteToInt(ourValues[1]);
        blue = convertByteToInt(ourValues[2]);

        intensity = Math.max(red, Math.max(green, blue));

        finishDmxUpdate(intensity != _pi || red != _pr || green != _pg || blue != _pb || focus != _pf || pan != _pp || tilt != _pt, prevAdvanced);
    }

    @Override
    public int getDeviceTypeId() { return 0x02; }

    @Override
    public String getModelName() { return "LED Fountain"; }

    @Override
    public ResourceLocation getFixtureId() { return Fixtures.LED_FOUNTAIN.getId(); }

    @Override
    public int getActivePersonality() { return 0; }

    public int convertByteToInt(byte val) { return Byte.toUnsignedInt(val); }

    @Override
    public String getTranslationKey() { return "block.theatricalextralights.led_fountain"; }

    @Override
    public int getBasePan() { return 0; }

    @Override
    public boolean isUpsideDown() { return false; }

    @Override
    public float getPartialIntensity(float partialTicks) {
        return getPrevIntensity() + (getIntensity() - getPrevIntensity()) * partialTicks;
    }

    @Override
    public int getColour() {
        return ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    @Override
    public float getPartialPanDeg(float partialTicks) {
        return getPrevPan() + (getPan() - getPrevPan()) * partialTicks;
    }

    @Override
    public float getPartialTiltDeg(float partialTicks) {
        return getPrevTilt() + (getTilt() - getPrevTilt()) * partialTicks;
    }
}
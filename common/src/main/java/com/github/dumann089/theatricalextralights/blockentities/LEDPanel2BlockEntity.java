package com.github.dumann089.theatricalextralights.blockentities;

import com.github.dumann089.theatricalextralights.fixtures.Fixtures;
import dev.imabad.theatrical.api.Fixture;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.github.dumann089.theatricalextralights.util.BlockEntitySync;

import java.util.Arrays;

public class LEDPanel2BlockEntity extends ExtraLightsLightBlockEntity {
    public LEDPanel2BlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.LED_PANEL_2.get(), blockPos, blockState);
        setChannelCount(4);
    }

    @Override
    public Fixture getFixture() {
        return Fixtures.LED_PANEL_2.get();
    }

    @Override
    public void consume(byte[] dmxValues) {
        int start = this.getChannelStart() > 0 ? this.getChannelStart() - 1 : 0;
        byte[] ourValues = Arrays.copyOfRange(dmxValues, start,
                start+ this.getChannelCount());
        if(ourValues.length < 4){
            return;
        }
                boolean prevAdvanced = beginDmxUpdate();
        int _pi = intensity, _pr = red, _pg = green, _pb = blue, _pf = focus, _pp = pan, _pt = tilt;
        intensity = convertByteToInt(ourValues[0]);
        red = convertByteToInt(ourValues[1]);
        green = convertByteToInt(ourValues[2]);
        blue = convertByteToInt(ourValues[3]);
        setChanged();
        BlockEntitySync.sendData(this);
    }

    @Override
    public int getDeviceTypeId() {
        return 0x03;
    }

    @Override
    public String getModelName() {
        return "LED Panel 2";
    }

    @Override
    public ResourceLocation getFixtureId() {
        return Fixtures.LED_PANEL_2.getId();
    }

    @Override
    public int getActivePersonality() {
        return 0;
    }

    public int convertByteToInt(byte val) {
        return Byte.toUnsignedInt(val);
    }

    @Override
    public float getMaxLightDistance() {
        return 1;
    }

    @Override
    public String getTranslationKey() {
        return "block.theatricalextralights.led_panel_2";
    }
}

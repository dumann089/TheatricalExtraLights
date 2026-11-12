package com.github.dumann089.theatricalextralights.util;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import net.minecraft.resources.ResourceLocation;

public final class GoboFieldAngleSupport {

    private GoboFieldAngleSupport() {
    }

    public static boolean supports(ExtraLightsLightBlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }

        ResourceLocation fixtureId = blockEntity.getFixtureId();

        if (fixtureId == null) {
            return false;
        }

        String path = fixtureId.getPath();

        return path.equals("led_fountain") || path.startsWith("par56_");
    }
}
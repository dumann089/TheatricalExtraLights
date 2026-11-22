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

        String path = fixtureId.getPath().replace("_", "").replace("-", "");
        return path.equals("ledfountain")
                || path.startsWith("par56")
                || path.equals("parscroller")
                || path.startsWith("par1000");
    }
}

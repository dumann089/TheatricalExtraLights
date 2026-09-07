package com.github.dumann089.theatricalextralights.client;

import com.github.dumann089.theatricalextralights.TheatricalExtraLightsScreens;
import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.FollowspotConsoleBlockEntity;
import com.github.dumann089.theatricalextralights.blockentities.LedFacadeBlockEntity;
import com.github.dumann089.theatricalextralights.client.gui.ExtraLightsConfigScreen;
import com.github.dumann089.theatricalextralights.client.gui.FixtureMountScreen;
import com.github.dumann089.theatricalextralights.client.gui.FollowspotConsoleScreen;
import com.github.dumann089.theatricalextralights.client.gui.LedFacadeScreen;
import com.github.dumann089.theatricalextralights.client.gui.WaterJetConfigScreen;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Environment(EnvType.CLIENT)
public class ExtraLightsClientScreens {

    public static void open(TheatricalExtraLightsScreens screenType, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        BlockEntity blockEntity = mc.level.getBlockEntity(pos);

        if (blockEntity == null) {
            return;
        }

        switch (screenType) {
            case FOLLOWSPOT_CONSOLE -> {
                if (blockEntity instanceof FollowspotConsoleBlockEntity console) {
                    mc.setScreen(new FollowspotConsoleScreen(console, pos));
                }
                return;
            }

            case MOUNT_WRENCH -> {
                if (blockEntity instanceof ExtraLightsLightBlockEntity mountable) {
                    mc.setScreen(new FixtureMountScreen(mountable, pos));
                }
                return;
            }

            case LED_FACADE -> {
                if (blockEntity instanceof LedFacadeBlockEntity facade) {
                    mc.setScreen(new LedFacadeScreen(facade, pos));
                }
                return;
            }
        }

        if (!(blockEntity instanceof BaseDMXConsumerLightBlockEntity lightBE)) {
            return;
        }

        Screen screen = switch (screenType) {
            case WATER_GENERIC, WATER_MANUAL, WATER_CONE -> {
                WaterJetConfigScreen.Mode mode = switch (screenType) {
                    case WATER_GENERIC -> WaterJetConfigScreen.Mode.GENERIC;
                    case WATER_MANUAL -> WaterJetConfigScreen.Mode.MANUAL;
                    case WATER_CONE -> WaterJetConfigScreen.Mode.CONE;
                    default -> throw new IllegalStateException("Unexpected screen type: " + screenType);
                };

                yield new WaterJetConfigScreen(
                        lightBE,
                        pos,
                        lightBE.getTranslationKey(),
                        mode
                );
            }

            case CHANNEL_MENU -> new ExtraLightsConfigScreen(
                    lightBE,
                    pos,
                    lightBE.getTranslationKey(),
                    false
            );

            case CHANNEL_PANTILT -> new ExtraLightsConfigScreen(
                    lightBE,
                    pos,
                    lightBE.getTranslationKey(),
                    true
            );

            case MOUNT_WRENCH, FOLLOWSPOT_CONSOLE, LED_FACADE -> null;
        };

        if (screen != null) {
            mc.setScreen(screen);
        }
    }
}
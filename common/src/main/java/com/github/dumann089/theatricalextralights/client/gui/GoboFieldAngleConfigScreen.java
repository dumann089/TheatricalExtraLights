
package com.github.dumann089.theatricalextralights.client.gui;

import com.github.dumann089.theatricalextralights.blockentities.ExtraLightsLightBlockEntity;
import com.github.dumann089.theatricalextralights.util.GoboFieldAngleSupport;
import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public final class GoboFieldAngleConfigScreen extends ExtraLightsConfigScreen {

    public GoboFieldAngleConfigScreen(
            BaseDMXConsumerLightBlockEntity blockEntity,
            BlockPos pos,
            String title
    ) {
        super(blockEntity, pos, title, true);
    }

    @Override
    protected int buildExtraWidgets(int y) {
        if (!(blockEntity instanceof ExtraLightsLightBlockEntity light)
                || !GoboFieldAngleSupport.supports(light)) {
            return y;
        }

        addRenderableWidget(
                new TelUi.FlatButton(
                        contentLeft,
                        y,
                        contentWidth,
                        WIDGET_HEIGHT,
                        Component.literal("Gobo Field Angle"),
                        TelUi.ButtonStyle.NORMAL,
                        button -> Minecraft.getInstance().setScreen(
                                new GoboFieldAngleScreen(
                                        light,
                                        pos,
                                        this
                                )
                        )
                )
        );

        return y + WIDGET_HEIGHT + SECTION_GAP;
    }
}

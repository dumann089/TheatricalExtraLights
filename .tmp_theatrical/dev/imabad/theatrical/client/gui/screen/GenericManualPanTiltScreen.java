package dev.imabad.theatrical.client.gui.screen;

import dev.imabad.theatrical.blockentities.light.BaseDMXConsumerLightBlockEntity;
import dev.imabad.theatrical.client.gui.widgets.BasicSlider;
import dev.imabad.theatrical.net.UpdateFixturePosition;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_7842;
import net.minecraft.class_7847;

public class GenericManualPanTiltScreen extends GenericDMXConfigurationScreen<BaseDMXConsumerLightBlockEntity> {
    private BasicSlider tiltSlider, panSlider;
    private final BaseDMXConsumerLightBlockEntity be;

    public GenericManualPanTiltScreen(BaseDMXConsumerLightBlockEntity be, String translationKey) {
        super(be, be.method_11016(), translationKey);
        this.be = be;
    }

    @Override
    public void addExtraWidgetsToUI() {
        this.tiltSlider = new BasicSlider(xCenter + 13, yCenter + 45, 150, 20, class_2561.method_43473(), be.getTilt(), -90, 90, (newTilt) -> {
            be.setTilt(newTilt.intValue());
        });
        this.panSlider = new BasicSlider(xCenter, yCenter + 75, 150, 20, class_2561.method_43473(), be.getPan(),-180, 180, (newPan) -> {
            be.setPan(newPan.intValue());
        });
        class_7847 layoutSettings = layout.method_46499().method_46479(2);
        layout.method_46496(new class_7842(class_2561.method_43471("fixture.tilt"), field_22793), layoutSettings);
        layout.method_46496(tiltSlider, layoutSettings);
        layout.method_46496(new class_7842(class_2561.method_43471("fixture.pan"), field_22793), layoutSettings);
        layout.method_46496(panSlider, layoutSettings);
    }

    @Override
    protected void update() {
        super.update();
        new UpdateFixturePosition(be.method_11016(), be.getTilt(), be.getPan()).sendToServer();
    }

    @Override
    protected void renderLabels(class_332 guiGraphics) {
        super.renderLabels(guiGraphics);
    }
}
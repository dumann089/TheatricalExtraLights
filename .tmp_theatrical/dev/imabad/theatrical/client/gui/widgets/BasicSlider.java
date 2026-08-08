package dev.imabad.theatrical.client.gui.widgets;

import java.util.function.Consumer;
import net.minecraft.class_2561;
import net.minecraft.class_3532;
import net.minecraft.class_357;

public class BasicSlider extends class_357 {

    private class_2561 initialMessage;
    private double minValue, maxValue;
    private final Consumer<Double> applyValue;
    public BasicSlider(int x, int y, int width, int height, class_2561 message, double value, double minValue, double maxValue, Consumer<Double> applyValue) {
        super(x, y, width, height, message, value);
        this.initialMessage = message;
        this.applyValue = applyValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.field_22753 = class_3532.method_33722(value, this.minValue, this.maxValue, 0D, 1D);
        method_25346();
    }
    public double getValue() {
        return Math.round(this.field_22753 * (maxValue - minValue) + minValue);
    }

    @Override
    protected void method_25346() {
        this.method_25355(class_2561.method_43473().method_27693(getValue() + ""));
    }

    @Override
    protected void method_25344() {
        applyValue.accept(getValue());
    }
}

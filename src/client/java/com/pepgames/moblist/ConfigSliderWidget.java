package com.pepgames.moblist;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ConfigSliderWidget extends SliderWidget {
    private final ValueChangeListener listener;

    public interface ValueChangeListener {
        void onValueChanged(double value);
    }

    public ConfigSliderWidget(int x, int y, int width, int height, float initialValue, ValueChangeListener listener) {
        super(x, y, width, height, Text.of("Scale"), initialValue);
        this.listener = listener;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal("Scale: " + String.format("%.2f", value))); // e.g., "Scale: 1.00"
    }

    @Override
    protected void applyValue() {
        listener.onValueChanged(value);
    }

    public void setSliderValue(float newValue) {
        this.value = newValue;
        updateMessage();
    }
}

package wfg.wrap_ui.ui.plugins;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;

import wfg.wrap_ui.ui.panels.Slider;

public class SliderPlugin extends CustomPanelPlugin<Slider, SliderPlugin> {
    @Override
    public void advance(float amount) {
        super.advance(amount);
        getPanel().advanceImpl(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);
        getPanel().processInputImpl(inputSnapshot);
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        getPanel().renderImpl(alphaMult);
    }
}
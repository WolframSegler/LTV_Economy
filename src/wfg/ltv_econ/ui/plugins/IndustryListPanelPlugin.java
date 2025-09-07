package wfg.ltv_econ.ui.plugins;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;

import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;

public class IndustryListPanelPlugin extends LtvCustomPanelPlugin<LtvIndustryListPanel, IndustryListPanelPlugin>  {
    
    @Override
    public void advance(float amount) {
        super.advance(amount);

        getPanel().advanceImpl(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);

        getPanel().processInputImpl(events);
    }
}

package wfg.ltv_econ.ui.plugins;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;

import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class IndustryListPanelPlugin extends CustomPanelPlugin<LtvIndustryListPanel, IndustryListPanelPlugin>  {
    
    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);

        getPanel().processInputImpl(events);
    }
}

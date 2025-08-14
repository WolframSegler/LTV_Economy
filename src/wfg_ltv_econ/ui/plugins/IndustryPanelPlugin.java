package wfg_ltv_econ.ui.plugins;

import wfg_ltv_econ.ui.panels.LtvIndustryWidget;

public class IndustryPanelPlugin extends LtvCustomPanelPlugin<LtvIndustryWidget, IndustryPanelPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);

        getPanel().renderImpl(alphaMult);
    }
}

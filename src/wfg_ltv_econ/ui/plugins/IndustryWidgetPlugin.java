package wfg_ltv_econ.ui.plugins;

import wfg_ltv_econ.ui.panels.LtvIndustryWidget;

public class IndustryWidgetPlugin extends LtvCustomPanelPlugin<LtvIndustryWidget, IndustryWidgetPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);

        getPanel().renderImpl(alphaMult);
    }
}

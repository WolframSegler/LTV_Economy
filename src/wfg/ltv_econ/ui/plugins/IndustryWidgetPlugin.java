package wfg.ltv_econ.ui.plugins;

import wfg.ltv_econ.ui.panels.LtvIndustryWidget;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class IndustryWidgetPlugin extends CustomPanelPlugin<LtvIndustryWidget, IndustryWidgetPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);

        getPanel().renderImpl(alphaMult);
    }
}

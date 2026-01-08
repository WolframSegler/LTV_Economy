package wfg.ltv_econ.ui.plugins;

import wfg.ltv_econ.ui.panels.IndustryWidget;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class IndustryWidgetPlugin extends CustomPanelPlugin<IndustryWidget, IndustryWidgetPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);

        getPanel().renderImpl(alphaMult);
    }
}

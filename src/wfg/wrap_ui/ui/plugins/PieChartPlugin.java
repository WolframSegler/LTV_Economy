package wfg.wrap_ui.ui.plugins;

import wfg.wrap_ui.ui.panels.PieChart;

public class PieChartPlugin extends CustomPanelPlugin<PieChart, PieChartPlugin> {
    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        getPanel().renderImpl(alphaMult);
    }
}
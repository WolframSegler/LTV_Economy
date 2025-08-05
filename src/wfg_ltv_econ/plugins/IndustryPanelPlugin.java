package wfg_ltv_econ.plugins;

import wfg_ltv_econ.ui.LtvIndustryPanel;

public class IndustryPanelPlugin extends LtvCustomPanelPlugin {
    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);

        ((LtvIndustryPanel)m_panel).renderImpl(alphaMult);
    }
}

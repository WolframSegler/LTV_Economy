package wfg_ltv_econ.ui.plugins;

import com.fs.starfarer.api.ui.CustomPanelAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;

public class BasePanelPlugin<
    PanelType extends LtvCustomPanel<
        ? extends LtvCustomPanelPlugin<?, ? extends BasePanelPlugin<PanelType>>, 
        PanelType,
        CustomPanelAPI
    >
> extends LtvCustomPanelPlugin<PanelType, BasePanelPlugin<PanelType>> {

}

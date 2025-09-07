package wfg.ltv_econ.ui.plugins;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.panels.LtvCustomPanel;

public class BasePanelPlugin<
    PanelType extends LtvCustomPanel<
        ? extends LtvCustomPanelPlugin<?, ? extends BasePanelPlugin<PanelType>>, 
        PanelType,
        ? extends UIPanelAPI
    >
> extends LtvCustomPanelPlugin<PanelType, BasePanelPlugin<PanelType>> {

}

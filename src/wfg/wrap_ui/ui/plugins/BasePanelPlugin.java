package wfg.wrap_ui.ui.plugins;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;

public class BasePanelPlugin<
    PanelType extends CustomPanel<
        ? extends CustomPanelPlugin<?, ? extends BasePanelPlugin<PanelType>>, 
        PanelType,
        ? extends UIPanelAPI
    >
> extends CustomPanelPlugin<PanelType, BasePanelPlugin<PanelType>> {}
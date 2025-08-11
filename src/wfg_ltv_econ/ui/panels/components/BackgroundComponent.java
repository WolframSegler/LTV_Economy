package wfg_ltv_econ.ui.panels.components;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg_ltv_econ.util.RenderUtils;

public final class BackgroundComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & HasBackground
> extends BaseComponent<PluginType, PanelType> {

    public BackgroundComponent(PluginType a) {
        super(a);
    }

    @Override
    public final void renderBelow(float alphaMult, InputSnapshot input) {
        if (!getPanel().isBgEnabled()) {
            return;
        }
        final var pos = getPanel().getPos();

        final int x = (int) pos.getX() + getPlugin().offsetX;
        final int y = (int) pos.getY() + getPlugin().offsetY;
        final int w = (int) pos.getWidth() + getPlugin().offsetW;
        final int h = (int) pos.getHeight() + getPlugin().offsetH;

        RenderUtils.drawQuad(x, y, w, h, getPanel().getBgColor(), alphaMult * getPanel().getBgTransparency(), false);
    }
}
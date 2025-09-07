package wfg.ltv_econ.ui.systems;

import wfg.ltv_econ.ui.panels.LtvCustomPanel;
import wfg.ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg.ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg.ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg.ltv_econ.util.RenderUtils;

public final class BackgroundSystem<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & HasBackground
> extends BaseSystem<PluginType, PanelType> {

    public BackgroundSystem(PluginType a) {
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

        RenderUtils.drawQuad(x, y, w, h, getPanel().getBgColor(), getPanel().getBgTransparency(), false);
    }
}
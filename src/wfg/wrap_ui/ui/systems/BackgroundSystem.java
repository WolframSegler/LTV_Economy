package wfg.wrap_ui.ui.systems;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;
import wfg.wrap_ui.util.RenderUtils;

public final class BackgroundSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & HasBackground
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

        RenderUtils.drawQuad(x, y, w, h, getPanel().getBgColor(), getPanel().getBgAlpha(), false);
    }
}
package wfg.wrap_ui.ui.systems;

import com.fs.starfarer.api.ui.PositionAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasOutline;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;
import wfg.wrap_ui.util.RenderUtils;
import wfg.wrap_ui.util.WrapUiUtils;

public final class OutlineSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & HasOutline
> extends BaseSystem<PluginType, PanelType> {

    public static enum Outline {
        NONE,
        LINE,
        VERY_THIN,
        THIN,
        MEDIUM,
        THICK,
        TEX_VERY_THIN,
        TEX_THIN,
        TEX_MEDIUM,
        TEX_THICK
    }

    public static final int pad = 3;

    public OutlineSystem(PluginType plugin) {
        super(plugin);
    }

    @Override
    public final void render(float alphaMult, InputSnapshot input) {
        if (getPanel().getOutline() == null || getPanel().getOutline() == Outline.NONE) return;

        PanelType panel = getPanel();
        final PositionAPI pos = panel.getPos();

        String textureID = null;
        int textureSize = 4;
        int borderThickness = 0;

        switch (getPanel().getOutline()) {
            case LINE: borderThickness = 1; break;
            case VERY_THIN: borderThickness = 2; break;
            case THIN: borderThickness = 3; break;
            case MEDIUM: borderThickness = 4; break;
            case THICK: borderThickness = 8; break;
            case TEX_VERY_THIN: textureID = "ui_border4"; break;
            case TEX_THIN: textureID = "ui_border3"; break;
            case TEX_MEDIUM:
                textureID = "ui_border1";
                textureSize = 8;
                break;
            case TEX_THICK:
                textureID = "ui_border2";
                textureSize = 24;
                break;
            default:
                break;
        }

        if (borderThickness != 0) {
            RenderUtils.drawFramedBorder(
                pos.getX() + getPlugin().offsetX,
                pos.getY() + getPlugin().offsetY,
                pos.getWidth() + getPlugin().offsetW,
                pos.getHeight() + getPlugin().offsetH,
                borderThickness,
                getPanel().getOutlineColor(),
                alphaMult
            );
        }

        if (textureID != null) {
            WrapUiUtils.drawRoundedBorder(
                pos.getX() - pad + getPlugin().offsetX,
                pos.getY() - pad + getPlugin().offsetY,
                pos.getWidth() + pad * 2 + getPlugin().offsetW,
                pos.getHeight() + pad * 2 + getPlugin().offsetH,
                1,
                textureID,
                textureSize,
                getPanel().getOutlineColor()
            );
        }
    }
}

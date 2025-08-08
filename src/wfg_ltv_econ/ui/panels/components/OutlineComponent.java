package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasOutline;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;

public final class OutlineComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType> & HasOutline
> extends BaseComponent<PluginType, PanelType> {

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

    public OutlineComponent(PluginType plugin) {
        super(plugin);
    }

    @Override
    public void render(float alphaMult, InputSnapshot input) {
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
            UiUtils.drawRoundedBorder(
                pos.getX() - pad,
                pos.getY() - pad,
                pos.getWidth() + pad * 2,
                pos.getHeight() + pad * 2,
                1,
                textureID,
                textureSize,
                getPanel().getOutlineColor()
            );
        }
    }
}

package wfg_ltv_econ.ui;

import java.awt.Color;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;

/**
 * Each instance implements createPanel, createTooltip etc. when creating the class.
 * The class just serves as a template.
 */
public class LtvTextPanel extends LtvCustomPanel implements LtvCustomPanel.TooltipProvider {

    public Color m_textColor;

    /**
     * Used by anonymous classes to transmit data
     */
    public ButtonAPI m_checkbox;
    public float textX1 = 0;
    public float textX2 = 0;
    public float textY1 = 0;
    public float textY2 = 0;
    public float textW1 = 0;
    public float textW2 = 0;
    public float textH1 = 0;
    public float textH2 = 0;

    public LtvTextPanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin, Color textColor) {
        super(root, parent, width, height, plugin, market);

        this.m_textColor = textColor;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvCustomPanelPlugin plugin = ((LtvCustomPanelPlugin) m_panel.getPlugin());
        plugin.init(this, GlowType.NONE, true, false, false);
    }

    public void createPanel() {

    }

    @Override
    public TooltipMakerAPI createTooltip() {
        return null;
    }

    @Override
    public void removeTooltip(TooltipMakerAPI tooltip) {

    }

    @Override
    public void attachCodexTooltip(TooltipMakerAPI codexTooltip) {

    }
}

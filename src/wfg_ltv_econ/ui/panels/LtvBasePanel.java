package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.ui.panels.LtvCustomPanel.ColoredPanel;
import wfg_ltv_econ.ui.ui_plugins.LtvBasePanelPlugin;
import wfg_ltv_econ.ui.ui_plugins.LtvCustomPanelPlugin.Glow;
import wfg_ltv_econ.ui.ui_plugins.LtvCustomPanelPlugin.Outline;

/**
 * An empty implementation of LtvCustomPanel
 */
public class LtvBasePanel extends LtvCustomPanel<LtvBasePanelPlugin> implements ColoredPanel{

    public Color BgColor = new Color(0, 0, 0, 255);
    public Color glowColor = getFaction().getBaseUIColor();
    public Color outlineColor = Misc.getDarkPlayerColor();

    public LtvBasePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        LtvBasePanelPlugin plugin) {
        super(root, parent, width, height, plugin, market);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this, Glow.NONE, false, false, Outline.NONE);
    }

    public void createPanel() {

    }

    public void setBgColor(Color color) {
        BgColor = color;

        getPlugin().setHasBackground(true);
    }
    public void setGlowColor(Color color) {
        glowColor = color;

        getPlugin().setHasGlow(Glow.OVERLAY);
    }
    public void setOutlineColor(Color a) {
        outlineColor = a;

        getPlugin().setOutline(Outline.LINE);
    }
    public Color getBgColor() {
        return BgColor;
    }
    public Color getOutlineColor() {
        return outlineColor;
    }
    public Color getGlowColor() {
        return glowColor;
    }
}

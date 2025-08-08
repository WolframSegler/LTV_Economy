package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.ui.panels.LtvCustomPanel.ColoredPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.plugins.LtvBasePanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.Glow;

/**
 * An empty implementation of LtvCustomPanel
 */
public class LtvBasePanel extends LtvCustomPanel<LtvBasePanelPlugin, LtvBasePanel> implements ColoredPanel, HasBackground{

    public Color BgColor = Color.BLACK;
    public boolean isBgEnabled = true;
    public Color glowColor = getFaction().getBaseUIColor();

    public LtvBasePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        LtvBasePanelPlugin plugin) {
        super(root, parent, width, height, plugin, market);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this, Glow.NONE);
    }

    public void createPanel() {

    }

    public void setBgColor(Color color) {
        BgColor = color;

        isBgEnabled = true;
    }
    public boolean isBgEnabled() {
        return true;
    }
    public void setGlowColor(Color color) {
        glowColor = color;

        getPlugin().setHasGlow(Glow.OVERLAY);
    }
    public Color getGlowColor() {
        return glowColor;
    }
}

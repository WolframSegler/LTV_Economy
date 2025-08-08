package wfg_ltv_econ.ui.panels;

import java.awt.Color;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasBackground;
import wfg_ltv_econ.ui.plugins.BasePanelPlugin;

/**
 * An empty implementation of LtvCustomPanel
 */
public class LtvBasePanel extends LtvCustomPanel<BasePanelPlugin<LtvBasePanel>, LtvBasePanel> implements HasBackground{

    public Color BgColor = Color.BLACK;
    public boolean isBgEnabled = true;

    public LtvBasePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        BasePanelPlugin<LtvBasePanel> plugin) {
        super(root, parent, width, height, plugin, market);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
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
}

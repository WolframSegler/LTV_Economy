package wfg_ltv_econ.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.plugins.LtvCustomPanelPlugin.GlowType;

/**
 * An empty implementation of LtvCustomPanel
 */
public class LtvBasePanel extends LtvCustomPanel{

    public LtvBasePanel(UIPanelAPI root, UIPanelAPI parent, MarketAPI market, int width, int height,
        CustomUIPanelPlugin plugin) {
        super(root, parent, width, height, plugin, market);

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        LtvCustomPanelPlugin plugin = ((LtvCustomPanelPlugin) m_panel.getPlugin());
        plugin.init(this, GlowType.NONE, false, false, false);
    }

    public void createPanel() {

    }
}

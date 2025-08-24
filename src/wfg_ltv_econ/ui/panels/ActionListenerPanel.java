package wfg_ltv_econ.ui.panels;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasActionListener;
import wfg_ltv_econ.ui.plugins.BasePanelPlugin;

/**
 * Empty panel for passing in a listener for cases where an LtvCustomPanel is not available.
 * Override the HasActionListener methods
 */
public class ActionListenerPanel extends LtvCustomPanel<
    BasePanelPlugin<ActionListenerPanel>, ActionListenerPanel, UIPanelAPI
> implements HasActionListener {

    public ActionListenerPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(root, parent, width, width, new BasePanelPlugin<>(), market);
    }

    public void createPanel() {}

    public void initializePlugin(boolean hasPlugin) {}
}

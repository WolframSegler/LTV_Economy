package wfg.wrap_ui.ui.panels;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;

/**
 * Empty panel for passing in a listener for cases where a {@link CustomPanel} is not available.
 * Override the {@link HasActionListener} methods
 */
public class ActionListenerPanel extends CustomPanel<
    BasePanelPlugin<ActionListenerPanel>, ActionListenerPanel, UIPanelAPI
> implements HasActionListener {

    public ActionListenerPanel(UIPanelAPI root, UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, width, new BasePanelPlugin<>(), market);
    }

    public void createPanel() {}

    public void initializePlugin(boolean hasPlugin) {}
}

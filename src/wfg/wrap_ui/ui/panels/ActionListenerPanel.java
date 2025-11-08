package wfg.wrap_ui.ui.panels;

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

    public ActionListenerPanel(UIPanelAPI parent, int width, int height) {
        super(parent, width, width, new BasePanelPlugin<>());
    }

    public void createPanel() {}

    public void initializePlugin(boolean hasPlugin) {}
}

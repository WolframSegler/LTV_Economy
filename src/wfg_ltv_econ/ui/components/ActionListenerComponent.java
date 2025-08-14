package wfg_ltv_econ.ui.components;

import org.lwjgl.input.Keyboard;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.AcceptsActionListener;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;

public final class ActionListenerComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & AcceptsActionListener
> extends BaseComponent<PluginType, PanelType>{

    boolean shortcutKeyDown = false;

    public ActionListenerComponent(PluginType plugin) {
        super(plugin);
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {
        getPanel().getActionListener().ifPresent(listener -> {
            if (!listener.isListenerEnabled()) {
                return;
            }
            
            if (input.LMBUpLastFrame) {
                listener.onClicked(getPanel(), true);
            }

            if (input.RMBUpLastFrame) {
                listener.onClicked(getPanel(), false);
            }

            listener.getShortcut().ifPresent(shortcut -> {
                if (Keyboard.isKeyDown(shortcut)) {
                    if (!shortcutKeyDown) {
                        listener.onShortcutPressed(getPanel());
                    }
                    shortcutKeyDown = true;
                } else {
                    shortcutKeyDown = false;
                }
            });

            if (input.hoverStarted) {
                listener.onHoverStarted(getPanel());
            }

            if (input.hoveredLastFrame) {
                listener.onHover(getPanel());
            }

            if (input.hoverEnded) {
                listener.onHoverEnded(getPanel());
            }
        });
    }
}

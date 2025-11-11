package wfg.wrap_ui.ui.systems;

import org.lwjgl.input.Keyboard;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;

public final class ActionListenerSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & AcceptsActionListener
> extends BaseSystem<PluginType, PanelType>{

    boolean shortcutKeyDown = false;

    public ActionListenerSystem(PluginType plugin) {
        super(plugin);
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {
        getPanel().getActionListener().ifPresent(listener -> {
            if (!listener.isListenerEnabled()) return;
            
            if (input.LMBUpLastFrame && input.hoveredLastFrame) {
                listener.onClicked(getPanel(), true);
            }

            if (input.RMBUpLastFrame && input.hoveredLastFrame) {
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

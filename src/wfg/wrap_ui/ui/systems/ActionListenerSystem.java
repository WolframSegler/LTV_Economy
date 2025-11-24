package wfg.wrap_ui.ui.systems;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;

public final class ActionListenerSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & AcceptsActionListener
> extends BaseSystem<PluginType, PanelType>{

    public ActionListenerSystem(PluginType plugin) {
        super(plugin);
    }

    @Override
    public void processInput(List<InputEventAPI> events, InputSnapshot input) {
        getPanel().getActionListener().ifPresent(listener -> {
            if (!listener.isListenerEnabled()) return;
            
            if (input.LMBUpLastFrame && input.hoveredLastFrame) {
                listener.onClicked(getPanel(), true);
            }

            if (input.RMBUpLastFrame && input.hoveredLastFrame) {
                listener.onClicked(getPanel(), false);
            }

            if (listener.getShortcut() > 0 && getPlugin().isValidUIContext()) {
                for (InputEventAPI event : events) {
                    if (!event.isConsumed() && event.isKeyDownEvent() &&
                        event.getEventValue() == listener.getShortcut()
                    ) {
                        listener.onShortcutPressed(getPanel());
                    }
                }
            };

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

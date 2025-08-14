package wfg_ltv_econ.ui.panels.components;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.input.InputEventAPI;

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
    public final void processInput(List<InputEventAPI> events, InputSnapshot input) {
        getPanel().getActionListener().ifPresent(listener -> {
            if (input.LMBUpLastFrame) {
                listener.onClicked(getPanel());
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

            for (InputEventAPI event : events) {
                listener.actionPerformed(event, getPanel());
            }
        });
    }
}

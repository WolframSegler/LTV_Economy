package wfg_ltv_econ.ui.panels.components;

import com.fs.starfarer.api.Global;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasAudioFeedback;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;

public final class AudioFeedbackComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & HasAudioFeedback
> extends BaseComponent<PluginType, PanelType> {

    public AudioFeedbackComponent(PluginType plugin) {
        super(plugin);
    }

    @Override
    public void advance(float amount, InputSnapshot input) {
        if (input.hoveredLastFrame && getPanel().isSoundEnabled() && getPlugin().isValidUIContext()) {
            if (!input.playedUIHoverSound) {
                Global.getSoundPlayer().playUISound("ui_button_mouseover", 1, 1);
                input.playedUIHoverSound = true;
            }
            if (input.LMBUpLastFrame) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1, 1);
            }
        }
    }
}

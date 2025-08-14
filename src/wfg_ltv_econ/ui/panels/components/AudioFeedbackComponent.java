package wfg_ltv_econ.ui.panels.components;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;

import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasAudioFeedback;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.plugins.LtvCustomPanelPlugin.InputSnapshot;

public final class AudioFeedbackComponent<
    PluginType extends LtvCustomPanelPlugin<PanelType, PluginType>,
    PanelType extends LtvCustomPanel<PluginType, PanelType, ?> & HasAudioFeedback
> extends BaseComponent<PluginType, PanelType> {

    /**
     * Newly created Components shouldn't make a sound for this many game ticks.
     */
    final private int initCompTicks = 10;
    private long accumulatedGameTicks = 0;

    public boolean playedUIHoverSound = false;

    public AudioFeedbackComponent(PluginType plugin) {
        super(plugin);
    }

    @Override
    public final void advance(float amount, InputSnapshot input) {
        if (input.hoveredLastFrame &&
            getPanel().isSoundEnabled() &&
            getPlugin().isValidUIContext() &&
            accumulatedGameTicks > initCompTicks
        ) {
            if (!playedUIHoverSound) {
                Global.getSoundPlayer().playUISound("ui_button_mouseover", 1, 1);
                playedUIHoverSound = true;
            }
            if (input.LMBUpLastFrame) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1, 1);
            }
        }

        accumulatedGameTicks++;
    }

    @Override
    public final void processInput(List<InputEventAPI> events, InputSnapshot input) {
        if (!input.hoveredLastFrame) {
            playedUIHoverSound = false;
        }
    }
}

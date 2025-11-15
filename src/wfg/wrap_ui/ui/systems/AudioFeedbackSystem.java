package wfg.wrap_ui.ui.systems;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;

import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin.InputSnapshot;

public final class AudioFeedbackSystem<
    PluginType extends CustomPanelPlugin<PanelType, PluginType>,
    PanelType extends CustomPanel<PluginType, PanelType, ?> & HasAudioFeedback
> extends BaseSystem<PluginType, PanelType> {

    /**
     * Newly created Systems shouldn't make a sound for this many game ticks.
     */
    final private int initCompTicks = 10;
    private long accumulatedGameTicks = 0;

    public boolean playedUIHoverSound = false;

    public AudioFeedbackSystem(PluginType plugin) {
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
                Global.getSoundPlayer().playUISound(getPanel().getMouseOverSound(), 1, 1);
                playedUIHoverSound = true;
            }
            if (input.LMBUpLastFrame) {
                Global.getSoundPlayer().playUISound(getPanel().getButtonPressedSound(), 1, 1);
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

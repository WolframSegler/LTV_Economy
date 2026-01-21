package wfg.ltv_econ.ui.panels.reusable;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.SpritePanel;
import wfg.wrap_ui.ui.components.AudioFeedbackComp;
import wfg.wrap_ui.ui.components.InteractionComp;
import wfg.wrap_ui.ui.components.NativeComponents;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasInteraction;

public class SettingsIcon extends SpritePanel<SettingsIcon> implements 
    HasAudioFeedback, HasInteraction
{
    public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
    public final InteractionComp<SettingsIcon> interaction = comp().get(NativeComponents.INTERACTION);

    public static final String SETTINGS_ICON = Global.getSettings()
        .getSpriteName("ui", "settings");

    public SettingsIcon(UIPanelAPI parent, int size, Color color) {
        super(parent, size, size, SETTINGS_ICON,
            color == null ? base : color, null
        );
    }
}
package wfg.ltv_econ.ui.reusable;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.native_ui.ui.visual.SpritePanel;
import wfg.native_ui.ui.component.AudioFeedbackComp;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIElementFlags.HasAudioFeedback;
import wfg.native_ui.ui.core.UIElementFlags.HasInteraction;

public class SettingsIcon extends SpritePanel<SettingsIcon> implements 
    HasAudioFeedback, HasInteraction
{
    private static final SpriteAPI SETTINGS_ICON = settings.getSprite("ui", "settings");

    public final AudioFeedbackComp audio = comp().get(NativeComponents.AUDIO_FEEDBACK);
    public final InteractionComp<SettingsIcon> interaction = comp().get(NativeComponents.INTERACTION);

    public SettingsIcon(UIPanelAPI parent, int size, Color color) {
        super(parent, size, size, SETTINGS_ICON,
            color == null ? base : color, null
        );
    }
}
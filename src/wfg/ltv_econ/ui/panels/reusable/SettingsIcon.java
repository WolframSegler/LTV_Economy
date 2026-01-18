package wfg.ltv_econ.ui.panels.reusable;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.wrap_ui.ui.panels.SpritePanel;
import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;

public class SettingsIcon extends SpritePanel<SettingsIcon> implements 
    HasAudioFeedback, HasActionListener, AcceptsActionListener
{
    public static final String SETTINGS_ICON = Global.getSettings()
        .getSpriteName("ui", "settings");

    public SettingsIcon(UIPanelAPI parent, int size, Color color) {
        super(parent, size, size, SETTINGS_ICON,
            color == null ? base : color, null
        );
    }

    public final Optional<HasActionListener> getActionListener() { return Optional.of(this); }
}
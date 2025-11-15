package wfg.wrap_ui.ui.panels;

import java.util.Optional;
import java.util.function.Supplier;

import org.lwjgl.input.Keyboard;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasBackground;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;

public class Button extends CustomPanel<BasePanelPlugin<Button>, Button, UIPanelAPI> implements 
    HasAudioFeedback, HasFader, HasActionListener, AcceptsActionListener, HasTooltip, HasBackground
{

    public float highlightBrightness = 1.2f;
    public boolean checked = false;
    public boolean disabled = false;
    public boolean quickMode = false;
    public boolean clickable = true;
    public boolean showTooltipWhileInactive = false;
    public boolean rightClicksOkWhenDisabled = false;
    public boolean performActionWhenDisabled = false;
    public boolean tooltipExpanded = false;
    public boolean tooltipEnabled = false;
    public Color bgColor = new Color(20, 125, 200);
    public Color bgDisabledColor = new Color(100, 100, 100);
    public Object customData = null;

    private String labelText = "";
    private String shortcutText = "";
    private String labelFont = "";
    private LabelAPI label = null;
    private Runnable onClick;
    private int shortcut = 0;
    private String buttonPressedSound = "ui_button_pressed";
    private String mouseOverSound = "ui_button_mouseover";
    private boolean appendShortcutToText = true;
    private final FaderUtil fader = new FaderUtil(0, 0, 0.2f, true, true);
    private final PendingTooltip<CustomPanelAPI> tooltip = new PendingTooltip<>();
    
    /**
     * @param onClick default action toggles the checked state.
     */
    public Button(UIPanelAPI parent, int width, int height, String text, String font, Runnable onClick) {
        super(parent, width, height, new BasePanelPlugin<>());

        labelText = text;
        labelFont = font;
        this.onClick = onClick;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();

        label = settings.createLabel(labelText, labelFont);
        
        add(label);
    }

    public Optional<HasActionListener> getActionListener() {
        return Optional.of(this);
    }

    public void click(boolean ignoreState) {
        if (ignoreState) onShortcutPressed(this);
        else onClicked(this, true);
    }

    public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
        if ((!isLeftClick && !rightClicksOkWhenDisabled) || !clickable) return;

        onShortcutPressed(source);
    }

    public void setOnClick(Runnable r) {
        onClick = r;
    }

    public void onShortcutPressed(CustomPanel<?, ?, ?> source) {
        if (disabled && !performActionWhenDisabled) return;
        if (onClick != null) onClick.run();
        if (!quickMode) checked = !checked;
    }

    public int getShortcut() {
        return shortcut;
    }

    /**
     * @param keyCode the key code corresponding to {@link org.lwjgl.input.Keyboard} constants
     */
    public void setShortcut(int keyCode) {
        this.shortcut = keyCode;
    }

    public boolean isSoundEnabled() {
        return !disabled;
    }

    public FaderUtil getFader() {
        return fader;
    }

    public CustomPanelAPI getTpParent() {
        return tooltip.parentSupplier.get();
    }

    public TooltipMakerAPI createAndAttachTp() {
        return tooltip.factory.get();
    }

    public boolean isExpanded() {
        return tooltipExpanded;
    }

    /**
     * Used by {@link wfg.wrap_ui.ui.systems.TooltipSystem} to reset state on condition
     */
    public void setExpanded(boolean a) {
        tooltipExpanded = a;
    }

    public boolean isTooltipEnabled() {
        return tooltipEnabled && (showTooltipWhileInactive || !disabled);
    }

    public String getButtonPressedSound() {
        return buttonPressedSound;
    }

    public String getMouseOverSound() {
        return mouseOverSound;
    }

    public void setButtonPressedSound(String settingsID) {
        buttonPressedSound = settingsID;
    }

    public void setMouseOverSound(String settingsID) {
        mouseOverSound = settingsID;
    }

    public boolean isPersistentGlow() {
        return checked;
    }

    public float getOverlayBrightness() {
        return highlightBrightness;
    }

    public void setText(String text) {
        shortcutText = appendShortcutToText ? Keyboard.getKeyName(shortcut) : "";
        labelText = text;
        label.setText(text + " ["+shortcutText+"]");
        if (appendShortcutToText) {
            label.setHighlightColor(Misc.getHighlightColor());
            label.setHighlight(shortcutText);
        }
    }

    public String getText() {
        return labelText;
    }

    public void setAppendShortcutToText(boolean a) {
        appendShortcutToText = a;
        setText(labelText);
    }

    public void setHighlightBounceDown(boolean bool) {
        fader.setBounceDown(bool);
    }

    public void setTooltipFactory(Supplier<TooltipMakerAPI> factory) {
        tooltip.factory = factory;
        if (tooltip.parentSupplier != null) tooltipEnabled = true;
    }

    public void setParentSupplier(Supplier<CustomPanelAPI> parentSupplier) {
        tooltip.parentSupplier = parentSupplier;
        if (tooltip.factory != null) tooltipEnabled = true;
    }

    public Color getBgColor() {
        return disabled ? bgDisabledColor : bgColor;
    }

    public float getBgTransparency() {
        return disabled ? 0.75f : 0.85f;
    }
}
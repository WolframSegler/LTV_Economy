package wfg.wrap_ui.ui.panels;

import java.util.Optional;
import java.util.function.Supplier;
import java.awt.Color;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import wfg.wrap_ui.ui.panels.CustomPanel.AcceptsActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.panels.CustomPanel.HasAudioFeedback;
import wfg.wrap_ui.ui.panels.CustomPanel.HasFader;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.plugins.ButtonPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.RenderUtils;

public class Button extends CustomPanel<ButtonPlugin, Button, UIPanelAPI> implements 
    HasAudioFeedback, HasFader, HasActionListener, AcceptsActionListener, HasTooltip
{

    public float highlightBrightness = 0.2f;
    public boolean checked = false;
    public boolean disabled = false;
    public boolean quickMode = false;
    public boolean clickable = true;
    public boolean showTooltipWhileInactive = false;
    public boolean rightClicksOkWhenDisabled = false;
    public boolean performActionWhenDisabled = false;
    public boolean tooltipExpanded = false;
    public boolean tooltipEnabled = false;
    public boolean disabledWhileInvisible = true;
    public Color bgColor = Misc.getDarkPlayerColor();
    public Color bgDisabledColor = new Color(17, 52, 62);
    public Color highlightColor = Misc.getBasePlayerColor();
    public Glow highlightType = Glow.OVERLAY;
    public Object customData = null;

    protected String labelText = "";
    protected Alignment labelAlg = Alignment.MID;
    protected String labelFont = "";
    protected LabelAPI label = null;
    protected CallbackRunnable<Button> onClick;
    protected int shortcut = 0;
    protected String buttonPressedSound = "ui_button_pressed";
    protected String mouseOverSound = "ui_button_mouseover";
    protected boolean appendShortcutToText = false;
    protected CutStyle cutStyle = CutStyle.NONE;
    protected int overrideCut = 0;
    protected Color labelColor = Misc.getButtonTextColor();
    protected final FaderUtil fader = new FaderUtil(0, 0, 0.2f, true, true);
    protected final PendingTooltip<CustomPanelAPI> tooltip = new PendingTooltip<>();
    
    /**
     * @param onClick default action toggles the checked state. Otherwise the toggle must be done by the Runnable
     */
    public Button(UIPanelAPI parent, int width, int height, String text, String font,
        CallbackRunnable<Button> onClick
    ) {
        super(parent, width, height, new ButtonPlugin());

        labelText = text;
        labelFont = font;
        this.onClick = onClick;

        initializePlugin(hasPlugin);
        createPanel();
    }

    public void initializePlugin(boolean hasPlugin) {
        getPlugin().init(this);
        getPlugin().setIgnoreUIState(true);
    }

    public void createPanel() {
        final SettingsAPI settings = Global.getSettings();
        final PositionAPI pos = getPos();
        if (label != null) remove(label);

        String finalText = labelText;
        if (appendShortcutToText) finalText = finalText + " [" + Keyboard.getKeyName(shortcut) + "]";
        label = settings.createLabel(finalText, labelFont);
        label.getPosition().setSize(pos.getWidth(), pos.getHeight());
        label.setColor(labelColor);
        label.setAlignment(labelAlg);
        if (appendShortcutToText) {
            label.setHighlightColor(Misc.getHighlightColor());
            label.setHighlight(Keyboard.getKeyName(shortcut));
        }

        add(label);
        final float centerX = pos.getX() + pos.getWidth() / 2f;
        final float centerY = pos.getY() + pos.getHeight() / 2f;
        final float labelW = label.getPosition().getWidth();
        final float labelH = label.getPosition().getHeight();
        label.getPosition().inBL(centerX - labelW / 2f, centerY - labelH / 2f);
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

    public void setOnClick(CallbackRunnable<Button> r) {
        onClick = r;
    }

    public void onShortcutPressed(CustomPanel<?, ?, ?> source) {
        if (disabled && !performActionWhenDisabled) return;
        if (getPanel().getOpacity() <= 0f && !disabledWhileInvisible) return;

        if (onClick != null) {
            onClick.run(this);
        } else if (!quickMode) checked = !checked;

        label.flash(0.2f, 1f);
    }

    public int getShortcut() {
        return shortcut;
    }

    /**
     * @param keyCode the key code corresponding to {@link org.lwjgl.input.Keyboard} constants
     */
    public void setShortcut(int keyCode) {
        shortcut = keyCode;
        appendShortcutToText = true;
        createPanel();
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
        return checked && !disabled && !quickMode;
    }

    public float getOverlayBrightness() {
        return highlightBrightness;
    }

    public void setText(String text) {
        labelText = text;
        createPanel();
    }

    public String getText() {
        return labelText;
    }

    public void setFont(String font) {
        labelFont = font;
        createPanel();
    }

    public void setAppendShortcutToText(boolean a) {
        appendShortcutToText = a;
        createPanel();
    }

    public void setHighlightBounceDown(boolean bool) {
        fader.setBounceDown(bool);
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(Color color) {
        labelColor = color;
        createPanel();
    }

    public Color getGlowColor() {
        return highlightColor;
    }

    public Glow getGlowType() {
        return highlightType;
    }

    public void setAlignment(Alignment alg) {
        labelAlg = alg;
        createPanel();
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

    public float getBgAlpha() {
        return disabled ? 0.8f : 0.9f;
    }

    public void setCutStyle(CutStyle style) {
        cutStyle = style;
    }

    public Button setCutSize(int px) {
        overrideCut = px;
        return this;
    }

    public float[] getFaderMaskVertices() {
        final PositionAPI pos = getPos();
        final float cutSize = computeCut((int) pos.getWidth(), (int) pos.getHeight());

        final float[] cuts = cutStyle.toVector4();
        for (int i = 0; i < 4; i++) cuts[i] *= cutSize;

        return RenderUtils.buildCornersVertices(
            pos.getX() + getPlugin().offsetX,
            pos.getY() + getPlugin().offsetY,
            pos.getWidth() + getPlugin().offsetW,
            pos.getHeight() + getPlugin().offsetH,
            cuts
        );
    }

    public void renderImpl(float alphaMult) {
        final PositionAPI pos = getPos();

        final float x = pos.getX() + getPlugin().offsetX;
        final float y = pos.getY() + getPlugin().offsetY;
        final float w = pos.getWidth() + getPlugin().offsetW;
        final float h = pos.getHeight() + getPlugin().offsetH;
        final float cutSize = computeCut((int) w, (int) h);

        final float[] cuts = cutStyle.toVector4();
        for (int i = 0; i < 4; i++) cuts[i] *= cutSize;
        final float[] verts = RenderUtils.buildCornersVertices(x, y, w, h, cuts);

        RenderUtils.drawPolygon(verts, getBgColor(), alphaMult * getBgAlpha());
    }

    protected float computeCut(int w, int h) {
        if (overrideCut > 0) return overrideCut;
        return Math.min(w, h) * 0.2f;
    }

    public enum CutStyle {
        NONE, TL, TR, BL, BR,
        TL_TR, TL_BL, TL_BR,
        TR_BL, TR_BR, BL_BR,
        TL_TR_BL, TL_TR_BR, TL_BL_BR,
        TR_BL_BR, ALL;

        /**  
         * 4-element array of the corners: [BL, BR, TR, TL]  
         * 1f = cut, 0f = no cut
         */
        public float[] toVector4() {
            switch (this) {
                case TL:       return new float[]{0f, 0f, 0f, 1f};
                case TR:       return new float[]{0f, 0f, 1f, 0f};
                case BL:       return new float[]{1f, 0f, 0f, 0f};
                case BR:       return new float[]{0f, 1f, 0f, 0f};
                case TL_TR:    return new float[]{0f, 0f, 1f, 1f};
                case TL_BL:    return new float[]{1f, 0f, 0f, 1f};
                case TL_BR:    return new float[]{0f, 1f, 0f, 1f};
                case TR_BL:    return new float[]{1f, 0f, 1f, 0f};
                case TR_BR:    return new float[]{0f, 1f, 1f, 0f};
                case BL_BR:    return new float[]{1f, 1f, 0f, 0f};
                case TL_TR_BL: return new float[]{1f, 0f, 1f, 1f};
                case TL_TR_BR: return new float[]{0f, 1f, 1f, 1f};
                case TL_BL_BR: return new float[]{1f, 1f, 0f, 1f};
                case TR_BL_BR: return new float[]{1f, 1f, 1f, 0f};
                case ALL:      return new float[]{1f, 1f, 1f, 1f};
                default:       return new float[]{0f, 0f, 0f, 0f};
            }
        }
    }
}
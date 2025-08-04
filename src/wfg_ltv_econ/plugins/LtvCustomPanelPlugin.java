package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.util.FaderUtil.State;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvCustomPanel.TooltipProvider;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.ui.LtvUIState.UIState;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    public enum Glow {
        NONE,
        ADDITIVE,
        OVERLAY
    }
    public enum Outline {
        NONE,
        LINE,
        VERY_THIN,
        THIN,
        MEDIUM,
        THICK
    }

    protected LtvCustomPanel m_panel;
    protected TooltipMakerAPI m_tooltip = null;
    protected boolean m_hasTooltip = false;
    protected FaderUtil m_fader;
    protected UIState targetUIState = UIState.NONE;
    protected Glow glowType = Glow.NONE;
    protected Outline outlineType = Outline.NONE;

    final protected float overlayBrightness = 1.2f;
    protected float tooltipDelay = 0.3f;
    protected float backgroundTransparency = 0.65f;
    protected boolean persistentGlow = false;
    protected boolean hoveredLastFrame = false;
    protected boolean LMBDownLastFrame = false;
    protected boolean LMBUpLastFrame = false;
    protected boolean hasClickedBefore = false;
    protected boolean hasBackground = false;
    protected boolean ignoreUIState = false;
    protected boolean soundEnabled = false;
    protected boolean playedUIHoverSound = false;

    protected float hoverTime = 0f;
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetW = 0;
    protected int offsetH = 0;

    private final static int pad = 3;

    public void init(LtvCustomPanel panel, Glow gT, boolean hasTooltip, boolean hasBackground,
        Outline outline) {
        m_panel = panel;
        m_hasTooltip = hasTooltip;
        outlineType = outline;
        this.glowType = gT;
        this.hasBackground = hasBackground;

        if (glowType != Glow.NONE) {
            m_fader = new FaderUtil(0, 0, 0.2f, true, true);
        }
    }

    public Glow getGlow() {
        return glowType;
    }

    public void setPersistentGlow(boolean a) {
        persistentGlow = a;
    }

    public void setHasBackground(boolean a) {
        hasBackground = a;
    }

    public void setOutline(Outline a) {
        outlineType = a;
    }

    public FaderUtil getFader() {
        return m_fader;
    }

    public void setTargetUIState(UIState a) {
        targetUIState = a;
    }

    public void setIgnoreUIState(boolean a) {
        ignoreUIState = a;
    }

    public void setSoundEnabled(boolean a) {
        soundEnabled = a;
    }

    public boolean isValidUIContext() {
        return LtvUIState.is(targetUIState) || ignoreUIState; 
    }

    public void setHasGlow(Glow a) {
        glowType = a;
    }

    public TooltipMakerAPI showTooltip() {
        if (m_panel instanceof LtvCustomPanel.TooltipProvider && m_tooltip == null) {
        
            m_tooltip = ((TooltipProvider) m_panel).createTooltip();

            // Might break later. Then just use RenderUtils
            ((StandardTooltipV2Expandable)m_tooltip).setShowBackground(true);
            ((StandardTooltipV2Expandable)m_tooltip).setShowBorder(true);
        }
        
        return m_tooltip;
    }

    public void hideTooltip() {
        if (!(m_panel instanceof LtvCustomPanel.TooltipProvider)) {
            return;
        }

        ((TooltipProvider) m_panel).removeTooltip(m_tooltip);
        m_tooltip = null;
    }

    public void setTooltipActive(boolean a) {
        m_hasTooltip = a;
    }

    public void setTooltipDelay(float tooltipDelay) {
        this.tooltipDelay = tooltipDelay;
    }

    public void positionChanged(PositionAPI position) {

    }

    public void setOffsets(int x, int y, int width, int height) {
        offsetX = x;
        offsetY = y;
        offsetW = width;
        offsetH = height;
    }

    /**
     * 1 for opaque.
     * 0 for transparent.
     */
    public void setBackgroundTransparency(float a) {
        backgroundTransparency = a;
    }

    public void renderBelow(float alphaMult) {
        final PositionAPI pos = m_panel.getPanelPos();

        if (hasBackground) {
            int x = (int)pos.getX() + offsetX;
            int y = (int)pos.getY() + offsetY;
            int w = (int)pos.getWidth() + offsetW;
            int h = (int)pos.getHeight() + offsetH;
            RenderUtils.drawQuad(x, y, w, h, m_panel.BgColor, alphaMult*backgroundTransparency);
            // Looks vanilla like with 0.65f
        }

        if (glowType == Glow.OVERLAY && m_fader.getBrightness() > 0) {
            float glowAmount = overlayBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            m_panel.glowColor, glowAmount);

            if (hasClickedBefore) {
                RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                m_panel.glowColor, glowAmount / 2);
            }
        }
    }

    public void render(float alphaMult) {
        final PositionAPI pos = m_panel.getPanelPos();

        if (outlineType == Outline.LINE) {
            RenderUtils.drawOutline(
                pos.getX(),
                pos.getY(),
                pos.getWidth(),
                pos.getHeight(),
                m_panel.getFaction().getGridUIColor(),
                alphaMult
            );
        }

        if (outlineType == Outline.VERY_THIN) {
            UiUtils.drawRoundedBorder(
                pos.getX() - pad,
                pos.getY() - pad,
                pos.getWidth() + pad*2,
                pos.getHeight() + pad*2,
                1,
                "ui_border4",
                4,
                m_panel.getFaction().getBaseUIColor()
            );
        }

        if (outlineType == Outline.THIN) {
            UiUtils.drawRoundedBorder(
                pos.getX() - pad,
                pos.getY() - pad,
                pos.getWidth() + pad*2,
                pos.getHeight() + pad*2,
                1,
                "ui_border3",
                4,
                m_panel.getFaction().getBaseUIColor()
            );
        }
    }

    public void advance(float amount) {
        // GLow Logic
        if (glowType != Glow.NONE) {
            State target = hoveredLastFrame ? State.IN : State.OUT;
            
            if (!isValidUIContext()) {
                target = State.OUT;
            }
            if (persistentGlow) {
                target = State.IN;
            }
                
            m_fader.setState(target);
            m_fader.advance(amount);
        }

        // Audio Logic
        if (hoveredLastFrame && soundEnabled && isValidUIContext()) {
            if (!playedUIHoverSound) {
                Global.getSoundPlayer().playUISound("ui_button_mouseover", 1, 1);
                playedUIHoverSound = true;
            }
            if (LMBUpLastFrame) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1, 1);
            }
        }

        // Tooltip Logic
        if (m_hasTooltip) {
            if (hoveredLastFrame && !hasClickedBefore && isValidUIContext()) {
                hoverTime += amount;
                if (hoverTime > tooltipDelay) {
                    showTooltip();
                }
            } else {
                hoverTime = 0f;
                hideTooltip();
            }
        }
    }

    public void processInput(List<InputEventAPI> events) {
        LMBDownLastFrame = false;
        LMBUpLastFrame = false;

        for (InputEventAPI event : events) {

            if (event.isMouseMoveEvent()) {
                float mouseX = event.getX();
                float mouseY = event.getY();

                PositionAPI pos = m_panel.getPanelPos();
                float x = pos.getX();
                float y = pos.getY();
                float w = pos.getWidth();
                float h = pos.getHeight();

                // Check for mouse over panel
                hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            }

            if (!hoveredLastFrame) {
                playedUIHoverSound = false;
                hasClickedBefore = false;
            }

            if (hoveredLastFrame && hasClickedBefore && event.isLMBUpEvent()) {
                LMBUpLastFrame = true;
            }

            if (hoveredLastFrame && event.isLMBDownEvent()) {
                LMBDownLastFrame = true;
                hasClickedBefore = true;
            }
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

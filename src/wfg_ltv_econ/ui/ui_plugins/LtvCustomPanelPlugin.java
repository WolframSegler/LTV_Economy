package wfg_ltv_econ.ui.ui_plugins;

import java.util.List;

import com.fs.starfarer.api.util.FaderUtil.State;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.TooltipProvider;
import wfg_ltv_econ.ui.panels.components.TooltipComponent;

public abstract class LtvCustomPanelPlugin<PanelType extends LtvCustomPanel<? extends LtvCustomPanelPlugin<PanelType>>> implements CustomUIPanelPlugin {
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
        THICK,
        TEX_VERY_THIN,
        TEX_THIN,
        TEX_MEDIUM,
        TEX_THICK
    }

    protected PanelType m_panel;

    public PanelType getPanel() {
        return m_panel;
    }
    
    protected TooltipComponent tpComp = null;
    protected FaderUtil m_fader;
    protected UIState targetUIState = UIState.NONE;
    protected Glow glowType = Glow.NONE;
    protected Outline outlineType = Outline.NONE;

    final protected float overlayBrightness = 1.2f;
    protected float backgroundTransparency = 0.65f;
    protected boolean persistentGlow = false;
    protected boolean hoveredLastFrame = false;
    protected boolean LMBDownLastFrame = false;
    protected boolean LMBUpLastFrame = false;
    protected boolean hasClickedBefore = false;
    protected boolean hasBackground = false;
    protected boolean ignoreUIState = false;
    protected boolean soundEnabled = false;
    protected boolean playedUIHoverSound = true;

    protected float hoverTime = 0f;
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetW = 0;
    protected int offsetH = 0;

    private final static int pad = 3;

    public void init(PanelType panel, Glow gT, boolean hasBg,
        Outline outline) {
        m_panel = panel;
        outlineType = outline;
        glowType = gT;
        hasBackground = hasBg;

        if (glowType != Glow.NONE) {
            m_fader = new FaderUtil(0, 0, 0.2f, true, true);
        }

        if (getPanel() instanceof TooltipProvider provider) {
            tpComp = new TooltipComponent(provider);
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

    public void setFader(FaderUtil a) {
        m_fader = a;
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

    public boolean getSoundEnabled() {
        return soundEnabled;
    }

    public boolean isValidUIContext() {
        return LtvUIState.is(targetUIState) || ignoreUIState; 
    }

    public void setHasGlow(Glow a) {
        glowType = a;
    }

    public void positionChanged(PositionAPI position) {

    }

    /**
     * Effects the background and outline position
     */
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
        final PositionAPI pos = m_panel.getPos();

        if (hasBackground) {
            int x = (int)pos.getX() + offsetX;
            int y = (int)pos.getY() + offsetY;
            int w = (int)pos.getWidth() + offsetW;
            int h = (int)pos.getHeight() + offsetH;
            RenderUtils.drawQuad(x, y, w, h, m_panel.BgColor, alphaMult*backgroundTransparency, false);
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
        final PositionAPI pos = m_panel.getPos();

        if (outlineType != Outline.NONE) {
            String textureID = null;
            int textureSize = 4;
            int borderThickness = 0;

            switch (outlineType) {
                case LINE:
                    borderThickness = 1;
                    break;
                case VERY_THIN:
                    borderThickness = 2;
                    break;
                case THIN:
                    borderThickness = 3;
                    break;
                case MEDIUM:
                    borderThickness = 4;
                    break;
                case THICK:
                    borderThickness = 8;
                    break;
                case TEX_VERY_THIN:
                    textureID = "ui_border4";
                    break;
                case TEX_THIN:
                    textureID = "ui_border3";
                    break;
                case TEX_MEDIUM:
                    textureID = "ui_border1";
                    textureSize = 8;
                    break;
                case TEX_THICK:
                    textureID = "ui_border2";
                    textureSize = 24;
                    break;
                default:
                    break;
            }

            if (borderThickness != 0) {
                RenderUtils.drawFramedBorder(
                    pos.getX() + offsetX,
                    pos.getY() + offsetY,
                    pos.getWidth() + offsetW,
                    pos.getHeight() + offsetH,
                    borderThickness,
                    m_panel.outlineColor,
                    alphaMult
                );
            }
            if (textureID != null) {
                UiUtils.drawRoundedBorder(
                    pos.getX() - pad,
                    pos.getY() - pad,
                    pos.getWidth() + pad*2,
                    pos.getHeight() + pad*2,
                    1,
                    textureID,
                    textureSize,
                    m_panel.outlineColor
                );
            }
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
        if (tpComp != null) {
            if (hoveredLastFrame && !hasClickedBefore && isValidUIContext()) {
                hoverTime += amount;
                if (hoverTime > tpComp.getDelay()) {
                    tpComp.showTooltip();
                }
            } else {
                hoverTime = 0f;
                tpComp.hideTooltip();
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

                PositionAPI pos = m_panel.getPos();
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

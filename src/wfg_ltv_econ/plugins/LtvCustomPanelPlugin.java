package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.util.FaderUtil.State;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.ui.LtvUIState.UIStateType;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    protected LtvCustomPanel m_panel;
    protected TooltipMakerAPI m_tooltip = null;
    protected boolean m_hasTooltip = false;
    protected FaderUtil m_fader;

    final protected float highlightBrightness = 1.2f;
    protected float tooltipDelay = 0.3f;
    protected boolean glowEnabled = false;
    protected boolean hoveredLastFrame = false;
    protected boolean clickedThisFrame = false;
    protected boolean hasBackground = false;
    protected boolean hasOutline = false;
    protected boolean ignoreUIState = false;

    protected float hoverTime = 0f;
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetW = 0;
    protected int offsetH = 0;

    public void init(LtvCustomPanel panel, boolean glowEnabled, boolean hasTooltip, boolean hasBackground, boolean hasOutline) {
        m_panel = panel;
        m_hasTooltip = hasTooltip;
        this.glowEnabled = glowEnabled;
        this.hasBackground = hasBackground;
        this.hasOutline = hasOutline;

        if (glowEnabled) {
            m_fader = new FaderUtil(0, 0, 0.2f, true, true);
        }
    }

    public boolean getGlowEnabled() {
        return glowEnabled;
    }

    public FaderUtil getFader() {
        return m_fader;
    }

    public void setIgnoreUIState(boolean a) {
        ignoreUIState = a;
    }

    public boolean isValidUIContext() {
        return LtvUIState.is(UIStateType.NONE) || ignoreUIState; 
    }

    public void setGlowEnabled(boolean a) {
        glowEnabled = a;
    }

    public TooltipMakerAPI showTooltip() {
        if (m_tooltip == null) {
            final int opad = 10;
            m_tooltip = ((CustomPanelAPI)m_panel.getParent()).createUIElement(500f, 400f, false);
            m_panel.createTooltip(m_tooltip);
            ((CustomPanelAPI)m_panel.getParent()).addUIElement(m_tooltip);

            m_tooltip.getPosition().inTL(-(m_tooltip.getPosition().getWidth() + opad), 0);

            // Might break later. Then just use RenderUtils
            ((StandardTooltipV2)m_tooltip).setShowBackground(true);
            ((StandardTooltipV2)m_tooltip).setShowBorder(true);
        }
        
        return m_tooltip;
    }

    public void hideTooltip() {
        ((CustomPanelAPI)m_panel.getParent()).removeComponent(m_tooltip);
        m_tooltip = null;
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

    public void renderBelow(float alphaMult) {
        PositionAPI pos = m_panel.getPanelPos();

        if (hasBackground) {
            int x = (int)pos.getX() + offsetX;
            int y = (int)pos.getY() + offsetY;
            int w = (int)pos.getWidth() + offsetW;
            int h = (int)pos.getHeight() + offsetH;
            RenderUtils.drawQuad(x, y, w, h, m_panel.BgColor, alphaMult*0.65f);
            // Looks vanilla like with 0.65f
        }
        if (hasOutline) {
            RenderUtils.drawOutline(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_panel.getFaction().getGridUIColor(), alphaMult);
        }

        if (glowEnabled && m_fader.getBrightness() > 0) {
            float glowAmount = highlightBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            m_panel.getFaction().getBaseUIColor(), glowAmount);

            if (clickedThisFrame) {
                RenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                m_panel.getFaction().getBaseUIColor(), glowAmount / 2);
            }
        }
    }

    public void render(float alphaMult) {

    }

    public void advance(float amount) {
        if (glowEnabled) {
            State target = hoveredLastFrame ? State.IN : State.OUT;
            m_fader.setState(target);

            if (!isValidUIContext()) {
                m_fader.setState(State.OUT);
            }

            m_fader.advance(amount);
        }

        // Tooltip Logic
        if (m_hasTooltip) {
            if (hoveredLastFrame && !clickedThisFrame && isValidUIContext()) {
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

            if (hoveredLastFrame && event.isLMBDownEvent()) {
                clickedThisFrame = true;
            }

            if (event.isLMBUpEvent()) {
                clickedThisFrame = false;
            }
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

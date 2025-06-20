package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.ui.impl.StandardTooltipV2;

import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.util.LtvRenderUtils;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    private LtvCustomPanel m_panel;
    private TooltipMakerAPI m_tooltip = null;
    private boolean m_hasTooltip = false;

    final private float highlightBrightness = 1.1f;
    final private float tooltipDelay = 0.3f;
    private boolean glowEnabled = false;
    private float glowFade = 0f;
    private boolean hoveredLastFrame = false;
    private boolean displayPrices = false;
    private boolean clickedThisFrame = false;
    private boolean hasBackground = false;
    private boolean hasOutline = false;

    private float hoverTime = 0f;
    private int offsetX = 0;
    private int offsetY = 0;
    private int offsetW = 0;
    private int offsetH = 0;

    public void init(LtvCustomPanel panel, boolean glowEnabled, boolean hasTooltip, boolean displayPrices, boolean hasBackground, boolean hasOutline) {
        m_panel = panel;
        m_hasTooltip = hasTooltip;
        this.glowEnabled = glowEnabled;
        this.displayPrices = displayPrices;
        this.hasBackground = hasBackground;
        this.hasOutline = hasOutline;
    }

    public boolean getGlowEnabled() {
        return glowEnabled;
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
            LtvRenderUtils.drawQuad(x, y, w, h, m_panel.BgColor, alphaMult*0.65f);
            // Looks vanilla like with 0.65f
        }
        if (hasOutline) {
            LtvRenderUtils.drawOutline(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_panel.getFaction().getGridUIColor(), alphaMult);
        }

        if (glowEnabled && glowFade > 0) {
            float glowAmount = highlightBrightness * glowFade * alphaMult;

            LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            m_panel.getFaction().getBaseUIColor(), glowAmount);

            if (clickedThisFrame) {
                LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                m_panel.getFaction().getBaseUIColor(), glowAmount / 2);
            }
        }
    }

    public void render(float alphaMult) {

    }

    public void advance(float amount) {
        // Glow Logic
        if (glowEnabled) {
            float target = hoveredLastFrame ? 1f : 0f;
            final float speed = 15f;
            glowFade += (target - glowFade) * amount * speed;
        }

        // Tooltip Logic
        if (m_hasTooltip) {
            if (hoveredLastFrame) {
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

            if (hoveredLastFrame && (event.isLMBDownEvent())) {
                clickedThisFrame = true;
            }

            if (hoveredLastFrame && (event.isLMBUpEvent())) {
                clickedThisFrame = false;

                if (displayPrices) {
                    // Open Dialog Panel showing Prices
                }
            }
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

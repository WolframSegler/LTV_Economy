package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityTooltipFactory;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.CommodityRowPanel;
import wfg_ltv_econ.util.LtvRenderUtils;
import wfg_ltv_econ.util.ReflectionUtils;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    private CommodityRowPanel m_comPanel;
    private StandardTooltipV2Expandable m_tooltip = null;
    private boolean m_hasTooltip = false;

    final private float highlightBrightness = 0.85f;
    final private float tooltipDelay = 0.35f;
    private boolean m_glowEnabled = false;
    private float glowFade = 0f;
    private boolean hoveredLastFrame = false;
    private boolean m_displayPrices = false;
    private boolean m_clickedThisFrame = false;

    private float hoverTime = 0f;

    public void init(CommodityRowPanel panel, boolean glowEnabled, boolean hasTooltip, boolean displayPrices) {
        this.m_comPanel = panel;
        this.m_glowEnabled = glowEnabled;
        this.m_hasTooltip = hasTooltip;
        this.m_displayPrices = displayPrices;
    }

    public boolean getGlowEnabled() {
        return m_glowEnabled;
    }

    public void setGlowEnabled(boolean a) {
        m_glowEnabled = a;
    }

    public TooltipMakerAPI showTooltip() {
        if (m_tooltip == null) {
            m_tooltip = (StandardTooltipV2Expandable) ReflectionUtils.invoke(CommodityTooltipFactory.class, 
            "super", m_comPanel.getCommodity());
        }
        if (ReflectionUtils.invoke(m_comPanel.getPanel(), "getTooltip") != m_tooltip) {
            ReflectionUtils.invoke(m_comPanel.getPanel(), "setTooltip", 0f, m_tooltip);
        }
        // Must be called each frame
        m_tooltip.getPosition().leftOfTop(m_comPanel.getParent(), 0);

        return m_tooltip;
    }

    public void hideTooltip() {
        m_tooltip = null;
        ReflectionUtils.invoke(m_comPanel.getPanel(), "setTooltip", 0f, null);
    }


    public void positionChanged(PositionAPI position) {

    }
    
    public void renderBelow(float alphaMult) {

    }

    public void render(float alphaMult) {
        if (glowFade <= 0f) {
            return;
        }

        PositionAPI pos = m_comPanel.getPanelPos();

        float glowAmount = highlightBrightness * glowFade * alphaMult;

        LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_comPanel.m_faction.getBaseUIColor(), glowAmount);

        if (m_clickedThisFrame) {
            LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), m_comPanel.m_faction.getBaseUIColor(), glowAmount/2);
        }
    }

    public void advance(float amount) {
        // Glow Logic
        if (m_glowEnabled) {
            float target = hoveredLastFrame ? 1f : 0f;
            float speed = 5f;
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

                PositionAPI pos = m_comPanel.getPanelPos();
                float x = pos.getX();
                float y = pos.getY();
                float w = pos.getWidth();
                float h = pos.getHeight();

                // Check for mouse over panel
                hoveredLastFrame = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            }

            if (hoveredLastFrame && (event.isLMBDownEvent())) {
                m_clickedThisFrame = true;
            }

            if (hoveredLastFrame && (event.isLMBUpEvent())) {
                m_clickedThisFrame = false;
            }

            if (m_displayPrices && event.isLMBEvent()) {
                MarketAPI globalMarket = Global.getSector().getEconomy().getMarket("global");
                if (globalMarket != null) {
                    Global.getSector().setCurrentlyOpenMarket(globalMarket);
                }
            }
        }
    }

    public void buttonPressed(Object buttonId) {

    }
}

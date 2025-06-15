package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityTooltipFactory;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.util.LtvRenderUtils;
import wfg_ltv_econ.util.ReflectionUtils;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    private LtvCustomPanel m_panel;
    private StandardTooltipV2Expandable m_tooltip = null;
    private boolean m_hasTooltip = false;
    private CommodityOnMarketAPI m_commodity = null;

    final private float highlightBrightness = 0.85f;
    final private float tooltipDelay = 0.35f;
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

    public void init(LtvCustomPanel panel, boolean glowEnabled, boolean hasTooltip, boolean displayPrices, CommodityOnMarketAPI commodity, boolean hasBackground, boolean hasOutline) {
        this.m_panel = panel;
        this.glowEnabled = glowEnabled;
        this.m_hasTooltip = hasTooltip;
        this.displayPrices = displayPrices;
        if (hasTooltip && commodity != null) {
            m_commodity = commodity;
        }
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
        if (m_tooltip == null && m_commodity != null) {
            m_tooltip = (StandardTooltipV2Expandable) ReflectionUtils.invoke(CommodityTooltipFactory.class,
                    "super", m_commodity);
        }
        if (ReflectionUtils.invoke(m_panel.getPanel(), "getTooltip") != m_tooltip) {
            ReflectionUtils.invoke(m_panel.getPanel(), "setTooltip", 0f, m_tooltip);
        }
        // Must be called each frame
        if (m_tooltip == null) {
            return null;
        }

        m_tooltip.getPosition().leftOfTop(m_panel.getParent(), 0);
        return m_tooltip;
    }

    public void hideTooltip() {
        m_tooltip = null;
        ReflectionUtils.invoke(m_panel.getPanel(), "setTooltip", 0f, null);
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
    }

    public void render(float alphaMult) {
        if (glowFade <= 0f) {
            return;
        }

        PositionAPI pos = m_panel.getPanelPos();

        float glowAmount = highlightBrightness * glowFade * alphaMult;
        if (glowEnabled) {
            LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            m_panel.getFaction().getBaseUIColor(), glowAmount);

            if (clickedThisFrame) {
                LtvRenderUtils.drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                m_panel.getFaction().getBaseUIColor(), glowAmount / 2);
            }
        }
    }

    public void advance(float amount) {
        // Glow Logic
        if (glowEnabled) {
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
            }

            if (displayPrices && event.isLMBEvent()) {
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

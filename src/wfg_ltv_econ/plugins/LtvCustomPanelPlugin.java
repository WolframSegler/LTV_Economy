package wfg_ltv_econ.plugins;

import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityTooltipFactory;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;

import wfg_ltv_econ.util.CommodityRow;
import wfg_ltv_econ.util.ReflectionUtils;

import org.lwjgl.opengl.GL11;

public class LtvCustomPanelPlugin implements CustomUIPanelPlugin {
    private CommodityRow m_panel;
    private StandardTooltipV2Expandable m_tooltip = null;
    private boolean m_hasTooltip = false;

    final private float highlightBrightness = 0.85f;
    final private float tooltipDelay = 0.35f;
    private boolean m_glowEnabled = false;
    private float glowFade = 0f;
    private boolean hoveredLastFrame = false;
    private boolean m_displayPrices = false;

    private float hoverTime = 0f;

    public void init(CommodityRow panel, boolean glowEnabled, boolean hasTooltip, boolean displayPrices) {
        this.m_panel = panel;
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
            "super", m_panel.getCommodity());
        }
        if (ReflectionUtils.invoke(m_panel.getPanel(), "getTooltip") != m_tooltip) {
            ReflectionUtils.invoke(m_panel.getPanel(), "setTooltip", 0f, m_tooltip);
        }
        // Must be called each frame
        m_tooltip.getPosition().leftOfTop(m_panel.getParent(), 0);

        return m_tooltip;
    }

    public void hideTooltip() {
        m_tooltip = null;
        ReflectionUtils.invoke(m_panel.getPanel(), "setTooltip", 0f, null);
    }

    /**
     * Called whenever the location or size of this UI panel changes.
     * 
     * @param position
     */
    public void positionChanged(PositionAPI position) {

    }

    /**
     * Below any UI elements in the panel.
     * 
     * @param alphaMult
     */
    public void renderBelow(float alphaMult) {

    }

    /**
     * alphaMult is the transparency the panel should be rendered at.
     * 
     * @param alphaMult
     */
    public void render(float alphaMult) {
        if (glowFade <= 0f) {
            return;
        }

        PositionAPI pos = m_panel.getPanelPos();

        float glowAmount = highlightBrightness * glowFade * alphaMult;

        drawGlowOverlay(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), glowAmount);
    }

    /**
     * @param amount in seconds.
     */
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

    /**
     * List of input events that occurred this frame. (Almost) always includes one
     * mouse move event.
     * 
     * Events should be consume()d if they are acted on.
     * Mouse-move events should generally not be consumed.
     * The loop processing events should check to see if an event has already been
     * consumed, and if so, skip it.
     * Accessing the data of a consumed event will throw an exception.
     * 
     * @param events
     */
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

    // Copied from obfuscated code. Basically it is the thing Alex uses to create a
    // glow effect. No Idea how it works
    private void drawGlowOverlay(float x, float y, float w, float h, float intensity) {
        Color glowColor = new Color(180, 180, 180, (int) (50 * intensity)); // white

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // additive blending

        GL11.glColor4ub(
                (byte) glowColor.getRed(),
                (byte) glowColor.getGreen(),
                (byte) glowColor.getBlue(),
                (byte) glowColor.getAlpha());

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}

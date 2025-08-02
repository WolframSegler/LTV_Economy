package wfg_ltv_econ.plugins;

import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.LtvIconPanel;
import wfg_ltv_econ.util.RenderUtils;
import wfg_ltv_econ.util.UiUtils;

public class LtvSpritePanelPlugin extends LtvCustomPanelPlugin {
    private SpriteAPI m_sprite;
    private Color m_color;
    private Color m_fillColor;
    private boolean drawBorder;
    private final float padding = 2f;
    private final float borderThickness = 2f;
    private final float outlineBrightness = 0.6f;

    private boolean isDrawFilledQuad = false;

    public void init(String spriteId, Color color, Color fillColor, boolean drawBorder) {
        this.m_sprite = Global.getSettings().getSprite(spriteId);

        m_color = color;
        m_fillColor = fillColor;
        this.drawBorder = drawBorder;

        if (fillColor != null) {
            isDrawFilledQuad = true;
        }
    }

    public void init(SpriteAPI sprite, Color color, Color fillColor, boolean drawBorder) {
        this.m_sprite = sprite;

        this.m_color = color;
        m_fillColor = fillColor;
        this.drawBorder = drawBorder;

        if (fillColor != null) {
            isDrawFilledQuad = true;
        }
    }

    public void setSprite(SpriteAPI sprite) {
        m_sprite = sprite;
    }

    public void setDrawFilledQuad(boolean a) {
        isDrawFilledQuad = a;
    }

    @Override
    public void render(float alphaMult) {
        super.render(alphaMult);
        if (m_sprite == null) {
            return;
        }

        if (m_color != null) {
            m_sprite.setColor(m_color);
        }

        PositionAPI pos = m_panel.getPanelPos();
        float x = pos.getX() + padding;
        float y = pos.getY() + padding;
        float width = pos.getWidth() - padding * 2;
        float height = pos.getHeight() - padding * 2;

        m_sprite.setSize(width, height);
        m_sprite.render(x, y);

        if (isDrawFilledQuad && m_fillColor != null) {
            m_sprite.setColor(m_fillColor);
            RenderUtils.drawQuad(x, y, width, height, m_fillColor, alphaMult);
        }

        if (drawBorder) {
            drawFramedBorder(
                x - borderThickness,
                y - borderThickness,
                width + borderThickness * 2,
                height + borderThickness * 2,
                borderThickness,
                Color.RED, alphaMult
            );
        }

        if (glowType == Glow.ADDITIVE && m_fader.getBrightness() > 0) {
            float glowAmount = outlineBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawAdditiveGlow(m_sprite, x, y, m_panel.getFaction().getBaseUIColor(),
                    glowAmount);
        }
    }

    private void drawFramedBorder(float x, float y, float width, float height, float thickness, Color color,
        float alphaMult) {
        // Top
        RenderUtils.drawRect(x, y + height - thickness, width, thickness, color, alphaMult, GL11.GL_QUADS);
        // Bottom
        RenderUtils.drawRect(x, y, width, thickness, color, alphaMult, GL11.GL_QUADS);
        // Left
        RenderUtils.drawRect(x, y, thickness, height, color, alphaMult, GL11.GL_QUADS);
        // Right
        RenderUtils.drawRect(x + width - thickness, y, thickness, height, color, alphaMult, GL11.GL_QUADS);
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);

        if (m_panel instanceof LtvIconPanel) {
            if (!m_hasTooltip || !hoveredLastFrame) {
                ((LtvIconPanel) m_panel).isExpanded = false;

                return;
            }

            for (InputEventAPI event : events) {
                if (event.isMouseEvent()) {
                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F1) {
                    ((LtvIconPanel) m_panel).isExpanded = !((LtvIconPanel) m_panel).isExpanded;
                    hideTooltip();

                    event.consume();

                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F2) {
                    CommodityOnMarketAPI com = ((LtvIconPanel) m_panel).m_com;
                    String codexID = CodexDataV2.getCommodityEntryId(com.getId());
                    UiUtils.openCodexPage(codexID);
                    hideTooltip();

                    event.consume();

                    continue;
                }
            }
        }

    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
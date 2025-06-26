package wfg_ltv_econ.plugins;

import java.util.List;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.util.RenderUtils;

public class LtvIconPanelPlugin extends LtvCustomPanelPlugin {
    private SpriteAPI sprite;
    private Color color;
    private boolean drawBorder;
    private final float padding = 2f;
    private final float borderThickness = 2f;
    private final float outlineBrightness = 0.6f;

    public void init(String spriteId, Color color, boolean drawBorder) {
        this.sprite = Global.getSettings().getSprite(spriteId);

        this.color = color;
        this.drawBorder = drawBorder;
    }

    @Override
    public void renderBelow(float alphaMult) {
        super.renderBelow(alphaMult);
        if (sprite == null) {
            return;
        }
        
        if (color != null) {
            sprite.setColor(color);
        }

        PositionAPI pos = m_panel.getPanelPos();
        float x = pos.getX() + padding;
        float y = pos.getY() + padding;
        float size = pos.getHeight() - padding*2;

        sprite.setSize(size, size);
        sprite.renderAtCenter(x + size / 2, y + size / 2);

        
        if (drawBorder) {
            drawFramedBorder(x - borderThickness, y - borderThickness, size + borderThickness * 2, borderThickness, Color.RED, alphaMult);
        }

        if (glowType == GlowType.ADDITIVE && m_fader.getBrightness() > 0) {
            float glowAmount = outlineBrightness * m_fader.getBrightness() * alphaMult;

            RenderUtils.drawAdditiveGlow(sprite, x, y, m_panel.getFaction().getBaseUIColor(),
                glowAmount);
        }
    }

    private void drawFramedBorder(float x, float y, float size, float thickness, Color color, float alphaMult) {
        // Top
        RenderUtils.drawRect(x, y + size - thickness, size, thickness, color, alphaMult, GL11.GL_QUADS);
        // Bottom
        RenderUtils.drawRect(x, y, size, thickness, color, alphaMult, GL11.GL_QUADS);
        // Left
        RenderUtils.drawRect(x, y, thickness, size, color, alphaMult, GL11.GL_QUADS);
        // Right
        RenderUtils.drawRect(x + size - thickness, y, thickness, size, color, alphaMult, GL11.GL_QUADS);
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
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
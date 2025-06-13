package wfg_ltv_econ.plugins;

import java.util.List;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg_ltv_econ.util.LtvRenderUtils;

public class CommodityRowIconPlugin implements CustomUIPanelPlugin {
    private UIPanelAPI panel;
    private SpriteAPI sprite;
    private Color color;
    private String spriteId;
    private boolean drawBorder;
    private final float padding = 2f;
    private final float borderThickness = 2f;

    public CommodityRowIconPlugin(String spriteId, Color color, boolean isIllegal) {
        this.spriteId = spriteId;
        this.color = color;
        this.drawBorder = isIllegal;
    }

    public void init(UIPanelAPI panel) {
        this.panel = panel;
        this.sprite = Global.getSettings().getSprite(spriteId);
    }

    @Override
    public void render(float alphaMult) {
        if (sprite == null)
            return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX() + padding;
        float y = pos.getY() + padding;
        float size = pos.getHeight() - padding*2;

        if (color != null) {
            sprite.setColor(color);
        }
        sprite.setSize(size, size);
        sprite.renderAtCenter(x + size / 2, y + size / 2);

        if (drawBorder) {
            drawFramedBorder(x - borderThickness, y - borderThickness, size + borderThickness * 2, borderThickness, Color.RED, alphaMult);
        }
    }

    private void drawFramedBorder(float x, float y, float size, float thickness, Color color, float alphaMult) {
        // Top
        LtvRenderUtils.drawRect(x, y + size - thickness, size, thickness, color, alphaMult, GL11.GL_QUADS);
        // Bottom
        LtvRenderUtils.drawRect(x, y, size, thickness, color, alphaMult, GL11.GL_QUADS);
        // Left
        LtvRenderUtils.drawRect(x, y, thickness, size, color, alphaMult, GL11.GL_QUADS);
        // Right
        LtvRenderUtils.drawRect(x + size - thickness, y, thickness, size, color, alphaMult, GL11.GL_QUADS);
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
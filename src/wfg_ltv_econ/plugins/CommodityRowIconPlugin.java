package wfg_ltv_econ.plugins;

import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.util.LtvRenderUtils;

public class CommodityRowIconPlugin implements CustomUIPanelPlugin {
    private CustomPanelAPI panel;
    private SpriteAPI sprite;
    private Color color;
    private String spriteId;
    private boolean drawBorder;
    private final float padding = 2f;
    private final float borderSize = 3f;

    public CommodityRowIconPlugin(String spriteId, Color color, boolean isIllegal) {
        this.spriteId = spriteId;
        this.color = color;
        this.drawBorder = isIllegal;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        this.sprite = Global.getSettings().getSprite(spriteId);
    }

    @Override
    public void render(float alphaMult) {
        if (sprite == null) return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX() + padding;
        float y = pos.getY() + padding;
        float size = pos.getHeight() - padding * 2;

        sprite.setColor(color);
        sprite.setSize(size, size);
        sprite.renderAtCenter(x + size / 2, y + size / 2);

        if (drawBorder) {
            float bx = x - borderSize;
            float by = y - borderSize;
            float bsize = size + borderSize * 2;

            // Outer black border
            LtvRenderUtils.drawRect(bx, by, bsize, bsize, Color.black, alphaMult);
            // Inner highlight border
            LtvRenderUtils.drawRect(bx + 1, by + 1, bsize - 2, bsize - 2, new Color(100, 200, 255), alphaMult);
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {}

    @Override
    public void renderBelow(float alphaMult) {}

    @Override
    public void advance(float amount) {}

    @Override
    public void processInput(List<InputEventAPI> events) {}

    @Override
    public void buttonPressed(Object buttonId) {}
}
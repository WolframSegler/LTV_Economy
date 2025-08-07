package wfg_ltv_econ.ui.ui_plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.util.RenderUtils;


public class CommodityinfobarPlugin implements CustomUIPanelPlugin {

    public static final String GLOW_BG = Global.getSettings().getSpriteName("ui", "glow_bg");
    public static final Color sideBarColor = Misc.getDarkPlayerColor();

    private CustomPanelAPI m_panel;
    private HashMap<Color, Float> m_barMap;
    private boolean hasOutline = false;

    public void init(CustomPanelAPI panel, boolean hasOutline, HashMap<Color, Float> barMap) {
        m_panel = panel;
        m_barMap = barMap;
        this.hasOutline = hasOutline;
    }
   
	public void positionChanged(PositionAPI position) {}
	
	public void renderBelow(float alphaMult) {}

	public void render(float alphaMult) {
        PositionAPI pos = m_panel.getPosition();
        final float x = pos.getX();
        final float y = pos.getY();
        final float w = pos.getWidth();
        final float h = pos.getHeight();

        if (hasOutline) {
            RenderUtils.drawFramedBorder(
                x + 1, y + 1, w - 2, h - 2,
                1, new Color(0, 0, 0, 100), alphaMult
            );
        }

        RenderUtils.drawQuad(x, y, 2, h, sideBarColor, alphaMult, false);
        final int sideBarGap = 4;

        float offsetX = x + sideBarGap;

        for (Map.Entry<Color, Float> mapEntry : m_barMap.entrySet()) {
            RenderUtils.drawQuad(offsetX, y, (w - sideBarGap)*mapEntry.getValue(), h, mapEntry.getKey(), alphaMult, false);
            offsetX += (w - sideBarGap)*mapEntry.getValue();
        }

        SpriteAPI glowBg = Global.getSettings().getSprite(GLOW_BG);
        
        glowBg.setAdditiveBlend();
        glowBg.setColor(new Color(255, 255, 255, 20));
        glowBg.setSize(w - sideBarGap, h);
        glowBg.render(x + sideBarGap, y);

        drawGlassLayer(x, y, w, h, alphaMult);
    }

    public void drawGlassLayer(float x, float y, float w, float h, float alphaMult) {
        Color topLight     = new Color(255, 255, 255, (int)(80 * alphaMult));
        Color centerHighlight = new Color(255, 255, 255, (int)(20 * alphaMult));
        Color bottomShadow  = new Color(0, 0, 0, (int)(90 * alphaMult));

        final float topY = y + h * 0.97f;
        final float midTop = y + h * 0.82f;
        final float midBottom = y + h * 0.18f;
        final float botY = y + h *0.03f;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBegin(GL11.GL_QUADS);

        // TOP
        RenderUtils.setGLColor(topLight, alphaMult);

        GL11.glVertex2f(x, midTop);
        GL11.glVertex2f(x + w, midTop);
        GL11.glVertex2f(x + w, topY);
        GL11.glVertex2f(x, topY);

        // MIDDLE
        RenderUtils.setGLColor(centerHighlight, alphaMult);

        GL11.glVertex2f(x, midBottom);
        GL11.glVertex2f(x + w, midBottom);
        GL11.glVertex2f(x + w, midTop);
        GL11.glVertex2f(x, midTop);

        // BOTTOM
        RenderUtils.setGLColor(bottomShadow, alphaMult);

        GL11.glVertex2f(x, botY);
        GL11.glVertex2f(x + w, botY);
        GL11.glVertex2f(x + w, midBottom);
        GL11.glVertex2f(x, midBottom);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
	
	public void advance(float amount) {}

	public void processInput(List<InputEventAPI> events) {}
	
	public void buttonPressed(Object buttonId) {}
}

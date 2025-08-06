package wfg_ltv_econ.plugins;

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

        drawBarLightingOverlay(x, y, w, h, alphaMult);
        drawGlossHighlight(x, y, w, h, alphaMult);
    }

    private void drawBarLightingOverlay(float x, float y, float w, float h, float alphaMult) {
        Color topShadow     = new Color(0, 0, 0, (int)(35 * alphaMult));
        Color centerHighlight = new Color(255, 255, 255, (int)(40 * alphaMult));
        Color bottomShadow  = new Color(0, 0, 0, (int)(45 * alphaMult));

        float topY = y + h;
        float midY = y + h * 0.5f;
        float botY = y;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBegin(GL11.GL_QUADS);

        // Top Half
        // TOP
        GL11.glColor4f(
            topShadow.getRed() / 255f,
            topShadow.getGreen() / 255f,
            topShadow.getBlue() / 255f,
            topShadow.getAlpha() / 255f
        );
        GL11.glVertex2f(x, topY);
        GL11.glVertex2f(x + w, topY);

        // MIDDLE
        GL11.glColor4f(
            centerHighlight.getRed() / 255f,
            centerHighlight.getGreen() / 255f,
            centerHighlight.getBlue() / 255f,
            centerHighlight.getAlpha() / 255f
        );
        GL11.glVertex2f(x + w, midY);
        GL11.glVertex2f(x, midY);

        // Bottom Half
        // MIDDLE
        GL11.glColor4f(
            centerHighlight.getRed() / 255f,
            centerHighlight.getGreen() / 255f,
            centerHighlight.getBlue() / 255f,
            centerHighlight.getAlpha() / 255f
        );
        GL11.glVertex2f(x, midY);
        GL11.glVertex2f(x + w, midY);

        // BOTTOM
        GL11.glColor4f(
            bottomShadow.getRed() / 255f,
            bottomShadow.getGreen() / 255f,
            bottomShadow.getBlue() / 255f,
            bottomShadow.getAlpha() / 255f
        );
        GL11.glVertex2f(x + w, botY);
        GL11.glVertex2f(x, botY);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawGlossHighlight(float x, float y, float w, float h, float alphaMult) {
        float highlightHeight = h * 0.75f; // Half the bar height
        float transitionY = y + h;                     // top
        float midY = y + h - highlightHeight * 0.3f;   // sharp falloff
        float fadeY = y + h - highlightHeight;         // fade to transparent

        Color strongWhite = new Color(255, 255, 255, (int)(65 * alphaMult));
        Color midWhite = new Color(255, 255, 255, (int)(40 * alphaMult));
        Color transparent = new Color(255, 255, 255, (int)(4 * alphaMult));

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBegin(GL11.GL_QUADS);

        // Top stripe: strong highlight
        GL11.glColor4f(
            strongWhite.getRed() / 255f,
            strongWhite.getGreen() / 255f,
            strongWhite.getBlue() / 255f,
            strongWhite.getAlpha() / 255f
        );
        GL11.glVertex2f(x, transitionY);
        GL11.glVertex2f(x + w, transitionY);

        GL11.glColor4f(
            midWhite.getRed() / 255f,
            midWhite.getGreen() / 255f,
            midWhite.getBlue() / 255f,
            midWhite.getAlpha() / 255f
        );
        GL11.glVertex2f(x + w, midY);
        GL11.glVertex2f(x, midY);

        // Lower stripe: fade out
        GL11.glColor4f(
            midWhite.getRed() / 255f,
            midWhite.getGreen() / 255f,
            midWhite.getBlue() / 255f,
            midWhite.getAlpha() / 255f
        );
        GL11.glVertex2f(x, midY);
        GL11.glVertex2f(x + w, midY);

        GL11.glColor4f(
            transparent.getRed() / 255f,
            transparent.getGreen() / 255f,
            transparent.getBlue() / 255f,
            transparent.getAlpha() / 255f
        );
        GL11.glVertex2f(x + w, fadeY);
        GL11.glVertex2f(x, fadeY);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
	
	public void advance(float amount) {}

	public void processInput(List<InputEventAPI> events) {}
	
	public void buttonPressed(Object buttonId) {}
}

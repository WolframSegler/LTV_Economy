package wfg_ltv_econ.util;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;

public class RenderUtils {
    public static void drawQuad(float x, float y, float w, float h, Color color, float alphaMult) {
        drawRect(x, y, w, h, color, alphaMult, GL11.GL_QUADS);
    }

    public static void drawOutline(float x, float y, float w, float h, Color color, float alphaMult) {
        drawRect(x, y, w, h, color, alphaMult, GL11.GL_LINE_LOOP);
    }

    public static void drawRect(float x, float y, float w, float h, Color color, float alphaMult, int mode) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4ub(
                (byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue(),
                (byte) (color.getAlpha() * alphaMult));

        GL11.glBegin(mode);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawGlowOverlay(float x, float y, float w, float h, Color color, float alphaMult) {
        Color baseClr = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (50 * alphaMult));

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // additive blending

        GL11.glColor4ub(
                (byte) baseClr.getRed(),
                (byte) baseClr.getGreen(),
                (byte) baseClr.getBlue(),
                (byte) baseClr.getAlpha());

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawAdditiveGlow(SpriteAPI sprite, float x, float y, Color glowColor,
            float intensity) {
        if (sprite == null || intensity <= 0f) {
            return;
        }

        x += sprite.getTexX();
        y += sprite.getTexY();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive blending

        Color base = new Color(
                Math.min(255, (int) (glowColor.getRed() * intensity)),
                Math.min(255, (int) (glowColor.getGreen() * intensity)),
                Math.min(255, (int) (glowColor.getBlue() * intensity)),
                Math.min(255, (int) (255 * intensity)));
        sprite.setColor(base);
        sprite.setAdditiveBlend();
        sprite.render(x, y);

        sprite.setNormalBlend();
        sprite.setColor(Color.white); // Reset
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}

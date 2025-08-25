package wfg_ltv_econ.util;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.Color;

public class RenderUtils {
    public static void drawQuad(float x, float y, float w, float h, Color color, float alphaMult,
        boolean additive) {
            
        drawRect(x, y, w, h, color, alphaMult, GL11.GL_QUADS, additive);
    }

    /**
     * @param x = posX
     * @param y = posY
     * @param w = width
     * @param h = height
     * @param t = thickness
     */
    public static void drawFramedBorder(float x, float y, float w, float h, float t, Color       
        color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        setGlColor(color, alphaMult);
        GL11.glBegin(GL11.GL_QUADS);

        // Bottom
        GL11.glVertex2f(x - t, y - t);
        GL11.glVertex2f(x + w, y - t);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x - t, y);
        
        // Right
        GL11.glVertex2f(x + w, y - t);
        GL11.glVertex2f(x + w + t, y - t);
        GL11.glVertex2f(x + w + t, y + h);
        GL11.glVertex2f(x + w, y + h);

        // Top
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w + t, y + h);
        GL11.glVertex2f(x + w + t, y + h + t);
        GL11.glVertex2f(x, y + h + t);

        // Left
        GL11.glVertex2f(x - t, y);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h + t);
        GL11.glVertex2f(x - t, y + h + t);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawRect(float x, float y, float w, float h, Color color, float alphaMult, int mode,
        boolean additive) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
            GL11.GL_SRC_ALPHA,
            additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA
        );

        setGlColor(color, alphaMult);

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
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        setGlColor(baseClr, alphaMult);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawAdditiveGlow(SpriteAPI sprite, float x, float y, Color glowColor, float intensity) {
        if (sprite == null || intensity <= 0f) {
            return;
        }

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

    public static final void setGlColor(Color color, float alphaMult) {
        GL11.glColor4ub(
            (byte) color.getRed(),
            (byte) color.getGreen(),
            (byte) color.getBlue(),
            (byte) (color.getAlpha() * alphaMult)
        );
    }

    public static void drawSpriteOutlinea(SpriteAPI sprite, float x, float y, float w, float h,
        Color color, float alpha, float radius) {

        if (sprite == null || color == null) return;

        final int steps = 12;
        final int angleStep = 360 / steps;

        sprite.setSize(w, h);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        // --- Pass 1: stencil/alpha mask ---
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);

        // Only write stencil where texture alpha > threshold
        final float threshold = 0.1f;
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, threshold);

        // Write "1" to stencil wherever sprite is drawn
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        sprite.setAlphaMult(alpha);
        sprite.setColor(Color.white);
        for (int i = 0; i < steps; i++) {
            double rad = Math.toRadians(i * angleStep);
            float dx = (float)Math.cos(rad) * radius;
            float dy = (float)Math.sin(rad) * radius;
            sprite.render(x + dx, y + dy);
        }

        // sprite.setSize(w + radius, h + radius);
        // sprite.render(x - radius / 2f, y - radius / 2f);

        GL11.glDisable(GL11.GL_ALPHA_TEST);

        // --- Pass 2: draw colored outline only where stencil == 1 ---
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4ub(
            (byte)color.getRed(),
            (byte)color.getGreen(),
            (byte)color.getBlue(),
            (byte)(color.getAlpha() * alpha)
        );

        GL11.glVertex2f(x - radius, y - radius);
        GL11.glVertex2f(x - radius, y + h + radius);
        GL11.glVertex2f(x + w + radius, y + h + radius);
        GL11.glVertex2f(x + w + radius, y - radius);
        GL11.glEnd();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static void drawSpriteOutline(SpriteAPI sprite, Color color, float x, float y, float w, float h,
        float alphaMult, float radius) {

        if (color == null || sprite == null) {
            return;
        }

        sprite.setSize(w, h);

        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColorMask(false, false, false, true);
        quadNoBlend(x - w, y - h, w * 2.0F, h * 2.0F, new Color(0, 0, 0, 0), 0.0F);

        sprite.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        sprite.setAlphaMult(alphaMult * 0.75f);
        sprite.setColor(Color.white);

        float var8 = 30.0F;
        float var9 = 0.0F;
        // if (this.ÕoöO00 == com.fs.starfarer.campaign.ui.marketinfo.f.Oo.Ó00000) {
        //     sprite.setAlphaMult(alphaMult);
        //     var8 = 90.0F;
        // } else if (this.ÕoöO00 == com.fs.starfarer.campaign.ui.marketinfo.f.Oo.new) {
        //     sprite.setAlphaMult(var5);
        //     var8 = 180.0F;
        //     var9 = 90.0F;
        // }

        for(float var10 = var9; var10 < 360 + var9; var10 += var8) {
            float var11 = (float)Math.sin(Math.toRadians((double)var10));
            float var12 = (float)Math.cos(Math.toRadians((double)var10));
            sprite.renderAtCenter(x + radius * var12, y + radius * var11);
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);

        quadWithBlend(x - w + 1, y - h + 1, w * 2 - 2, h * 2 - 2, Color.white, alphaMult);
        quadWithBlend(x - w + 1, y - h + 1, w * 2 - 2, h * 2 - 2, Color.white, alphaMult);
        GL11.glColorMask(true, true, true, true);
        GL11.glBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        quadWithBlend(x - w + 1, y - h + 1, w * 2 - 2, h * 2 - 2, color, alphaMult);
    }

    public static void quadNoBlend(float x, float y, float w, float h, Color color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO);
        GL11.glColor4ub(
            (byte)  color.getRed(),
            (byte)  color.getGreen(),
            (byte)  color.getBlue(),
            (byte) (color.getAlpha() * alphaMult)
        );
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }

    public static void quadWithBlend(float x, float y, float w, float h, Color color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4ub(
            (byte)  color.getRed(),
            (byte)  color.getGreen(),
            (byte)  color.getBlue(),
            (byte) (color.getAlpha() * alphaMult)
        );
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }
}

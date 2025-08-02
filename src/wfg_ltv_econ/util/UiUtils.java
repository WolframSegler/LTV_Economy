package wfg_ltv_econ.util;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.codex2.CodexDialog;

public class UiUtils {
    public static final void resetFlowLeft(TooltipMakerAPI tooltip, float opad) {
        float prevHeight = tooltip.getHeightSoFar();
        LabelAPI alignReset = tooltip.addPara("", 0);
        alignReset.getPosition().inTL(opad / 2, prevHeight);
        tooltip.setHeightSoFar(prevHeight);
    }

    public static final void positionCodexLabel(TooltipMakerAPI tooltip, int opad, int pad) {
        // LabelAPI F2Label = ((StandardTooltipV2Expandable) tooltip).expandLabel;
        LabelAPI F2Label = (LabelAPI) ReflectionUtils.get(tooltip, "expandLabel", LabelAPI.class, true);
        if (F2Label != null) {
            F2Label.getPosition().inBL(opad + pad, -pad * 6);
        }
    }

    /**
     * This function assumes that the sprite is pointing right.
     * In other words, it's directed towards the positive x-axis in Hyperspace.
     */
    public static float rotateSprite(Vector2f origin, Vector2f target) {
        Vector2f delta = Vector2f.sub(target, origin, null);

        float angleDegrees = (float) Math.toDegrees(Math.atan2(delta.y, delta.x));

        return angleDegrees;
    }

    public static void openCodexPage(String codexID) {
        CodexDialog.show(codexID);
    }

    /**
     * The texture size should match the actual size of the sprites.
     * <pre>
     * Available prefixes:
     * "ui_border1"
     * "ui_border2"
     * "ui_border3"
     * "ui_border4"
     * </pre>
     * @hidden
     */
    public static void drawRoundedBorder(float x, float y, float w, float h, float alpha, String borderPrefix,
        int textureSize, Color color) {

        SpriteAPI nw = Global.getSettings().getSprite("ui", borderPrefix + "_top_left");
        SpriteAPI ne = Global.getSettings().getSprite("ui", borderPrefix + "_top_right");
        SpriteAPI sw = Global.getSettings().getSprite("ui", borderPrefix + "_bot_left");
        SpriteAPI se = Global.getSettings().getSprite("ui", borderPrefix + "_bot_right");

        SpriteAPI n = Global.getSettings().getSprite("ui", borderPrefix + "_top");
        SpriteAPI s = Global.getSettings().getSprite("ui", borderPrefix + "_bot");
        SpriteAPI wSprite = Global.getSettings().getSprite("ui", borderPrefix + "_left");
        SpriteAPI e = Global.getSettings().getSprite("ui", borderPrefix + "_right");

        for (SpriteAPI sprite : new SpriteAPI[] { nw, ne, sw, se, n, s, wSprite, e }) {
            sprite.setAlphaMult(alpha);
            sprite.setColor(color);
        }

        // The outline overflows the panel
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Draw corners
        nw.render(x, y + h - textureSize);
        ne.render(x + w - textureSize, y + h - textureSize);
        sw.render(x, y);
        se.render(x + w - textureSize, y);

        // Resize edges to stretch between corners
        n.setSize(w - 2 * textureSize, textureSize);
        s.setSize(w - 2 * textureSize, textureSize);
        wSprite.setSize(textureSize, h - 2 * textureSize);
        e.setSize(textureSize, h - 2 * textureSize);

        // Draw edges
        n.render(x + textureSize, y + h - textureSize);
        s.render(x + textureSize, y);
        wSprite.render(x, y + textureSize);
        e.render(x + w - textureSize, y + textureSize);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }
}

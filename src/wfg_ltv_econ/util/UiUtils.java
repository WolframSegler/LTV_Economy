package wfg_ltv_econ.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.codex2.CodexDialog;

import wfg_ltv_econ.ui.plugins.CommodityinfobarPlugin;

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
    public static final float rotateSprite(Vector2f origin, Vector2f target) {
        Vector2f delta = Vector2f.sub(target, origin, null);

        float angleDegrees = (float) Math.toDegrees(Math.atan2(delta.y, delta.x));

        return angleDegrees;
    }

    public static final void openCodexPage(String codexID) {
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
    public static final void drawRoundedBorder(float x, float y, float w, float h, float alpha, String borderPrefix,
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
    }

    public static final Color getInFactionColor() {
        return new Color(35, 70, 130, 255);
    }

    public static final Color COLOR_DEFICIT = new Color(140, 15, 15);
    public static final Color COLOR_IMPORT = new Color(200, 140, 60);
    public static final Color COLOR_FACTION_IMPORT = new Color(240, 240, 100);
    public static final Color COLOR_LOCAL_PROD = new Color(122, 200, 122);
    public static final Color COLOR_EXPORT = new Color(63,  175, 63);
    public static final Color COLOR_NOT_EXPORTED = new Color(100, 140, 180);

    public static void CommodityInfoBar(TooltipMakerAPI tooltip, int barHeight, int barWidth, CommodityStats comStats) {
        final CustomPanelAPI infoBar = CommodityInfoBar(barHeight, barWidth, comStats);

        tooltip.addCustom(infoBar, 3);
    }

    public static final CustomPanelAPI CommodityInfoBar(int barHeight, int barWidth, CommodityStats comStats) {

        float localProducedRatio = (float)comStats.demandMetWithLocal / (float)comStats.totalActivity;
        float inFactionImportRatio = (float)comStats.inFactionImports / (float)comStats.totalActivity;
        float externalImportRatio = (float)comStats.externalImports / (float)comStats.totalActivity;
        float exportedRatio = (float)comStats.totalExports / (float)comStats.totalActivity;
        float notExportedRatio = (float)comStats.canNotExport / (float)comStats.totalActivity;
        float deficitRatio = (float)comStats.localDeficit / (float)comStats.totalActivity;

        final HashMap<Color, Float> barMap = new HashMap<Color, Float>();
        barMap.put(COLOR_DEFICIT, deficitRatio);
        barMap.put(COLOR_IMPORT, externalImportRatio);
        barMap.put(COLOR_FACTION_IMPORT, inFactionImportRatio);
        barMap.put(COLOR_LOCAL_PROD, localProducedRatio);
        barMap.put(COLOR_EXPORT, exportedRatio);
        barMap.put(COLOR_NOT_EXPORTED, notExportedRatio);

        for (Map.Entry<Color, Float> barPiece : barMap.entrySet()) {
            if (barPiece.getValue() < 0) {
                barPiece.setValue(0f);
            }
        }

        CustomPanelAPI infoBar = Global.getSettings().createCustom(
            barWidth, barHeight, new CommodityinfobarPlugin()
        );
        ((CommodityinfobarPlugin)infoBar.getPlugin()).init(
            infoBar, true, barMap
        );

        return infoBar;
    }

    public static final Color adjustAlpha(Color color, float alphaMult) {
        int newAplha = (int) (color.getAlpha() * alphaMult);
        if (newAplha > 255) {
            newAplha = 255;
        }

        if (newAplha < 0) {
            newAplha = 0;
        }

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAplha);
    }

    /**
     * Small utility to anchor the panel without actually using PositionAPI anchors.
     * Makes UI lifecycle dependencies easier to manage.
     * Does not handle screen bounds or overflow.
     */
    public static final void anchorPanel(UIPanelAPI panel, UIPanelAPI anchor, AnchorType type, int gap) {
        if (panel == null || anchor == null) return;

        final PositionAPI Ppos = panel.getPosition();
        final PositionAPI Apos = anchor.getPosition();

        Ppos.inBL(0, 0); // Reset the position. It's still relative
        final float panelX = Ppos.getX();
        final float panelY = Ppos.getY();
        final float panelW = Ppos.getWidth();
        final float panelH = Ppos.getHeight();

        final float anchorX = Apos.getX();
        final float anchorY = Apos.getY();
        final float anchorW = Apos.getWidth();
        final float anchorH = Apos.getHeight();

        float offsetX = 0;
        float offsetY = 0;
        
        switch (type) {
            case LeftOfTop:
                offsetX = anchorX - panelX - panelW - gap;
                offsetY = anchorY + anchorH - panelY - panelH;
                break;

            case LeftOfMid:
                offsetX = anchorX - panelX - panelW - gap;
                offsetY = anchorY - panelY + (anchorH - panelH) / 2f;
                break;

            case LeftOfBottom:
                offsetX = anchorX - panelX - panelW  - gap;
                offsetY = anchorY - panelY;
                break;

            case RightOfTop:
                offsetX = anchorX + anchorW - panelX + gap;
                offsetY = anchorY + anchorH - panelY - panelH;
                break;

            case RightOfMid:
                offsetX = anchorX + anchorW - panelX + gap;
                offsetY = anchorY - panelY + (anchorH - panelH) / 2f;
                break;

            case RightOfBottom:
                offsetX = anchorX + anchorW - panelX + gap;
                offsetY = anchorY - panelY;
                break;

            case AboveLeft:
                offsetX = anchorX - panelX;
                offsetY = anchorY + anchorH - panelY + gap;
                break;

            case AboveMid:
                offsetX = anchorX - panelX + (anchorW - panelW) / 2f;
                offsetY = anchorY + anchorH - panelY + gap;
                break;

            case AboveRight:
                offsetX = anchorX + anchorW - panelX - panelW;
                offsetY = anchorY + anchorH - panelY + gap;
                break;

            case BelowLeft:
                offsetX = anchorX - panelX;
                offsetY = anchorY - panelY - panelH - gap;
                break;

            case BelowMid:
                offsetX = anchorX - panelX + (anchorW - panelW) / 2f;
                offsetY = anchorY - panelY - panelH - gap;
                break;

            case BelowRight:
                offsetX = anchorX + anchorW - panelX - panelW;
                offsetY = anchorY - panelY - panelH - gap;
                break;
            }
        Ppos.setXAlignOffset(offsetX);
        Ppos.setYAlignOffset(offsetY);
    }

    public enum AnchorType {
        LeftOfTop,
        LeftOfMid,
        LeftOfBottom,
        RightOfTop,
        RightOfMid,
        RightOfBottom,
        AboveLeft,
        AboveMid,
        AboveRight,
        BelowLeft,
        BelowMid,
        BelowRight
    }
}

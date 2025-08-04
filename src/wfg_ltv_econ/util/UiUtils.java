package wfg_ltv_econ.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.codex2.CodexDialog;

import wfg_ltv_econ.plugins.CommodityinfobarPlugin;

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

    public static Color getInFactionColor() {
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

    public static CustomPanelAPI CommodityInfoBar(int barHeight, int barWidth, CommodityStats comStats) {

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
}

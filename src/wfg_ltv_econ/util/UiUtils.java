package wfg_ltv_econ.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.codex2.CodexDialog;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.ui.dialogs.LtvCustomDialogDelegate;
import wfg_ltv_econ.ui.plugins.CommodityinfobarPlugin;

public class UiUtils {

    public static final int APICodexHeight = 28;
    public static final int opad = 10;

    public static final void resetFlowLeft(TooltipMakerAPI tooltip, float opad) {
        float prevHeight = tooltip.getHeightSoFar();
        LabelAPI alignReset = tooltip.addPara("", 0);
        alignReset.getPosition().inTL(opad / 2, prevHeight);
        tooltip.setHeightSoFar(prevHeight);
    }

    public static final void positionCodexLabel(TooltipMakerAPI tooltip, int opad, int pad) {
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

        final SpriteAPI nw = Global.getSettings().getSprite("ui", borderPrefix + "_top_left");
        final SpriteAPI ne = Global.getSettings().getSprite("ui", borderPrefix + "_top_right");
        final SpriteAPI sw = Global.getSettings().getSprite("ui", borderPrefix + "_bot_left");
        final SpriteAPI se = Global.getSettings().getSprite("ui", borderPrefix + "_bot_right");

        final SpriteAPI n = Global.getSettings().getSprite("ui", borderPrefix + "_top");
        final SpriteAPI s = Global.getSettings().getSprite("ui", borderPrefix + "_bot");
        final SpriteAPI wSprite = Global.getSettings().getSprite("ui", borderPrefix + "_left");
        final SpriteAPI e = Global.getSettings().getSprite("ui", borderPrefix + "_right");

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

        float localProducedRatio = (float)comStats.localProduction / (float)comStats.getTotalActivity();
        float inFactionImportRatio = (float)comStats.inFactionImports / (float)comStats.getTotalActivity();
        float externalImportRatio = (float)comStats.externalImports / (float)comStats.getTotalActivity();
        float exportedRatio = (float)comStats.getTotalExports() / (float)comStats.getTotalActivity();
        float notExportedRatio = (float)comStats.getCanNotExport() / (float)comStats.getTotalActivity();
        float deficitRatio = (float)comStats.getDeficit() / (float)comStats.getTotalActivity();

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
     * values below 1 lower brightness. Values above 1 increase it.
     */
    public static final Color adjustBrightness(Color base, float factor) {
        int r = Math.min(255, (int) (base.getRed()   * factor));
        int g = Math.min(255, (int) (base.getGreen() * factor));
        int b = Math.min(255, (int) (base.getBlue()  * factor));
        
        return new Color(r, g, b, base.getAlpha());
    }

    /**
     * Anchors a panel relative to another panel via {@link #anchorPanel} and then clamps
     * the result to stay within screen bounds.
     */
    public static final void anchorPanelWithBounds(
        UIComponentAPI panel, UIComponentAPI anchor, AnchorType type, int gap
    ) {
        if (panel == null || anchor == null) return;

        AnchorPanelOffset offsets = anchorPanel(panel, anchor, type, gap);

        final PositionAPI Ppos = panel.getPosition();
        final int panelX = (int) Ppos.getX();
        final int panelY = (int) Ppos.getY();
        final int panelW = (int) Ppos.getWidth();
        final int panelH = (int) Ppos.getHeight();

        final int screenW = (int) Global.getSettings().getScreenWidth();
        final int screenH = (int) Global.getSettings().getScreenHeight();

        float offsetX = 0;
        if (panelX < 0) {
            offsetX = opad + (-panelX);
        }
        else if (panelX + panelW > screenW) {
            offsetX = screenW - (panelX + panelW + opad);
        }

        float offsetY = 0;
        if (panelY < 0) {
            offsetY = opad + (-panelY);
            if (panel instanceof TooltipMakerAPI tp) {
                if (tp.getCodexEntryId() != null) {
                    offsetY += APICodexHeight;
                }
            }
        } else if (panelY + panelH > screenH) {
            offsetY = screenH - (panelY + panelH + opad);
        }

        Ppos.inBL(offsets.x + offsetX, offsets.y + offsetY);
    }

    protected static class AnchorPanelOffset {
        public final float x;
        public final float y;
        public AnchorPanelOffset(float x, float y) {
            this.x = x; this.y = y;
        }
    }

    /**
     * Small utility to anchor the panel without actually using PositionAPI anchors.
     * Makes UI lifecycle dependencies easier to manage.
     * Does not handle screen bounds or overflow.
     */
    public static final AnchorPanelOffset anchorPanel(UIComponentAPI panel, UIComponentAPI anchor, AnchorType type, int gap) {

        if (panel == null || anchor == null) return null;

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

        int widthCompensation = 0;

        if (panel instanceof TooltipMakerAPI ^ anchor instanceof TooltipMakerAPI) {
            boolean panelIsTooltip = panel instanceof TooltipMakerAPI;
            TooltipMakerAPI tooltip = panelIsTooltip ? 
                (TooltipMakerAPI) panel : (TooltipMakerAPI) anchor;
            int comp = (int) ((tooltip.getPosition().getWidth() - tooltip.getWidthSoFar()) / 2f);
            widthCompensation = panelIsTooltip ? comp : -comp;
        }

        int heightCompensation = 0;

        if (panel instanceof TooltipMakerAPI tp) {
            if (tp.getCodexEntryId() != null) {
                heightCompensation += APICodexHeight / 2f;
            }
        } 

        float offsetX = 0;
        float offsetY = 0;
        
        switch (type) {
        case LeftTop:
            offsetX = anchorX - panelX - panelW - gap + widthCompensation;
            offsetY = anchorY + anchorH - panelY - panelH - heightCompensation;
            break;

        case LeftMid:
            offsetX = anchorX - panelX - panelW - gap + widthCompensation;
            offsetY = anchorY - panelY + (anchorH - panelH) / 2f - heightCompensation;
            break;

        case LeftBottom:
            offsetX = anchorX - panelX - panelW - gap + widthCompensation;
            offsetY = anchorY - panelY - heightCompensation;
            break;

        case RightTop:
            offsetX = anchorX + anchorW - panelX + gap - widthCompensation;
            offsetY = anchorY + anchorH - panelY - panelH - heightCompensation;
            break;

        case RightMid:
            offsetX = anchorX + anchorW - panelX + gap - widthCompensation;
            offsetY = anchorY - panelY + (anchorH - panelH) / 2f - heightCompensation;
            break;

        case RightBottom:
            offsetX = anchorX + anchorW - panelX + gap - widthCompensation;
            offsetY = anchorY - panelY - heightCompensation;
            break;

        case TopLeft:
            offsetX = anchorX - panelX - widthCompensation;
            offsetY = anchorY + anchorH - panelY + gap;
            break;

        case TopMid:
            offsetX = anchorX - panelX + (anchorW - panelW - widthCompensation*2) / 2f;
            offsetY = anchorY + anchorH - panelY + gap;
            break;

        case TopRight:
            offsetX = anchorX + anchorW - panelX - panelW + widthCompensation;
            offsetY = anchorY + anchorH - panelY + gap;
            break;

        case BottomLeft:
            offsetX = anchorX - panelX - widthCompensation;
            offsetY = anchorY - panelY - panelH - gap;
            break;

        case BottomMid:
            offsetX = anchorX - panelX + (anchorW - panelW - widthCompensation*2) / 2f;
            offsetY = anchorY - panelY - panelH - gap;
            break;

        case BottomRight:
            offsetX = anchorX + anchorW - panelX - panelW + widthCompensation;
            offsetY = anchorY - panelY - panelH - gap;
            break;

        case MidTopLeft:
            offsetX = anchorX + (anchorW / 2f) - panelX + gap - widthCompensation;
            offsetY = anchorY + (anchorH / 2f) - panelY - panelH;
            break;

        case MidTopRight:
            offsetX = anchorX + (anchorW / 2f) - panelX - panelW + gap - widthCompensation;
            offsetY = anchorY + (anchorH / 2f) - panelY - panelH;
            break;

        case MidBottomLeft:
            offsetX = anchorX + (anchorW / 2f) - panelX + gap - widthCompensation;
            offsetY = anchorY + (anchorH / 2f) - panelY;
            break;

        case MidBottomRight:
            offsetX = anchorX + (anchorW / 2f) - panelX - panelW + gap - widthCompensation;
            offsetY = anchorY + (anchorH / 2f) - panelY;
            break;
        }

        Ppos.inBL(offsetX, offsetY);

        return new AnchorPanelOffset(offsetX, offsetY);
    }

    /**
     * Defines anchor positions for UI panel alignment relative to a reference component.
     * <p>
     * The enum names consist of two parts:
     * </p>
     * <ol>
     *   <li><b>Direction</b> - The first word indicates the direction from the anchor component where the panel will be placed:
     *     <ul>
     *       <li><code>Left</code>: Panel is positioned to the left side of the anchor.</li>
     *       <li><code>Right</code>: Panel is positioned to the right side of the anchor.</li>
     *       <li><code>Top</code>: Panel is positioned above the anchor.</li>
     *       <li><code>Bottom</code>: Panel is positioned below the anchor.</li>
     *       <li><code>Mid</code>: Panel is positioned to the vertical center line of the anchor.</li>
     *     </ul>
     *   </li>
     *   <li><b>Alignment</b> - The second word indicates the alignment along the axis perpendicular to the direction:
     *     <ul>
     *       <li>For <code>Left</code> and <code>Right</code> directions, alignment is vertical:
     *         <ul>
     *           <li><code>Top</code>: Align panel's top edge with anchor's top edge.</li>
     *           <li><code>Mid</code>: Align panel's vertical center with anchor's vertical center.</li>
     *           <li><code>Bottom</code>: Align panel's bottom edge with anchor's bottom edge.</li>
     *         </ul>
     *       </li>
     *       <li>For <code>Top</code> and <code>Bottom</code> directions, alignment is horizontal:
     *         <ul>
     *           <li><code>Left</code>: Align panel's left edge with anchor's left edge.</li>
     *           <li><code>Mid</code>: Align panel's horizontal center with anchor's horizontal center.</li>
     *           <li><code>Right</code>: Align panel's right edge with anchor's right edge.</li>
     *         </ul>
     *       </li>
     *       <li>For <code>Mid</code> direction, alignment is Omnidirectional:
     *         <ul>
     *           <li><code>TopLeft</code>: Align panel's top left corner with anchor's center.</li>
     *           <li><code>TopRight</code>: Align panel's top right corner with anchor's center.</li>
     *           <li><code>BottomLeft</code>: Align panel's bottom left corner with anchor's center.</li>
     *           <li><code>BottomRight</code>: Align panel's bottom right corner with anchor's center.</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ol>
     */
    public enum AnchorType {
        LeftTop,
        LeftMid,
        LeftBottom,
        RightTop,
        RightMid,
        RightBottom,
        TopLeft,
        TopMid,
        TopRight,
        BottomLeft,
        BottomMid,
        BottomRight,
        MidTopLeft,
        MidTopRight,
        MidBottomLeft,
        MidBottomRight
    }

    /**
    <div>
        <p>
            Opens a CustomDialogDelegate inside a temporary 
            InteractionDialog. This is useful when no 
            InteractionDialogAPI is currently active (e.g. opening 
            dialogs from command UI).
        </p>
        <h4>Usage Example</h4>
        <pre>
            ComDetailDialog panel = new ComDetailDialog(rowPanel, commodity);
            UiUtils.showCustomDialogAsInteraction(panel, market.getPrimaryEntity());
        </pre>
    </div>
     */
    public static boolean showStandaloneCustomDialog(
        final LtvCustomDialogDelegate dialogPanel, float width, float height
    ) {
        return Global.getSector().getCampaignUI().showInteractionDialogFromCargo(
            new InteractionDialogPlugin() {
                @Override
                public void init(InteractionDialogAPI dialog) {
                    dialogPanel.setInteractionDialog(dialog);
                    dialog.showCustomDialog(width, height, dialogPanel);

                    dialog.setPromptText("");
                }

                public void optionSelected(String optionText, Object optionData) {}
                public void optionMousedOver(String optionText, Object optionData) {}
                public void advance(float amount) {}
                public void backFromEngagement(EngagementResultAPI result) {}
                public Object getContext() { return null;}
                public Map<String, MemoryAPI> getMemoryMap() { return new HashMap<>();}
            },
            null, null
        );
    }

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createCreditsLabel(String font, int height) {
        MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();

        LabelAPI label = Global.getSettings().createLabel("Credits: " + Misc.getWithDGS(credits.get()), font);
        if (font == "small_insignia") {
            label.setAlignment(Alignment.LMID);
        }
        label.setColor(Global.getSettings().getColor("textGrayColor"));
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }

        label.setHighlightColor(Misc.getHighlightColor());
        label.highlightLast(Misc.getWithDGS(credits.get()));
        return label;
    }

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static LabelAPI createMaxIndustriesLabel(String font, int height, MarketAPI market) {
        final int numInd = Misc.getNumIndustries(market);
        final int maxInd = Misc.getMaxIndustries(market);

        String text = numInd + " / " + maxInd;

        LabelAPI label = Global.getSettings().createLabel("Industries: " + text, font);
        if (font == "small_insignia") {
            label.setAlignment(Alignment.LMID);
        }

        label.setColor(Global.getSettings().getColor("textGrayColor"));
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }
        if (numInd > maxInd) {
            label.setHighlightColor(Misc.getNegativeHighlightColor());
        } else {
            label.setHighlightColor(Misc.getHighlightColor());
        }

        label.highlightLast(text);
        return label;
    }
}

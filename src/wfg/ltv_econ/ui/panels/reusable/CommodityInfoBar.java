package wfg.ltv_econ.ui.panels.reusable;

import org.lwjgl.opengl.GL11;

import static wfg.native_ui.util.UIConstants.dark;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.util.RenderUtils;

public class CommodityInfoBar extends CustomPanel<CommodityInfoBar> {
    private static final String GLOW_BG = Global.getSettings().getSpriteName("ui", "glow_bg");

    private final Map<Color, Float> barMap;
    private boolean hasOutline = false;

    public CommodityInfoBar(UIPanelAPI parent, int width, int height, boolean hasOutline,
        CommodityCell cell
    ) {
        super(parent, width, height);
        this.hasOutline = hasOutline;

        if (cell.getFlowEconomicFootprint() <= 0) {
            throw new IllegalStateException(
                "CommodityInfoBar cannot display info: economic footprint is zero for " 
                + cell.comID
            );
        }

        final float footprint = cell.getFlowEconomicFootprint();

        final float demandMetLocalRatio = cell.getFlowDeficitMetLocally() / footprint;
        final float inFactionImportRatio = cell.getFlowDeficitMetViaFactionTrade() / footprint;
        final float globalImportRatio = (cell.getFlowDeficitMetViaGlobalTrade() +
            cell.getFlowDeficitMetViaInformalTrade()) / footprint;
        final float overImportRatio = cell.getFlowOverImports() / footprint;
        final float importExclusiveRatio = cell.getImportExclusiveDemand() / footprint;
        final float exportedRatio = cell.getTotalExports() / footprint;
        final float notExportedRatio = cell.getFlowCanNotExport() / footprint;
        final float deficitRatio = cell.getFlowDeficit() / footprint;

        barMap = new LinkedHashMap<>(8) {{
            put(UIColors.COLOR_LOCAL_PROD, demandMetLocalRatio);
            put(UIColors.COLOR_EXPORT, exportedRatio);
            put(UIColors.COLOR_NOT_EXPORTED, notExportedRatio);
            put(UIColors.COLOR_FACTION_IMPORT, inFactionImportRatio);
            put(UIColors.COLOR_IMPORT, globalImportRatio);
            put(UIColors.COLOR_OVER_IMPORT, overImportRatio);
            put(UIColors.COLOR_IMPORT_EXCLUSIVE, importExclusiveRatio);
            put(UIColors.COLOR_DEFICIT, deficitRatio);
        }};

        for (Map.Entry<Color, Float> barPiece : barMap.entrySet()) {
            if (barPiece.getValue() < 0f) barPiece.setValue(0f);
        }
    }
    public void createPanel() {}

    @Override
    public void render(float alpha) {
        final float x = pos.getX();
        final float y = pos.getY();
        final float w = pos.getWidth();
        final float h = pos.getHeight();

        if (hasOutline) {
            RenderUtils.drawFramedBorder(
                x + 1, y + 1, w - 2, h - 2,
                1, new Color(0, 0, 0, 100), alpha
            );
        }

        RenderUtils.drawQuad(x, y, 2, h, dark, alpha, false);
        final int sideBarGap = 4;

        float offsetX = x + sideBarGap;

        for (Map.Entry<Color, Float> mapEntry : barMap.entrySet()) {
            RenderUtils.drawQuad(offsetX, y, (w - sideBarGap)*mapEntry.getValue(), h, mapEntry.getKey(), alpha, false);
            offsetX += (w - sideBarGap)*mapEntry.getValue();
        }

        SpriteAPI glowBg = Global.getSettings().getSprite(GLOW_BG);
        
        glowBg.setAdditiveBlend();
        glowBg.setColor(new Color(255, 255, 255, 20));
        glowBg.setSize(w - sideBarGap, h);
        glowBg.render(x + sideBarGap, y);

        drawGlassLayer(x, y, w, h, alpha);
    }

    private static final void drawGlassLayer(float x, float y, float w, float h, float alpha) {
        Color topLight     = new Color(255, 255, 255, (int)(80 * alpha));
        Color centerHighlight = new Color(255, 255, 255, (int)(20 * alpha));
        Color bottomShadow  = new Color(0, 0, 0, (int)(90 * alpha));

        final float topY = y + h * 0.97f;
        final float midTop = y + h * 0.82f;
        final float midBottom = y + h * 0.18f;
        final float botY = y + h *0.03f;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBegin(GL11.GL_QUADS);

        // TOP
        RenderUtils.setGlColor(topLight, alpha);

        GL11.glVertex2f(x, midTop);
        GL11.glVertex2f(x + w, midTop);
        GL11.glVertex2f(x + w, topY);
        GL11.glVertex2f(x, topY);

        // MIDDLE
        RenderUtils.setGlColor(centerHighlight, alpha);

        GL11.glVertex2f(x, midBottom);
        GL11.glVertex2f(x + w, midBottom);
        GL11.glVertex2f(x + w, midTop);
        GL11.glVertex2f(x, midTop);

        // BOTTOM
        RenderUtils.setGlColor(bottomShadow, alpha);

        GL11.glVertex2f(x, botY);
        GL11.glVertex2f(x + w, botY);
        GL11.glVertex2f(x + w, midBottom);
        GL11.glVertex2f(x, midBottom);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
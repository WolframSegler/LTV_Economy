package wfg.ltv_econ.util;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.plugins.CommodityinfobarPlugin;

public class UiUtils {

    public static final Color inFactionColor = new Color(35, 70, 130, 255);

    public static final Color COLOR_DEFICIT = new Color(140, 15, 15);
    public static final Color COLOR_OVER_IMPORT = new Color(180, 90, 180);
    public static final Color COLOR_IMPORT_EXCLUSIVE = new Color(0, 180, 200);
    public static final Color COLOR_IMPORT = new Color(200, 140, 60);
    public static final Color COLOR_FACTION_IMPORT = new Color(240, 240, 100);
    public static final Color COLOR_LOCAL_PROD = new Color(122, 200, 122);
    public static final Color COLOR_EXPORT = new Color(63,  175, 63);
    public static final Color COLOR_NOT_EXPORTED = new Color(100, 140, 180);

    public static final CustomPanelAPI CommodityInfoBar(int barHeight, int barWidth, CommodityCell cell) {
        if (cell.getFlowEconomicFootprint() <= 0) {
            throw new IllegalStateException(
                "CommodityInfoBar cannot display info: economic footprint is zero for " 
                + cell.comID
            );
        }

        final float footprint = cell.getFlowEconomicFootprint();

        float demandMetLocalRatio = (float)cell.getFlowDeficitMetLocally() / footprint;
        float inFactionImportRatio = (float)cell.getFlowDeficitMetViaFactionTrade() / footprint;
        float globalImportRatio = (float)cell.getFlowDeficitMetViaGlobalTrade() / footprint;
        float overImportRatio = (float)cell.getFlowOverImports() / footprint;
        float importExclusiveRatio = (float)cell.getImportExclusiveDemand() / footprint;
        float exportedRatio = (float)cell.getTotalExports() / footprint;
        float notExportedRatio = (float)cell.getFlowCanNotExport() / footprint;
        float deficitRatio = (float)cell.getFlowDeficit() / footprint;

        final Map<Color, Float> barMap = new LinkedHashMap<>(8) {{
            put(COLOR_LOCAL_PROD, demandMetLocalRatio);
            put(COLOR_EXPORT, exportedRatio);
            put(COLOR_NOT_EXPORTED, notExportedRatio);
            put(COLOR_FACTION_IMPORT, inFactionImportRatio);
            put(COLOR_IMPORT, globalImportRatio);
            put(COLOR_OVER_IMPORT, overImportRatio);
            put(COLOR_IMPORT_EXCLUSIVE, importExclusiveRatio);
            put(COLOR_DEFICIT, deficitRatio);
        }};

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

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createPlayerCreditsLabel(String font, int height) {
        final MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();

        final LabelAPI label = Global.getSettings().createLabel(
            "Player Credits: " + Misc.getWithDGS(credits.get()), font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }

        label.setHighlightColor(highlight);
        label.highlightLast(Misc.getWithDGS(credits.get()));
        return label;
    }

    public static final LabelAPI createColonyCreditsLabel(String font, int height, String marketID) {
        final long credits = EconomyEngine.getInstance().getCredits(marketID);

        final LabelAPI label = Global.getSettings().createLabel(
            "Colony Credits: " + String.format("%,d", credits), font
        );
        if (font == "small_insignia") label.setAlignment(Alignment.LMID);
        
        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) label.getPosition().setSize(label.getPosition().getWidth(), height);

        label.setHighlightColor(credits < 0 ? negative : highlight);
        label.highlightLast(String.format("%,d", credits));
        return label;
    }

    /**
     * I copied and cleaned this from the obfuscated code.
     * Because this is not available through the API for some reason.
     */
    public static final LabelAPI createMaxIndustriesLabel(String font, int height, MarketAPI market) {
        final int numInd = Misc.getNumIndustries(market);
        final int maxInd = Misc.getMaxIndustries(market);

        String text = numInd + " / " + maxInd;

        LabelAPI label = Global.getSettings().createLabel("Industries: " + text, font);
        if (font == "small_insignia") {
            label.setAlignment(Alignment.LMID);
        }

        label.setColor(gray);
        label.autoSizeToWidth(label.computeTextWidth(label.getText()));
        if (height > 0) {
            label.getPosition().setSize(label.getPosition().getWidth(), height);
        }
        if (numInd > maxInd) {
            label.setHighlightColor(negative);
        } else {
            label.setHighlightColor(highlight);
        }

        label.highlightLast(text);
        return label;
    }

    /**
     * @param t Must be between 0 and 1
     */
    public static final Color lerpColor(Color c1, Color c2, float t) {
        final int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
        final int g = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
        final int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
        final int a = (int) (c1.getAlpha() + t * (c2.getAlpha() - c1.getAlpha()));

        return new Color(r, g, b, a);
    }

    public static final boolean canViewPrices() {
        return Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay() ||
            Global.getSettings().getBoolean("allowPriceViewAtAnyColony");
    }
}
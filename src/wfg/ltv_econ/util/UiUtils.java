package wfg.ltv_econ.util;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.ui.plugins.CommodityinfobarPlugin;

public class UiUtils {

    public static final int opad = 10;

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
        if (comStats.getEconomicFootprint() <= 0) {
            throw new IllegalStateException(
                "CommodityInfoBar cannot display info: economic footprint is zero for " 
                + comStats.comID
            );
        }

        float demandMetLocalRatio = (float)comStats.getDemandMetLocally() / comStats.getEconomicFootprint();
        float inFactionImportRatio = (float)comStats.inFactionImports / comStats.getEconomicFootprint();
        float globalImportRatio = (float)comStats.globalImports / comStats.getEconomicFootprint();
        float exportedRatio = (float)comStats.getTotalExports() / comStats.getEconomicFootprint();
        float notExportedRatio = (float)comStats.getCanNotExport() / comStats.getEconomicFootprint();
        float deficitRatio = (float)comStats.getDeficit() / comStats.getEconomicFootprint();

        final Map<Color, Float> barMap = new LinkedHashMap<>() {{
            put(COLOR_LOCAL_PROD, demandMetLocalRatio);
            put(COLOR_EXPORT, exportedRatio);
            put(COLOR_NOT_EXPORTED, notExportedRatio);
            put(COLOR_FACTION_IMPORT, inFactionImportRatio);
            put(COLOR_IMPORT, globalImportRatio);
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

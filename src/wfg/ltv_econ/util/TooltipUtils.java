package wfg.ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.ui.impl.CargoTooltipFactory;

import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.submarkets.OpenSubmarketPlugin;
import wfg.reflection.ReflectionUtils;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasTooltip;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;

public class TooltipUtils {

    private final static String cargoTooltipArrow_PATH;

    private final static int pad = 3;
    private final static int opad = 10;

    static {
        cargoTooltipArrow_PATH = Global.getSettings().getSpriteName("ui", "cargoTooltipArrow");
    }

    /**
     * Reflectively calls the original factory method
     */
    public static void cargoTooltipFactory(TooltipMakerAPI tooltip, float pad, CommoditySpecAPI com,
        int rowsPerTable, boolean showExplanation, boolean showBestSell, boolean showBestBuy
    ) {
        ReflectionUtils.invoke(
            CargoTooltipFactory.class, "super", tooltip, pad, com, rowsPerTable,
            showExplanation, showBestSell, showBestBuy
        );
        tooltip.getPosition().setSize(1000, 0);
    }

    /**
     * Literally copied this from com.fs.starfarer.ui.impl.CargoTooltipFactory.
     * Only modified the parts that concern me. All hail Alex, the Lion of Sindria.
     * 
     * @param showExplanation Displays explanation paragraphs
     * @param showBestSell Shows the best places to make a profit selling the commodity.
     * @param showBestBuy Shows the best places to buy the commodity at a discount.
     */
    public static void cargoComTooltip(TooltipMakerAPI tooltip, int pad, int opad, CommoditySpecAPI spec,
        int rowsPerTable, boolean showExplanation, boolean showBestSell, boolean showBestBuy
    ) {
        final Color gray = Misc.getGrayColor();
        final Color highlight = Misc.getHighlightColor();
        final int rowH = 20;
        final EconomyEngine engine = EconomyEngine.getInstance();

        if (!Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay()) {
            if (showExplanation) {
                tooltip.addPara(
                    "Seeing remote price data for various colonies requires being within range of a functional comm relay.",
                    gray, pad
                );
            }
            return;
        }

        final CountingMap<String> countingMap = new CountingMap<>();
        final String comID = spec.getId();
        final int econUnit = (int) spec.getEconUnit();

        if (showBestSell) {
            final ArrayList<CommodityStats> marketList = new ArrayList<>();
            for (CommodityStats stats : engine.getCommodityInfo(comID).getAllStats()) {
                if (!stats.market.isHidden() && stats.market.getEconGroup() == null &&
                    stats.market.hasSubmarket(Submarkets.SUBMARKET_OPEN)
                ) {
                    if (1f - stats.getAvailabilityRatio() > 0 && stats.getDeficit() > econUnit) {
                        marketList.add(stats);
                    }
                }
            }

            Collections.sort(marketList, createSellComparator(comID, econUnit));
            if (!marketList.isEmpty()) {
                tooltip.addPara("Best places to sell:", pad);
                final int relativeY = (int) tooltip.getPosition().getY() -
                    (int) tooltip.getPrev().getPosition().getY();

                tooltip.beginTable(Global.getSector().getPlayerFaction(), rowH, new java.lang.Object[] {
                    "Price", 100, "Demand", 70, "Deficit", 70, "Location", 230,
                    "Star system", 140, "Dist (ly)", 80
                });
                countingMap.clear();

                int rowCount = 0;
                for (CommodityStats stats : marketList) {
                    final MarketAPI market = stats.market;
                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());

                        final float deficit = stats.getDeficit();
                        Color labelColor = highlight;
                        Color deficitColor = gray;
                        String quantityLabel = "---";
                        if (deficit > 0) {
                            quantityLabel = NumFormat.engNotation((long)deficit);
                            deficitColor = Misc.getNegativeHighlightColor();
                        }

                        String lessThanSymbol = "";
                        long marketDemand = (long) stats.getBaseDemand(false);
                        marketDemand = marketDemand / 100 * 100;
                        if (marketDemand < 100) {
                            marketDemand = 100;
                            lessThanSymbol = "<";
                            labelColor = gray;
                        }

                        final String factionName = market.getFaction().getDisplayName();
                        String location = "In hyperspace";
                        Color locationColor = gray;
                        if (market.getStarSystem() != null) {
                            final StarSystemAPI starSystem = market.getStarSystem();
                            location = starSystem.getBaseName();
                            final PlanetAPI star = starSystem.getStar();
                            locationColor = star.getSpec().getIconColor();
                            locationColor = Misc.setBrightness(locationColor, 235);
                        }

                        final float distanceToPlayer = Misc.getDistanceToPlayerLY(market.getPrimaryEntity());

                        tooltip.addRow(new Object[] {
                            highlight,
                            Misc.getDGSCredits(stats.getPlayerSellPrice(econUnit)),
                            labelColor,
                            lessThanSymbol + NumFormat.engNotation(marketDemand),
                            deficitColor,
                            quantityLabel,
                            Alignment.LMID,
                            market.getFaction().getBaseUIColor(),
                            market.getName() + " - " + factionName,
                            locationColor,
                            location,
                            highlight,
                            Misc.getRoundedValueMaxOneAfterDecimal(distanceToPlayer)
                        });

                        final SpriteAPI arrow = Global.getSettings().getSprite(cargoTooltipArrow_PATH);

                        final Base arrowPanel = new Base(
                            tooltip, 20, 20, "", null,
                            null, false
                        );

                        arrowPanel.setSprite(arrow);

                        final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                        final Vector2f targetLoc = market.getStarSystem().getLocation();

                        arrow.setAngle(WrapUiUtils.rotateSprite(playerLoc, targetLoc));

                        final int arrowY = relativeY + rowH * (2 + rowCount);
                        tooltip.addComponent(arrowPanel.getPanel()).inTL(610, arrowY);

                        ++rowCount;
                        if (rowCount >= rowsPerTable) {
                            break;
                        }
                    }
                }

                tooltip.addTable("", 0, opad);
            }
        }

        if (showBestBuy) {
            final ArrayList<CommodityStats> marketList = new ArrayList<>();
            for (CommodityStats stats : engine.getCommodityInfo(comID).getAllStats()) {
                if (!stats.market.isHidden() && stats.market.getEconGroup() == null &&
                    stats.market.hasSubmarket(Submarkets.SUBMARKET_OPEN)
                ) {
                    final int stockpileLimit = (int) OpenSubmarketPlugin.getBaseStockpileLimit(
                        stats.comID, stats.marketID
                    );
                    if (stockpileLimit > 0 && stockpileLimit >= econUnit) {
                        marketList.add(stats);
                    }
                }
            }

            Collections.sort(marketList, createBuyComparator(comID, econUnit));
            if (!marketList.isEmpty()) {

                tooltip.addPara("Best places to buy:", showBestSell ? opad * 2 : opad);
                final int relativeY = (int) tooltip.getPosition().getY() -
                    (int) tooltip.getPrev().getPosition().getY();
                tooltip.beginTable(Global.getSector().getPlayerFaction(), 20, new java.lang.Object[] {
                    "Price", 100, "Available", 70, "Excess", 70, "Location", 230,
                    "Star system", 140, "Dist (ly)", 80 
                });
                countingMap.clear();

                int rowCount = 0;
                for (CommodityStats stats : marketList) {
                    final MarketAPI market = stats.market;
                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());
                        long stockpileLimit = (long) OpenSubmarketPlugin.getBaseStockpileLimit(
                            stats.comID, stats.marketID
                        );
                        stockpileLimit += market.getCommodityData(comID).getPlayerTradeNetQuantity();
                        if (stockpileLimit < 0) stockpileLimit = 0;

                        int excess = (int) stats.getCanNotExport();
                        Color excessColor = gray;
                        String excessStr = "---";
                        if (excess > 0) {
                            excessStr = NumFormat.engNotation(excess);
                            excessColor = Misc.getPositiveHighlightColor();
                        }

                        String availableStr = "";
                        stockpileLimit = stockpileLimit / 100 * 100;
                        if (stockpileLimit < 100) {
                            stockpileLimit = 100;
                            availableStr = "<";
                        }

                        final String factionName = market.getFaction().getDisplayName();
                        String location = "In hyperspace";
                        Color locationColor = gray;
                        if (market.getStarSystem() != null) {
                            final StarSystemAPI StarSystem = market.getStarSystem();
                            location = StarSystem.getBaseName();
                            final PlanetAPI star = StarSystem.getStar();
                            locationColor = star.getSpec().getIconColor();
                            locationColor = Misc.setBrightness(locationColor, 235);
                        }

                        final float distance = Misc.getDistanceToPlayerLY(market.getPrimaryEntity());

                        tooltip.addRow(new java.lang.Object[] {
                            highlight,
                            Misc.getDGSCredits(stats.getPlayerBuyPrice(econUnit)),
                            highlight,
                            availableStr + NumFormat.engNotation(stockpileLimit),
                            excessColor,
                            excessStr,
                            Alignment.LMID,
                            market.getFaction().getBaseUIColor(),
                            market.getName() + " - " + factionName,
                            locationColor,
                            location,
                            highlight,
                            Misc.getRoundedValueMaxOneAfterDecimal(distance)
                        });

                        final SpriteAPI arrow = Global.getSettings().getSprite(cargoTooltipArrow_PATH);

                        final Base arrowPanel = new Base(tooltip, 20, 20,
                            "", null, null, false
                        );

                        arrowPanel.setSprite(arrow);

                        final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                        final Vector2f targetLoc = market.getStarSystem().getLocation();

                        arrow.setAngle(WrapUiUtils.rotateSprite(playerLoc, targetLoc));

                        final int arrowY = relativeY + rowH * (2 + rowCount);
                        tooltip.addComponent(arrowPanel.getPanel()).inTL(610, arrowY);

                        rowCount++;
                        if (rowCount >= rowsPerTable) {
                            break;
                        }
                    }
                }

                tooltip.addTable("", 0, opad);
            }
        }

        if (showExplanation) {
            tooltip.addPara(
                "All values approximate. Prices do not include tariffs, which can be avoided through black market trade.",
                Misc.getGrayColor(), opad);

            final Color txtColor = Misc.setAlpha(highlight, 155);
            tooltip.addPara(
                "*Per unit prices assume buying or selling a batch of %s" +
                "units. Each unit bought costs more as the market's supply is reduced, and" +
                "each unit sold brings in less as demand is fulfilled.",
                opad, gray, txtColor, new String[]{"" + econUnit}
            );
        }
    }

    /**
     * Creates a static Codex footer with no functionality.
     * The Codex is static and its labels must be updated manually.
     * The Codex must be attached manually.
     * The F1 and F2 events must be handled using the Plugin.
     */
    public static <PanelType extends CustomPanel<?, ?, CustomPanelAPI> & HasTooltip> TooltipMakerAPI 
        createCustomCodex(PanelType panel, String codexF1, String codexF2, int codexW) {

        final Color gray = new Color(100, 100, 100);
        final Color highlight = Misc.getHighlightColor();

        // Create the custom Footer
        TooltipMakerAPI codexTooltip = ((CustomPanelAPI)panel.getCodexParent().get()).createUIElement(codexW, 0, false);

        codexTooltip.setParaFont(Fonts.ORBITRON_12);

        codexTooltip.setParaFontColor(gray);
        LabelAPI lbl1 = codexTooltip.addPara(codexF1, 0, highlight, "F1");
        LabelAPI lbl2 = codexTooltip.addPara(codexF2, 0, highlight, "F2");

        lbl1.getPosition().inTL(opad / 2f, -2);
        int lbl2X = (int) (lbl1.computeTextWidth(lbl1.getText()) + opad + pad);
        lbl2.getPosition().inTL(lbl2X, -2);

        int tooltipH = (int) lbl1.computeTextHeight(lbl1.getText()) - opad / 2;

        codexTooltip.setHeightSoFar(tooltipH);

        return codexTooltip;
    }

    private static Comparator<CommodityStats> createSellComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.getPlayerSellPrice(econUnit);
            int price2 = (int) s2.getPlayerSellPrice(econUnit);
            return Integer.compare(price2, price1);
        };
    }

    private static Comparator<CommodityStats> createBuyComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.getPlayerBuyPrice(econUnit);
            int price2 = (int) s2.getPlayerBuyPrice(econUnit);
            return Integer.compare(price1, price2);
        };
    }

    public static float createStatModGridRow(
            TooltipMakerAPI tooltip,
            float currentY,
            float valueTxtWidth,
            boolean firstPara,
            Color valueColor,
            float value,
            boolean engNotate,
            boolean flatNum,
            String desc,
            String modSource,
            String valuePrefix // "+", "x", or ""
    ) {
        final String valueTxt;
        if (engNotate) {
            if (Math.abs(value) < 1000 && !flatNum) {
                valueTxt = valuePrefix + NumFormat.formatSmart(value);
            } else {
                valueTxt = valuePrefix + NumFormat.engNotation((long) value);
            } 
        } else {
            valueTxt = valuePrefix + value;
        }

        String fullDesc = desc;
        if (modSource != null) {
            fullDesc = desc + " (" + modSource + ")";
        } 

        // Draw text
        LabelAPI lbl = tooltip.addPara(valueTxt, pad, valueColor, valueTxt);

        float textH = lbl.computeTextHeight(valueTxt);
        float textX = (valueTxtWidth - lbl.computeTextWidth(valueTxt)) + pad;

        if (!firstPara) {
            currentY += textH + pad;
        }

        lbl.getPosition().inTL(textX, currentY);

        lbl = tooltip.addPara(fullDesc, pad);
        lbl.getPosition().inTL(valueTxtWidth + opad, currentY);

        return currentY; // return updated Y offset
    }

    public static void createCommodityProductionBreakdown(
        TooltipMakerAPI tooltip,
        CommodityStats comStats,
        Color highlight,
        Color negative
    ) {
        tooltip.setParaFontDefault();
        LabelAPI title = tooltip.addPara("Available: %s", pad, highlight,
            NumFormat.engNotation((long)comStats.getAvailable()));

        final int valueTxtWidth = 50;
        boolean firstPara = true;
		float y = tooltip.getHeightSoFar() + pad;

        for (Map.Entry<String, MutableStat> entry : comStats.getLocalProductionStat().entrySet()) {
            MutableStat stat = entry.getValue();
            Industry ind = comStats.market.getIndustry(entry.getKey());

            if (stat.getModifiedInt() > 0) {
                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, highlight, stat.base, true, true,
                    BaseIndustry.BASE_VALUE_TEXT, ind.getCurrentName(), "+"
                );

                firstPara = false;
            }

            // Flat mods
            for (StatMod mod : stat.getFlatMods().values()) {
                if (mod.getDesc() == null || mod.getValue() < 1) {
                    continue;
                }

                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, highlight, mod.getValue(), true, true,
                    mod.getDesc(), ind.getCurrentName(), "+"
                );

                firstPara = false;
            }

            // Mult mods
            for (StatMod mod : stat.getMultMods().values()) {
                if (!(stat.base > 0)) break;

                if (mod.getDesc() == null || mod.getValue() < 0) {
                    continue;
                }

                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, highlight, mod.getValue(), true, false,
                    mod.getDesc(), ind.getCurrentName(), Strings.X
                );

                firstPara = false;
            }
        }

        
        if (comStats.localProdMult != 1f && comStats.getLocalProduction(false) > 0) {
            final float value =  ((int) (comStats.localProdMult * 100f)) / 100f;

            y = TooltipUtils.createStatModGridRow(
                tooltip, y, valueTxtWidth, firstPara, negative, value, false, true,
                "Input shortages", null, Strings.X
            );

            firstPara = false;
        }

        // Import mods
        if (comStats.inFactionImports > 0) {
            y = addRow(tooltip, y, valueTxtWidth, firstPara, 
                comStats.inFactionImports, "In-faction imports", highlight, negative
            );
            firstPara = false;
        }

        if (comStats.globalImports > 0) {
            y = addRow(tooltip, y, valueTxtWidth, firstPara, 
                comStats.globalImports, "Global imports", highlight, negative
            );
            firstPara = false;
        }

        if (comStats.getDeficit() > 0) {
            y = addRow(tooltip, y, valueTxtWidth, firstPara, -comStats.getDeficit(),
                "Post-trade shortage", highlight, negative
            );
            firstPara = false;
        }

        if (firstPara) {
            title.setText("Not available.");
            title.setHighlight("");
        }

        tooltip.setHeightSoFar(y);
        WrapUiUtils.resetFlowLeft(tooltip, opad);
    }

    private static final float addRow(TooltipMakerAPI tooltip, float y, float valueTxtWidth, boolean firstPara,
        float value, String desc, Color highlight, Color negative) {

        String symbol = value >= 0 ? "+" : "";
        Color valueColor = value >= 0 ? highlight : negative;

        return TooltipUtils.createStatModGridRow(
            tooltip, y, valueTxtWidth, firstPara,
            valueColor, value, true, true, desc, null, symbol
        );
    }

    public static void createCommodityDemandBreakdown(
        TooltipMakerAPI tooltip,
        CommodityStats comStats,
        Color highlight,
        Color negative
    ) {
        Color valueColor = highlight;
        if (comStats.getDeficit() > 0) {
            valueColor = negative;
        }
        
        LabelAPI title = tooltip.addPara("Total demand: %s", opad, valueColor,
            NumFormat.engNotation((long)comStats.getBaseDemand(false)));

        final int valueTxtWidth = 50;
        boolean firstPara = true;
		float y = tooltip.getHeightSoFar() + pad;

        for (Map.Entry<String, MutableStat> entry : comStats.getBaseDemandStat().entrySet()) {
            MutableStat stat = entry.getValue();
            Industry ind = comStats.market.getIndustry(entry.getKey());

            if (stat.getModifiedInt() > 0) {
                if (firstPara) {
                    firstPara = false;
                    y -= pad;
                }

                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, valueColor, stat.base, true, true,
                    BaseIndustry.BASE_VALUE_TEXT, ind.getCurrentName(), "+"
                );

                firstPara = false;
            }

            // Flat mods
            for (StatMod mod : stat.getFlatMods().values()) {
                if (mod.getValue() < 1) {
                    continue;
                }

                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, valueColor, mod.getValue(), true, true,
                    "Needed by " + ind.getCurrentName(), null, "+"
                );

                firstPara = false;
            }

            // Mult mods
            for (StatMod mod : stat.getMultMods().values()) {
                if (!(stat.base > 0)) break;

                if (mod.getDesc() == null || mod.getValue() < 0) {
                    continue;
                }

                y = TooltipUtils.createStatModGridRow(
                    tooltip, y, valueTxtWidth, firstPara, valueColor, mod.getValue(), true, false,
                    mod.getDesc(), ind.getCurrentName(), Strings.X
                );

                firstPara = false;
            }
        }

        if (firstPara) {
            title.setText("No local demand.");
            title.setHighlight("");
        }

        tooltip.setHeightSoFar(y);
        WrapUiUtils.resetFlowLeft(tooltip, opad);
    }
}
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
import com.fs.starfarer.api.combat.MutableStatWithTempMods;
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
import static wfg.wrap_ui.util.UIConstants.*;

public class TooltipUtils {

    private final static String cargoTooltipArrow_PATH;

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
                    if (1f - stats.getFlowAvailabilityRatio() > 0 && stats.getFlowDeficit() > econUnit) {
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

                        final float deficit = stats.getFlowDeficit();
                        Color labelColor = highlight;
                        Color deficitColor = gray;
                        String quantityLabel = "---";
                        if (deficit > 0) {
                            quantityLabel = NumFormat.engNotation((long)deficit);
                            deficitColor = negative;
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
                            Misc.getDGSCredits(stats.computeVanillaPrice(econUnit, true, true)),
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

                        int excess = (int) stats.getFlowCanNotExport();
                        Color excessColor = gray;
                        String excessStr = "---";
                        if (excess > 0) {
                            excessStr = NumFormat.engNotation(excess);
                            excessColor = positive;
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
                            Misc.getDGSCredits(stats.computeVanillaPrice(econUnit, false, true)),
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
                gray, opad);

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

        // Create the custom Footer
        final TooltipMakerAPI codexTooltip = panel.getCodexParent().get().createUIElement(codexW, 0, false);

        codexTooltip.setParaFont(Fonts.ORBITRON_12);

        codexTooltip.setParaFontColor(gray);

        LabelAPI lbl1 = null;
        LabelAPI lbl2 = null;
        if (codexF1 != null) {
            lbl1 = codexTooltip.addPara(codexF1, 0, highlight, "F1");
            lbl1.getPosition().inTL(opad / 2f, -2);
        }
        if (codexF2 != null) {
            lbl2 = codexTooltip.addPara(codexF2, 0, highlight, "F2");

            if (lbl1 != null) {
                int lbl2X = (int) (lbl1.computeTextWidth(codexF1) + opad + pad);
                lbl2.getPosition().inTL(lbl2X, -2);
            } else {
                lbl2.getPosition().inTL(opad / 2f, -2);
            }
        }

        final int tooltipH = lbl1 != null ?
            (int) lbl1.computeTextHeight(codexF1) - opad / 2:
            (int) lbl2.computeTextHeight(codexF2) - opad / 2;

        codexTooltip.setHeightSoFar(tooltipH);
        panel.getCodexParent().get().addUIElement(codexTooltip);

        return codexTooltip;
    }

    private static Comparator<CommodityStats> createSellComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, true, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, true, true);
            return Integer.compare(price2, price1);
        };
    }

    private static Comparator<CommodityStats> createBuyComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, false, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, false, true);
            return Integer.compare(price1, price2);
        };
    }

    public static void createCommodityProductionBreakdown(
        TooltipMakerAPI tp,
        CommodityStats comStats
    ) {
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara("Available: %s", pad, highlight,
            NumFormat.engNotation((long)comStats.getFlowAvailable()));
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;

        tp.addPara(
            "Values reflect the current economic cycle. Stockpiles are ignored for display purposes",
            gray, pad
        );
        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : comStats.getFlowProdIndStats().entrySet()) {
            final MutableStat mutable = entry.getValue();
            final Industry ind = comStats.market.getIndustry(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getCurrentName()+")",
                    "+" + NumFormat.engNotation((long)mutable.base));
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getCurrentName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value));
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getCurrentName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%");
            }

            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getCurrentName()+")",
                    Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                mod.value < 1f ? negative:highlight
            );
            }
            }
        }

        if (comStats.getProduction(false) > 0) {
            final MutableStatWithTempMods mutable = comStats.getProductionStat();

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+mod.source+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value));
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+mod.source+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%");
            }
            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+mod.source+")",
                    Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 1f ? negative:highlight
                );
            }
            }
        }

        // Import mods
        if (comStats.inFactionImports > 0) {
            tp.addToGrid(0, rowCount++, "In-faction imports", "+" + NumFormat.engNotation(
                (long) comStats.inFactionImports), comStats.inFactionImports < 0 ? negative : highlight);
        }

        if (comStats.globalImports > 0) {
            tp.addToGrid(0, rowCount++, "Global imports", "+" + NumFormat.engNotation(
                (long) comStats.globalImports), comStats.globalImports < 0 ? negative : highlight);
        }

        if (comStats.importEffectiveness < 1f) {
            final float value = ((int) (comStats.importEffectiveness * 100f)) / 100f;

            tp.setGridValueColor(negative);
            tp.addToGrid(0, rowCount++, "Shipping losses", Strings.X + value);
        }

        if (comStats.getFlowDeficit() >= 1) {
            tp.addToGrid(0, rowCount++, "Post-trade shortage", "" + NumFormat.engNotation(
                (long)-comStats.getFlowDeficit()), negative);
        }

        if (rowCount < 0) {
            title.setText("Not available.");
            title.setHighlight("");
        }

        tp.addGrid(0);
    }

    public static void createCommodityDemandBreakdown(
        TooltipMakerAPI tp,
        CommodityStats comStats
    ) {
        final Color valueColor = comStats.getFlowDeficit() > 0 ? negative : highlight;
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;
        
        LabelAPI title = tp.addPara("Total demand: %s", opad, valueColor,
            NumFormat.engNotation((long)comStats.getBaseDemand(false)));

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : comStats.getFlowDemandIndStats().entrySet()) {
            final MutableStat mutable = entry.getValue();
            final Industry ind = comStats.market.getIndustry(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getCurrentName()+")",
                    "+" + NumFormat.engNotation((long)mutable.base), valueColor);
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, "Needed by " + ind.getCurrentName(),
                    "+" + NumFormat.formatMagnitudeAware(mod.value), valueColor);
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getCurrentName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%", valueColor);
            }

            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getCurrentName()+")",
                    Strings.X + NumFormat.formatMagnitudeAware(mod.value), valueColor);
            }
            }
        }

        if (rowCount < 1) {
            title.setText("No local demand.");
            title.setHighlight("");
        }

        tp.addGrid(0);
    }
}
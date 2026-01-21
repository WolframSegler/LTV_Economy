package wfg.ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.submarkets.OpenSubmarketPlugin;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import static wfg.wrap_ui.util.UIConstants.*;

public class TooltipUtils {

    public final static String TP_ARROW_PATH = Global.getSettings()
        .getSpriteName("ui", "cargoTooltipArrow");
    public final static String STOCKPILES_FULL_PATH = Global.getSettings()
        .getSpriteName("icons", "stockpiles_full");
    public final static String STOCKPILES_MEDIUM_PATH = Global.getSettings()
        .getSpriteName("icons", "stockpiles_medium");
    public final static String STOCKPILES_LOW_PATH = Global.getSettings()
        .getSpriteName("icons", "stockpiles_low");
    public final static String STOCKPILES_EMPTY_PATH = Global.getSettings()
        .getSpriteName("icons", "stockpiles_empty");

    /**
     * Literally copied this from com.fs.starfarer.ui.impl.CargoTooltipFactory.
     * Only modified the parts that concern me. All hail Alex, the Lion of Sindria.
     * 
     * @param showExplanation Displays explanation paragraphs
     * @param showBestSell Shows the best places to make a profit selling the commodity.
     * @param showBestBuy Shows the best places to buy the commodity at a discount.
     */
    public static final void cargoComTooltip(TooltipMakerAPI tooltip, int pad, int opad, CommoditySpecAPI spec,
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
            final ArrayList<CommodityCell> marketList = new ArrayList<>();
            for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
                if (!cell.market.isHidden() && cell.market.getEconGroup() == null &&
                    cell.market.hasSubmarket(Submarkets.SUBMARKET_OPEN)
                ) {
                    if (1f - cell.getFlowAvailabilityRatio() > 0 && cell.getFlowDeficit() > econUnit) {
                        marketList.add(cell);
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
                for (CommodityCell cell : marketList) {
                    final MarketAPI market = cell.market;
                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());

                        final float deficit = cell.getFlowDeficit();
                        Color labelColor = highlight;
                        Color deficitColor = gray;
                        String quantityLabel = "---";
                        if (deficit > 0) {
                            quantityLabel = NumFormat.engNotation((long)deficit);
                            deficitColor = negative;
                        }

                        String lessThanSymbol = "";
                        long marketDemand = (long) cell.getStoredDeficit();
                        marketDemand = (marketDemand / 100) * 100;
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
                            Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, true, true)),
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

                        final Base arrowPanel = new Base(tooltip, 20, 20, TP_ARROW_PATH, null, null);

                        final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                        final Vector2f targetLoc = market.getStarSystem().getLocation();

                        arrowPanel.getSprite().setAngle(WrapUiUtils.rotateSprite(playerLoc, targetLoc));

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
            final ArrayList<CommodityCell> marketList = new ArrayList<>();
            for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
                if (!cell.market.isHidden() && cell.market.getEconGroup() == null &&
                    cell.market.hasSubmarket(Submarkets.SUBMARKET_OPEN)
                ) {
                    final int stockpileLimit = (int) OpenSubmarketPlugin.getBaseStockpileLimit(
                        cell.comID, cell.marketID
                    );
                    if (stockpileLimit > 0 && stockpileLimit >= econUnit) {
                        marketList.add(cell);
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
                for (CommodityCell cell : marketList) {
                    final MarketAPI market = cell.market;
                    if (countingMap.getCount(market.getFactionId()) < 3) {
                        countingMap.add(market.getFactionId());
                        long available = cell.getRoundedStored();
                        available += market.getCommodityData(comID).getPlayerTradeNetQuantity();
                        if (available < 0) available = 0;

                        long excess = (long) cell.getStoredExcess();
                        Color excessColor = gray;
                        String excessStr = "---";
                        if (excess > 0) {
                            excessStr = NumFormat.engNotation(excess);
                            excessColor = positive;
                        }

                        String availableStr = "";
                        available = available / 100 * 100;
                        if (available < 100) {
                            available = 100;
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
                            Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, false, true)),
                            highlight,
                            availableStr + NumFormat.engNotation(available),
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

                        final Base arrowPanel = new Base(tooltip, 20, 20, TP_ARROW_PATH, null, null);

                        final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                        final Vector2f targetLoc = market.getStarSystem().getLocation();

                        arrowPanel.getSprite().setAngle(WrapUiUtils.rotateSprite(playerLoc, targetLoc));

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

    public static final void createCommodityProductionBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara("Available: %s", pad, highlight,
            NumFormat.engNotation((long)cell.getFlowAvailable()));
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;

        tp.addPara(
            "Values reflect the current day. Stockpiles are ignored for display purposes",
            gray, pad
        );
        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : cell.getFlowProdIndStats().entrySet()) {
            final MutableStat mutable = entry.getValue();
            final Industry ind = cell.market.getIndustry(entry.getKey());

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

        if (cell.getProduction(false) > 0) {
            final MutableStat mutable = cell.getProductionStat();

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    "+" + NumFormat.formatMagnitudeAware(mod.value));
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%");
            }
            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 1f ? negative:highlight
                );
            }
            }
        }

        // Import mods
        if (cell.inFactionImports > 0) {
            tp.addToGrid(0, rowCount++, "In-faction imports", "+" + NumFormat.engNotation(
                (long) cell.inFactionImports), cell.inFactionImports < 0 ? negative : highlight);
        }

        if (cell.globalImports > 0) {
            tp.addToGrid(0, rowCount++, "Global imports", "+" + NumFormat.engNotation(
                (long) cell.globalImports), cell.globalImports < 0 ? negative : highlight);
        }

        if (cell.importEffectiveness < 1f) {
            final float value = ((int) (cell.importEffectiveness * 100f)) / 100f;

            tp.setGridValueColor(negative);
            tp.addToGrid(0, rowCount++, "Shipping losses", Strings.X + value);
        }

        if (cell.getFlowDeficit() >= 1) {
            tp.addToGrid(0, rowCount++, "Post-trade shortage", "" + NumFormat.engNotation(
                (long)-cell.getFlowDeficit()), negative);
        }

        if (rowCount < 0) {
            title.setText("Not available.");
            title.setHighlight("");
        }

        tp.addGrid(0);
    }

    public static final void createCommodityDemandBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        final Color valueColor = cell.getFlowDeficit() > 0 ? negative : highlight;
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;
        
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara("Total demand: %s", opad, valueColor,
            NumFormat.engNotation((long)cell.getBaseDemand(true)));

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : cell.getFlowDemandIndStats().entrySet()) {
            final MutableStat mutable = entry.getValue();
            final Industry ind = cell.market.getIndustry(entry.getKey());

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

        if (cell.getBaseDemand(false) > 0) {
            final MutableStat mutable = cell.getDemandStat();

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    "+" + NumFormat.formatMagnitudeAware(mod.value));
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%");
            }
            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 1f ? negative:highlight
                );
            }
            }
        }

        if (rowCount < 1) {
            title.setText("No local demand.");
            title.setHighlight("");
        }

        tp.addGrid(0);
    }

    public static final void createCommodityStockpilesBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        tp.addPara("Stored: %s", pad, highlight, NumFormat.engNotation(cell.getRoundedStored()));
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        tp.addToGrid(0, rowCount++, "Desired Stockpiles",
            NumFormat.engNotation(cell.getPreferredStockpile()));
        tp.addToGrid(0, rowCount++, "Today's Imports",
            NumFormat.engNotation(cell.getTotalImports(true)));
        tp.addToGrid(0, rowCount++, "Today's Exports",
            NumFormat.engNotation(cell.getTotalExports()));
        if (cell.getFlowRealBalance() < 0f) {
            tp.addToGrid(0, rowCount++, "Last Change",
                NumFormat.engNotation(cell.getFlowRealBalance()), negative);
        } else {
            tp.addToGrid(0, rowCount++, "Last Change",
                NumFormat.engNotation(cell.getFlowRealBalance()));
        }

        tp.addGrid(0);
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final FactionSpecAPI faction, final boolean addRatioColors
    ) {
        final Color color = getStockpileColor(ratio, faction, addRatioColors);
        return getStockpilesIcon(ratio, size, parent, color, null, false);
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final Color iconColor
    ) {
        return getStockpilesIcon(ratio, size, parent, iconColor, null, false);
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final Color iconColor, final Color bgColor, final boolean drawBorder
    ) {
        final String iconPath;
        if (ratio <= 0.25f) {
            iconPath = STOCKPILES_EMPTY_PATH;
        } else if (ratio <= 0.5f) {
            iconPath = STOCKPILES_LOW_PATH;
        } else if (ratio <= 0.75f) {
            iconPath = STOCKPILES_MEDIUM_PATH;
        } else {
            iconPath = STOCKPILES_FULL_PATH;
        }
        final Base icon = new Base(parent, size, size, iconPath, iconColor, bgColor);
        icon.outline.enabled = drawBorder;
        return icon;
    }

    // PRIVATE METHODS
    private static final Color getStockpileColor(final float ratio, final FactionSpecAPI faction,
        final boolean addRatioColors
    ) {
        if (!addRatioColors) return faction.getBaseUIColor();
        if (ratio <= 0.25f) return UiUtils.COLOR_DEFICIT;
        if (ratio <= 0.5f) return UiUtils.COLOR_IMPORT;
        if (ratio <= 0.75f) return UiUtils.COLOR_LOCAL_PROD;
        return UiUtils.COLOR_NOT_EXPORTED;
    }

    private static final Comparator<CommodityCell> createSellComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, true, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, true, true);
            return Integer.compare(price2, price1);
        };
    }

    private static final Comparator<CommodityCell> createBuyComparator(String comID, int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, false, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, false, true);
            return Integer.compare(price1, price2);
        };
    }
}
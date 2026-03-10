package wfg.ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.ui.panels.SpritePanel.Base;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import static wfg.native_ui.util.UIConstants.*;

public class TooltipUtils {

    public static final SettingsAPI settings = Global.getSettings();
    public static final String TP_ARROW_PATH = settings.getSpriteName("ui", "cargoTooltipArrow");
    public static final String STOCKPILES_FULL_PATH = settings.getSpriteName("icons", "stockpiles_full");
    public static final String STOCKPILES_MEDIUM_PATH = settings.getSpriteName("icons", "stockpiles_medium");
    public static final String STOCKPILES_LOW_PATH = settings.getSpriteName("icons", "stockpiles_low");
    public static final String STOCKPILES_EMPTY_PATH = settings.getSpriteName("icons", "stockpiles_empty");
    public static final String STOCKPILES_NO_DEMAND_PATH = settings.getSpriteName("icons", "stockpiles_no_demand");

    /**
     * Literally copied this from com.fs.starfarer.ui.impl.CargoTooltipFactory.
     * Only modified the parts that concern me. All hail Alex, the Lion of Sindria.
     * 
     * @param showExplanation Displays explanation paragraphs
     * @param showBestSell Shows the best places to make a profit selling the commodity.
     * @param showBestBuy Shows the best places to buy the commodity at a discount.
     */
    public static final void cargoComTooltip(TooltipMakerAPI tp, CommoditySpecAPI spec,
        int rowsPerTable, boolean showExplanation, boolean showBestSell, boolean showBestBuy
    ) {
        if (!showExplanation && !showBestSell && !showBestBuy) {
            throw new IllegalArgumentException("cargoComTooltip: nothing to display; all sections disabled");
        }

        final int rowH = 20;
        final EconomyEngine engine = EconomyEngine.getInstance();
        final int baseY = (int) tp.getHeightSoFar();

        if (!Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay()) {
            if (showExplanation) {
                tp.addPara(
                    "Seeing remote price data for various colonies requires being within range of a functional comm relay.",
                    gray, pad
                );
            }
            return;
        }

        final CountingMap<String> countingMap = new CountingMap<>();
        final String comID = spec.getId();
        final int econUnit = (int) spec.getEconUnit();

        final ArrayList<CommodityCell> marketList = new ArrayList<>();
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (!cell.market.isHidden() && cell.market.getEconGroup() == null &&
                cell.market.hasSubmarket(Submarkets.SUBMARKET_OPEN)
            ) { marketList.add(cell); }
        }

        if (showBestSell) {
            Collections.sort(marketList, createSellComparator(econUnit));
            if (!marketList.isEmpty()) {
                tp.addPara("Best places to sell:", pad);
                final PositionAPI prevPos = tp.getPrev().getPosition();
                final int relativeY = (int) (baseY + tp.getPosition().getY() + prevPos.getHeight() - prevPos.getY());

                tp.beginTable(Global.getSector().getPlayerFaction(), rowH, new Object[] {
                    "Price", 100, "Desired", 70, "Deficit", 70, "Location", 230,
                    "Star system", 140, "Dist (ly)", 80
                });
                countingMap.clear();

                int rowCount = 0;
                for (CommodityCell cell : marketList) {
                    final MarketAPI market = cell.market;
                    if (countingMap.getCount(market.getFactionId()) >= 3) continue;
                    countingMap.add(market.getFactionId());

                    final int desired = (int) ((cell.getPreferredStockpile() / 100f) * 100f);
                    final boolean lowDemand = desired < 100;
                    final String lessThanSymbol = lowDemand ? "<" : "";
                    final Color labelColor = lowDemand ? gray : highlight;

                    final double deficit = cell.getStoredDeficit();
                    final boolean deficitPresent = deficit > 10.0;
                    final Color deficitColor = deficitPresent ? negative : gray;
                    final String quantityLabel = deficitPresent ? NumFormat.engNotation(deficit) : "---";

                    final String factionName = market.getFaction().getDisplayName();
                    String location = "In hyperspace";
                    Color locationColor = gray;
                    if (market.getStarSystem() != null) {
                        final StarSystemAPI starSystem = market.getStarSystem();
                        location = starSystem.getBaseName();
                        locationColor = starSystem.getStar().getSpec().getIconColor();
                        locationColor = Misc.setBrightness(locationColor, 235);
                    }

                    final float distanceToPlayer = Misc.getDistanceToPlayerLY(market.getPrimaryEntity());

                    tp.addRow(
                        highlight,
                        Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, true, true) / econUnit),
                        labelColor,
                        lessThanSymbol + NumFormat.engNotation(desired),
                        deficitColor,
                        quantityLabel,
                        Alignment.LMID,
                        market.getFaction().getBaseUIColor(),
                        market.getName() + " - " + factionName,
                        locationColor,
                        location,
                        highlight,
                        Misc.getRoundedValueMaxOneAfterDecimal(distanceToPlayer)
                    );

                    final Base arrowPanel = new Base(tp, 20, 20, TP_ARROW_PATH, null, null);

                    final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                    final Vector2f targetLoc = market.getStarSystem().getLocation();

                    arrowPanel.getSprite().setAngle(NativeUiUtils.rotateSprite(playerLoc, targetLoc));

                    final int arrowY = relativeY + rowH * (2 + rowCount) + pad;
                    tp.addCustom(arrowPanel.getPanel(), 0f).getPosition().inTL(610, arrowY);

                    ++rowCount;
                    if (rowCount >= rowsPerTable) break;
                }

                tp.setHeightSoFar(relativeY);
                NativeUiUtils.resetFlowLeft(tp, opad/2f);
                tp.addTable("", 0, pad);
            }
        }

        if (showBestBuy) {
            Collections.sort(marketList, createBuyComparator(econUnit));
            if (!marketList.isEmpty()) {

                tp.addPara("Best places to buy:", opad);
                final PositionAPI prevPos = tp.getPrev().getPosition();
                final int relativeY = (int) (baseY + tp.getPosition().getY() + prevPos.getHeight() - prevPos.getY());
                tp.beginTable(Global.getSector().getPlayerFaction(), 20, new Object[] {
                    "Price", 100, "Stored", 70, "Excess", 70, "Location", 230,
                    "Star system", 140, "Dist (ly)", 80 
                });
                countingMap.clear();

                int rowCount = 0;
                for (CommodityCell cell : marketList) {
                    final MarketAPI market = cell.market;
                    if (countingMap.getCount(market.getFactionId()) >= 3) continue;
                    countingMap.add(market.getFactionId());

                    final long available = Math.max(0l, cell.getRoundedStored() +
                        market.getCommodityData(comID).getPlayerTradeNetQuantity());
                    final long availableValue = Math.max((available / 100) * 100, 100l);
                    final String availableStr = available < 100l ? "<" : "";

                    final long excess = (long) cell.getStoredExcess();
                    final boolean hasExcess = excess > 10l;

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

                    tp.addRow(
                        highlight,
                        Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, false, true) / econUnit),
                        highlight,
                        availableStr + NumFormat.engNotation(availableValue),
                        hasExcess ? positive : gray,
                        hasExcess ? NumFormat.engNotation(excess) : "---",
                        Alignment.LMID,
                        market.getFaction().getBaseUIColor(),
                        market.getName() + " - " + factionName,
                        locationColor,
                        location,
                        highlight,
                        Misc.getRoundedValueMaxOneAfterDecimal(distance)
                    );

                    final Base arrowPanel = new Base(tp, 20, 20, TP_ARROW_PATH, null, null);

                    final Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocationInHyperspace();
                    final Vector2f targetLoc = market.getStarSystem().getLocation();

                    arrowPanel.getSprite().setAngle(NativeUiUtils.rotateSprite(playerLoc, targetLoc));

                    final int arrowY = relativeY + rowH * (2 + rowCount) + pad;
                    tp.addCustom(arrowPanel.getPanel(), 0f).getPosition().inTL(610, arrowY);
                    NativeUiUtils.resetFlowLeft(tp, opad/2f);

                    rowCount++;
                    if (rowCount >= rowsPerTable) break;
                }

                tp.setHeightSoFar(relativeY);
                NativeUiUtils.resetFlowLeft(tp, opad/2f);
                tp.addTable("", 0, pad);
            }
        }

        if (showExplanation) {
            tp.addPara(
                "All values approximate. Prices do not include tariffs, which can be avoided through black market trade.",
                gray, opad);

            final Color txtColor = Misc.setAlpha(highlight, 155);
            tp.addPara(
                "*Per unit prices assume buying or selling a batch of %s " +
                "units. Each unit bought costs more as the market's supply is reduced, and" +
                "each unit sold brings in less as demand is fulfilled.",
                opad, gray, txtColor, new String[]{"" + econUnit}
            );
        }
    }

    public static final void createComProductionBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara("Available: %s", pad, highlight,
            NumFormat.engNotation((long)cell.getFlowAvailable()));
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : cell.getFlowProdIndStats().singleEntrySet()) {
            final MutableStat mutable = entry.getValue();
            final IndustrySpecAPI ind = settings.getIndustrySpec(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getName()+")",
                    "+" + NumFormat.engNotation((long)mutable.base));
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value));
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%");
            }

            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
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

    public static final void createComDemandBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        final Color valueColor = cell.getFlowDeficit() > 0 ? negative : highlight;
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;
        
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara("Total demand: %s", opad, valueColor,
            NumFormat.engNotation(cell.getBaseDemand(true))
        );

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        for (Map.Entry<String, MutableStat> entry : cell.getFlowDemandIndStats().singleEntrySet()) {
            final MutableStat mutable = entry.getValue();
            final IndustrySpecAPI ind = settings.getIndustrySpec(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getName()+")",
                    "+" + NumFormat.engNotation((long)mutable.base), valueColor);
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, "Needed by " + ind.getName(),
                    "+" + NumFormat.formatMagnitudeAware(mod.value), valueColor);
            }
            for (StatMod mod : mutable.getPercentMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                    "+" + NumFormat.formatMagnitudeAware(mod.value) + "%", valueColor);
            }

            if (mutable.base > 0) {
            for (StatMod mod : mutable.getMultMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
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

    public static final void createComStockpilesChangeBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        tp.addPara("Current Stockpiles: %s", pad, highlight, NumFormat.engNotation(cell.getRoundedStored()));
        final int gridWidth = 430;
        final int valueWidth = 50;
        int rowCount = 0;

        tp.beginGridFlipped(gridWidth, 2, valueWidth, 5);

        tp.addToGrid(0, rowCount++, "Desired Stockpiles",
            NumFormat.engNotation(cell.getPreferredStockpile())
        );
        tp.addToGrid(0, rowCount++, "Latest Change", NumFormat.engNotation(cell.getFlowRealBalance()),
            cell.getFlowRealBalance() < 0f ? negative : highlight
        );

        { // Exports
            if (cell.inFactionExports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest in-faction exports", "+" +
                    NumFormat.engNotation(cell.inFactionExports)
                );
            }
            if (cell.globalExports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest global exports", "+" +
                    NumFormat.engNotation(cell.globalExports)
                );
            }
            if (cell.informalExports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest informal market exports", "+" +
                    NumFormat.engNotation(cell.informalExports)
                );
            }
        }

        { // Imports
            if (cell.inFactionImports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest in-faction imports", "+" +
                    NumFormat.engNotation(cell.inFactionImports)
                );
            }
            if (cell.globalImports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest global imports", "+" +
                    NumFormat.engNotation(cell.globalImports)
                );
            }
            if (cell.informalImports > 0f) {
                tp.addToGrid(0, rowCount++, "Latest informal market imports", "+" +
                    NumFormat.engNotation(cell.informalImports)
                );
            }
            if (cell.importEffectiveness < 1f && cell.getTotalImports(false) > 0f) {
                final float value = ((int) (cell.importEffectiveness * 100f)) / 100f;

                tp.setGridValueColor(negative);
                tp.addToGrid(0, rowCount++, "Shipping losses", Strings.X + value);
            }
        }

        tp.addGrid(0);
    }

    public static final void createComTradeLedgerSection(TooltipMakerAPI tp, CommodityCell cell) {
        final EconomyEngine engine = EconomyEngine.getInstance();
        final CommodityDomain dom = engine.getComDomain(cell.comID);
        final String marketName = cell.market.getName();
        final String comName = cell.spec.getName();
        
        final long exportIncomeLastMonth = dom.hasLedger(cell.marketID) ?
            dom.getLedger(cell.marketID).lastMonthExportIncome : 0l;
        final long exportIncomeThisMonth = dom.hasLedger(cell.marketID) ?
            dom.getLedger(cell.marketID).monthlyExportIncome : 0l;

        if (exportIncomeLastMonth > 1l || exportIncomeThisMonth > 1l) {
            tp.addPara(
                marketName + " profitably exported %s units of " + comName + " and accounted for %s of the global market share. They generated %s last month and %s so far this month.",
                opad, highlight,
                NumFormat.engNotation(cell.getTotalExports()),
                engine.info.getExportMarketShare(cell.comID, cell.marketID) + "%",
                NumFormat.formatCredit(exportIncomeLastMonth),
                NumFormat.formatCredit(exportIncomeThisMonth)
            );
        } else if (cell.getTotalExports() < 1f) {
            tp.addPara("No local production to export for today.", opad);
        } else if (exportIncomeLastMonth < 1l && exportIncomeThisMonth < 1l) {
            tp.addPara(
                marketName + " exported %s units of " + comName + " and accounted for %s of the global market share. Export income is not tracked for non-player colonies.",
                opad, highlight,
                NumFormat.engNotation(cell.getTotalExports()),
                engine.info.getExportMarketShare(cell.comID, cell.marketID) + "%"
            );
        }

        if (cell.getFlowCanNotExport() > 0f) {
            tp.addPara(
                "Exports are reduced by %s due to insufficient importers.",
                pad, negative, NumFormat.engNotation((int)cell.getFlowCanNotExport())
            );
        }

        final long importExpenseLastMonth = dom.hasLedger(cell.marketID) ?
            dom.getLedger(cell.marketID).lastMonthImportExpense : 0l;
        final long importExpenseThisMonth = dom.hasLedger(cell.marketID) ?
            dom.getLedger(cell.marketID).monthlyImportExpense : 0l;

        if (importExpenseLastMonth > 1l || importExpenseThisMonth > 1l) {
            tp.addPara(
                marketName + " imported %s units of " + comName + " and accounted for %s of the global market share. They expended %s last month and %s so far this month.",
                opad, highlight,
                NumFormat.engNotation(cell.getTotalImports(false)),
                engine.info.getImportMarketShare(cell.comID, cell.marketID) + "%",
                NumFormat.formatCredit(importExpenseLastMonth),
                NumFormat.formatCredit(importExpenseThisMonth)
            );
        } else if (cell.getTotalImports(false) < 1f) {
            tp.addPara("No local demand.", opad);
        } else if (exportIncomeLastMonth < 1l && exportIncomeThisMonth < 1l) {
            tp.addPara(
                marketName + " imported %s units of " + comName + " and accounted for %s of the global market share. Import expenses are not tracked for non-player colonies.",
                opad, highlight,
                NumFormat.engNotation(cell.getTotalImports(false)),
                engine.info.getImportMarketShare(cell.comID, cell.marketID) + "%"
            );
        }
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final FactionSpecAPI faction, final boolean addRatioColors
    ) {
        final Color color = getStockpileColor(ratio, faction, addRatioColors);
        return getStockpilesIcon(ratio, size, parent, color, null, false, false);
    }

    public static final Base getStockpilesIcon(final CommodityCell cell, final int size,
        final UIPanelAPI parent, final Color iconColor
    ) {
        return getStockpilesIcon(cell.getDesiredAvailabilityRatio(), size, parent, iconColor,
            null, false, cell.getBaseDemand(true) < 0.1f
        );
    }

    public static final Base getStockpilesIcon(final float ratio, final int size,
        final UIPanelAPI parent, final Color iconColor, final Color bgColor, final boolean drawBorder,
        final boolean useNoDemandIcon
    ) {
        final String iconPath;
        if (useNoDemandIcon) {
            iconPath = STOCKPILES_NO_DEMAND_PATH;
        } else if (ratio <= 0.25f) {
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
        if (ratio <= 0.25f) return UIColors.COLOR_DEFICIT;
        if (ratio <= 0.5f) return UIColors.COLOR_IMPORT;
        if (ratio <= 0.75f) return UIColors.COLOR_LOCAL_PROD;
        return UIColors.COLOR_NOT_EXPORTED;
    }

    private static final Comparator<CommodityCell> createSellComparator(int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, true, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, true, true);
            return Integer.compare(price2, price1);
        };
    }

    private static final Comparator<CommodityCell> createBuyComparator(int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, false, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, false, true);
            return Integer.compare(price1, price2);
        };
    }
}
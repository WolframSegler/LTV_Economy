package wfg.ltv_econ.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.strings.Income.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

public class TooltipUtils {
    private static final String TP_ARROW = settings.getSpriteName("ui", "cargoTooltipArrow");
    private static final int GRID_W = 430;
    private static final int VALUE_W = 50;

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
        final EconomyEngine engine = EconomyEngine.instance();
        final SectorAPI sector = Global.getSector();
        final int baseY = (int) tp.getHeightSoFar();

        if (!sector.getIntelManager().isPlayerInRangeOfCommRelay()) {
            if (showExplanation) {
                tp.addPara(str("uiTpTxtRemotePrices"), gray, pad);
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
                tp.addPara(str("uiBestPlacesToSell"), pad);
                final PositionAPI prevPos = tp.getPrev().getPosition();
                final int relativeY = (int) (baseY + tp.getPosition().getY() + prevPos.getHeight() - prevPos.getY());

                tp.beginTable(sector.getPlayerFaction(), rowH, new Object[] {
                    str("uiTablePrice"), 100, str("uiTableDesired"), 70, str("uiTableDeficit"), 70, str("uiTableLocation"), 230,
                    str("uiTableStarSystem"), 140, str("uiTableDistance"), 80
                });
                countingMap.clear();

                int rowCount = 0;
                for (CommodityCell cell : marketList) {
                    final MarketAPI market = cell.market;
                    if (countingMap.getCount(market.getFactionId()) >= 3) continue;
                    countingMap.add(market.getFactionId());

                    final int target = (int) ((cell.getTargetStored() / 100f) * 100f);
                    final boolean lowTarget = target < 100;
                    final String lessThanSymbol = lowTarget ? "<" : "";
                    final Color labelColor = lowTarget ? gray : highlight;

                    final double deficit = cell.getStoredDeficit();
                    final boolean deficitPresent = deficit > 10d;
                    final Color deficitColor = deficitPresent ? negative : gray;
                    final String quantityLabel = deficitPresent ? NumFormat.engNotate(deficit) : "---";

                    final String factionName = market.getFaction().getDisplayName();
                    String location = str("uiLocationHyperspaceCapitalized");
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
                        Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, 0d, true, true) / econUnit),
                        labelColor,
                        lessThanSymbol + NumFormat.engNotate(target),
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

                    final Base arrowPanel = new Base(tp, 20, 20, TP_ARROW, null, null);

                    final Vector2f playerLoc = sector.getPlayerFleet().getLocationInHyperspace();
                    final Vector2f targetLoc = market.getLocationInHyperspace();

                    NativeUiUtils.rotateSprite(playerLoc, targetLoc, arrowPanel.getSprite());

                    final int arrowY = relativeY + rowH * (2 + rowCount) + pad;
                    tp.addCustom(arrowPanel.getPanel(), 0f).getPosition().inTL(610, arrowY);

                    ++rowCount;
                    if (rowCount >= rowsPerTable) break;
                }

                tp.setHeightSoFar(relativeY);
                NativeUiUtils.resetFlowLeft(tp, hpad);
                tp.addTable("", 0, pad);
            }
        }

        if (showBestBuy) {
            Collections.sort(marketList, createBuyComparator(econUnit));
            if (!marketList.isEmpty()) {

                tp.addPara(str("uiBestPlacesToBuy"), opad);
                final PositionAPI prevPos = tp.getPrev().getPosition();
                final int relativeY = (int) (baseY + tp.getPosition().getY() + prevPos.getHeight() - prevPos.getY());
                tp.beginTable(sector.getPlayerFaction(), 20, new Object[] {
                    str("uiTablePrice"), 100, str("uiTableStored"), 70, str("uiTableExcess"), 70, str("uiTableLocation"), 230,
                    str("uiTableStarSystem"), 140, str("uiTableDistance"), 80 
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

                    final double excess = cell.getStoredExcess();
                    final boolean hasExcess = excess > 10d;

                    final String factionName = market.getFaction().getDisplayName();
                    String location = str("uiLocationHyperspaceCapitalized");
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
                        Misc.getDGSCredits(cell.computeVanillaPrice(econUnit, 0d, false, true) / econUnit),
                        highlight,
                        availableStr + NumFormat.engNotate(availableValue),
                        hasExcess ? positive : gray,
                        hasExcess ? NumFormat.engNotate(excess) : "---",
                        Alignment.LMID,
                        market.getFaction().getBaseUIColor(),
                        market.getName() + " - " + factionName,
                        locationColor,
                        location,
                        highlight,
                        Misc.getRoundedValueMaxOneAfterDecimal(distance)
                    );

                    final Base arrowPanel = new Base(tp, 20, 20, TP_ARROW, null, null);

                    final Vector2f playerLoc = sector.getPlayerFleet().getLocationInHyperspace();
                    final Vector2f targetLoc = market.getLocationInHyperspace();

                    NativeUiUtils.rotateSprite(playerLoc, targetLoc, arrowPanel.getSprite());

                    final int arrowY = relativeY + rowH * (2 + rowCount) + pad;
                    tp.addCustom(arrowPanel.getPanel(), 0f).getPosition().inTL(610, arrowY);
                    NativeUiUtils.resetFlowLeft(tp, hpad);

                    rowCount++;
                    if (rowCount >= rowsPerTable) break;
                }

                tp.setHeightSoFar(relativeY);
                NativeUiUtils.resetFlowLeft(tp, hpad);
                tp.addTable("", 0, pad);
            }
        }

        if (showExplanation) {
            tp.addPara(str("uiTpTxtcargoCom1"), gray, opad);

            final Color txtColor = Misc.setAlpha(highlight, 155);
            tp.addPara(str("uiTpTxtcargoCom2"), opad, gray, txtColor, Integer.toString(econUnit));
        }
    }

    public static final void createComStockpilesChangeBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        tp.addPara(str("uiCurrentStockpiles"), pad, highlight, NumFormat.engNotate(cell.getRoundedStored()));
        int rowCount = 0;

        tp.beginGridFlipped(GRID_W, 2, VALUE_W, hpad);

        tp.addToGrid(0, rowCount++, str("uiDesiredStockpilesTxt"),
            NumFormat.engNotate(cell.getTargetStored())
        );
        tp.addToGrid(0, rowCount++, str("uiLatestChange"), NumFormat.engNotate(cell.getQuantumNetChange()),
            cell.getQuantumNetChange() < 0f ? negative : highlight
        );

        { // Exports
            if (cell.inFactionExports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiDispatchedInFactionExports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.inFactionExports)
                );
            }
            if (cell.globalExports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiDispatchedGlobalExports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.globalExports)
                );
            }
            if (cell.informalExports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiDailyInformalExports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.informalExports)
                );
            }
        }

        { // Imports
            if (cell.inFactionImports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiIncomingInFactionImports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.inFactionImports)
                );
            }
            if (cell.globalImports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiIncomingGlobalImports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.globalImports)
                );
            }
            if (cell.informalImports > 0f) {
                tp.addToGrid(0, rowCount++, str("uiDailyInformalImports"), "+" +
                    NumFormat.formatMagnitudeAware(cell.informalImports)
                );
            }
        }

        tp.addGrid(0);
    }

    public static final void createComTargetBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();

        final float dailyTarget = cell.getTargetQuantum(true);
        int rowCount = 0;

        tp.addPara(str("uiPrefixDemand"), pad, highlight, NumFormat.formatMagnitudeAware(dailyTarget));

        tp.beginGridFlipped(GRID_W, 2, VALUE_W, hpad);

        final ArrayMutableStat targetStat = cell.getTargetQuantumStat();

        for (StatMod mod : targetStat.getBaseMods().values()) {
            final String formatted = (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value);
            tp.addToGrid(0, rowCount++, mod.desc, formatted, mod.value < 0 ? highlight : negative);
        }

        for (StatMod mod : targetStat.getPercentMods().values()) {
            final String formatted = (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value) + "%";
            tp.addToGrid(0, rowCount++, mod.desc, formatted, mod.value < 0 ? highlight : negative);
        }

        for (StatMod mod : targetStat.getMultMods().values()) {
            final String formatted = Strings.X + NumFormat.formatMagnitudeAware(mod.value);
            tp.addToGrid(0, rowCount++, mod.desc, formatted, mod.value < 1f ? highlight : negative);
        }

        for (StatMod mod : targetStat.getFlatMods().values()) {
            final String formatted = (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value);
            tp.addToGrid(0, rowCount++, mod.desc, formatted, mod.value < 0 ? highlight : negative);
        }

        if (rowCount <= 0) {
            tp.addToGrid(0, rowCount++, str("noLocalDemand"), "", base);
        }

        tp.addGrid(0);
    }

    public static final void createComProductionBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara(str("uiPrefixProduction"), pad, highlight, NumFormat.formatMagnitudeAware(cell.getProduction(true)));
        int rowCount = 0;

        tp.beginGridFlipped(GRID_W, 2, VALUE_W, hpad);

        for (Map.Entry<String, MutableStat> entry : cell.getIndProductionStats().singleEntrySet()) {
            final MutableStat mutable = entry.getValue();
            final IndustrySpecAPI ind = settings.getIndustrySpec(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getName()+")",
                    (mutable.getBaseValue() >= 0f ? "+" : "") + NumFormat.formatMagnitudeAware(mutable.getBaseValue()),
                    mutable.getBaseValue() < 0f ? negative : highlight
                );

                for (StatMod mod : mutable.getPercentMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                        (mod.value >= 0f ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value) + "%",
                        mod.value < 0f ? negative : highlight
                    );
                }

                for (StatMod mod : mutable.getMultMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                            Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                        mod.value < 1f ? negative:highlight
                    );
                }
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                    (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 0f ? negative : highlight
                );
            }
        }

        if (cell.getProduction(false) > 0) {
            final ArrayMutableStat mutable = cell.getProductionStat();

            if (mutable.getModifiedInt() > 0) {
                for (StatMod mod : mutable.getPercentMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc,
                        (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value) + "%",
                        mod.value < 0f ? negative : highlight
                    );
                }

                for (StatMod mod : mutable.getMultMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc,
                        Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                        mod.value < 1f ? negative:highlight
                    );
                }
            }
            
            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 0f ? negative : highlight
                );
            }
        }

        if (rowCount <= 0) {
            title.setText(str("noLocalProduction"));
            title.setHighlight("");
        }

        tp.addGrid(0);
    }

    public static final void createComConsumptionBreakdown(TooltipMakerAPI tp, CommodityCell cell) {
        int rowCount = 0;
        
        tp.setParaFontDefault();
        final LabelAPI title = tp.addPara(str("uiPrefixConsumption"), opad, highlight,
            NumFormat.formatMagnitudeAware(cell.getConsumption(true))
        );

        tp.beginGridFlipped(GRID_W, 2, VALUE_W, hpad);

        for (Map.Entry<String, MutableStat> entry : cell.getIndConsumptionStats().singleEntrySet()) {
            final MutableStat mutable = entry.getValue();
            final IndustrySpecAPI ind = settings.getIndustrySpec(entry.getKey());

            if (mutable.getModifiedInt() > 0) {
                tp.addToGrid(0, rowCount++, BaseIndustry.BASE_VALUE_TEXT + " ("+ind.getName()+")",
                    (mutable.getBaseValue() >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mutable.getBaseValue()),
                    mutable.getBaseValue() < 0f ? highlight : negative
                );

                for (StatMod mod : mutable.getPercentMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                        (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value) + "%",
                        mod.value < 0f ? highlight : negative
                    );
                }

                for (StatMod mod : mutable.getMultMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc + " ("+ind.getName()+")",
                        Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                        mod.value < 0f ? highlight : negative
                    );
                }
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, strf("uiPrefixNeededByIndustry", ind.getName()),
                    (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 0f ? highlight : negative
                );
            }
        }

        if (cell.getConsumption(false) > 0f) {
            final ArrayMutableStat mutable = cell.getConsumptionStat();
            if (mutable.getModifiedInt() > 0) {
                for (StatMod mod : mutable.getPercentMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc,
                        (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value) + "%",
                        mod.value < 0f ? highlight : negative
                    );
                }
                for (StatMod mod : mutable.getMultMods().values()) {
                    tp.addToGrid(0, rowCount++, mod.desc,
                        Strings.X + NumFormat.formatMagnitudeAware(mod.value),
                        mod.value < 1f ? highlight:negative
                    );
                }
            }

            for (StatMod mod : mutable.getFlatMods().values()) {
                tp.addToGrid(0, rowCount++, mod.desc,
                    (mod.value >= 0 ? "+" : "") + NumFormat.formatMagnitudeAware(mod.value),
                    mod.value < 0f ? highlight : negative
                );
            }
        }

        if (rowCount <= 0) {
            title.setText(str("noLocalConsumption"));
            title.setHighlight("");
        }

        tp.addGrid(0);
    }

    public static final void createComTradeLedgerSection(TooltipMakerAPI tp, CommodityCell cell) {
        final String comID = cell.comID;
        final String marketID = cell.marketID;
        final String comName = cell.spec.getName();
        final String marketName = cell.market.getName();
        final EconomyEngine engine = EconomyEngine.instance();
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketID);

        final long exportIncomeLastMonth = ledger.getLastMonth(TRADE_EXPORT_KEY + comID);
        final long exportIncomeThisMonth = ledger.getCurrentMonth(TRADE_EXPORT_KEY + comID);

        if (exportIncomeLastMonth > 0l || exportIncomeThisMonth > 0l) {
            tp.addPara(strf("uiTpTxtLedger1", marketName, comName), opad, highlight,
                NumFormat.formatMagnitudeAware(cell.getTotalExports()),
                engine.info.getExportMarketShare(comID, marketID) + "%",
                NumFormat.formatCredit(exportIncomeLastMonth),
                NumFormat.formatCredit(exportIncomeThisMonth)
            );
        } else {
            tp.addPara(str("uiTpTxtLedger2"), opad);
        }

        final long importExpenseLastMonth = ledger.getLastMonth(TRADE_IMPORT_KEY + comID);
        final long importExpenseThisMonth = ledger.getCurrentMonth(TRADE_IMPORT_KEY + comID);

        if (importExpenseLastMonth > 0l || importExpenseThisMonth > 0l) {
            tp.addPara(strf("uiTpTxtLedger3", marketName, comName), opad, highlight,
                NumFormat.formatMagnitudeAware(cell.getTotalImports()),
                engine.info.getImportMarketShare(comID, marketID) + "%",
                NumFormat.formatCredit(importExpenseLastMonth),
                NumFormat.formatCredit(importExpenseThisMonth)
            );
        } else {
            tp.addPara(str("uiTpTxtLedger4"), opad);
        }
    }

    public static final TooltipCreator createAccessTp(MarketAPI market) {
        return new TooltipCreator() {
        public boolean isTooltipExpandable(Object args) {
            return false;
        }

        public float getTooltipWidth(Object args) {
            return 450f;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object args) {
            final StatBonus accessMod = market.getAccessibilityMod();
            final FactionAPI faction = market.getFaction();
            final Color facBase = faction.getBaseUIColor();

            tp.addTitle(str("uiTitleAccessibility"), facBase);
            final int access = Math.round(accessMod.computeEffective(0f) * 100f);

            tp.addPara(str("uiTpTxtAccessibility1"), opad);
            tp.addPara(str("uiAccessibilitySuffix"), opad, access <= 0 ? negative : highlight, access + "%");
            tp.addStatModGrid(getTooltipWidth(null), 50f, opad, pad, accessMod, new StatModValueGetter(){

                public String getPercentValue(StatMod mod) {return null;}
                public String getMultValue(StatMod mod) {return null;}

                public Color getModColor(StatMod mod) {
                    if (mod.value < 0f) return negative;
                    return null;
                }

                public String getFlatValue(StatMod mod) {
                    return (mod.value >= 0f ? "+" : "") + Math.round(mod.value * 100f) + "%";
                }
            });

            tp.addSectionHeading(str("uiTitleTradeCycle"), facBase, faction.getDarkUIColor(), Alignment.MID, opad);
            tp.addPara(str("uiTpTxtTradeCycle"), opad, new Color[] {highlight, base},
                Integer.toString(EconConfig.TRADE_INTERVAL), str("uiBtnTitleEconomy")
            );

            tp.addPara(str("uiTpTxtAccessibility2"), opad, highlight,Strings.X + Float.toString(EconConfig.FORCED_FUEL_IMPORT_COST_MULT));

            tp.addPara(str("uiTpTxtAccessibility3"), pad, highlight, NumFormat.engNotate(EconomyEngine.instance().getActiveMissions().size()));
        }
        };
    }

    // PRIVATE METHODS
    private static final Comparator<CommodityCell> createSellComparator(int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, 0d, true, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, 0d, true, true);
            return Integer.compare(price2, price1);
        };
    }

    private static final Comparator<CommodityCell> createBuyComparator(int econUnit) {
        return (s1, s2) -> {
            int price1 = (int) s1.computeVanillaPrice(econUnit, 0d, false, true);
            int price2 = (int) s2.computeVanillaPrice(econUnit, 0d, false, true);
            return Integer.compare(price1, price2);
        };
    }
}
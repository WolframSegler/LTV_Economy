package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.ltv_econ.constants.strings.Income.*;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.container.DockPanel;
import wfg.native_ui.ui.widget.RadioPanel;
import wfg.native_ui.ui.widget.RadioPanel.LayoutMode;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.util.NumFormat;

public class IncomeBreakdownDialog extends DockPanel {
    private static final int WIDTH = 440;
    private static final int GAP = 100;
    private static final int I_WIDTH = WIDTH - hpad * 3;

    private final MarketAPI market;
    private boolean lastMonth = true;
    private float scrollLen = 0f;

    public IncomeBreakdownDialog(final MarketAPI market) {
        super(Attachments.getCoreUI(), WIDTH, screenH - GAP * 2, Side.LEFT);
        this.market = market;

        offsetY = GAP;
        bgAlpha = 0.95f;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();
        final LabelAPI title = settings.createLabel("Income Breakdown", Fonts.INSIGNIA_LARGE);
        add(title).inTL(opad, opad * 2);

        final TooltipMakerAPI scrollPanel = ComponentFactory.createTooltip(I_WIDTH, true);

        final RadioPanel monthSwitch = new RadioPanel(m_panel, 110, 18, LayoutMode.HORIZONTAL)
            .addOption("Prev.", lastMonth)
            .addOption("Curr.", !lastMonth);
        monthSwitch.optionSelected = code -> {
            lastMonth = code == 0;
            scrollLen = scrollPanel.getExternalScroller().getYOffset();
            buildUI();
        };
        monthSwitch.buildUI();
        add(monthSwitch).inTR(opad*2, opad*2);

        incomeBreakdownUI(scrollPanel);

        final int offset = opad * 2 + 30;
        final float scrollPanelH = pos.getHeight() - offset - opad;
        ComponentFactory.addTooltip(scrollPanel, scrollPanelH, true, m_panel).inTL(pad, offset);

        scrollPanel.getExternalScroller().setYOffset(Arithmetic.clamp(
            scrollLen, 0f, scrollPanel.getHeightSoFar() - scrollPanelH
        ));
    }

    public final void incomeBreakdownUI(final TooltipMakerAPI tp) {
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(market);
        final EconomyEngine engine = EconomyEngine.instance();
        final PlayerMarketData data = engine.getPlayerMarketData(market.getId());
        final List<CommodityDomain> domains = engine.getComDomains();
        final List<String> policyKeys = data != null ? MarketPolicy.getPolicyLedgerKeys(data) : Collections.emptyList();
        final FactionAPI faction = market.getFaction();
        final Color base = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();
        final float rowH = 20f;
        
        // DATA
        
        final List<String> allKeys = new ArrayList<>((lastMonth ? ledger.getAllLast() : ledger.getAllCurrent()).keySet());
        final ArrayList<Industry> industries = new ArrayList<>(market.getIndustries());
        final ArrayMap<String, Long> indIncomeMap = new ArrayMap<>(industries.size());
        final ArrayMap<String, Long> indUpkeepMap = new ArrayMap<>(industries.size());
        long totalIndustryIncome = 0l;
        long totalIndustryUpkeep = 0l;
        for (Industry ind : industries) {
            final String indID = ind.getId();
            final String incKey = INDUSTRY_INCOME_KEY + indID;
            final long inc = lastMonth ? ledger.getLastMonth(incKey) : ledger.getCurrentMonth(incKey);
            totalIndustryIncome += inc;
            indIncomeMap.put(indID, inc);
            allKeys.remove(incKey);
            
            final String upKey = INDUSTRY_UPKEEP_KEY + indID;
            final long up = lastMonth ? ledger.getLastMonth(upKey) : ledger.getCurrentMonth(upKey);
            totalIndustryUpkeep += up;
            indUpkeepMap.put(indID, up);
            allKeys.remove(upKey);
        }
        industries.sort((i1, i2) -> Long.compare(
            indIncomeMap.getOrDefault(i2.getId(), 0l),
            indIncomeMap.getOrDefault(i1.getId(), 0l)
        ));
        long policyCosts = 0l;
        for (String key : policyKeys) {
            policyCosts += lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
            allKeys.remove(key);
        }

        allKeys.remove(WORKER_WAGES_KEY);
        allKeys.remove(FACTION_CREW_WAGES_KEY);
        allKeys.remove(FACTION_SHIP_PRODUCTION_KEY);
        allKeys.remove(TRADE_FLEET_SHIPMENT_KEY);
        allKeys.remove(COLONY_HAZARD_PAY_KEY);
        allKeys.remove(PLAYER_MARKET_TRANSACTION_KEY);

        final long netIncome = lastMonth ? ledger.getNetLastMonth() : ledger.getNetCurrentMonth();
        final long grossIncome = lastMonth ? ledger.getIncomeLastMonth() : ledger.getIncomeCurrentMonth();
        final long grossExpense = lastMonth ? ledger.getExpenseLastMonth() : ledger.getExpenseCurrentMonth();
        final String indIncome = NumFormat.formatCreditAbs(totalIndustryIncome);
        final String indUpkeep = NumFormat.formatCreditAbs(totalIndustryUpkeep);
        final long exportIncome = engine.info.getExportIncome(market, lastMonth);
        final long importExpense = engine.info.getImportExpense(market, lastMonth);
        final long monthlyWages = lastMonth ? ledger.getLastMonth(WORKER_WAGES_KEY) : ledger.getCurrentMonth(WORKER_WAGES_KEY);
        final long factionShipsCrewWages = lastMonth ? ledger.getLastMonth(FACTION_CREW_WAGES_KEY) : ledger.getCurrentMonth(FACTION_CREW_WAGES_KEY);
        final long factionShipsProd = lastMonth ? ledger.getLastMonth(FACTION_SHIP_PRODUCTION_KEY) : ledger.getCurrentMonth(FACTION_SHIP_PRODUCTION_KEY);
        final long tradeFleetShipment = lastMonth ? ledger.getLastMonth(TRADE_FLEET_SHIPMENT_KEY) : ledger.getCurrentMonth(TRADE_FLEET_SHIPMENT_KEY);
        final String policyCostStr = NumFormat.formatCreditAbs(policyCosts);
        final int incentive = (int) (lastMonth ? ledger.getLastMonth(COLONY_HAZARD_PAY_KEY) : ledger.getCurrentMonth(COLONY_HAZARD_PAY_KEY));
        final int sumbarketTransaction = (int) (lastMonth ? ledger.getLastMonth(PLAYER_MARKET_TRANSACTION_KEY) : ledger.getCurrentMonth(PLAYER_MARKET_TRANSACTION_KEY));

        // VISUALS

        tp.addTitle(lastMonth ? "Previous Month" : "Current Month", base);
        tp.addSpacer(opad);

        tp.addPara("Net income: %s", opad, netIncome > 0l ? highlight : negative, NumFormat.formatCredit(netIncome));
        tp.addPara("Gross income: %s", opad, highlight, NumFormat.formatCreditAbs(grossIncome));
        tp.addPara("Gross expense: %s", opad, negative, NumFormat.formatCreditAbs(grossExpense));

        tp.addSectionHeading("Industries & Structures", base, dark, Alignment.MID, opad);

        tp.addPara("Income: %s", opad, highlight, indIncome);
        tp.addPara("Upkeep: %s", opad, negative, indUpkeep);

        tp.beginTable(Global.getSector().getPlayerFaction(), rowH, new Object[] {
            "Industry", 220, "Income", 90, "Upkeep", 90
        });

        for (Industry ind : industries) {
            tp.addRow(
                Alignment.LMID, text_color,
                ind.getCurrentName(),
                Alignment.MID, highlight,
                NumFormat.formatCreditAbs(indIncomeMap.get(ind.getId())),
                Alignment.MID, negative,
                NumFormat.formatCreditAbs(-indUpkeepMap.get(ind.getId()))
            );
        }
        tp.addTable("", 0, pad);

        tp.addPara("Income multiplier: %s", opad, highlight,
            Math.round(market.getIncomeMult().getModifiedValue() * 100f) + "%"
        );
        tp.addStatModGrid(I_WIDTH, 50f, opad, pad, market.getIncomeMult(), true, null);

        tp.addPara("Upkeep multiplier: %s", opad, highlight,
            Math.round(market.getUpkeepMult().getModifiedValue() * 100f) + "%"
        );
        tp.addStatModGrid(
            I_WIDTH, 50f, opad, pad, market.getUpkeepMult(), true, null
        );

        tp.addPara("These modifiers affect industry income & upkeep only.", gray, opad);

        tp.addSectionHeading("Imports & Exports", base, dark, Alignment.MID, opad);
        
        tp.addPara("Export Income: %s", opad, highlight, NumFormat.formatCreditAbs(exportIncome));
        tp.addPara("Import Expense: %s", opad, negative, NumFormat.formatCreditAbs(importExpense));

        if (exportIncome > 0l) {
            tp.beginTable(faction, rowH, "Commodity", 220f, "Income", 180f);
 
            domains.sort((c1, c2) ->
                Long.compare(
                    lastMonth ? ledger.getLastMonth(TRADE_EXPORT_KEY + c2.comID) : ledger.getCurrentMonth(TRADE_EXPORT_KEY + c2.comID),
                    lastMonth ? ledger.getLastMonth(TRADE_EXPORT_KEY + c1.comID) : ledger.getCurrentMonth(TRADE_EXPORT_KEY + c1.comID)
                )
            );
            for (CommodityDomain com : domains) {
                final String key = TRADE_EXPORT_KEY + com.comID;
                final long income = lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
                allKeys.remove(key);
                if (income < 1) continue;

                tp.addRow(
                    Alignment.LMID, text_color, com.spec.getName(),
                    highlight, NumFormat.formatCreditAbs(income)
                );
            }

            tp.addTable("No exports", 0, opad);
        }

        if (importExpense > 0l) {
            tp.beginTable(faction, rowH, "Commodity", 220f, "Expense", 180f);

            domains.sort((c1, c2) ->
                Long.compare(
                    lastMonth ? ledger.getLastMonth(TRADE_IMPORT_KEY + c2.comID) : ledger.getCurrentMonth(TRADE_IMPORT_KEY + c1.comID),
                    lastMonth ? ledger.getLastMonth(TRADE_IMPORT_KEY + c1.comID) : ledger.getCurrentMonth(TRADE_IMPORT_KEY + c2.comID)
                )
            );
            for (CommodityDomain com : domains) {
                final String key = TRADE_IMPORT_KEY + com.comID;
                final long expense = lastMonth ? -ledger.getLastMonth(key) : -ledger.getCurrentMonth(key);
                allKeys.remove(key);
                if (expense < 1l) continue;

                tp.addRow(
                    Alignment.LMID, text_color, com.spec.getName(),
                    negative, NumFormat.formatCreditAbs(expense)
                );
            }

            tp.addTable("No imports", 0, opad);
        }

        tp.addSectionHeading("Wages & Population", base, dark, Alignment.MID, opad);

        tp.addPara(getDesc(COLONY_HAZARD_PAY_KEY) + ": %s", opad, negative, NumFormat.formatCreditAbs(incentive));

        tp.addPara(getDesc(WORKER_WAGES_KEY) + ": %s", opad, negative, NumFormat.formatCreditAbs(monthlyWages));

        tp.addPara(getDesc(FACTION_CREW_WAGES_KEY) + ": %s", opad, negative, NumFormat.formatCreditAbs(factionShipsCrewWages));
        
        tp.addSectionHeading("Faction Expenses", base, dark, Alignment.MID, opad);

        tp.addPara(getDesc(FACTION_SHIP_PRODUCTION_KEY) + ": %s", opad, negative, NumFormat.formatCreditAbs(factionShipsProd));
        
        tp.addPara(getDesc(TRADE_FLEET_SHIPMENT_KEY) + ": %s", opad, negative, NumFormat.formatCreditAbs(tradeFleetShipment));

        tp.addSectionHeading("Policy Expenses", base, dark, Alignment.MID, opad);

        tp.addPara("Total Costs: %s", opad, negative, policyCostStr);

        for (String key : policyKeys) {
            final long cost = lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
            if (cost == 0l) continue;

            final String desc = lastMonth ? ledger.getDescLastMonth(key) : ledger.getDescCurrentMonth(key);
            tp.addPara("  - " + desc + ": %s", opad, negative, NumFormat.formatCreditAbs(cost));
        }

        tp.addSectionHeading("Other Transactions", base, dark, Alignment.MID, opad);

        if (sumbarketTransaction > 0l) {
            tp.addPara(getDesc(PLAYER_MARKET_TRANSACTION_KEY) + ": %s", opad, highlight,
                NumFormat.formatCreditAbs(sumbarketTransaction)
            );
        }

        for (String key : allKeys) {
            final long value = lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
            if (value == 0l) continue;

            final String desc = lastMonth ? ledger.getDescLastMonth(key) : ledger.getDescCurrentMonth(key);
            tp.addPara(desc + ": %s", opad, value < 0l ? negative : highlight, NumFormat.formatCreditAbs(value));
        }

        tp.addPara(REDISTRIBUTION_DISCLAIMER, gray, opad);
    }
}
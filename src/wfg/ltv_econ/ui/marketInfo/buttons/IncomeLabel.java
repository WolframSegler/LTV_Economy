package wfg.ltv_econ.ui.marketInfo.buttons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.ltv_econ.ui.marketInfo.dialogs.IncomeBreakdownDialog;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.functional.DockClickable;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.util.NumFormat;

import static wfg.ltv_econ.constants.strings.Income.*;
import static wfg.native_ui.util.UIConstants.*;

public class IncomeLabel extends DockClickable<IncomeBreakdownDialog> implements HasTooltip, UIBuildableAPI {

    private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
    private final MarketAPI market;

    public IncomeLabel(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, () -> new IncomeBreakdownDialog(market));

        this.market = market;

        final float TP_WIDTH = 500f;
        tooltip.expandable = true;
        tooltip.expandTxt = "%s Show details";
        tooltip.unexpandTxt = "%s Hide";
        tooltip.width = TP_WIDTH;
        tooltip.positioner = (tp, expanded) -> {
            NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.LeftTop, 50);
        };
        tooltip.builder = (tp, expanded) -> {
            final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(market);
            final EconomyInfo info = EconomyEngine.instance().info;
            final List<CommodityDomain> domains = EconomyEngine.instance().getComDomains();
            final FactionAPI faction = market.getFaction();
            final Color base = faction.getBaseUIColor();
            final Color dark = faction.getDarkUIColor();
            final float rowH = 20f;

            final LabelAPI title = tp.addTitle("Monthly Income & Upkeep [1]", base);
            title.setHighlightColor(highlight);
            title.setHighlight("1");
            final long income = ledger.getNetLastMonth();

            final String incomeTxt = NumFormat.formatCreditAbs(income);
            if (income >= 0l) {
                tp.addPara("The net monthly income of this colony last month was %s.", opad, highlight, incomeTxt);
            } else {
                tp.addPara("The net monthly upkeep for this colony last month was %s.", opad, negative, incomeTxt);
            }

            final ArrayList<Industry> industries = new ArrayList<>(market.getIndustries());
            final ArrayMap<String, Long> indIncomeMap = new ArrayMap<>(industries.size());
            final ArrayMap<String, Long> indUpkeepMap = new ArrayMap<>(industries.size());
            long totalIndustryIncome = 0;
            long totalIndustryUpkeep = 0;
            for (Industry ind : industries) {
                final String incKey = INDUSTRY_INCOME_KEY + ind.getId();
                final long inc = ledger.getLastMonth(incKey);
                totalIndustryIncome += inc;
                indIncomeMap.put(ind.getId(), inc);
                
                final String upKey = INDUSTRY_UPKEEP_KEY + ind.getId();
                final long up = ledger.getLastMonth(upKey);
                totalIndustryUpkeep += up;
                indUpkeepMap.put(ind.getId(), up);
            }
            final String indIncome = NumFormat.formatCredit(totalIndustryIncome);
            final String indUpkeep = NumFormat.formatCredit(-totalIndustryUpkeep);
            industries.sort((i1, i2) -> Long.compare(
                indIncomeMap.getOrDefault(i2.getId(), 0l),
                indIncomeMap.getOrDefault(i1.getId(), 0l)
            ));

            tp.addSectionHeading("Industries & Structures", base, dark, Alignment.MID, opad);

            tp.addPara("Income: %s", opad, highlight, indIncome);
            tp.addPara("Upkeep: %s", opad, negative, indUpkeep);

            if (expanded) {
                tp.addPara("Income multiplier: %s", opad, highlight,
                    Math.round(market.getIncomeMult().getModifiedValue() * 100f) + "%"
                );
                tp.addStatModGrid(TP_WIDTH, 50f, opad, pad, market.getIncomeMult(), true, null);

                tp.addPara("Upkeep multiplier: %s", opad, highlight,
                    Math.round(market.getUpkeepMult().getModifiedValue() * 100f) + "%"
                );
                tp.addStatModGrid(
                    TP_WIDTH, 50f, opad, pad, market.getUpkeepMult(), true, null
                );

                tp.addPara("These modifiers affect industry income & upkeep only.", gray, opad);

                tp.beginTable(Global.getSector().getPlayerFaction(), rowH, new Object[] {
                    "Industry", 250, "Income", 115, "Upkeep", 115
                });

                for (Industry ind : industries) {
                    tp.addRow(
                        Alignment.LMID, text_color,
                        ind.getCurrentName(),
                        Alignment.MID, highlight,
                        Misc.getDGSCredits(indIncomeMap.get(ind.getId())),
                        Alignment.MID, negative,
                        Misc.getDGSCredits(-indUpkeepMap.get(ind.getId()))
                    );
                }
                tp.addTable("", 0, pad);
            }

            tp.addSectionHeading("Imports & Exports", base, dark, Alignment.MID, opad);

            final int maxCommoditiesToDisplay = 8;
            final float extraPad = 30f;

            final long exportIncome = info.getExportIncome(market, true);
            tp.addPara("Last Month's Exports: %s", opad, highlight, NumFormat.formatCredit(exportIncome));
            if (exportIncome > 0l && expanded) {
                tp.beginTable(faction, rowH, "Commodity", 200f + extraPad, "Market share", 100f + extraPad, "Income", 100f + extraPad);
                int exportedCount = 0;
                for (CommodityDomain com : domains) {
                    if (ledger.getLastMonth(TRADE_EXPORT_KEY + com.comID) > 0l) {
                        ++exportedCount;
                    }
                }

                domains.sort((c1, c2) ->
                    Long.compare(
                        ledger.getLastMonth(TRADE_EXPORT_KEY + c2.comID),
                        ledger.getLastMonth(TRADE_EXPORT_KEY + c1.comID)
                    )
                );
                int comCount = 0;
                for (CommodityDomain com : domains) {
                    final String name = com.spec.getName();
                    final long comExportIncome = ledger.getLastMonth(TRADE_EXPORT_KEY + com.comID);
                    if (comExportIncome < 1) continue;

                    final int exportMarketShare = info.getExportMarketShare(
                        com.spec.getId(), market.getId()
                    );

                    tp.addRow(Alignment.LMID, text_color, " " + name, highlight, exportMarketShare + "%", highlight, NumFormat.formatCredit(comExportIncome));
                    comCount++;
                    if (comCount + 1 > maxCommoditiesToDisplay && exportedCount - comCount > 1) {
                        break;
                    }
                }

                tp.addTable("No exports", exportedCount - comCount, opad);
            }

            final long importExpense = info.getImportExpense(market, true);
            tp.addPara("Last Month's Imports: %s", opad, negative, NumFormat.formatCredit(importExpense));
            if (importExpense > 0l && expanded) {
                tp.beginTable(faction, rowH, "Commodity", 200f + extraPad, "Market share", 100f + extraPad, "Expense", 100f + extraPad);
                int importedCount = 0;
                for (CommodityDomain com : domains) {
                    if (ledger.getLastMonth(TRADE_IMPORT_KEY + com.comID) < 0l) {
                        ++importedCount;
                    }
                }

                domains.sort((c1, c2) ->
                    Long.compare(
                        ledger.getLastMonth(TRADE_IMPORT_KEY + c2.comID),
                        ledger.getLastMonth(TRADE_IMPORT_KEY + c1.comID)
                    )
                );
                int comCount = 0;
                for (CommodityDomain com : domains) {
                    final String name = com.spec.getName();
                    final long comImportExpense = -ledger.getLastMonth(TRADE_IMPORT_KEY + com.comID);
                    if (comImportExpense < 1l) continue;

                    final int importMarketShare = info.getImportMarketShare(
                        com.spec.getId(), market.getId()
                    );

                    tp.addRow(Alignment.LMID, text_color, " " + name, negative, importMarketShare + "%", negative, NumFormat.formatCredit(comImportExpense));
                    comCount++;
                    if (comCount + 1 > maxCommoditiesToDisplay && importedCount - comCount > 1) {
                        break;
                    }
                }

                tp.addTable("No imports", importedCount - comCount, opad);
            }

            final long monthlyWages = ledger.getLastMonth(WORKER_WAGES_KEY);
            if (monthlyWages > 0l) {
                tp.addPara(getDesc(WORKER_WAGES_KEY) + ": %s", opad, negative, NumFormat.formatCredit(monthlyWages));
            }

            final long factionShipsCrewWages = ledger.getLastMonth(FACTION_CREW_WAGES_KEY);
            if (factionShipsCrewWages > 0l) {
                tp.addPara(getDesc(FACTION_CREW_WAGES_KEY) + ": %s", pad, negative, Misc.getDGSCredits(factionShipsCrewWages));
            }

            final long factionShipsProd = ledger.getLastMonth(FACTION_SHIP_PRODUCTION_KEY);
            if (factionShipsProd > 0l) {
                tp.addPara(getDesc(FACTION_SHIP_PRODUCTION_KEY) + ": %s", pad, negative, Misc.getDGSCredits(factionShipsProd));
            }

            final long tradeFleetShipment = ledger.getLastMonth(TRADE_FLEET_SHIPMENT_KEY);
            if (tradeFleetShipment > 0l) {
                tp.addPara(getDesc(TRADE_FLEET_SHIPMENT_KEY) + ": %s", pad, negative, Misc.getDGSCredits(tradeFleetShipment));
            }

            final long policyCost = ledger.getLastMonth(POLICY_COST_KEY);
            if (policyCost > 0l) {
                tp.addPara(getDesc(POLICY_COST_KEY) + ": %s", pad, negative, Misc.getDGSCredits(policyCost));
            }

            final int incentive = (int) ledger.getLastMonth(COLONY_HAZARD_PAY_KEY);
            if (incentive > 0) {
                tp.addPara(getDesc(COLONY_HAZARD_PAY_KEY) + ": %s", pad, negative, Misc.getDGSCredits(incentive));
            }

            tp.addPara(REDISTRIBUTION_DISCLAIMER, gray, opad);
        };
    
        setShortcut(Keyboard.KEY_1);

        buildUI();
    }

    public void buildUI() {
        final long value = MarketFinanceRegistry.instance().getLedger(market).getNetLastMonth();
        final String txt = "Credits/month";
        final String valueTxt = NumFormat.formatCredit(value);
        final Color valueColor = value < 0l ? negative : market.getFaction().getBrightUIColor();

        ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt,
            market.getFaction().getBaseUIColor(), valueColor
        );
    }
}
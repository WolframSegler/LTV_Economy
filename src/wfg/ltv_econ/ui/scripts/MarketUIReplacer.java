package wfg.ltv_econ.ui.scripts;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.ltv_econ.ui.marketInfo.CommodityRowPanel;
import wfg.ltv_econ.ui.marketInfo.LtvCommodityPanel;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.ltv_econ.ui.marketInfo.buttons.ColonyStockpilesButton;
import wfg.ltv_econ.ui.marketInfo.buttons.ManagePopButton;
import wfg.ltv_econ.ui.marketInfo.buttons.MarketEventsButton;
import wfg.ltv_econ.ui.marketInfo.dialogs.ComDetailDialog;
import wfg.ltv_econ.util.UIUtils;
import wfg.ltv_econ.util.wrappers.MarketWrapper;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.components.InteractionComp.ClickHandler;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.TextPanel;

import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ShippingPanel;

import rolflectionlib.util.RolfLectionUtil;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.UIColors.GRAY;
import static wfg.ltv_econ.constants.strings.Income.*;

public class MarketUIReplacer implements EveryFrameScript {

    private static final Class<?> knownClass1 = IndustryListPanel.class;
    private static final Class<?> knownClass2 = LtvIndustryListPanel.class;
    private static final Class<?> knownClass3 = CommodityPanel.class;
    private static final Class<?> knownClass4 = LtvCommodityPanel.class;

    public static Object marketAPIField = null;
    public static Object marketField = null;

    public static MarketAPI marketAPI = null;
    public static Market market = null;

    public static Object incomeLblPlugin;

    @Override
    public void advance(float amount) {
        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;

        final UIPanelAPI tradePanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getTradePanel", masterTab);
        if (tradePanel == null) return;

        final List<?> outpostChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, tradePanel);
        final UIPanelAPI overviewPanel = outpostChildren.stream()
            .filter(c -> RolfLectionUtil.hasMethodOfName("showOverview", c))
            .map(child -> (UIPanelAPI) child)
            .findFirst().orElse(null);
        if (overviewPanel == null) return;

        final List<?> overviewChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, overviewPanel);
        final UIPanelAPI managementPanel = overviewChildren.stream()
            .filter(c -> RolfLectionUtil.hasMethodOfName("recreateWithEconUpdate", c))
            .map(child -> (UIPanelAPI) child)
            .findFirst().orElse(null);
        if (managementPanel == null) return;

        if (marketAPIField == null) {
            marketAPIField = RolfLectionUtil.getAllFields(managementPanel.getClass())
                .stream().filter(f -> MarketAPI.class.isAssignableFrom(
                    RolfLectionUtil.getFieldType(f)
                )).findFirst().get();
        }
        marketAPI = (MarketAPI) RolfLectionUtil.getPrivateVariable(marketAPIField, managementPanel);

        final List<?> managementChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, managementPanel);

        UIPanelAPI anchorChild = null;

        for (Object child : managementChildren) {
            Class<?> childClass = child.getClass();
            if (!childClass.equals(knownClass1) &&
                    !childClass.equals(knownClass2) &&
                    !childClass.equals(knownClass3) &&
                    !childClass.equals(knownClass4)) {
                anchorChild = (UIPanelAPI) child;
                break;
            }
        }
        if (anchorChild == null) return;

        addManagementButtons(managementPanel, managementChildren, anchorChild);

        replaceMarketCreditsLabel(managementPanel, managementChildren, anchorChild);

        replaceIndustryListPanel(managementPanel, managementChildren, anchorChild);

        replaceCommodityPanel(managementPanel, managementChildren, anchorChild);

        replaceMarketInstanceForPriceControl(masterTab);
    }

    private static final void addManagementButtons(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        if (!RolfLectionUtil.hasMethodOfName("getShipping", colonyInfoPanel)) return;
        final ShippingPanel shipPanel = (ShippingPanel) RolfLectionUtil.invokeMethod(
            "getShipping", colonyInfoPanel);

        final ButtonAPI useStockpilesBtn = (ButtonAPI) RolfLectionUtil.invokeMethod(
            "getUseStockpiles", shipPanel);

        if (!useStockpilesBtn.isEnabled()) return;
        useStockpilesBtn.setOpacity(0f);
        useStockpilesBtn.setEnabled(false);

        if (!marketAPI.isPlayerOwned() &&
            (!DebugFlags.COLONY_DEBUG || DebugFlags.HIDE_COLONY_CONTROLS)
        ) return;

        final int panelWidth = LtvCommodityPanel.STANDARD_WIDTH;
        final int buttonWidth = (int) (panelWidth / 3f - 4f);
        final int buttonHeight = (int) (buttonWidth / 1.63f);

        final MarketEventsButton eventsBtn = new MarketEventsButton(managementPanel, buttonWidth, buttonHeight, marketAPI);
        final ColonyStockpilesButton stockpilesBtn = new ColonyStockpilesButton(managementPanel, buttonWidth, buttonHeight, marketAPI);
        final ManagePopButton popBtn = new ManagePopButton(managementPanel, buttonWidth, buttonHeight, marketAPI);

        final int gap = (panelWidth - buttonWidth * 3) / 2;

        colonyInfoPanel.addComponent(eventsBtn.getPanel());
        colonyInfoPanel.addComponent(stockpilesBtn.getPanel());
        colonyInfoPanel.addComponent(popBtn.getPanel());

        NativeUiUtils.anchorPanel(popBtn.getPanel(), colonyInfoPanel, AnchorType.BottomRight, -100);
        NativeUiUtils.anchorPanel(stockpilesBtn.getPanel(), popBtn.getPanel(), AnchorType.LeftBottom, gap);
        NativeUiUtils.anchorPanel(eventsBtn.getPanel(), stockpilesBtn.getPanel(), AnchorType.LeftBottom, gap);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Added Market Buttons");
        }
    }

    private static final void replaceMarketCreditsLabel(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        if (!DebugFlags.COLONY_DEBUG && !marketAPI.isPlayerOwned()) return;

        final List<?> children = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, colonyInfoPanel);
        RolfLectionUtil.invokeMethodDirectly(CustomPanel.getChildrenNonCopyMethod, colonyInfoPanel);
        for (Object child : children) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() == incomeLblPlugin) {
                return;
            }
        }
        
        if (!RolfLectionUtil.hasMethodOfName("getIncome", colonyInfoPanel)) return;
        
        final UIPanelAPI incomePanel = (UIPanelAPI) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getIncome", colonyInfoPanel);
        final List<?> incomePanelChildren = (List<?>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, incomePanel);
        
        final var Buttons = (List<ButtonAPI>) incomePanelChildren.stream()
            .filter(c -> c instanceof ButtonAPI).map(c -> (ButtonAPI) c).toList();
        final ButtonAPI creditBtn = Buttons.get(0);
        final ButtonAPI hazardBtn = Buttons.get(1);
        incomePanel.removeComponent(creditBtn);

        final TextPanel colonyCreditLabel = new TextPanel(colonyInfoPanel, 150, 50) {
            @Override
            public void buildUI() {
                final long value = MarketFinanceRegistry.instance().getLedger(marketAPI).getNetLastMonth();
                final String txt = "Credits/month";
                final String valueTxt = NumFormat.formatCredit(value);
                final Color valueColor = value < 0l ? negative : marketAPI.getFaction().getBrightUIColor();

                ComponentFactory.addCaptionValueBlock(m_panel, txt, valueTxt,
                    marketAPI.getFaction().getBaseUIColor(), valueColor
                );
            }

            final float TP_WIDTH = 450f;
            {
            tooltip.expandable = true;
            tooltip.expandTxt = "%s Show details";
            tooltip.unexpandTxt = "%s Hide";
            tooltip.width = TP_WIDTH;
            tooltip.positioner = (tp, expanded) -> {
                NativeUiUtils.anchorPanel(tp, getPanel(), AnchorType.LeftTop, 50);
            };
            tooltip.builder = (tp, expanded) -> {
                // TODO Add dockpanel that opens when player clicks on colony income label that breaks down monthly income
                final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketAPI);
                final EconomyInfo info = EconomyEngine.instance().info;
                final List<CommodityDomain> domains = EconomyEngine.instance().getComDomains();
                final FactionAPI faction = marketAPI.getFaction();
                final Color base = faction.getBaseUIColor();
                final Color dark = faction.getDarkUIColor();

                tp.addTitle("Monthly Income & Upkeep", base);
                final long income = ledger.getNetLastMonth();

                final String incomeTxt = NumFormat.formatCreditAbs(income);
                if (income >= 0l) {
                    tp.addPara("The net monthly income of this colony last month was %s.", opad, highlight, incomeTxt);
                } else {
                    tp.addPara("The net monthly upkeep for this colony last month was %s.", opad, negative, incomeTxt);
                }

                tp.addPara(
                    "Income multiplier: %s", opad, highlight,
                    Math.round(marketAPI.getIncomeMult().getModifiedValue() * 100f) + "%"
                );
                tp.addStatModGrid(
                    TP_WIDTH, 50f, opad, pad, marketAPI.getIncomeMult(), true, null
                );
                tp.setParaFontColor(gray);
                tp.addPara(
                    "This multiplier affects industry income & upkeep, "+ 
                    "but does not affect wages or trade (exports/imports).",
                    opad
                );
                tp.setParaFontColor(Color.WHITE);

                tp.addPara(
                    "Upkeep multiplier: %s", opad, highlight,
                    Math.round(marketAPI.getUpkeepMult().getModifiedValue() * 100f) + "%"
                );
                tp.addStatModGrid(
                    TP_WIDTH, 50f, opad, pad, marketAPI.getUpkeepMult(), true, null
                );

                final String indIncome = NumFormat.formatCredit(ledger.getLastMonth(INDUSTRY_INCOME_KEY));
                final String indUpkeep = NumFormat.formatCredit(ledger.getLastMonth(INDUSTRY_UPKEEP_KEY));

                final ArrayList<Industry> industries = new ArrayList<>(marketAPI.getIndustries());

                tp.addSectionHeading("Income", base, dark, Alignment.MID, opad);

                tp.addPara("Local income: %s", opad, highlight, indIncome);
                industries.sort((i1, i2) ->
                    Integer.compare(
                        info.getIndustryIncome(i2).getModifiedInt(),
                        info.getIndustryIncome(i1).getModifiedInt()
                    )
                );
                tp.beginGridFlipped(TP_WIDTH, 1, 65f, opad);

                int indCount = 0;
                for (Industry ind : industries) {
                    int perIndIncome = info.getIndustryIncome(ind).getModifiedInt();
                    if (perIndIncome > 0) {
                        tp.addToGrid(0, indCount++, ind.getCurrentName(),
                            NumFormat.formatCredit(perIndIncome), highlight
                        );
                    }
                }

                if (indCount > 0) {
                    tp.addGrid(pad);
                } else {
                    tp.cancelGrid();
                }

                final int maxCommoditiesToDisplay = 8;
                final float somethingWidth = 500f - 14f - 395f;
                final float extraPad = ((int)(somethingWidth / 3f));

                final long exportIncome = info.getExportIncome(marketAPI, true);
                tp.addPara("Last Month's Exports: %s", opad, highlight, NumFormat.formatCredit(exportIncome));
                if (exportIncome > 0l && expanded) {
                    tp.beginTable(faction, 20f, "Commodity", 150f + extraPad, "Market share", 100f + extraPad, "Income", 100f + extraPad);
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
                            com.spec.getId(), marketAPI.getId()
                        );

                        tp.addRow(Alignment.LMID, Misc.getTextColor(), " " + name, highlight, exportMarketShare + "%", highlight, NumFormat.formatCredit(comExportIncome));
                        comCount++;
                        if (comCount + 1 > maxCommoditiesToDisplay && exportedCount - comCount > 1) {
                            break;
                        }
                    }

                    tp.addTable("No exports", exportedCount - comCount, opad);
                }

                tp.addSectionHeading("Upkeep", base, dark, Alignment.MID, opad);
                if (expanded && info.getIndustryUpkeep(marketAPI) > 0) {
                    tp.addPara("Industry and structure upkeep: %s", opad, negative, indUpkeep);

                    industries.sort((i1, i2) ->
                        Integer.compare(
                            info.getIndustryUpkeep(i2).getModifiedInt(),
                            info.getIndustryUpkeep(i1).getModifiedInt()
                        )
                    );

                    tp.beginGridFlipped(TP_WIDTH, 1, 65f, opad);
                    indCount = 0;

                    for (Industry ind : industries) {
                        int perIndIncome = info.getIndustryUpkeep(ind).getModifiedInt();
                        if (perIndIncome > 0) {
                            tp.addToGrid(0, indCount++, ind.getCurrentName(),
                                NumFormat.formatCredit(perIndIncome), negative
                            );
                        }
                    }

                    if (indCount > 0) {
                        tp.addGrid(pad);
                    } else {
                        tp.cancelGrid();
                    }
                }

                final long importExpense = info.getImportExpense(marketAPI, true);
                tp.addPara("Last Month's Imports: %s", opad, negative, NumFormat.formatCredit(importExpense));
                if (importExpense > 0 && expanded) {
                    tp.beginTable(faction, 20f, "Commodity", 150f + extraPad, "Market share", 100f + extraPad, "Expense", 100f + extraPad);
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
                            com.spec.getId(), marketAPI.getId()
                        );

                        tp.addRow(Alignment.LMID, Misc.getTextColor(), " " + name, negative, importMarketShare + "%", negative, NumFormat.formatCredit(comImportExpense));
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

                final int sumbarketTransaction = (int) ledger.getLastMonth(PLAYER_MARKET_TRANSACTION_KEY);
                if (sumbarketTransaction > 0) {
                    tp.addPara(getDesc(PLAYER_MARKET_TRANSACTION_KEY) + ": %s", pad, negative, Misc.getDGSCredits(sumbarketTransaction));
                }

                tp.addPara(REDISTRIBUTION_DISCLAIMER, GRAY, opad);
            };
            }
        };
        incomeLblPlugin = ((CustomPanelAPI)colonyCreditLabel.getPanel()).getPlugin();

        final PositionAPI posS = colonyCreditLabel.getPos();
        final PositionAPI posA = hazardBtn.getPosition();
        colonyInfoPanel.addComponent(colonyCreditLabel.getPanel()).inTL(
            (posA.getX() - posS.getX()) + (posA.getWidth() - posS.getWidth()) / 2f, 0
        );
    }

    private static final void replaceIndustryListPanel(UIPanelAPI managementPanel,
        List<?> managementChildren, UIPanelAPI anchor
    ) {
        UIPanelAPI industryPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof IndustryListPanel) {
                industryPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (industryPanel == null) return;

        // Steal the members for the constructor
        final int width = (int) industryPanel.getPosition().getWidth();
        final int height = (int) industryPanel.getPosition().getHeight();

        final LtvIndustryListPanel replacement = new LtvIndustryListPanel(
            managementPanel, width, height,
            marketAPI, industryPanel
        );

        managementPanel.addComponent(replacement.getPanel());
        NativeUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomLeft, 25);

        if (LtvIndustryListPanel.indOptCtor == null) {
            // Acquire the popup class from one of the widgets
            final List<?> widgets = (List<?>) RolfLectionUtil.getMethodAndInvokeDirectly(
                "getWidgets", industryPanel);

            if (!widgets.isEmpty()) {
            final Object widget0 = widgets.get(0);

            // Attach the popup;
            RolfLectionUtil.getMethodAndInvokeDirectly("actionPerformed", widget0,
                null, null
            );

            // Now the popup class is a child of:
            final UIPanelAPI dialogParent = Attachments.getCampaignScreenPanel();
            final List<?> children = (List<?>) RolfLectionUtil.invokeMethodDirectly(
                CustomPanel.getChildrenNonCopyMethod, dialogParent);

            final UIPanelAPI indOps = children.stream()
                .filter(child -> child instanceof DialogCreatorUI && child instanceof UIPanelAPI)
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);

            LtvIndustryListPanel.indOptCtor = RolfLectionUtil.getConstructor(indOps.getClass(),
                RolfLectionUtil.getConstructorParamTypesSingleConstructor(indOps.getClass())
            );

            // Dismiss the indOpsPanel after getting its constructor
            RolfLectionUtil.getMethodAndInvokeDirectly(
                "dismiss", indOps, 0
            );
            }
        }

        // No need for the old panel
        managementPanel.removeComponent(industryPanel);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Replaced IndustryListPanel");
        }
    }

    private static final void replaceCommodityPanel(UIPanelAPI managementPanel,
        List<?> managementChildren,UIPanelAPI anchor
    ) {
        UIPanelAPI commodityPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof CommodityPanel) {
                commodityPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (commodityPanel == null) return;

        final int width = (int) commodityPanel.getPosition().getWidth();
        final int height = (int) commodityPanel.getPosition().getHeight();

        final LtvCommodityPanel replacement = new LtvCommodityPanel(
            managementPanel, width, height, marketAPI
        );

        final ClickHandler<CommodityRowPanel> listener = (source, isLeftClick) -> {
            if (!isLeftClick) return;

            CommodityRowPanel panel = ((CommodityRowPanel) source);

            replacement.selectRow(panel);

            if (UIUtils.canViewPrices()) {
                final ComDetailDialog dialogPanel = new ComDetailDialog(
                    marketAPI, marketAPI.getFaction().getFactionSpec(), panel.com
                );
                dialogPanel.show(0.3f, 0.3f);
            }
        };

        replacement.selectionListener = listener;
        replacement.buildUI();

        // Got the Y offset by looking at the getY() difference of replacement and
        // commodityPanel
        // Might automate the getY() difference later
        managementPanel.addComponent(replacement.getPanel());
        NativeUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomRight, -43);

        managementPanel.removeComponent(commodityPanel);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(MarketUIReplacer.class).info("Replaced CommodityPanel");
        }
    }

    private static final void replaceMarketInstanceForPriceControl(UIPanelAPI masterTab) {
        final UIPanelAPI handler = (UIPanelAPI) RolfLectionUtil.invokeMethod(
            "getTransferHandler", masterTab);

        if (marketField == null) {
            marketField = RolfLectionUtil.getAllFields(handler.getClass())
                .stream().filter(f -> Market.class.isAssignableFrom(
                    RolfLectionUtil.getFieldType(f)
                )).findFirst().get();
        }

        final Market original = (Market) RolfLectionUtil.getPrivateVariable(marketField, handler);
        if (original instanceof MarketWrapper) return;

        RolfLectionUtil.setPrivateVariable(marketField, handler, new MarketWrapper(original));
    }

    public boolean isDone() { return !Global.getSector().isPaused(); }
    public boolean runWhilePaused() { return true; }
}
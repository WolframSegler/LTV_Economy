package wfg.ltv_econ.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.input.Keyboard;

import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.CommodityInfo;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ColonyInvDialog;
import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.dialogs.ManageWorkersDialog;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.ltv_econ.util.TooltipUtils;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.panels.CustomPanel.HasActionListener;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.plugins.ButtonPlugin;
import wfg.reflection.ReflectionUtils;
import wfg.reflection.ReflectionUtils.ReflectedConstructor;
import wfg.reflection.ReflectionUtils.ReflectedField;

import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ShippingPanel;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;
import static wfg.wrap_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.economyValues.*;

public class LtvMarketReplacer implements EveryFrameScript {

    private final SectorAPI sector = Global.getSector();
    private int frames = 0;

    @Override
    public void advance(float amount) {

        if (!sector.isPaused()) {
            frames = 0;
            return;
        }

        if (!sector.getCampaignUI().isShowingDialog()) {
            return;
        }

        frames++;
        if (frames < 2 || Global.getCurrentState() != GameState.CAMPAIGN) {
            return;
        }

        // Get the management panel depending on context
        UIPanelAPI masterTab = Attachments.getInteractionCurrentTab();
        if (masterTab == null) { // If there is no interaction target
            masterTab = Attachments.getCurrentTab();
        }
        if (masterTab == null)
            return;

        final List<?> listChildren = (List<?>) ReflectionUtils.invoke(masterTab, "getChildrenCopy");
        final UIPanelAPI outpostPanel = listChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "getOutpostPanelParams").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (outpostPanel == null)
            return;

        final List<?> outpostChildren = (List<?>) ReflectionUtils.invoke(outpostPanel, "getChildrenCopy");
        final UIPanelAPI overviewPanel = outpostChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "showOverview").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (overviewPanel == null)
            return;

        final List<?> overviewChildren = (List<?>) ReflectionUtils.invoke(overviewPanel, "getChildrenCopy");
        final UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "recreateWithEconUpdate").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null)
            return;

        final List<?> managementChildren = (List<?>) ReflectionUtils.invoke(managementPanel, "getChildrenCopy");

        final Class<?> knownClass1 = IndustryListPanel.class;
        final Class<?> knownClass2 = LtvIndustryListPanel.class;
        final Class<?> knownClass3 = CommodityPanel.class;
        final Class<?> knownClass4 = LtvCommodityPanel.class;

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
        if (anchorChild == null)
            return;

        // Replace the "Use stockpiles during shortages" button
        replaceUseStockpilesBtnAddManageWorkersBtn(managementPanel, managementChildren, anchorChild);

        // Replaces the Colony effective income label
        replaceMarketCreditsLabel(managementPanel, managementChildren, anchorChild);

        // Replace the Panel which holds the widgets
        replaceIndustryListPanel(managementPanel, managementChildren, anchorChild);

        // Replace the Commodity Panel which shows the total imports and exports
        replaceCommodityPanel(managementPanel, managementChildren, anchorChild);

        // Replace the market instance of the masterTab
        replaceMarketInstanceForPriceControl(masterTab);
    }

    private static final void replaceUseStockpilesBtnAddManageWorkersBtn(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        for (Object child : managementChildren) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof ButtonPlugin) {
                return;
            }
        }

        final ShippingPanel shipPanel = (ShippingPanel) ReflectionUtils.invoke(colonyInfoPanel, "getShipping");

        // final ButtonAPI useStockpilesBtn = (ButtonAPI) shipPanel.getUseStockpiles();
        final ButtonAPI useStockpilesBtn = (ButtonAPI) ReflectionUtils.invoke(shipPanel, "getUseStockpiles");

        final MarketAPI market = (MarketAPI) ReflectionUtils.get(shipPanel, null, MarketAPI.class);

        if (DebugFlags.COLONY_DEBUG || market.isPlayerOwned()) {
            final CallbackRunnable<Button> stockpilesBtnRunnable = (btn) -> {
                final ColonyInvDialog dialogPanel = new ColonyInvDialog(market, btn);

                WrapUiUtils.CustomDialogViewer(
                        dialogPanel, ColonyInvDialog.PANEL_W, ColonyInvDialog.PANEL_H);
            };

            final Button inventoryBtn = new Button(
                    managementPanel, LtvCommodityPanel.STANDARD_WIDTH, 20, "Colony Stockpiles",
                    Fonts.ORBITRON_12, stockpilesBtnRunnable);
            inventoryBtn.setLabelColor(base);
            inventoryBtn.bgColor = new Color(0, 0, 0, 255);
            inventoryBtn.bgDisabledColor = new Color(0, 0, 0, 255);
            inventoryBtn.quickMode = true;
            inventoryBtn.setShortcut(Keyboard.KEY_G);

            managementPanel.addComponent(inventoryBtn.getPanel()).inBL(0, 0);

            final int xOffset = (int) (useStockpilesBtn.getPosition().getX() - inventoryBtn.getPos().getX());
            final int yOffset = (int) (useStockpilesBtn.getPosition().getY() - inventoryBtn.getPos().getY());

            inventoryBtn.getPos().inBL(xOffset, yOffset);

            if (!EconomyEngine.getInstance().isPlayerMarket(market.getId())) return;

            final CallbackRunnable<Button> manageWorkersBtnRunnable = (btn) -> {
                final ManageWorkersDialog dialogPanel = new ManageWorkersDialog(market);

                WrapUiUtils.CustomDialogViewer(
                        dialogPanel, ManageWorkersDialog.PANEL_W, ManageWorkersDialog.PANEL_H);
            };

            final Button manageBtn = new Button(
                    managementPanel, LtvCommodityPanel.STANDARD_WIDTH, 20, "Manage Workers",
                    Fonts.ORBITRON_12, manageWorkersBtnRunnable);
            manageBtn.setLabelColor(base);
            manageBtn.bgColor = new Color(0, 0, 0, 255);
            manageBtn.bgDisabledColor = new Color(0, 0, 0, 255);
            manageBtn.quickMode = true;
            manageBtn.setShortcut(Keyboard.KEY_W);

            managementPanel.addComponent(manageBtn.getPanel()).inBL(0, 0);
            WrapUiUtils.anchorPanel(
                manageBtn.getPanel(), inventoryBtn.getPanel(), AnchorType.TopMid, opad
            );
        }

        useStockpilesBtn.setOpacity(0);
        useStockpilesBtn.setEnabled(false);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced UseStockpilesButton");
            Global.getLogger(LtvMarketReplacer.class).info("Added manageWorkersButton");
        }
    }

    private static final void replaceMarketCreditsLabel(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        final List<?> children = (List<?>) ReflectionUtils.invoke(colonyInfoPanel, "getChildrenCopy");
        for (Object child : children) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof BasePanelPlugin) {
                return;
            }
        }
        
        final var fields = ReflectionUtils.getMethodsMatching(colonyInfoPanel, "getIncome", UIPanelAPI.class, 0);
        if (fields.isEmpty()) return;
        
        final UIPanelAPI incomePanel = (UIPanelAPI) fields.get(0).invoke(colonyInfoPanel);
        final List<?> incomePanelChildren = (List<?>) ReflectionUtils.invoke(incomePanel, "getChildrenCopy");
        
        final ButtonAPI oldBtn = incomePanelChildren.stream().filter(c -> c instanceof ButtonAPI).findFirst()
            .map(c -> (ButtonAPI) c).get();
        incomePanel.removeComponent(oldBtn);

        final MarketAPI market = (MarketAPI) ReflectionUtils.get(colonyInfoPanel, null, MarketAPI.class);
        final TextPanel colonyCreditLabel = new TextPanel(colonyInfoPanel, 150, 50) {
            @Override
            public void createPanel() {
                final long value = EconomyEngine.getInstance().getNetIncome(market, true);
                final String txt = "Credits/month";
                final String valueTxt = NumFormat.formatCredit(value);
                final Color valueColor = value < 0 ? negative
                        : market.getFaction().getBrightUIColor();

                final LabelAPI lbl1 = Global.getSettings().createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(market.getFaction().getBaseUIColor());
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = Global.getSettings().createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(valueColor);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.computeTextHeight(txt);
                final float textH2 = lbl2.computeTextHeight(valueTxt);
                final float textW1 = lbl1.computeTextWidth(txt);
                final float textW2 = lbl2.computeTextWidth(valueTxt);

                add(lbl1).inTL(0, 0).setSize(textW1, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(textW1, textH2);

                getPos().setSize(Math.max(textW1, textW2), textH1 + pad + textH2);
            }

            @Override
            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            private TooltipMakerAPI m_tp;

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final int TP_WIDTH = 450;
                m_tp = getPanel().createUIElement(TP_WIDTH, 0, false);

                final FactionAPI faction = market.getFaction();
                final Color base = faction.getBaseUIColor();
                final Color dark = faction.getDarkUIColor();
                final EconomyEngine engine = EconomyEngine.getInstance();

                m_tp.addTitle("Monthly Income & Upkeep", base);
                final long income = engine.getNetIncome(market, true);

                final String incomeTxt = NumFormat.formatCreditAbs(income);
                if (income >= 0) {
                    m_tp.addPara("The net monthly income of this colony last month was %s.", opad, highlight, incomeTxt);
                } else {
                    m_tp.addPara("The net monthly upkeep for this colony last month was %s.", opad, negative, incomeTxt);
                }

                m_tp.addPara(
                    "Income multiplier: %s", opad, highlight,
                    Math.round(market.getIncomeMult().getModifiedValue() * 100f) + "%"
                );
                m_tp.addStatModGrid(
                    TP_WIDTH, 50f, opad, pad, market.getIncomeMult(), true, null
                );

                m_tp.addPara(
                    "Upkeep multiplier: %s", opad, highlight,
                    Math.round(market.getUpkeepMult().getModifiedValue() * 100f) + "%"
                );
                m_tp.addStatModGrid(
                    TP_WIDTH, 50f, opad, pad, market.getUpkeepMult(), true, null
                );

                final String indIncome = NumFormat.formatCredit(engine.getIndustryIncome(market));
                final String indUpkeep = NumFormat.formatCredit(engine.getIndustryUpkeep(market));

                final ArrayList<Industry> industries = new ArrayList<>(market.getIndustries());

                m_tp.addSectionHeading("Income", base, dark, Alignment.MID, opad);

                m_tp.addPara("Local income: %s", opad, highlight, indIncome);
                industries.sort((i1, i2) ->
                    Integer.compare(
                        engine.getIndustryIncome(i2, market).getModifiedInt(),
                        engine.getIndustryIncome(i1, market).getModifiedInt()
                    )
                );
                m_tp.beginGridFlipped(TP_WIDTH, 1, 65.0F, opad);

                int indCount = 0;
                for (Industry ind : industries) {
                    int perIndIncome = engine.getIndustryIncome(ind, market).getModifiedInt();
                    if (perIndIncome > 0) {
                        m_tp.addToGrid(0, indCount++, ind.getCurrentName(),
                            NumFormat.formatCredit(perIndIncome), highlight
                        );
                    }
                }

                if (indCount > 0) {
                    m_tp.addGrid(pad);
                } else {
                    m_tp.cancelGrid();
                }

                final long exportIncome = engine.getExportIncome(market, true);
                m_tp.addPara("Last Month's Exports: %s", opad, highlight, NumFormat.formatCredit(exportIncome));
                if (exportIncome > 0 && expanded) {
                    final int maxCommoditiesToDisplay = 10;
                    final float somethingWidth = 500f - 14f - 395f;
                    final float extraPad = ((int)(somethingWidth / 3f));

                    m_tp.beginTable(faction, 20f, "Commodity", 150f + extraPad, "Market share", 100f + extraPad, "Income", 100f + extraPad);
                    int exportedCount = 0;
                    final List<CommodityInfo> commodities = engine.getCommodityInfos();
                    for (CommodityInfo com : commodities) {
                        if (com.getLedger(market.getId())
                                .lastMonthExportIncome > 0
                            ) {
                            ++exportedCount;
                        }
                    }

                    commodities.sort((c1, c2) ->
                        Long.compare(
                            c2.getLedger(market.getId()).lastMonthExportIncome,
                            c1.getLedger(market.getId()).lastMonthExportIncome
                        )
                    );
                    int comCount = 0;
                    for (CommodityInfo com : commodities) {
                        final String name = com.spec.getName();
                        final long comExportIncome = com.getLedger(market.getId()).lastMonthExportIncome;
                        if (comExportIncome < 1) continue;

                        final int exportMarketShare = engine.getExportMarketShare(
                            com.spec.getId(), market.getId()
                        );

                        m_tp.addRow(Alignment.LMID, Misc.getTextColor(), " " + name, highlight, exportMarketShare + "%", highlight, NumFormat.formatCredit(comExportIncome));
                        comCount++;
                        if (comCount + 1 > maxCommoditiesToDisplay && exportedCount - comCount > 1) {
                            break;
                        }
                    }

                    m_tp.addTable("No exports", exportedCount - comCount, opad);
                }

                m_tp.addSectionHeading("Upkeep", base, dark, Alignment.MID, opad);
                if (expanded && engine.getIndustryUpkeep(market) > 0) {
                    m_tp.addPara("Industry and structure upkeep: %s", opad, negative, indUpkeep);

                    industries.sort((i1, i2) ->
                        Integer.compare(
                            engine.getIndustryUpkeep(i2, market).getModifiedInt(),
                            engine.getIndustryUpkeep(i1, market).getModifiedInt()
                        )
                    );

                    m_tp.beginGridFlipped(TP_WIDTH, 1, 65f, opad);
                    indCount = 0;

                    for (Industry ind : industries) {
                        int perIndIncome = engine.getIndustryUpkeep(ind, market).getModifiedInt();
                        if (perIndIncome > 0) {
                            m_tp.addToGrid(0, indCount++, ind.getCurrentName(),
                                NumFormat.formatCredit(perIndIncome), negative
                            );
                        }
                    }

                    if (indCount > 0) {
                        m_tp.addGrid(pad);
                    } else {
                        m_tp.cancelGrid();
                    }
                }

                final long importExpense = engine.getImportExpense(market, true);
                m_tp.addPara("Last Month's Imports: %s", opad, negative, NumFormat.formatCredit(importExpense));
                if (importExpense > 0 && expanded) {
                    final int maxCommoditiesToDisplay = 10;
                    final float somethingWidth = 500f - 14f - 395f;
                    final float extraPad = ((int)(somethingWidth / 3f));

                    m_tp.beginTable(faction, 20f, "Commodity", 150f + extraPad, "Market share", 100f + extraPad, "Expense", 100f + extraPad);
                    int importedCount = 0;
                    final List<CommodityInfo> commodities = engine.getCommodityInfos();
                    for (CommodityInfo com : commodities) {
                        if (com.getLedger(market.getId())
                                .lastMonthImportExpense > 0
                            ) {
                            ++importedCount;
                        }
                    }

                    commodities.sort((c1, c2) ->
                        Long.compare(
                            c2.getLedger(market.getId()).lastMonthImportExpense,
                            c1.getLedger(market.getId()).lastMonthImportExpense
                        )
                    );
                    int comCount = 0;
                    for (CommodityInfo com : commodities) {
                        final String name = com.spec.getName();
                        final long comImportExpense = com.getLedger(market.getId()).lastMonthImportExpense;
                        if (comImportExpense < 1) continue;

                        final int importMarketShare = engine.getImportMarketShare(
                            com.spec.getId(), market.getId()
                        );

                        m_tp.addRow(Alignment.LMID, Misc.getTextColor(), " " + name, negative, importMarketShare + "%", negative, NumFormat.formatCredit(comImportExpense));
                        comCount++;
                        if (comCount + 1 > maxCommoditiesToDisplay && importedCount - comCount > 1) {
                            break;
                        }
                    }

                    m_tp.addTable("No imports", importedCount - comCount, opad);
                }

                final long monthlyWages = (long) (engine.getWagesForMarket(market.getId())*MONTH);
                if (monthlyWages > 0) {
                    m_tp.addPara("Worker wages: %s", opad, negative,
                        NumFormat.formatCredit(monthlyWages)
                    );
                }

                final int incentive = (int) market.getImmigrationIncentivesCost();
                if (incentive > 0 && market.isImmigrationIncentivesOn()) {
                    m_tp.addPara("Hazard pay: %s", opad, negative, Misc.getDGSCredits(incentive));
                }

                add(m_tp);
                WrapUiUtils.anchorPanel(m_tp, getPanel(), AnchorType.LeftTop, 50);
                return m_tp;
            }
            
            private boolean expanded = false;

            @Override
            public boolean isExpanded() {
                return expanded;
            };

            @Override
            public void setExpanded(boolean a) {
                expanded = a;
            };

            @Override
            public Optional<CustomPanelAPI> getCodexParent() {
                return Optional.ofNullable(getPanel());
            }

            private static final String notExpandedCodexF1 = "F1 more info";
            private static final String ExpandedCodexF1 = "F1 hide";

            @Override
            public Optional<TooltipMakerAPI> createAndAttachCodex() {
                TooltipMakerAPI codex;

                if (!expanded) {
                    final int codexW = 100;

                    codex = TooltipUtils.createCustomCodex(this, notExpandedCodexF1, null, codexW);
                } else {
                    final int codexW = 70;

                    codex = TooltipUtils.createCustomCodex(this, ExpandedCodexF1, null, codexW);  
                }

                WrapUiUtils.anchorPanel(codex, m_tp, AnchorType.BottomLeft, opad + pad);

                return Optional.ofNullable(codex);
            }
        };

        colonyInfoPanel.addComponent(colonyCreditLabel.getPanel()).inTR(
            318, 0
        );
    }

    private static final void replaceIndustryListPanel(
            UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI anchor) {
        UIPanelAPI industryPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof IndustryListPanel) {
                industryPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (industryPanel == null)
            return;

        // Steal the members for the constructor
        final MarketAPI market = (MarketAPI) ReflectionUtils.get(industryPanel, null, MarketAPI.class);
        final int width = (int) industryPanel.getPosition().getWidth();
        final int height = (int) industryPanel.getPosition().getHeight();

        final LtvIndustryListPanel replacement = new LtvIndustryListPanel(
                managementPanel,
                width,
                height,
                market,
                industryPanel);

        managementPanel.addComponent(replacement.getPanel());
        WrapUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomLeft, 25);

        if (LtvIndustryListPanel.indOptCtor == null) {
            // Acquire the popup class from one of the widgets
            final Object widget0 = ((IndustryListPanel) industryPanel).getWidgets().get(0);

            // Attach the popup
            ReflectionUtils.invoke(widget0, "actionPerformed", null, null);

            // Now the popup class is a child of:
            // CampaignEngine.getInstance().getCampaignUI().getDialogParent();

            final List<?> children = CampaignEngine.getInstance().getCampaignUI().getDialogParent()
                    .getChildrenNonCopy();

            final UIPanelAPI indOps = children.stream()
                    .filter(child -> child instanceof DialogCreatorUI && child instanceof UIPanelAPI)
                    .map(child -> (UIPanelAPI) child)
                    .findFirst().orElse(null);

            final ReflectedConstructor indOpsPanelConstr = ReflectionUtils.getConstructorsMatching(
                    indOps.getClass(), 5).get(0);

            LtvIndustryListPanel.setindustryOptionsPanelConstructor(indOpsPanelConstr);

            // Dismiss the indOpsPanel after getting its constructor
            ReflectionUtils.invoke(indOps, "dismiss", 0);
        }

        // No need for the old panel
        managementPanel.removeComponent(industryPanel);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced IndustryListPanel");
        }
    }

    private static final void replaceCommodityPanel(
            UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI anchor) {
        UIPanelAPI commodityPanel = null;
        for (Object child : managementChildren) {
            if (child instanceof CommodityPanel) {
                commodityPanel = (UIPanelAPI) child;
                break;
            }
        }
        if (commodityPanel == null) return;

        try {
            // Steal the members for the constructor
            final MarketAPI market = (MarketAPI) (ReflectionUtils.get(commodityPanel, null, MarketAPI.class));

            final int width = (int) commodityPanel.getPosition().getWidth();
            final int height = (int) commodityPanel.getPosition().getHeight();

            final LtvCommodityPanel replacement = new LtvCommodityPanel(
                managementPanel,
                width,
                height,
                new BasePanelPlugin<LtvCommodityPanel>()
            );
            replacement.setMarket(market);

            final HasActionListener listener = new HasActionListener() {
                @Override
                public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
                    if (!isLeftClick) return;

                    LtvCommodityRowPanel panel = ((LtvCommodityRowPanel) source);

                    replacement.selectRow(panel);

                    if (replacement.m_canViewPrices) {
                        final ComDetailDialog dialogPanel = new ComDetailDialog(
                            market, panel.getCommodity()
                        );

                        WrapUiUtils.CustomDialogViewer(
                            dialogPanel, dialogPanel.PANEL_W, dialogPanel.PANEL_H
                        );
                    }
                }
            };

            replacement.setActionListener(listener);
            replacement.createPanel();

            // Got the Y offset by looking at the getY() difference of replacement and
            // commodityPanel
            // Might automate the getY() difference later
            managementPanel.addComponent(replacement.getPanel());
            WrapUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomRight, -43);

            managementPanel.removeComponent(commodityPanel);

        } catch (Exception e) {
            Global.getLogger(LtvMarketReplacer.class).error("Failed to replace CommodityPanel", e);
        }

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced CommodityPanel");
        }
    }

    private static final void replaceMarketInstanceForPriceControl(UIPanelAPI masterTab) {
        final UIPanelAPI handler = (UIPanelAPI) ReflectionUtils.invoke(masterTab, "getTransferHandler");
        final ReflectedField field = ReflectionUtils.getFieldsMatching(
                handler, null, Market.class).get(0);

        final Market original = (Market) field.get(handler);
        if (original instanceof MarketWrapper)
            return;

        MarketAPI ltvMarket = new MarketWrapper(original);
        field.set(handler, ltvMarket);
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }
}
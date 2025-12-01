package wfg.ltv_econ.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ColonyInvDialog;
import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.panels.ActionListenerPanel;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.TextPanel;
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
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
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

public class LtvMarketReplacer implements EveryFrameScript {

    private static final int pad = 3;
    private static final int opad = 10;

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
        replaceUseStockpilesButton(managementPanel, managementChildren, anchorChild);

        // Replaces the Colony effective income label
        replaceMarketCreditsLabel(managementPanel, managementChildren, anchorChild);

        // Replace the Panel which holds the widgets
        replaceIndustryListPanel(managementPanel, managementChildren, anchorChild);

        // Replace the Commodity Panel which shows the total imports and exports
        replaceCommodityPanel(managementPanel, managementChildren, anchorChild);

        // Replace the market instance of the masterTab
        replaceMarketInstanceForPriceControl(masterTab);
    }

    private static final void replaceUseStockpilesButton(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        Global.getLogger(LtvMarketReplacer.class).error(colonyInfoPanel.getClass());
        
        for (Object child : managementChildren) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof ButtonPlugin) {
                return;
            }
        }

        final ShippingPanel shipPanel = (ShippingPanel) ReflectionUtils.invoke(colonyInfoPanel, "getShipping");

        final ButtonAPI useStockpilesBtn = (ButtonAPI) shipPanel.getUseStockpiles();

        final MarketAPI market = (MarketAPI) ReflectionUtils.get(shipPanel, null, MarketAPI.class);

        if (DebugFlags.COLONY_DEBUG || market.isPlayerOwned()) {
            final CallbackRunnable<Button> buildButtonRunnable = (btn) -> {
                final ColonyInvDialog dialogPanel = new ColonyInvDialog(market);

                WrapUiUtils.CustomDialogViewer(
                        dialogPanel, ColonyInvDialog.PANEL_W, ColonyInvDialog.PANEL_H);
            };

            final Button inventoryBtn = new Button(
                    managementPanel, LtvCommodityPanel.STANDARD_WIDTH, 20, "Colony Stockpiles",
                    Fonts.ORBITRON_12, buildButtonRunnable);
            inventoryBtn.setLabelColor(Misc.getBasePlayerColor());
            inventoryBtn.bgColor = new Color(0, 0, 0, 255);
            inventoryBtn.bgDisabledColor = new Color(0, 0, 0, 255);
            inventoryBtn.quickMode = true;

            managementPanel.addComponent(inventoryBtn.getPanel()).inBL(0, 0);

            final int xOffset = (int) (useStockpilesBtn.getPosition().getX() - inventoryBtn.getPos().getX());
            final int yOffset = (int) (useStockpilesBtn.getPosition().getY() - inventoryBtn.getPos().getY());

            inventoryBtn.getPos().inBL(xOffset, yOffset);
        }

        useStockpilesBtn.setOpacity(0);
        useStockpilesBtn.setEnabled(false);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced UseStockpilesButton");
        }
    }

    private static final void replaceMarketCreditsLabel(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        Global.getLogger(LtvMarketReplacer.class).error(colonyInfoPanel.getClass());
        final UIPanelAPI incomePanel = (UIPanelAPI) ReflectionUtils.invoke(colonyInfoPanel, "getIncome");

        final List<?> children = (List<?>) ReflectionUtils.invoke(incomePanel, "getChildrenCopy");
        for (Object child : children) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof BasePanelPlugin) {
                return;
            }
        }
        final MarketAPI market = (MarketAPI) ReflectionUtils.get(incomePanel, null, MarketAPI.class);
        final ButtonAPI oldBtn = children.stream().filter(c -> c instanceof ButtonAPI).findFirst()
                .map(c -> (ButtonAPI) c).get();
        incomePanel.removeComponent(oldBtn);

        final int LABEL_W = 100;
        final TextPanel creditsLabel = new TextPanel(incomePanel, 150, 50) {
            @Override
            public void createPanel() {
                final long value = EconomyEngine.getInstance().getNetIncome(market);
                final String txt = "Credits/month";
                final String valueTxt = NumFormat.formatCredits(value);
                final Color valueColor = value < 0 ? Misc.getNegativeHighlightColor()
                        : market.getFaction().getBrightUIColor();

                final LabelAPI lbl1 = Global.getSettings().createLabel(txt, Fonts.ORBITRON_12);
                lbl1.setColor(market.getFaction().getBaseUIColor());
                lbl1.setHighlightOnMouseover(true);
                lbl1.setAlignment(Alignment.MID);

                final LabelAPI lbl2 = Global.getSettings().createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                lbl2.setColor(valueColor);
                lbl2.setHighlightOnMouseover(true);
                lbl2.setAlignment(Alignment.MID);

                final float textH1 = lbl1.getPosition().getHeight();

                add(lbl1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(lbl2).inTL(0, textH1 + pad).setSize(LABEL_W, lbl2.getPosition().getHeight());
            }

            @Override
            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final int TP_WIDTH = 450;
                final TooltipMakerAPI tp = getPanel().createUIElement(TP_WIDTH, 1, false);

                final Color highlight = Misc.getHighlightColor();
                final Color negative = Misc.getNegativeHighlightColor();
                final FactionAPI faction = market.getFaction();
                final Color base = faction.getBaseUIColor();
                final Color dark = faction.getDarkUIColor();

                tp.addTitle("Monthly Income & Upkeep", base);
                final int income = EconomyEngine.getInstance().getNetIncome(market);

                final String incomeTxt = Misc.getDGSCredits(Math.abs(income));
                if (income >= 0) {
                    tp.addPara("The net monthly income of this colony is %s.", opad, highlight, incomeTxt);
                } else {
                    tp.addPara("The net monthly upkeep for this colony is %s.", opad, negative, incomeTxt);
                }

                tp.addPara(
                        "Income multiplier: %s", opad, highlight,
                        Math.round(market.getIncomeMult().getModifiedValue() * 100f) + "%");

                tp.addStatModGrid(
                        TP_WIDTH, 50f, opad, pad, market.getIncomeMult(), true, null);

                tp.addPara(
                        "Upkeep multiplier: %s", opad, highlight,
                        Math.round(market.getUpkeepMult().getModifiedValue() * 100f) + "%");

                tp.addStatModGrid(
                        TP_WIDTH, 50f, opad, pad, market.getUpkeepMult(), true, null);
                final String indIncome = Misc.getDGSCredits(Math.round(market.getIndustryIncome()));
                final String indUpkeep = Misc.getDGSCredits(Math.round(market.getIndustryUpkeep()));

                final ArrayList<Industry> industries = new ArrayList<>(market.getIndustries());

                final boolean hasUpkeep = Math.round(market.getIndustryUpkeep()) > 0;
                if (hasUpkeep) {
                    tp.addSectionHeading("Income & Upkeep", base, dark, Alignment.MID, opad);
                } else {
                    tp.addSectionHeading("Income", base, dark, Alignment.MID, opad);
                }

                tp.addPara("Local income: %s", opad, highlight, indIncome);
                if (!hasUpkeep) {
                    Collections.sort(industries, new Comparator<Industry>() {
                        @Override
                        public int compare(Industry i1, Industry i2) {
                            return Integer.compare(i2.getIncome().getModifiedInt(), i1.getIncome().getModifiedInt());
                        }
                    });
                    tp.beginGridFlipped(TP_WIDTH, 1, 65.0F, opad);

                    int indCount = 0;
                    for (Industry ind : industries) {
                        int perIndIncome = ind.getIncome().getModifiedInt();
                        if (perIndIncome > 0) {
                            tp.addToGrid(0, indCount++, ind.getCurrentName(),
                                    Misc.getDGSCredits(perIndIncome), highlight);
                        }
                    }

                    if (indCount > 0) {
                        tp.addGrid(pad);
                    } else {
                        tp.cancelGrid();
                    }
                }

                int exportIncome = (int) market.getExportIncome(false);
                tp.addPara("Exports: %s", opad, highlight, Misc.getDGSCredits(exportIncome));
                if (hasUpkeep && exportIncome > 0) {
                    final int maxCommoditiesToDisplay = 10;
                    final float somethingWidth = 500f - 14f - 395f;
                    final float extraPad = ((int)(somethingWidth / 3f));

                    tp.beginTable(faction, 20f, "Commodity", 150f + extraPad, "Market share", 100f + extraPad, "Income", 100f + extraPad);
                    int cumulativeIncome = 0;
                    final List<CommodityOnMarketAPI> commodities = market.getCommoditiesCopy();
                    for (CommodityOnMarketAPI com : commodities) {
                        if (com.getExportIncome() > 0) {
                            ++cumulativeIncome;
                        }
                    }

                    Collections.sort(commodities, new Comparator<CommodityOnMarketAPI>() {
                        @Override
                        public int compare(CommodityOnMarketAPI c1, CommodityOnMarketAPI c2) {
                            return Long.compare(c2.getExportIncome(), c1.getExportIncome());
                        }
                    });
                    int var16 = 0;
                    for (CommodityOnMarketAPI com : commodities) {
                        final String name = com.getCommodity().getName();
                        final int comExportIncome = com.getExportIncome();
                        if (comExportIncome <= 0) continue;

                        final int exportMarketShare = EconomyEngine.getInstance()
                            .getExportMarketShare(com.getId(), market.getId());

                        tp.addRow(Alignment.LMID, Misc.getTextColor(), " " + name, highlight, exportMarketShare + "%", highlight, Misc.getDGSCredits(comExportIncome));
                        var16++;
                        if (var16 + 1 > maxCommoditiesToDisplay && cumulativeIncome - var16 > 1) {
                        break;
                        }
                    }

                    tp.addTable("No exports", cumulativeIncome - var16, opad);
                }

                if (hasUpkeep) {
                    tp.addSectionHeading("Upkeep", base, dark, Alignment.MID, opad);
                }

                tp.addPara("Industry and structure upkeep: %s", opad, negative, indUpkeep);
                if (hasUpkeep) {
                    Collections.sort(industries, new Comparator<Industry>() {
                        @Override
                        public int compare(Industry i1, Industry i2) {
                            return Integer.compare(i2.getIncome().getModifiedInt(), i1.getIncome().getModifiedInt());
                        }
                    });
                    tp.beginGridFlipped(TP_WIDTH, 1, 65f, opad);
                    int indCount = 0;

                    for (Industry ind : industries) {
                        int var24 = ind.getUpkeep().getModifiedInt();
                        if (var24 > 0) {
                            java.lang.String var25 = Misc.getDGSCredits((float) var24);
                            tp.addToGrid(0, indCount++, ind.getCurrentName(), var25, negative);
                        }
                    }

                    if (indCount > 0) {
                        tp.addGrid(pad);
                    } else {
                        tp.cancelGrid();
                    }
                }

                final int shortage = (int) market.getShortageCounteringCost();
                if (shortage > 0 && market.isUseStockpilesForShortages()) {
                    tp.addPara("Shortage countering: %s", opad, negative, Misc.getDGSCredits(shortage));
                }

                final int incentive = (int) market.getImmigrationIncentivesCost();
                if (incentive > 0 && market.isImmigrationIncentivesOn()) {
                    tp.addPara("Hazard pay: %s", opad, negative, Misc.getDGSCredits(incentive));
                }
                return tp;
            }
        };

        incomePanel.addComponent(creditsLabel.getPanel()).inTL(0, 0);
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
        if (commodityPanel == null)
            return;

        try {
            // Steal the members for the constructor
            final MarketAPI market = (MarketAPI) (ReflectionUtils.get(commodityPanel, null, MarketAPI.class));

            final int width = (int) commodityPanel.getPosition().getWidth();
            final int height = (int) commodityPanel.getPosition().getHeight();

            final LtvCommodityPanel replacement = new LtvCommodityPanel(
                    managementPanel,
                    width,
                    height,
                    new BasePanelPlugin<LtvCommodityPanel>());
            replacement.setMarket(market);

            final ActionListenerPanel listener = new ActionListenerPanel(managementPanel, 0, 0) {
                @Override
                public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
                    if (!isLeftClick)
                        return;

                    LtvCommodityRowPanel panel = ((LtvCommodityRowPanel) source);

                    replacement.selectRow(panel);

                    if (replacement.m_canViewPrices) {
                        final ComDetailDialog dialogPanel = new ComDetailDialog(market, panel.getCommodity());

                        WrapUiUtils.CustomDialogViewer(
                                dialogPanel, dialogPanel.PANEL_W, dialogPanel.PANEL_H);
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
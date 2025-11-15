package wfg.ltv_econ.plugins;

import java.util.List;
import java.awt.Color;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import wfg.ltv_econ.ui.dialogs.ColonyInvDialog;
import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.panels.ActionListenerPanel;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.plugins.ButtonPlugin;
import wfg.reflection.ReflectionUtils;
import wfg.reflection.ReflectionUtils.ReflectedConstructor;

import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ShippingPanel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;

public class LtvMarketReplacer implements EveryFrameScript {

    public static final int pad = 3;
    public static final SectorAPI sector = Global.getSector();

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
        if (masterTab == null) return;

        final List<?> listChildren = (List<?>) ReflectionUtils.invoke(masterTab, "getChildrenCopy");
        final UIPanelAPI outpostPanel = listChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "getOutpostPanelParams").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (outpostPanel == null) return;


        final List<?> outpostChildren = (List<?>) ReflectionUtils.invoke(outpostPanel, "getChildrenCopy");
        final UIPanelAPI overviewPanel = outpostChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "showOverview").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (overviewPanel == null) return;


        final List<?> overviewChildren = (List<?>) ReflectionUtils.invoke(overviewPanel, "getChildrenCopy");
        final UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "recreateWithEconUpdate").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null) return;


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
                !childClass.equals(knownClass4)
            ) {
                anchorChild = (UIPanelAPI) child;
                break;
            }
        }
        if (anchorChild == null) return;

        // Replace the "Use stockpiles during shortages" button
        replaceUseStockpilesButton(managementPanel, managementChildren, anchorChild);

        // Replace the Panel which holds the widgets
        replaceIndustryListPanel(managementPanel, managementChildren, anchorChild);

        // Replace the Commodity Panel which shows the total imports and exports
        replaceCommodityPanel(managementPanel, managementChildren, anchorChild);
    }

    private static final void replaceUseStockpilesButton(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI colonyInfoPanel
    ) {
        for (Object child : managementChildren) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof ButtonPlugin) {
                return;
            }
        }

        final ShippingPanel shipPanel = (ShippingPanel) ReflectionUtils.invoke(colonyInfoPanel, "getShipping");

        final ButtonAPI useStockpilesBtn = (ButtonAPI) ReflectionUtils.invoke(shipPanel, "getUseStockpiles");

        final MarketAPI market = (MarketAPI) ReflectionUtils.get(shipPanel, null, MarketAPI.class);

        final Runnable buildButtonRunnable = () -> {
            final ColonyInvDialog dialogPanel = new ColonyInvDialog(market);

            WrapUiUtils.CustomDialogViewer(
                dialogPanel, ColonyInvDialog.PANEL_W, ColonyInvDialog.PANEL_H
            );
        };

        final Button inventoryBtn = new Button(
            managementPanel, LtvCommodityPanel.STANDARD_WIDTH, 20, "Colony Inventory",
            Fonts.ORBITRON_12, buildButtonRunnable
        );
        inventoryBtn.setLabeColor(Misc.getBasePlayerColor());
        inventoryBtn.bgColor = new Color(0, 0, 0, 255);
        inventoryBtn.bgDisabledColor = new Color(0, 0, 0, 255);
        inventoryBtn.quickMode = true;

        managementPanel.addComponent(inventoryBtn.getPanel()).inBL(0, 0);

        final int xOffset = (int) (useStockpilesBtn.getPosition().getX() - inventoryBtn.getPos().getX());
        final int yOffset = (int) (useStockpilesBtn.getPosition().getY() - inventoryBtn.getPos().getY());

        inventoryBtn.getPos().inBL(xOffset, yOffset);

        useStockpilesBtn.setOpacity(0);
        useStockpilesBtn.setEnabled(false);

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced UseStockpilesButton");
        }
    }

    private static final void replaceIndustryListPanel(
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI anchor
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
        final MarketAPI market = (MarketAPI)ReflectionUtils.get(industryPanel, null, MarketAPI.class);
        final int width = (int) industryPanel.getPosition().getWidth();
        final int height = (int) industryPanel.getPosition().getHeight();

        final LtvIndustryListPanel replacement = new LtvIndustryListPanel(
            managementPanel,
            width,
            height,
            market,
            industryPanel
        );

        managementPanel.addComponent(replacement.getPanel());
        WrapUiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomLeft, 25);

        if (LtvIndustryListPanel.indOptCtor == null) {
            // Acquire the popup class from one of the widgets
            final Object widget0 = ((IndustryListPanel) industryPanel).getWidgets().get(0);

            // Attach the popup
            ReflectionUtils.invoke(widget0, "actionPerformed", null, null);

            // Now the popup class is a child of: 
            // CampaignEngine.getInstance().getCampaignUI().getDialogParent();

            final List<?> children = CampaignEngine.getInstance().getCampaignUI().getDialogParent().getChildrenNonCopy();

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
        UIPanelAPI managementPanel, List<?> managementChildren, UIPanelAPI anchor
    ) {
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
            final MarketAPI market = (MarketAPI)(ReflectionUtils.get(commodityPanel, null, MarketAPI.class));

            final int width = (int) commodityPanel.getPosition().getWidth();
            final int height = (int) commodityPanel.getPosition().getHeight();

            final LtvCommodityPanel replacement = new LtvCommodityPanel(
                managementPanel,
                width,
                height,
                new BasePanelPlugin<LtvCommodityPanel>()
            );
            replacement.setMarket(market);

            final ActionListenerPanel listener = new ActionListenerPanel(managementPanel,0, 0) {
                @Override
                public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
                    if (!isLeftClick) return;
                    
                    LtvCommodityRowPanel panel = ((LtvCommodityRowPanel)source);

                    replacement.selectRow(panel);

                    if (replacement.m_canViewPrices) {
                        final ComDetailDialog dialogPanel = new ComDetailDialog(market, panel.getCommodity());

                        WrapUiUtils.CustomDialogViewer(
                            dialogPanel, dialogPanel.PANEL_W, dialogPanel.PANEL_H
                        );
                    } 
                }
            };

            replacement.setActionListener(listener);
            replacement.createPanel();

            // Got the Y offset by looking at the getY() difference of replacement and commodityPanel
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

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
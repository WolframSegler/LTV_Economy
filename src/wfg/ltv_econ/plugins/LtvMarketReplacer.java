package wfg.ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.state.AppDriver;

import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.panels.ActionListenerPanel;
import wfg.ltv_econ.ui.panels.ColonyInventoryButton;
import wfg.ltv_econ.ui.panels.LtvCommodityPanel;
import wfg.ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg.ltv_econ.ui.panels.LtvCustomPanel;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.ltv_econ.ui.panels.ColonyInventoryButton.ColonyInvButtonPlugin;
import wfg.ltv_econ.ui.plugins.BasePanelPlugin;
import wfg.ltv_econ.util.UiUtils;
import wfg.ltv_econ.util.UiUtils.AnchorType;
import wfg.ltv_econ.util.ReflectionUtils;
import wfg.ltv_econ.util.ReflectionUtils.ReflectedConstructor;

import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.campaign.ui.marketinfo.ShippingPanel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;

public class LtvMarketReplacer implements EveryFrameScript {

    public final static int pad = 3;

    private int frames = 0;

    @Override
    public void advance(float amount) {

        if (!Global.getSector().isPaused()) {
            frames = 0;
            return;
        }

        if (!Global.getSector().getCampaignUI().isShowingDialog()) {
            return;
        }

        frames++;
        if (frames < 2) {
            return;
        }
        Object state = AppDriver.getInstance().getCurrentState();
        if (!(state instanceof CampaignState)) {
            return;
        }

        // Find the master UI Panel
        UIPanelAPI master = null;
        Object dialog = ((CampaignState) state).getEncounterDialog();
        if (dialog != null) {
            master = (UIPanelAPI) ReflectionUtils.invoke(dialog, "getCoreUI");
        }
        if (master == null) {
            master = (UIPanelAPI)ReflectionUtils.invoke(state, "getCore");
            // Access the Market from the Command menu (remote access)
        }
        if (master == null) {
            return;
        }

        // Find managementPanel
        Object masterTab = ReflectionUtils.invoke(master, "getCurrentTab", new Object[0]);
        if (!(masterTab instanceof UIPanelAPI)) {
            return;
        }

        List<?> listChildren = (List<?>) ReflectionUtils.invoke(masterTab, "getChildrenCopy");
        if (listChildren == null) {
            return;
        }

        UIPanelAPI outpostPanel = listChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "getOutpostPanelParams").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (outpostPanel == null) {
            return;
        }

        List<?> outpostChildren = (List<?>) ReflectionUtils.invoke(outpostPanel, "getChildrenCopy");
        UIPanelAPI overviewPanel = outpostChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "showOverview").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (overviewPanel == null) {
            return;
        }

        List<?> overviewChildren = (List<?>) ReflectionUtils.invoke(overviewPanel, "getChildrenCopy");
        UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "recreateWithEconUpdate").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null) {
            return;
        }

        List<?> managementChildren = (List<?>) ReflectionUtils.invoke(managementPanel, "getChildrenCopy");

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
        if (anchorChild == null) {
            return;
        }

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
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof ColonyInvButtonPlugin) {
                return;
            }
        }

        ShippingPanel shipPanel = (ShippingPanel) ReflectionUtils.invoke(colonyInfoPanel, "getShipping");

        ButtonAPI useStockpilesBtn = (ButtonAPI) ReflectionUtils.invoke(shipPanel, "getUseStockpiles");

        MarketAPI market = (MarketAPI) ReflectionUtils.get(shipPanel, null, MarketAPI.class);

        ColonyInventoryButton inventoryBtn = new ColonyInventoryButton(
            managementPanel, managementPanel, market
        );

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
        MarketAPI market = (MarketAPI)ReflectionUtils.get(industryPanel, null, MarketAPI.class);
        int width = (int) industryPanel.getPosition().getWidth();
        int height = (int) industryPanel.getPosition().getHeight();

        LtvIndustryListPanel replacement = new LtvIndustryListPanel(
            managementPanel,
            managementPanel,
            width,
            height,
            market,
            industryPanel
        );

        managementPanel.addComponent(replacement.getPanel());
        UiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomLeft, 25);

        if (LtvIndustryListPanel.indOptCtor == null) {
            // Acquire the popup class from one of the widgets
            Object widget0 = ((IndustryListPanel) industryPanel).getWidgets().get(0);

            // Attach the popup
            ReflectionUtils.invoke(widget0, "actionPerformed", null, null);

            // Now the popup class is a child of: 
            // CampaignEngine.getInstance().getCampaignUI().getDialogParent();

            List<?> children = CampaignEngine.getInstance().getCampaignUI().getDialogParent().getChildrenNonCopy();

            UIPanelAPI indOps = children.stream()
                .filter(child -> child instanceof DialogCreatorUI && child instanceof UIPanelAPI)
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);

            ReflectedConstructor indOpsPanelConstr = ReflectionUtils.getConstructorsMatching(
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
            MarketAPI market = (MarketAPI)(ReflectionUtils.get(commodityPanel, null, MarketAPI.class));

            int width = (int) commodityPanel.getPosition().getWidth();
            int height = (int) commodityPanel.getPosition().getHeight();

            LtvCommodityPanel replacement = new LtvCommodityPanel(
                managementPanel,
                managementPanel,
                width,
                height,
                market,
                new BasePanelPlugin<LtvCommodityPanel>()
            );

            ActionListenerPanel listener = new ActionListenerPanel(
                managementPanel,
                managementPanel,
                0, 0, market
            ) {
                @Override
                public void onClicked(LtvCustomPanel<?, ?, ?> source, boolean isLeftClick) {
                    if (!isLeftClick) {
                        return;
                    }
                    
                    LtvCommodityRowPanel panel = ((LtvCommodityRowPanel)source);

                    replacement.selectRow(panel);

                    if (replacement.m_canViewPrices) {
                        final ComDetailDialog dialogPanel = new ComDetailDialog(panel, panel.getCommodity());

                        UiUtils.CustomDialogViewer(
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
            UiUtils.anchorPanel(replacement.getPanel(), anchor, AnchorType.BottomRight, -43);
            
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
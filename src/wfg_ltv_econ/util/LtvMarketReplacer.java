package wfg_ltv_econ.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.state.AppDriver;

import wfg_ltv_econ.ui.panels.LtvCommodityPanel;
import wfg_ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg_ltv_econ.ui.ui_plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.util.ReflectionUtils.ReflectedConstructor;

import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;

import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.campaign.ui.marketinfo.s;

import java.util.List;

public class LtvMarketReplacer implements EveryFrameScript {

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
        // if (master == null) {
        //     master = (UIPanelAPI)ReflectionUtils.invoke(state, "getCore");
        //     // Access the Market from the Command menu (remote access)
        // }
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
        // managementPanel = com.fs.starfarer.campaign.ui.marketinfo.s
        UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "recreateWithEconUpdate").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null) {
            return;
        }

        List<?> managementChildren = (List<?>) ReflectionUtils.invoke(managementPanel, "getChildrenCopy");

        // Replace the Panel which holds the widgets
        replaceIndustryListPanel(managementPanel, managementChildren);

        // Replace the Commodity Panel which shows the total imports and exports
        replaceCommodityPanel(managementPanel, managementChildren);
    }

    private static final void replaceIndustryListPanel(UIPanelAPI managementPanel, List<?> managementChildren) {
        UIPanelAPI industryPanel = managementChildren.stream()
                .filter(child -> child instanceof IndustryListPanel)
                .map(child -> (IndustryListPanel) child)
                .findFirst().orElse(null);
        if (industryPanel instanceof LtvIndustryListPanel) {
            return;
        }

        try {
            // Steal the members for the constructor
            MarketAPI market = (MarketAPI)ReflectionUtils.get(industryPanel, null, MarketAPI.class);
            L lInstance = (L)ReflectionUtils.get(industryPanel, null, L.class);
            s sInstance = (s)ReflectionUtils.get(industryPanel, null, s.class);

            LtvIndustryListPanel replacement = new LtvIndustryListPanel(market, lInstance, sInstance);

            float width = industryPanel.getPosition().getWidth();
            float height = industryPanel.getPosition().getHeight();
            // The Panel with the player portrait
            UIPanelAPI managementPanelChild1 = (UIPanelAPI)managementChildren.get(0);

            managementPanel.addComponent(replacement).setSize(width, height).belowLeft(managementPanelChild1, 25);
            
            // // Acquire the popup class from one of the widgets
            // Object widget0 = ((IndustryListPanel) industryPanel).getWidgets().get(0);

            // // Now the popup class is a child of: 
            // // CampaignEngine.getInstance().getCampaignUI().getDialogParent();
            // ReflectionUtils.invoke(widget0, "actionPerformed", null, null);

            // List<?> children = CampaignEngine.getInstance().getCampaignUI().getDialogParent().getChildrenNonCopy();

            // UIPanelAPI indOps = children.stream()
            //     .filter(child -> child instanceof DialogCreatorUI && child instanceof UIPanelAPI)
            //     .map(child -> (UIPanelAPI) child)
            //     .findFirst().orElse(null);

            // ReflectedConstructor indOpsPanelConstr = ReflectionUtils.getConstructorsMatching(
            //     indOps.getClass(), 5).get(0);

            // LtvIndustryListPanel.setindustryOptionsPanelConstructor(indOpsPanelConstr);

            // // Delete the indOpsPanel after getting its constructor
            // children.remove(indOps);
            
            // No need for the old panel
            managementPanel.removeComponent(industryPanel);

        } catch (Exception e) {
            Global.getLogger(LtvMarketReplacer.class).error("Failed to replace IndustryListPanel", e);
        }

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced IndustryListPanel");
        }
    }

    private static final void replaceCommodityPanel(UIPanelAPI managementPanel, List<?> managementChildren) {
        UIPanelAPI commodityPanel = managementChildren.stream()
                .filter(child -> child instanceof CommodityPanel)
                .map(child -> (CommodityPanel) child)
                .findFirst().orElse(null);
        if (commodityPanel == null || commodityPanel instanceof LtvCommodityPanel) {
            return;
        }

        try {
            // Steal the members for the constructor
            MarketAPI market = (MarketAPI)(ReflectionUtils.get(commodityPanel, null, MarketAPI.class));

            int width = (int) commodityPanel.getPosition().getWidth();
            int height = (int) commodityPanel.getPosition().getHeight();
            // The Panel with the player portrait
            UIPanelAPI managementPanelChild1 = (UIPanelAPI)managementChildren.get(0);

            LtvCommodityPanel replacement = new LtvCommodityPanel(null ,managementPanel, width, height,
                market, new LtvCustomPanelPlugin());

            // Got the Y offset by looking at the getY() difference of replacement and commodityPanel
            // Might automate the getY() difference later
            managementPanel.addComponent(replacement.getPanel()).setSize(width, height).belowRight(managementPanelChild1, -43);
            
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
package wfg_ltv_econ.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.state.AppDriver;

import wfg_ltv_econ.plugins.LtvCustomPanelPlugin;
import wfg_ltv_econ.ui.BuildingWidgetPanel;
import wfg_ltv_econ.ui.LtvCommodityPanel;
import wfg_ltv_econ.ui.LtvIndustryListPanel;

import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.ui.marketinfo.intnew;
import com.fs.starfarer.ui.newui.o0Oo;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityPanel;

import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.campaign.ui.marketinfo.s;

import java.util.ArrayList;
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
        o0Oo dialog = ((CampaignState) state).getEncounterDialog();
        if (dialog != null && dialog.getCoreUI() != null) {
            master = (UIPanelAPI) dialog.getCoreUI();
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

            LtvCommodityPanel replacement = new LtvCommodityPanel(managementPanel, width, height, market, new LtvCustomPanelPlugin());

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

    @Deprecated
    @SuppressWarnings("unused")
    private void repopulateWidgetsList(UIPanelAPI UIindustryPanel) {
        if (UIindustryPanel == null || !(UIindustryPanel instanceof IndustryListPanel)) {
            return;
        }
        MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null) {
            return;
        }

        List<intnew> currentWidgets = ((IndustryListPanel)UIindustryPanel).getWidgets();

        // If all the widgets are already my custom widget
        if (currentWidgets.stream().allMatch(widget -> widget instanceof BuildingWidgetPanel)) {
            return;
        }

        List<BuildingWidgetPanel> LtvWidgets = new ArrayList<>();
        for (Object widget : currentWidgets) {
            MarketAPI widgetMarket = (MarketAPI) ReflectionUtils.get(widget, "market");
            Industry industry = (Industry) ReflectionUtils.get(widget, null, Industry.class, true); // "øôöO00"
            IndustryListPanel industryPanel = (IndustryListPanel) ReflectionUtils.invoke(widget, "getIndustryPanel");
            Integer queueIndex = (Integer) ReflectionUtils.invoke(widget, "getQueueIndex");

            if (industry == null || widgetMarket == null) {
                continue;
            }
            if (queueIndex == null) {
                queueIndex = -1;
            }

            BuildingWidgetPanel newWidget = new BuildingWidgetPanel(widgetMarket, industry, industryPanel, queueIndex);

            // Correct missing variables
            ReflectionUtils.set(newWidget, "constructionActionButton", ((intnew)widget).getButton());


            if (newWidget instanceof BuildingWidgetPanel) {
                LtvWidgets.add((BuildingWidgetPanel) newWidget);
            }
        }
        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketReplacer.class).info("Replaced IndustryListPanel widgets");
        }
        ReflectionUtils.set((IndustryListPanel)UIindustryPanel, "widgets", LtvWidgets);
        List<?> newWidgets = ((IndustryListPanel)UIindustryPanel).getWidgets();

        // force a refresh
        try {
            for (BuildingWidgetPanel widget : (List<BuildingWidgetPanel>)newWidgets) {
            widget.notifySizeChanged();
        } 
        } catch (Exception e) {
            Global.getLogger(LtvMarketReplacer.class).error("Replaced IndustryListPanel widgets refresh failed: ", e);
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
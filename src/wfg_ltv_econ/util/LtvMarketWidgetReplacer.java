package wfg_ltv_econ.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.state.AppDriver;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import wfg_ltv_econ.industry.BuildingWidget;
import com.fs.starfarer.campaign.ui.marketinfo.intnew;
import com.fs.starfarer.ui.newui.o0Oo;

import com.fs.starfarer.ui.newui.L;
import com.fs.starfarer.campaign.ui.marketinfo.s;

import java.util.ArrayList;
import java.util.List;

public class LtvMarketWidgetReplacer implements EveryFrameScript {

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
        if (frames < 3) {
            return;
        }
        Object state = AppDriver.getInstance().getCurrentState();
        if (!(state instanceof CampaignState)) {
            return;
        }

        // Find the master UI Panel
        UIPanelAPI master = null;
        o0Oo dialog = ((CampaignState) state).getEncounterDialog();
        if (dialog == null || dialog.getCoreUI() == null) {
            return;
        }
        master = (UIPanelAPI) dialog.getCoreUI();
        // if (master == null) {
        //     master = (UIPanelAPI)ReflectionUtils.invoke(state, "getCore");
        //     // Access to the Market from the Game Menu or smh
        // }
        if (master == null) {
            return;
        }

        // Find IndustryListPanel
        UIPanelAPI industryPanel = null;

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
        industryPanel = managementChildren.stream()
                .filter(child -> child instanceof IndustryListPanel)
                .map(child -> (IndustryListPanel) child)
                .findFirst().orElse(null);

        // Replace the Panel which holds the widgets
        replaceIndustryListPanel(managementPanel, industryPanel);

        // Get the "widgets" List and replace it with the custom List
        // repopulateWidgetsList(industryPanel);
    }

    private void replaceIndustryListPanel(UIPanelAPI managementPanel, UIPanelAPI industryPanel) {
        if (industryPanel instanceof LtvIndustryListPanel) {
            return;
        }
        try {
            // Steal the members for the constructor
            MarketAPI market = (MarketAPI)ReflectionUtils.get(industryPanel, null, MarketAPI.class);
            L lInstance = (L)ReflectionUtils.get(industryPanel, null, L.class);
            s sInstance = (s)ReflectionUtils.get(industryPanel, null, s.class);

            LtvIndustryListPanel replacement = new LtvIndustryListPanel(market, lInstance, sInstance);
            managementPanel.removeComponent(industryPanel);

            float x = industryPanel.getPosition().getX();
            float y = industryPanel.getPosition().getY();
            float width = industryPanel.getPosition().getWidth();
            float height = industryPanel.getPosition().getHeight();

            managementPanel.addComponent(replacement).setLocation(x, y).setSize(width, height);
            replacement.recreate(); // Required for complete initialization

        } catch (Exception e) {
            Global.getLogger(getClass()).error("Failed to replace IndustryListPanel", e);
        }

        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketWidgetReplacer.class).info("Replaced IndustryListPanel");
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
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
        if (currentWidgets.stream().allMatch(widget -> widget instanceof BuildingWidget)) {
            return;
        }

        List<BuildingWidget> LtvWidgets = new ArrayList<>();
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

            BuildingWidget newWidget = new BuildingWidget(widgetMarket, industry, industryPanel, queueIndex);

            // Correct missing variables
            ReflectionUtils.set(newWidget, "constructionActionButton", ((intnew)widget).getButton());


            if (newWidget instanceof BuildingWidget) {
                LtvWidgets.add((BuildingWidget) newWidget);
            }
        }
        if (Global.getSettings().isDevMode()) {
            Global.getLogger(LtvMarketWidgetReplacer.class).info("Replaced IndustryListPanel widgets");
        }
        ReflectionUtils.set((IndustryListPanel)UIindustryPanel, "widgets", LtvWidgets);
        List<?> newWidgets = ((IndustryListPanel)UIindustryPanel).getWidgets();

        // force a refresh
        try {
            for (BuildingWidget widget : (List<BuildingWidget>)newWidgets) {
            widget.notifySizeChanged();
        } 
        } catch (Exception e) {
            Global.getLogger(LtvMarketWidgetReplacer.class).error("Replaced IndustryListPanel widgets refresh failed: ", e);
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
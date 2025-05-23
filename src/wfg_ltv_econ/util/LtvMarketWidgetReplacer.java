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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class LtvMarketWidgetReplacer implements EveryFrameScript {

    private int frames = 0;

    @Override
    public void advance(float amount) {
        Object state = AppDriver.getInstance().getCurrentState();
        if (!(state instanceof CampaignState) || !Global.getSector().isPaused()) {
            frames = 0;
            return;
        }
        if (Global.getSector().getCampaignUI().isShowingDialog()) {
            return;
        }

        frames = Math.min(frames + 1, 3);
        if (frames < 2) {
            return; // pause for 2 frames
        }

        // Find the master UI Panel
        UIPanelAPI master = null;

        Object dialog = ReflectionUtils.invoke(state, "getEncounterDialog");
        if (dialog != null) {
            master = (UIPanelAPI) ReflectionUtils.invoke(dialog, "getCoreUI");
        }
        if (master == null) {
            master = (UIPanelAPI) ReflectionUtils.invoke(state, "getCore");
        }
        if (master == null) {
            return;
        }
        Global.getLogger(LtvMarketWidgetReplacer.class).info("passed stage 6");

        // Find IndustryListPanel
        UIPanelAPI industryPanel = null;

        Object masterTab = ReflectionUtils.invoke(master, "getCurrentTab");
        if (!(masterTab instanceof UIPanelAPI)) {
            return;
        }

        UIPanelAPI masterPanel = (UIPanelAPI) masterTab;
        List<?> tabChildren = (List<?>) ReflectionUtils.invoke(masterPanel, "getChildren");
        UIPanelAPI outpostPanel = tabChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "getOutpostPanelParams").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (outpostPanel == null) {
            return;
        }

        List<?> outpostChildren = (List<?>) ReflectionUtils.invoke(outpostPanel, "getChildren");
        UIPanelAPI overviewPanel = outpostChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "showOverview").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (overviewPanel == null) {
            return;
        }

        List<?> overviewChildren = (List<?>) ReflectionUtils.invoke(overviewPanel, "getChildren");
        UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> !ReflectionUtils.getMethodsMatching(child, "recreateWithEconUpdate").isEmpty())
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null) {
            return;
        }

        List<?> managementChildren = (List<?>) ReflectionUtils.invoke(managementPanel, "getChildren");
        industryPanel = managementChildren.stream()
                .filter(child -> child instanceof IndustryListPanel)
                .map(child -> (IndustryListPanel) child)
                .findFirst().orElse(null);

        // Get the "widgets" List and replace it with the custom List
        repopulateWidgetsList(industryPanel);
    }

    private void repopulateWidgetsLista(UIPanelAPI industryPanel) {
        if (industryPanel == null || !(industryPanel instanceof IndustryListPanel)) {
            return;
        }
        MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null) {
            return;
        }

        List<?> originalWidgets = (List<?>) ReflectionUtils.get((IndustryListPanel) industryPanel, "widgets", List.class);

        List<BuildingWidget> LtvWidgets = new ArrayList<>();
        for (Object widget : originalWidgets) {
            MarketAPI widgetMarket = (MarketAPI) ReflectionUtils.get(widget, "market");
            Industry industry = (Industry) ReflectionUtils.get(widget, "øôöO00"); // Industry
            Integer queueIndex = (Integer) ReflectionUtils.get(widget, "ÖõöO00"); // var4

            if (industry == null || widgetMarket == null) {
                continue;
            }
            if (queueIndex == null) {
                queueIndex = -1;
            }

            try {
                Constructor<?> ctor = intnew.class.getConstructor(MarketAPI.class, Industry.class, IndustryListPanel.class, int.class);
                Object newWidget = ctor.newInstance(widgetMarket, industry, industryPanel, queueIndex);

                if (newWidget instanceof BuildingWidget) {
                    LtvWidgets.add((BuildingWidget) newWidget);
                }

                Global.getLogger(BuildingWidget.class).info("gloryToLife: " + industry.getId());
            } catch (Exception e) {
                Global.getLogger(LtvMarketWidgetReplacer.class).error("Widget instantiation failed", e);
            }
        }

        ReflectionUtils.set(industryPanel, "widgets", LtvWidgets);
    }

    private void repopulateWidgetsList(UIPanelAPI industryPanel) {
        if (industryPanel == null || !(industryPanel instanceof IndustryListPanel)) {
            return;
        }
        MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null) {
            return;
        }

        List<?> originalWidgets = (List<?>) ReflectionUtils.get((IndustryListPanel) industryPanel, "widgets", List.class);

        List<BuildingWidget> LtvWidgets = new ArrayList<>();
        for (Object widget : originalWidgets) {
            MarketAPI widgetMarket = (MarketAPI) ReflectionUtils.get(widget, "market");
            Industry industry = (Industry) ReflectionUtils.get(widget, "øôöO00"); // Industry
            Integer queueIndex = (Integer) ReflectionUtils.get(widget, "ÖõöO00"); // var4

            if (industry == null || widgetMarket == null) {
                continue;
            }
            if (queueIndex == null) {
                queueIndex = -1;
            }

            try {
                Constructor<?> ctor = intnew.class.getConstructor(MarketAPI.class, Industry.class, IndustryListPanel.class, int.class);
                Object newWidget = ctor.newInstance(widgetMarket, industry, industryPanel, queueIndex);

                if (newWidget instanceof BuildingWidget) {
                    LtvWidgets.add((BuildingWidget) newWidget);
                }

                Global.getLogger(BuildingWidget.class).info("gloryToLife: " + industry.getId());
            } catch (Exception e) {
                Global.getLogger(LtvMarketWidgetReplacer.class).error("Widget instantiation failed", e);
            }
        }

        ReflectionUtils.set(industryPanel, "widgets", LtvWidgets);
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
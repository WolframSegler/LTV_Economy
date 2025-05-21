package wfg_ltv_econ.util;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModPlugin;
import com.fs.state.AppDriver;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import wfg_ltv_econ.industry.BuildingWidget;
import wfg_ltv_econ.plugins.WidgetSniffer;

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
        if (Global.getSector().getCampaignUI().isShowingDialog()
                || Global.getSector().getCampaignUI().getCurrentCoreTab() == null) {
            return;
        }
        if (Global.getSector().getCampaignUI().getCurrentCoreTab() != CoreUITabId.OUTPOSTS) {
            return;
        }

        frames = Math.min(frames + 1, 3);
        if (frames < 2) {
            return; // pause for 2 frames
        }

        // Find the master UI Panel
        UIPanelAPI master = null;
        MarketAPI market = Global.getSector().getCurrentlyOpenMarket();
        if (market == null) {
            return;
        }

        Object dialog = Reflection.invoke("getEncounterDialog", state);
        if (dialog != null) {
            master = (UIPanelAPI) Reflection.invoke("getCoreUI", dialog);
        }
        if (master == null) {
            master = (UIPanelAPI) Reflection.invoke("getCore", state);
        }
        if (master == null) {
            return;
        }

        // Find IndustryListPanel
        UIPanelAPI industryPanel = null;

        Object masterTab = Reflection.invoke("getCurrentTab", master);
        if (!(masterTab instanceof UIPanelAPI)) {
            return;
        }

        UIPanelAPI masterPanel = (UIPanelAPI) masterTab;
        List<?> tabChildren = (List<?>) Reflection.invoke("getChildren", masterPanel);
        UIPanelAPI outpostPanel = tabChildren.stream()
                .filter(child -> Reflection.hasMethodOfName("getOutpostPanelParams", child))
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (outpostPanel == null) {
            return;
        }

        List<?> outpostChildren = (List<?>) Reflection.invoke("getChildren", outpostPanel);
        UIPanelAPI overviewPanel = outpostChildren.stream()
                .filter(child -> Reflection.hasMethodOfName("showOverview", child))
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (overviewPanel == null) {
            return;
        }

        List<?> overviewChildren = (List<?>) Reflection.invoke("getChildren", overviewPanel);
        UIPanelAPI managementPanel = overviewChildren.stream()
                .filter(child -> Reflection.hasMethodOfName("recreateWithEconUpdate", child))
                .map(child -> (UIPanelAPI) child)
                .findFirst().orElse(null);
        if (managementPanel == null) {
            return;
        }

        List<?> managementChildren = (List<?>) Reflection.invoke("getChildren", managementPanel);
        industryPanel = managementChildren.stream()
                .filter(child -> child instanceof IndustryListPanel)
                .map(child -> (IndustryListPanel) child)
                .findFirst().orElse(null);

        // Get the "widgets" List and replace it with the custom List
        repopulateWidgetsList(industryPanel, market);
    }

    private void repopulateWidgetsList(UIPanelAPI industryPanel, MarketAPI market) {
        if (industryPanel == null || !(industryPanel instanceof IndustryListPanel)) {
            return;
        }

        List<?> originalWidgets = (List<?>) Reflection.get("widgets", (IndustryListPanel) industryPanel);

        List<BuildingWidget> LtvWidgets = new ArrayList<>();
        for (Object widget : originalWidgets) {
            // intnew(MarketAPI var1, Industry var2, IndustryListPanel var3, int var4)
            MarketAPI widgetMarket = (MarketAPI) Reflection.get("market", widget);
            Industry industry = (Industry) Reflection.get("øôöO00", widget); // Industry
            Integer queueIndex = (Integer) Reflection.get("ÖõöO00", widget); // var4

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
            } catch (Exception e) {
                Global.getLogger(LtvMarketWidgetReplacer.class).error("Widget instantiation failed", e);
            }
        }

        Reflection.set("widgets", industryPanel, LtvWidgets);
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
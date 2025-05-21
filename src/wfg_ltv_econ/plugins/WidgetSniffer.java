package wfg_ltv_econ.plugins;

// delete later

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.campaign.ui.marketinfo.IndustryListPanel;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.lang.reflect.Field;
import java.util.List;

/** Runs only while a colony screen is up; logs widget classes. */
public class WidgetSniffer implements EveryFrameScript {

    private boolean done = false;

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {

        // 1. Only continue if a colony screen is open
        if (!Global.getSector().isPaused())
            return;
        if (!Global.getSector().getCampaignUI().isShowingDialog())
            return;

        Object dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null)
            return;
        Global.getLogger(this.getClass()).info("arbeiterBauern");

        // 2. Find the industry-management panel
        try {

            CampaignUIAPI ui = Global.getSector().getCampaignUI();
            if (ui.getCurrentCoreTab() == null) return;

            CoreUITabId coreTab = ui.getCurrentCoreTab();
            // dialog.coreUI.currentTab (private); grab via reflection
            // 2) Its direct children include the IndustryListPanel
        List<UIComponentAPI> children = coreTab.getChildrenCopy();
        IndustryListPanel listPanel = null;
        for (UIComponentAPI c : children) {
            if (c instanceof IndustryListPanel) {
                listPanel = (IndustryListPanel) c;
                break;
            }
        }
        if (listPanel == null) return;

        // 3) Grab its private 'widgets' list
        Field widgetsF = listPanel.getClass().getDeclaredField("widgets");
        widgetsF.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<?> widgets = (List<?>) widgetsF.get(listPanel);

        Global.getLogger(WidgetSniffer.class).info("*** Found "
                + widgets.size() + " industry widgets:");
        for (Object w : widgets) {
            Global.getLogger(WidgetSniffer.class)
                    .info("   -> " + w.getClass().getName());
        }

        done = true;  // only run once
        } catch (Exception ex) {
            Global.getLogger(WidgetSniffer.class).error("Widget sniff failed", ex);
            done = true;
        }
    }

    public static void register() {
        Global.getSector().addTransientScript(new WidgetSniffer());
    }
}
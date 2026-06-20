package wfg.ltv_econ.ui.scripts;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.comms.IntelTabData;

import wfg.ltv_econ.constant.strings.LocalizedStrings;
import wfg.ltv_econ.ui.economyTab.EconomyOverviewPanel;
import wfg.ltv_econ.ui.reusable.AbstractTabButtonInjector;

public class IntelTabUIBuilder extends AbstractTabButtonInjector {
    private final IntelTabData tabData = CampaignEngine.getInstance().getUIData().getIntelData();

    protected int getCurrentTabIndex() {
        return tabData.getSelectedTabIndex();
    }

    protected void setCurrentTabIndex(int index) {
        tabData.setSelectedTabIndex(index);
    }

    protected CoreUITabId getTargetCoreTabId() {
        return CoreUITabId.INTEL;
    }

    protected String getButtonLabel() {
        return LocalizedStrings.str("uiBtnTitleEconomy");
    }

    protected UIComponentAPI createCustomComponent(UIPanelAPI parent) {
        return new EconomyOverviewPanel(parent).getPanel();
    }
}
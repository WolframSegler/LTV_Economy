package wfg.ltv_econ.intel;

import static wfg.ltv_econ.constants.EconomyConstants.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;

public class PolicyNotificationIntel extends BaseIntelPlugin {

    private static final String ICON = settings.getSpriteName("icons", "policy_icon");

    private final PlayerMarketData data;
    private final MarketPolicy policy;
    private final boolean available;

    public PolicyNotificationIntel(PlayerMarketData data, MarketPolicy policy, boolean available) {
        this.data = data;
        this.policy = policy;
        this.available = available;

        endingTimeRemaining = (float) MONTH;
    }

    @Override
    public String getSmallDescriptionTitle() {
        return available
            ? str("intelPolicyTitle1") + data.market.getName()
            : str("intelPolicyTitle2") + data.market.getName();
    }
    
    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return data.market.getPrimaryEntity();
	}

    @Override
    public void createSmallDescription(TooltipMakerAPI tp, float width, float height) {
        tp.addPara(
            available
                ? str("intelPolicyTpTxt1")
                : str("intelPolicyTpTxt2"),
            0f,
            highlight, policy.spec.name,
            data.market.getName()
        );
    }

    public String getIcon() { return ICON;}
    public boolean isImportant() { return false;}
    public IntelSortTier getSortTier() { return IntelSortTier.TIER_3;}
    protected String getName() { return getSmallDescriptionTitle();}
    public boolean isEnding() { return true;}
}
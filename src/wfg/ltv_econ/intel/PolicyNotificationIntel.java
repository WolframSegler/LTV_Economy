package wfg.ltv_econ.intel;

import static wfg.ltv_econ.constants.economyValues.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;

public class PolicyNotificationIntel extends BaseIntelPlugin {

    private final PlayerMarketData data;
    private final MarketPolicy policy;
    private final boolean available;

    public PolicyNotificationIntel(PlayerMarketData data, MarketPolicy policy, boolean available) {
        this.data = data;
        this.policy = policy;
        this.available = available;

        important = false;
        endingTimeRemaining = (float) MONTH;
    }

    @Override
    public String getSmallDescriptionTitle() {
        return available
            ? "Policy Available - " + data.market.getName()
            : "Policy Finished - " + data.market.getName();
    }

    @Override
    protected String getName() {
        return getSmallDescriptionTitle();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return data.market.getPrimaryEntity();
	}

    @Override
    public void createSmallDescription(TooltipMakerAPI tp, float width, float height) {
        tp.addPara(
            available
                ? "The policy %s is now available on %s."
                : "The policy %s has finished on %s.",
            0f,
            highlight,
            policy.spec.name,
            data.market.getName()
        );
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("icons", "policy_icon");
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_3;
    }

    @Override
    public boolean isEnding() {
        return true;
    }
}
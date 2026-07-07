package wfg.ltv_econ.intel.market.events;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class GeneralStrikeEvent extends MarketEvent {
    private static final int BASE_DUR = 30;
    private static final int BASE_COOLDOWN = 90;
    private static final float PROD_MULT = 0.05f;

    private int cooldownDaysRemaining = 0;
    private int activeDaysRemaining = 0;

    @Override
    public void preAdvance(MarketPopulationData data) {
        if (active || cooldownDaysRemaining > 0) return;

        final float consciousness = data.getClassConsciousness();
        if (consciousness <= 70f) return;

        final float factor = (consciousness - 70f) / 30f;
        final float maxDailyChance = 0.02f;
        if (Math.random() >= factor * maxDailyChance) return;

        activate(data);
    }

    private final void activate(MarketPopulationData data) {
        if (active) return;
        active = true;

        activeDaysRemaining = BASE_DUR;
        cooldownDaysRemaining = BASE_DUR + BASE_COOLDOWN;

        final String marketID = data.marketID;
        for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
            final CommodityCell cell = dom.getCell(marketID);
            cell.getProductionStat().modifyMult(id, PROD_MULT, spec.name);
            cell.getConsumptionStat().modifyMult(id, PROD_MULT, spec.name);
            cell.getTargetQuantumStat().modifyPercent(id, 100f / PROD_MULT, spec.name); // to counter-balance the reduction in consumption.
        }

        Global.getSector().getIntelManager().addIntel(
            new GeneralStrikeIntel(data.market), false
        );
    }

    @Override
        public void postAdvance(MarketPopulationData data) {
        if (active) {
            activeDaysRemaining--;
            if (activeDaysRemaining <= 0) deactivate(data);
        } else if (cooldownDaysRemaining > 0) {
            cooldownDaysRemaining--;
        }
    }

    private final void deactivate(MarketPopulationData data) {
        if (!active) return;
        active = false;

        final String marketID = data.marketID;
        for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
            final CommodityCell cell = dom.getCell(marketID);
            cell.getProductionStat().unmodifyMult(id);
            cell.getConsumptionStat().unmodifyMult(id);
            cell.getTargetQuantumStat().unmodifyPercent(id);
        }
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        tp.setTitleSmallOrbitron();
        tp.addTitle(spec.name, negative);

        tp.addPara(spec.description, pad);

        tp.addPara(str("marketEventProdReducedToTxt"), pad, negative, String.format("%.0f%%", PROD_MULT * 100f));

        tp.addPara(str("marketEventActiveForTxt"), opad, negative, Integer.toString(activeDaysRemaining));
    }

    private class GeneralStrikeIntel extends BaseIntelPlugin {
        private final MarketAPI market;

        public GeneralStrikeIntel(MarketAPI market) {
            this.market = market;
        }

        @Override
        public final String getSmallDescriptionTitle() {
            return str("marketEventGeneralStrikeTitle") + market.getName();
        }
        
        @Override
        public final SectorEntityToken getMapLocation(SectorMapAPI map) {
            return market.getPrimaryEntity();
        }

        @Override
        public final void createSmallDescription(TooltipMakerAPI tp, float width, float height) {
            tp.addPara(spec.description, pad);
        }

        public final String getIcon() { return spec.iconPath;}
        public final boolean isImportant() { return false;}
        public final IntelSortTier getSortTier() { return IntelSortTier.TIER_3;}
        public final boolean isEnding() { return true;}
        protected final String getName() { return getSmallDescriptionTitle();}
    }
}
package wfg.ltv_econ.intel.market.events;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class GeneralStrikeEvent extends MarketEvent {
    private static final int BASE_DUR = 30;
    private static final int BASE_COOLDOWN = 90;
    private static final float PROD_MULT = 0.05f;

    private int cooldownDaysRemaining = 0;
    private int activeDaysRemaining = 0;

    @Override
    public void preAdvance(PlayerMarketData data) {
        if (active || cooldownDaysRemaining > 0) return;

        final float consciousness = data.getClassConsciousness();
        if (consciousness < 50f) return;

        final float factor = (consciousness - 50f) / 50f;
        final float maxDailyChance = 0.05f;
        if (Math.random() >= factor * maxDailyChance) return;

        activate(data);
    }

    private final void activate(PlayerMarketData data) {
        if (active) return;
        active = true;

        activeDaysRemaining = BASE_DUR;
        cooldownDaysRemaining = BASE_DUR + BASE_COOLDOWN;

        final String marketID = data.marketID;
        for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
            dom.getCell(marketID).getProductionStat().modifyMult(id, PROD_MULT, spec.name);
        }
    }

    @Override
        public void postAdvance(PlayerMarketData data) {
        if (active) {
            activeDaysRemaining--;
            if (activeDaysRemaining <= 0) deactivate(data);
        } else if (cooldownDaysRemaining > 0) {
            cooldownDaysRemaining--;
        }
    }

    private final void deactivate(PlayerMarketData data) {
        if (!active) return;
        active = false;

        final String marketID = data.marketID;
        for (CommodityDomain dom : EconomyEngine.instance().getComDomains()) {
            dom.getCell(marketID).getProductionStat().unmodifyMult(id);
        }
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.setTitleSmallOrbitron();
        tp.addTitle(spec.name, negative);

        tp.addPara(spec.description, pad);

        tp.addPara("Production reduced to %s", pad, negative, String.format("%.0f%%", PROD_MULT * 100f));

        tp.addPara("Active for %s more days", opad, negative, Integer.toString(activeDaysRemaining));
    }
}
package wfg.ltv_econ.intel.market.policies;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import static wfg.native_ui.util.UIConstants.*;

import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class ConvergenceFestivalPolicy extends MarketPolicy {
    public static final float HAPPINESS_BUFF = 0.67f;
    public static final float COHESION_BUFF = 0.34f;
    public static final float CLASS_DEBUFF = -0.06f;

    public static final float PRODUCTION_DEBUFF = 0.8f;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);

        for (CommodityDomain dom : EconomyEngine.getInstance().getComDomains()) {
            for (Industry ind : dom.getCell(data.marketID).getVisibleIndustries()) {
                ind.getSupplyBonus().modifyMult(
                    id, PRODUCTION_DEBUFF, spec.name
                );
            }
        }
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        for (CommodityDomain dom : EconomyEngine.getInstance().getComDomains()) {
            for (Industry ind : dom.getCell(data.marketID).getVisibleIndustries()) {
                ind.getSupplyBonus().unmodifyMult(id);
            }
        }
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Happiness", String.format("%+.2f", HAPPINESS_BUFF));
        tp.addToGrid(0, 1, "Social Cohesion", String.format("%+.2f", COHESION_BUFF));
        tp.addToGrid(0, 2, "Class Consciousness", String.format("%.3f", CLASS_DEBUFF));
        tp.addToGrid(0, 3, "Local Production", Strings.X +
            String.format("%.1f", PRODUCTION_DEBUFF), negative
        );

        tp.addGrid(0);
    }
}
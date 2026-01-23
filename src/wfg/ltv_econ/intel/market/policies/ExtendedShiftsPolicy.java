package wfg.ltv_econ.intel.market.policies;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;

import static wfg.native_ui.util.UIConstants.*;

public class ExtendedShiftsPolicy extends MarketPolicy {

    public static final int PRODUCTION_BUFF = 15; // 15%
    public static final float HAPPINESS_DEBUFF = -0.1f;
    public static final float HEALTH_DEBUFF = -0.07f;
    public static final float CLASS_BUFF = 0.01f;

    public void apply(PlayerMarketData data) {
        for (CommodityDomain dom : EconomyEngine.getInstance().getComDomains()) {
            for (Industry ind : dom.getCell(data.marketID).getVisibleIndustries()) {
                ind.getSupplyBonus().modifyPercent(
                    id, PRODUCTION_BUFF, spec.name
                );
            }
        }
        
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        for (CommodityDomain dom : EconomyEngine.getInstance().getComDomains()) {
            for (Industry ind : dom.getCell(data.marketID).getVisibleIndustries()) {
                ind.getSupplyBonus().unmodifyPercent(id);
            }
        }
        
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Production", "+" + PRODUCTION_BUFF + "%");
        tp.addToGrid(0, 1, "Health", String.format("%+.2f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 2, "Happiness", String.format("%+.1f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 3, "Class Consciousness", String.format("%+.2f", CLASS_BUFF), negative);

        tp.addGrid(0);
    }
}
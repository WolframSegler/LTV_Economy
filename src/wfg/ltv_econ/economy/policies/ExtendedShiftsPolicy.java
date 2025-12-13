package wfg.ltv_econ.economy.policies;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.CommodityInfo;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;
import static wfg.wrap_ui.util.UIConstants.pad;

public class ExtendedShiftsPolicy extends MarketPolicy {

    private static final int PRODUCTION_BUFF = 15; // 15%
    private static final float HAPPINESS_DEBUFF = -0.1f;
    private static final float HEALTH_DEBUFF = -0.07f;
    private static final float CLASS_BUFF = 0.01f;

    public void apply(PlayerMarketData data) {
        for (CommodityInfo info : EconomyEngine.getInstance().getCommodityInfos()) {
            info.getStats(data.marketID).getProductionStat().modifyPercent(
                id, PRODUCTION_BUFF, spec.name
            );
        }
        
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        for (CommodityInfo info : EconomyEngine.getInstance().getCommodityInfos()) {
            info.getStats(data.marketID).getProductionStat().unmodifyPercent(id);
        }
        
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    public void postAdvance(PlayerMarketData data) {}
    public void preAdvance(PlayerMarketData data) {}

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.addPara(
            "Management mandates longer work shifts to maximize output. Colonists labor harder, "+
            "producing more for the market, but the added strain leaves them fatigued and less content. "+
            "Temporary gains in production come at the cost of worker morale and health.", 3
        );

        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Production", "+" + PRODUCTION_BUFF + "%");
        tp.addToGrid(0, 1, "Happiness", String.format("%+.1f", HAPPINESS_DEBUFF));
        tp.addToGrid(0, 2, "Health", String.format("%+.2f", HEALTH_DEBUFF));
        tp.addToGrid(0, 3, "Class Consciousness", String.format("%+.2f", CLASS_BUFF));

        tp.addGrid(0);
    }
}
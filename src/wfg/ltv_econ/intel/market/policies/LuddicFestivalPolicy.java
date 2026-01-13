package wfg.ltv_econ.intel.market.policies;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

import static wfg.wrap_ui.util.UIConstants.*;

public class LuddicFestivalPolicy extends MarketPolicy {
    public static final float HAPPINESS_BUFF = 0.8f;
    public static final float COHESION_BUFF = 0.5f;
    public static final float CLASS_BUFF = 0.005f;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);

        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Happiness", String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 1, "Social Cohesion", String.format("%+.1f", COHESION_BUFF));
        tp.addToGrid(0, 2, "Class Consciousness", String.format("%+.3f", CLASS_BUFF), negative);

        tp.addGrid(0);
    }
}
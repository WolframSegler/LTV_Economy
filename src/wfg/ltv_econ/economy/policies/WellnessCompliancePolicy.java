package wfg.ltv_econ.economy.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class WellnessCompliancePolicy extends MarketPolicy {
    public static final float HEALTH_BUFF = 0.07f;
    public static final float HAPPINESS_DEBUFF = -0.1f;
    public static final float COHESION_DEBUFF = -0.09f;
    public static final float CLASS_DEBUFF = -0.008f;

    public void apply(PlayerMarketData data) {
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.healthDelta.unmodifyFlat(id);
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
        tp.addToGrid(0, 0, "Health", String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 1, "Happiness", String.format("%+.2f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 2, "Social Cohesion", String.format("%+.2f", COHESION_DEBUFF), negative);
        tp.addToGrid(0, 3, "Class Consciousness", String.format("%+.3f", CLASS_DEBUFF));

        tp.addGrid(0);
    }
}
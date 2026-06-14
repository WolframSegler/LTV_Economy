package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;

public class WellnessCompliancePolicy extends MarketPolicy {
    private static final String memKey = "$policy_wellness_compliance";
    private static final float HEALTH_BUFF = 0.06f;
    private static final float HAPPINESS_DEBUFF = -0.1f;
    private static final float COHESION_DEBUFF = -0.09f;
    private static final float CLASS_DEBUFF = -0.008f;

    public void apply(MarketPopulationData data) {
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);

        data.market.getMemoryWithoutUpdate().set(memKey, true);
    }

    public void unapply(MarketPopulationData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        data.market.getMemoryWithoutUpdate().set(memKey, false);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPopDataHealthTxt"), String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 1, str("marketPopDataHappinessTxt"), String.format("%+.1f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 2, str("marketPopDataCohesionTxt"), String.format("%+.2f", COHESION_DEBUFF), negative);
        tp.addToGrid(0, 3, str("marketPopDataConsciousness"), String.format("%+.3f", CLASS_DEBUFF));

        tp.addGrid(0);
    }
}
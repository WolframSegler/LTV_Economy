package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;

public class BresVitalisPolicy extends MarketPolicy {
    private static final String memKey = "$policy_bres_vitalis";
    public static final float HEALTH_DEBUFF = -0.05f;
    public static final float HAPPINESS_BUFF = 0.1f;
    public static final float CLASS_BUFF = 0.001f;

    public void apply(MarketPopulationData data) {
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);

        data.market.getMemoryWithoutUpdate().set(memKey, true);
    }

    public void unapply(MarketPopulationData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        data.market.getMemoryWithoutUpdate().set(memKey, false);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPopDataHealthTxt"), String.format("%+.2f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 1, str("marketPopDataHappinessTxt"), String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 2, str("marketPopDataConsciousnessTxt"), String.format("%+.3f", CLASS_BUFF));

        tp.addGrid(0);
    }
}
package wfg.ltv_econ.intel.market.policies;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class BresVitalisPolicy extends MarketPolicy {
    public static final float HEALTH_DEBUFF = -0.05f;
    public static final float HAPPINESS_BUFF = 0.1f;
    public static final float CLASS_BUFF = 0.001f;

    public void apply(PlayerMarketData data) {
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Health", String.format("%+.2f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 1, "Happiness", String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 2, "Class Consciousness", String.format("%+.3f", CLASS_BUFF));

        tp.addGrid(0);
    }
}
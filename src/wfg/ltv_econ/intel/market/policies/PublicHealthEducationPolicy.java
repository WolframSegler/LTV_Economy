package wfg.ltv_econ.intel.market.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class PublicHealthEducationPolicy extends MarketPolicy {
    public static final float HEALTH_BUFF = 0.15f;
    public static final float HAPPINESS_DEBUFF = -0.01f;
    public static final float COHESION_BUFF = 0.02f;

    public void apply(PlayerMarketData data) {
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Health", String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 1, "Happiness", String.format("%+.2f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 2, "Social Cohesion", String.format("%+.2f", COHESION_BUFF));

        tp.addGrid(0);
    }
}
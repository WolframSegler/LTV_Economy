package wfg.ltv_econ.intel.market.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class IronFistPolicy extends MarketPolicy {
    public static final float HAPPINESS_DEBUFF = -0.3f;
    public static final float COHESION_BUFF = 0.5f;
    public static final float CLASS_BUFF = 0.09f;

    public static final int STABILITY_BUFF = 3;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);

        data.market.getStability().modifyFlat(id, STABILITY_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        data.market.getStability().unmodifyFlat(id);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return data.getSocialCohesion() < 30f;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        tp.addPara("Order is temporarily restored. The populace marches in line… for now.", pad);

        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Happiness", String.format("%+.1f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 1, "Social Cohesion", String.format("%+.1f", COHESION_BUFF));
        tp.addToGrid(0, 2, "Stability Bonus", "+"+STABILITY_BUFF);
        
        // This is a surprise for later
        // tp.addToGrid(0, 2, "Class Consciousness", String.format("%+.2f", CLASS_BUFF), negative);

        tp.addGrid(0);
    }
}
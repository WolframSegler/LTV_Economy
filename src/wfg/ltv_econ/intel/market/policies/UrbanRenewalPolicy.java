package wfg.ltv_econ.intel.market.policies;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;

public class UrbanRenewalPolicy extends MarketPolicy {
    public static final float HEALTH_BUFF = 0.4f;
    public static final float HAPPINESS_BUFF = 0.15f;
    public static final float CLASS_DEBUFF = -0.002f;

    public static final int POP_GROWTH_BUFF = 10;

    public void apply(PlayerMarketData data) {
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);

        data.market.getPopulation().getWeight().modifyFlat(id, POP_GROWTH_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        data.market.getPopulation().getWeight().unmodifyFlat(id);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return data.market.getSize() > 3;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Population Growth", "+"+POP_GROWTH_BUFF);
        tp.addToGrid(0, 1, "Health", String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 2, "Happiness", String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 3, "Class Consciousness", String.format("%+.3f", CLASS_DEBUFF));

        tp.addGrid(0);
    }
}
package wfg.ltv_econ.intel.market.events;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import wfg.ltv_econ.economy.MarketPopulationData;

public class MiasmaInTheStreetsEvent extends MarketEvent {
    private static final int BASE_DURATION = 40;
    private static final int BASE_COOLDOWN = 360;
    private static final float HEALTH_DEBUFF = -0.25f;
    private static final float HAPPINESS_DEBUFF = -0.1f;

    private int activeDaysRemaining = 0;
    private int cooldownDaysRemaining = 0;

    @Override
    public void preAdvance(MarketPopulationData data) {
        if (active || cooldownDaysRemaining > 0) return;

        final float dailyChance = 0.002f;
        if (Math.random() >= dailyChance) return;

        active = true;
        activeDaysRemaining = BASE_DURATION;
        cooldownDaysRemaining = BASE_COOLDOWN + BASE_DURATION;

        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
    }

    @Override
    public void postAdvance(MarketPopulationData data) {
        if (active) {
            activeDaysRemaining--;
            if (activeDaysRemaining <= 0) deactivate(data);
        } else if (cooldownDaysRemaining > 0) {
            cooldownDaysRemaining--;
        }
    }

    private final void deactivate(MarketPopulationData data) {
        if (!active) return;

        active = false;
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(MarketPopulationData data, com.fs.starfarer.api.ui.TooltipMakerAPI tp) {
        tp.setTitleSmallOrbitron();
        tp.addTitle(spec.name, negative);

        tp.addPara(spec.description, pad);

        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), opad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPopDataHealthTxt"), String.format("%.2f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 1, str("marketPopDataHappinessTxt"), String.format("%.1f", HAPPINESS_DEBUFF), negative);
        tp.addGrid(0);

        tp.addPara(str("marketEventActiveForTxt"), opad, negative, Integer.toString(activeDaysRemaining));
    }
}
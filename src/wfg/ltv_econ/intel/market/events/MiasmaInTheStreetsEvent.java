package wfg.ltv_econ.intel.market.events;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import wfg.ltv_econ.economy.PlayerMarketData;

public class MiasmaInTheStreetsEvent extends MarketEvent {
    public static final int BASE_DURATION = 40;
    public static final int BASE_COOLDOWN = 360;
    public static final float HEALTH_DEBUFF = -0.25f;
    public static final float HAPPINESS_DEBUFF = -0.1f;

    private int activeDaysRemaining = 0;
    private int cooldownDaysRemaining = 0;

    @Override
    public void preAdvance(PlayerMarketData data) {
        if (active || cooldownDaysRemaining > 0) return;

        final float dailyChance = 0.003f;
        if (Math.random() >= dailyChance) return;

        active = true;
        activeDaysRemaining = BASE_DURATION;
        cooldownDaysRemaining = BASE_COOLDOWN + BASE_DURATION;

        data.happinessDelta.modifyFlat(spec.id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(spec.id, HEALTH_DEBUFF, spec.name);
    }

    @Override
    public void postAdvance(PlayerMarketData data) {
        if (active) {
            activeDaysRemaining--;
            if (activeDaysRemaining <= 0) deactivate(data);
        } else if (cooldownDaysRemaining > 0) {
            cooldownDaysRemaining--;
        }
    }

    private final void deactivate(PlayerMarketData data) {
        if (!active) return;

        active = false;
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
    }

    @Override
    public void createTooltip(PlayerMarketData data, com.fs.starfarer.api.ui.TooltipMakerAPI tp) {
        tp.setParaFontOrbitron();
        tp.setParaFontColor(negative);
        tp.addPara(spec.name, pad);

        tp.setParaFontDefault();
        tp.setParaFontColor(Color.WHITE);
        tp.addPara(spec.description, pad);

        final int cols = 2;
        tp.addPara("Daily effects", opad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Health", String.format("%.2f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 1, "Happiness", String.format("%.1f", HAPPINESS_DEBUFF), negative);
        tp.addGrid(0);

        tp.addPara("Active for %s more days", opad, negative, Integer.toString(activeDaysRemaining));
    }
}
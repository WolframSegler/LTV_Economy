package wfg.ltv_econ.intel.market.events;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.intel.market.events.LocalizedGangSkirmishesEvent;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.List;

public class LocalizedGangSkirmishesEvent extends MarketEvent implements MarketImmigrationModifier {
    public static final int BASE_DUR = 7;
    public static final int BASE_COOLDOWN = 30;
    public static final int DISRUPTION_DUR = 21;
    public static final int GROWTH_DEBUFF = -10;
    public static final float HAPPINESS_DEBUFF = -0.3f;

    private int cooldownDaysRemaining = 0;
    private int activeDaysRemaining = 0;

    @Override
    public void preAdvance(PlayerMarketData data) {
        if (active || cooldownDaysRemaining > 0) return;

        final float cohesion = data.getSocialCohesion();
        if (cohesion >= 25f) return;

        final float factor = (25f - cohesion) / 25f;
        final float maxDailyChance = 0.0334f; // ~3.3% per day at cohesion = 0
        if (Math.random() >= factor * maxDailyChance) return;

        active = true;

        final float durationMult = 0.7f + (25f - clamp(cohesion, 0f, 25f)) / 25f * 0.6f;
        activeDaysRemaining = Math.round(BASE_DUR * durationMult); // base 7 days
        cooldownDaysRemaining = BASE_COOLDOWN + BASE_DUR;

        data.market.addTransientImmigrationModifier(this);

        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);

        if (Math.random() < 0.5) {
            final List<Industry> candidates = WorkerRegistry.getVisibleIndustries(data.market).stream()
                .filter(ind -> ind.canBeDisrupted() && !ind.isDisrupted())
                .toList();
            if (!candidates.isEmpty()) {
                final Industry target = candidates.get((int) (Math.random() * candidates.size()));
                target.setDisrupted(DISRUPTION_DUR);
            }
        }
    }

    @Override
    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.getWeight().modifyFlat(id, GROWTH_DEBUFF, spec.name);
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

        data.market.removeTransientImmigrationModifier(this);
        data.happinessDelta.unmodifyFlat(id);

        active = false;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.setParaFontOrbitron();
        tp.setParaFontColor(negative);
        tp.addPara(spec.name, pad);

        tp.setParaFontDefault();
        tp.setParaFontColor(Color.WHITE);
        tp.addPara(spec.description, pad);

        final int cols = 2;
        tp.addPara("Daily effects", opad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Happiness", String.format("%.1f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 1, "Pop. growth", String.format("%d", GROWTH_DEBUFF), negative);
        tp.addGrid(0);

        tp.addPara("Active for %s more days", opad, negative, Integer.toString(activeDaysRemaining));
    }

    private static final float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
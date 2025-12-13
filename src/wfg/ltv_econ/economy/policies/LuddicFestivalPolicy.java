package wfg.ltv_econ.economy.policies;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import static wfg.wrap_ui.util.UIConstants.pad;

public class LuddicFestivalPolicy extends MarketPolicy {
    private static final float HAPPINESS_BUFF = 0.8f;
    private static final float COHESION_BUFF = 0.5f;
    private static final float CLASS_BUFF = 0.005f;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.culturalCohesionDelta.modifyFlat(id, COHESION_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.culturalCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    public void postAdvance(PlayerMarketData data) {}
    public void preAdvance(PlayerMarketData data) {}

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.addPara(
            "Colonists gather to honor Prophet Ludd's teachings and the founding of Gilead. "+
            "For several days, work slows as the faithful celebrate, meditate, and reject "+
            "the trappings of machinery. The air hums with hymns and ritual, reminding all "+
            "of the strength of the community over industry.", 3
        );

        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Happiness", String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 1, "Cultural Cohesion", String.format("%+.1f", COHESION_BUFF));
        tp.addToGrid(0, 2, "Class Consciousness", String.format("%+.3f", CLASS_BUFF));

        tp.addGrid(0);
    }
}
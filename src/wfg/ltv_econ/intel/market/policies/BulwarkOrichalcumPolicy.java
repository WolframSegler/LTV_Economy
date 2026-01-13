package wfg.ltv_econ.intel.market.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class BulwarkOrichalcumPolicy extends MarketPolicy {
    public static final float HAPPINESS_BUFF = 0.05f;

    public static final int MARINES_COST = 20;
    public static final int HAND_WEAPONS_COST = 6;

    public static final float DEFENSE_MULT_BUFF = 2.5f;
    public static final int DEFENSE_FLAT_BUFF = 300;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);

        final EconomyEngine engine = EconomyEngine.getInstance();
        engine.getComCell(Commodities.SUPPLIES, data.marketID)
            .getDemandStat().modifyFlat(id, MARINES_COST, spec.name);
        engine.getComCell(Commodities.SHIPS, data.marketID)
            .getDemandStat().modifyFlat(id, HAND_WEAPONS_COST, spec.name);

        data.market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(
            id, DEFENSE_FLAT_BUFF, spec.name);
        data.market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
            id, DEFENSE_MULT_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);

        final EconomyEngine engine = EconomyEngine.getInstance();
        engine.getComCell(Commodities.SUPPLIES, data.marketID)
            .getDemandStat().unmodifyFlat(id);
        engine.getComCell(Commodities.SHIPS, data.marketID)
            .getDemandStat().unmodifyFlat(id);

        data.market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(id);
        data.market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return data.market.getIndustry(Industries.HEAVYBATTERIES) != null;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Marines Cost", Integer.toString(MARINES_COST), negative);
        tp.addToGrid(0, 1, "Heavy Armaments Cost", Integer.toString(HAND_WEAPONS_COST), negative);
        tp.addToGrid(0, 2, "Happiness", String.format("%+.2f", HAPPINESS_BUFF));
        tp.addToGrid(0, 3, "Defense Bonus", "+"+DEFENSE_FLAT_BUFF);
        tp.addToGrid(0, 4, "Defense Bonus", Strings.X + String.format("%.1f", DEFENSE_MULT_BUFF));

        tp.addGrid(0);
    }
}
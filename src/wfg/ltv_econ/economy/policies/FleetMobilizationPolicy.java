package wfg.ltv_econ.economy.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;

public class FleetMobilizationPolicy extends MarketPolicy {
    public static final float HAPPINESS_DEBUFF = -0.08f;

    public static final int SUPPLIES_COST = 30;
    public static final int SHIPS_COST = 25;

    public static final int LIGHT_FLEETS = 6;
    public static final int MEDIUM_FLEETS = 4;
    public static final int HEAVY_FLEETS = 2;

    public void apply(PlayerMarketData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);

        final EconomyEngine engine = EconomyEngine.getInstance();
        engine.getComCell(Commodities.SUPPLIES, data.marketID)
            .getDemandStat().modifyFlat(id, SUPPLIES_COST, spec.name);
        engine.getComCell(Commodities.SHIPS, data.marketID)
            .getDemandStat().modifyFlat(id, SHIPS_COST, spec.name);

        final MarketAPI market = data.market;
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD)
            .modifyFlat(id, LIGHT_FLEETS, spec.name);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD)
            .modifyFlat(id, MEDIUM_FLEETS, spec.name);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD)
            .modifyFlat(id, HEAVY_FLEETS, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        data.happinessDelta.unmodifyFlat(id);

        final EconomyEngine engine = EconomyEngine.getInstance();
        engine.getComCell(Commodities.SUPPLIES, data.marketID)
            .getDemandStat().unmodifyFlat(id);
        engine.getComCell(Commodities.SHIPS, data.marketID)
            .getDemandStat().unmodifyFlat(id);

        final MarketAPI market = data.market;
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).unmodify(id);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).unmodify(id);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return data.market.getIndustry(Industries.MILITARYBASE) != null ||
            data.market.getIndustry(Industries.HIGHCOMMAND) != null;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Supplies Cost", Integer.toString(SUPPLIES_COST), negative);
        tp.addToGrid(0, 1, "Ships Cost", Integer.toString(SHIPS_COST), negative);
        tp.addToGrid(0, 2, "Happiness", String.format("%+.2f", HAPPINESS_DEBUFF), negative);

        tp.addGrid(0);
    }
}
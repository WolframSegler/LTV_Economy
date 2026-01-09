package wfg.ltv_econ.economy.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.wrap_ui.util.NumFormat;

public class OrganHarvestingPolicy extends MarketPolicy implements MarketImmigrationModifier {
    public static final float HEALTH_BUFF = 0.03f;
    public static final float HAPPINESS_DEBUFF = -0.03f;
    public static final float CLASS_BUFF = 0.002f;

    public static final int POP_GROWTH_DEBUFF = -15;
    public static final int HARVESTED_ORGANS_BUFF = 10;
    public static final int ASSET_SEIZURE_CREDITS_BUFF = 100;

    public void apply(PlayerMarketData data) {     
        final MarketAPI market = data.market;   
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);

        market.addTransientImmigrationModifier(this);
        EconomyEngine.getInstance().getComCell(Commodities.ORGANS, data.marketID)
            .getProductionStat().modifyFlat(id, HARVESTED_ORGANS_BUFF, spec.name);
    }

    public void unapply(PlayerMarketData data) {
        final MarketAPI market = data.market;   
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        market.removeTransientImmigrationModifier(this);
        EconomyEngine.getInstance().getComCell(Commodities.ORGANS, data.marketID)
            .getProductionStat().unmodifyFlat(id);
    }

    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		incoming.add(Factions.POOR, POP_GROWTH_DEBUFF);
		incoming.getWeight().modifyFlat(id, POP_GROWTH_DEBUFF, spec.name);
	}

    public void postAdvance(PlayerMarketData data) {
        EconomyEngine.getInstance().addCredits(data.marketID, ASSET_SEIZURE_CREDITS_BUFF);
    }

    @Override
    public boolean isEnabled(PlayerMarketData data) {
        return data.market.getSize() > 4;
    }

    @Override
    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara("Daily effects", pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, "Population Growth", ""+POP_GROWTH_DEBUFF, negative);
        tp.addToGrid(0, 1, "Harvested Organs", "+"+HARVESTED_ORGANS_BUFF);
        tp.addToGrid(0, 2, "Asset Liquidation", NumFormat.formatCredit(ASSET_SEIZURE_CREDITS_BUFF));
        tp.addToGrid(0, 3, "Health", String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 4, "Happiness", String.format("%+.2f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 5, "Class Consciousness", String.format("%+.3f", CLASS_BUFF), negative);

        tp.addGrid(0);
    }
}
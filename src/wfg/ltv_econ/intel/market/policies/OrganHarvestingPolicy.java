package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.native_ui.util.NumFormat;

public class OrganHarvestingPolicy extends MarketPolicy {
    public static final float HEALTH_BUFF = 0.03f;
    public static final float HAPPINESS_DEBUFF = -0.03f;
    public static final float CLASS_BUFF = 0.003f;

    public static final int POP_GROWTH_DEBUFF = -15;
    public static final int HARVESTED_ORGANS_BUFF = 10;
    public static final int ASSET_SEIZURE_CREDITS_BUFF = 100;

    public void apply(MarketPopulationData data) {     
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_BUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF, spec.name);
        
		data.market.addTransientImmigrationModifier(new OrganHarvestingModifier());
        EconomyEngine.instance().getComCell(Commodities.ORGANS, data.marketID)
            .getProductionStat().modifyFlat(id, HARVESTED_ORGANS_BUFF, spec.name);
    }

    public void unapply(MarketPopulationData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.healthDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id) ;

        for (MarketImmigrationModifier immigMod : data.market.getTransientImmigrationModifiers()) {
            if (immigMod instanceof OrganHarvestingModifier organMod) {
                data.market.removeTransientImmigrationModifier(organMod);
                break;
            }
        }
        EconomyEngine.instance().getComCell(Commodities.ORGANS, data.marketID)
            .getProductionStat().unmodifyFlat(id);
    }

    public void postAdvance(MarketPopulationData data) {
        MarketFinanceRegistry.instance().getLedger(data.marketID).add(id, ASSET_SEIZURE_CREDITS_BUFF, spec.name);
    }

    @Override
    public boolean isEnabled(MarketPopulationData data) {
        return data.market.getSize() > 4;
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketEventPopGrowthTxt"), ""+POP_GROWTH_DEBUFF, negative);
        tp.addToGrid(0, 1, str("marketPolicyHarvestedOrgansTxt"), "+"+HARVESTED_ORGANS_BUFF);
        tp.addToGrid(0, 2, str("marketPolicyAssetLiquidated"), NumFormat.formatCredit(ASSET_SEIZURE_CREDITS_BUFF));
        tp.addToGrid(0, 3, str("marketPopDataHealthTxt"), String.format("%+.2f", HEALTH_BUFF));
        tp.addToGrid(0, 4, str("marketPopDataHappinessTxt"), String.format("%+.2f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 5, str("marketPopDataConsciousnessTxt"), String.format("%+.3f", CLASS_BUFF), negative);

        tp.addGrid(0);
    }

    private class OrganHarvestingModifier implements MarketImmigrationModifier {
        public final void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
            incoming.add(Factions.POOR, 0f);
		
            incoming.getWeight().modifyFlat(id, POP_GROWTH_DEBUFF, spec.name);
        }
    }
}
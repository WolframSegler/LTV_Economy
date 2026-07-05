package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;

public class ArrestStrikeOrganizersPolicy extends MarketPolicy {
    public static final float HAPPINESS_DEBUFF = -2f;
    public static final float COHESION_DEBUFF = -0.5f;
    public static final float CLASS_DEBUFF = -1.5f;

    private static final int HEAVY_WEAPONS_COST = 40;
    private static final int MARINES_COST = 400;
    private static final int SUPPLIES_COST = 100;

    @Override
    public void apply(MarketPopulationData data) {
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);

        ExpandShipyardsPolicy.removeStored(data.market, Commodities.HAND_WEAPONS, HEAVY_WEAPONS_COST);
        ExpandShipyardsPolicy.removeStored(data.market, Commodities.SUPPLIES, SUPPLIES_COST);
    }

    @Override
    public void unapply(MarketPopulationData data) {
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
    }

    @Override
    public boolean isEnabled(MarketPopulationData data) {
        return data.getClassConsciousness() > 70f;
    }

    @Override
    public boolean isAvailable(MarketPopulationData data) {
        if (ExpandShipyardsPolicy.getStored(data.market, Commodities.HAND_WEAPONS) < HEAVY_WEAPONS_COST) return false;
        if (ExpandShipyardsPolicy.getStored(data.market, Commodities.MARINES) < MARINES_COST) return false;
        if (ExpandShipyardsPolicy.getStored(data.market, Commodities.SUPPLIES) < SUPPLIES_COST) return false;

        return super.isAvailable(data);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);

        final Color heavyWeaponsColor = ExpandShipyardsPolicy.getStored(data.market, Commodities.HAND_WEAPONS) >= HEAVY_WEAPONS_COST ? highlight : negative;
        final Color marinesColor = ExpandShipyardsPolicy.getStored(data.market, Commodities.MARINES) >= MARINES_COST ? highlight : negative;
        final Color suppliesColor = ExpandShipyardsPolicy.getStored(data.market, Commodities.SUPPLIES) >= SUPPLIES_COST ? highlight : negative;

        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPopDataHappinessTxt"), String.format("%.1f", HAPPINESS_DEBUFF), negative);
        tp.addToGrid(0, 1, str("marketPopDataCohesionTxt"), String.format("%.1f", COHESION_DEBUFF), negative);
        tp.addToGrid(0, 2, str("marketPopDataConsciousnessTxt"), String.format("%.1f", CLASS_DEBUFF));
        tp.addGrid(0);

        tp.addPara(str("marketPolicyRequiredResourcesTxt"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, settings.getCommoditySpec(Commodities.HAND_WEAPONS).getName(), Integer.toString(HEAVY_WEAPONS_COST), heavyWeaponsColor);
        tp.addToGrid(0, 1, settings.getCommoditySpec(Commodities.MARINES).getName(), Integer.toString(MARINES_COST), marinesColor);
        tp.addToGrid(0, 2, settings.getCommoditySpec(Commodities.SUPPLIES).getName(), Integer.toString(SUPPLIES_COST), suppliesColor);
        tp.addGrid(0);
    }
}
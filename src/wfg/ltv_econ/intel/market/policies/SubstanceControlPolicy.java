package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class SubstanceControlPolicy extends MarketPolicy {
    public static final float DRUGS_MULT_P1 = 0.6f;
    public static final float HEALTH_BUFF_P1 = 0.3f;
    public static final float HAPPINESS_DEBUFF_P1 = -0.04f;
    public static final float COHESION_BUFF_P1 = 0.008f;

    public static final float DRUGS_MULT_P2 = 0.8f;
    public static final float HEALTH_BUFF_P2 = 0.1f;
    public static final float HAPPINESS_DEBUFF_P2 = -0.05f;
    public static final float COHESION_DEBUFF_P2 = -0.01f;
    public static final float CLASS_BUFF_P2 = 0.004f;

    private boolean phase2 = false;
    private CommodityCell cell;

    public void apply(MarketPopulationData data) {
        cell = EconomyEngine.instance().getComCell(Commodities.DRUGS, data.marketID);
        cell.getConsumptionStat().modifyMult(id, DRUGS_MULT_P1, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_BUFF_P1, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF_P1, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_BUFF_P1, spec.name);
    }

    @Override
    public void preAdvance(MarketPopulationData data) {
        if (activeDaysRemaining > 180 || phase2) return;

        cell.getConsumptionStat().modifyMult(id, DRUGS_MULT_P2, spec.name);
        data.healthDelta.modifyFlat(id, HEALTH_BUFF_P2, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_DEBUFF_P2, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_DEBUFF_P2, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_BUFF_P2, spec.name);
        phase2 = true;
    }

    public void unapply(MarketPopulationData data) {
        cell.getConsumptionStat().unmodifyMult(id);
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);
        phase2 = false;
    }

    @Override
    public boolean isEnabled(MarketPopulationData data) {
        return !data.market.isFreePort() || isActive(data);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPolicyDrugsDemandTxt"), Strings.X + String.format("%.1f", DRUGS_MULT_P1));
        tp.addToGrid(0, 1, str("marketPopDataHealthTxt"), String.format("%+.1f", HEALTH_BUFF_P1));
        tp.addToGrid(0, 2, str("marketPopDataHappinessTxt"), String.format("%+.2f", HAPPINESS_DEBUFF_P1), negative);
        tp.addToGrid(0, 3, str("marketPopDataCohesionTxt"), String.format("%+.3f", COHESION_BUFF_P1), negative);

        tp.addGrid(0);

        tp.setParaFontColor(gray);
        tp.addPara(str("effectsMayChangeOverTimeTxt"), pad);
    }
}
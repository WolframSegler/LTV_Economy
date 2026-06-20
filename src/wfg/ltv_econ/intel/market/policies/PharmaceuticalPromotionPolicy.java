package wfg.ltv_econ.intel.market.policies;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.economy.MarketPopulationData;

public class PharmaceuticalPromotionPolicy extends MarketPolicy {
    public static final float HEALTH_DEBUFF = -0.4f;
    public static final float HAPPINESS_BUFF = 0.5f;
    public static final float COHESION_DEBUFF = -0.03f;
    public static final float CLASS_DEBUFF = -0.001f;
    public static final float FACTION_RELATION_DROP = 0.1f;
    public static final float FACTION_RELATION_INCREASE = 0.07f;

    public void apply(MarketPopulationData data) {
        data.healthDelta.modifyFlat(id, HEALTH_DEBUFF, spec.name);
        data.happinessDelta.modifyFlat(id, HAPPINESS_BUFF, spec.name);
        data.socialCohesionDelta.modifyFlat(id, COHESION_DEBUFF, spec.name);
        data.classConsciousnessDelta.modifyFlat(id, CLASS_DEBUFF, spec.name);
    }

    public void unapply(MarketPopulationData data) {
        data.healthDelta.unmodifyFlat(id);
        data.happinessDelta.unmodifyFlat(id);
        data.socialCohesionDelta.unmodifyFlat(id);
        data.classConsciousnessDelta.unmodifyFlat(id);

        final SectorAPI sector = Global.getSector();
        final var factions = sector.getAllFactions();
        factions.removeIf(f -> !f.getFactionSpec().isShowInIntelTab());
        for (FactionAPI faction : factions) {
            final boolean drugsIllegal = faction.isIllegal(Commodities.DRUGS);
            faction.adjustRelationship(Factions.PLAYER, drugsIllegal ?
                -FACTION_RELATION_DROP : FACTION_RELATION_INCREASE
            );
        }
        String relationChanges = str("marketPolicyRelationChangesTxt");
        Global.getSoundPlayer().playUISound("ui_rep_drop", 1f, 1f);
        for (FactionAPI faction : factions) {
            final boolean drugsIllegal = faction.isIllegal(Commodities.DRUGS);
            final String changeVerb = drugsIllegal ? str("decreasedByTxt") : str("increasedByTxt");
            relationChanges += "\n" + str("relationWithTxt") + faction.getDisplayName() + " " +
                changeVerb + " " + (drugsIllegal ? "-"+FACTION_RELATION_DROP : ""+FACTION_RELATION_INCREASE);
        }
        sector.getCampaignUI().addMessage(relationChanges);
    }

    @Override
    public void createTooltip(MarketPopulationData data, TooltipMakerAPI tp) {
        super.createTooltip(data, tp);
        
        final int cols = 2;
        tp.addPara(str("marketEventDailyEffects"), pad);
        tp.beginGridFlipped(250f, cols, 60f, pad);
        tp.addToGrid(0, 0, str("marketPopDataHealthTxt"), String.format("%+.1f", HEALTH_DEBUFF), negative);
        tp.addToGrid(0, 1, str("marketPopDataHappinessTxt"), String.format("%+.1f", HAPPINESS_BUFF));
        tp.addToGrid(0, 2, str("marketPopDataCohesionTxt"), String.format("%+.2f", COHESION_DEBUFF), negative);
        tp.addToGrid(0, 3, str("marketPopDataConsciousness"), String.format("%+.3f", CLASS_DEBUFF));

        tp.addGrid(0);
    }
}
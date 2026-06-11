package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.util.Misc.Token;

public class AdjustRepNPC extends BaseCommandPlugin {

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        final int size = params.size();
        if (size < 2) throw new IllegalArgumentException("Parameters must contain memId and personId");

        final PersonAPI person = CreateRandomNPC.getPerson(memoryMap, params.get(0), params.get(1));
        final CustomRepImpact impact = new CustomRepImpact();
        if (size > 3) {
            impact.delta = params.get(2).getFloat(memoryMap) / 100f;
            impact.limit = RepLevel.valueOf(params.get(3).getString(memoryMap));
        } else if (size == 3) {
            impact.delta = params.get(2).getFloat(memoryMap) / 100f;
        } else {
            throw new IllegalStateException("params must be of form { memId, personId, impact, repLimit } or { memId, personId, impact }");
        }
		
        final ReputationAdjustmentResult result = Global.getSector().adjustPlayerReputation(
                new RepActionEnvelope(RepActions.CUSTOM, impact, null, dialog.getTextPanel(), true), person);
        return result.delta != 0;
	}
}
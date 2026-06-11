package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.Misc.Token;

public class DisplayNPC extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        final int size = params.size();
        if (size < 2) throw new IllegalArgumentException("Parameters must contain memId and personId");

        final boolean isMinimal = size > 2 ? params.get(2).getBoolean(memoryMap) : false;

        final PersonAPI person = CreateRandomNPC.getPerson(memoryMap, params.get(0), params.get(1));
        dialog.getVisualPanel().showPersonInfo(person, isMinimal);
	
		return true;
	}
}
package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.util.Misc.Token;

public class CreateRandomNPC extends BaseCommandPlugin {
    
    public final boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        final int size = params.size();
        if (size < 3) throw new IllegalArgumentException("Parameters must contain factionId, memId and personId");

        final Token factionToken = params.get(0);
        final Token memToken = params.get(1);
        final Token personToken = params.get(2);

        final String factionId = factionToken.isLiteral() ? factionToken.string : factionToken.getString(memoryMap);
        final String memId = memToken.isLiteral() ? memToken.string : memToken.getString(memoryMap);
        final String genderId = size > 3 ? params.get(3).getString(memoryMap) : null;
        final String rankId = size > 4 ? params.get(4).getString(memoryMap) : null;
        final String postId = size > 5 ? params.get(5).getString(memoryMap) : null;

        final PersonAPI person = Global.getSector().getFaction(factionId).createRandomPerson();
        if (genderId != null) person.setGender(Gender.valueOf(genderId.toUpperCase()));
        if (rankId != null) person.setRankId(rankId);
        if (postId != null) person.setPostId(postId);

        memoryMap.get(memId).set(personToken.string, person);
        return true;
    }

    public static final PersonAPI getPerson(Map<String, MemoryAPI> memoryMap, Token memToken, Token personToken) {
        final String memId = memToken.isLiteral() ? memToken.string : memToken.getString(memoryMap);

        return(PersonAPI) memoryMap.get(memId).get(personToken.string);
    }
}
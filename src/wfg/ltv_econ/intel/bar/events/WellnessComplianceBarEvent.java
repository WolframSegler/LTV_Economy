package wfg.ltv_econ.intel.bar.events;

import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;

public class WellnessComplianceBarEvent extends BaseBarEvent {

    private enum OptionID {
        OBSERVE_PATRONS,
        ASK_QUESTIONS,
        LEAVE
    }

    private static final Random rand = new Random();

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        final PlayerMarketData data = EconomyEngine.getInstance().getPlayerMarketData(market.getId());
        if (data == null || !data.getPolicy("wellness_compliance_directive").isActive()) return false;
        return rand.nextFloat() < 0.3f || DebugFlags.BAR_DEBUG;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().addPara("As you enter the bar, the chatter seems unusually tense. Conversations trail off mid-sentence, and people keep glancing at each other with a quiet, uneasy suspicion.");

        dialog.getOptionPanel().addOption("Go to the counter", OptionID.OBSERVE_PATRONS);
        dialog.getOptionPanel().addOption("Leave", OptionID.LEAVE);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionID)) return;

        final OptionID option = (OptionID) optionData;
        final TextPanelAPI text = dialog.getTextPanel();
        final OptionPanelAPI options = dialog.getOptionPanel();

        options.clearOptions();

        switch (option) {
        case OBSERVE_PATRONS:
            text.addPara(
                "As you make your way to the counter, you notice patrons giving each other subtle side-eye, and a few are keeping their distance without any obvious reason. Nobody seems relaxed."
            );
            options.addOption("Ask the bartender what's going on", OptionID.ASK_QUESTIONS);
            options.addOption("Leave quietly", OptionID.LEAVE);
            break;

        case ASK_QUESTIONS:
            text.addPara(
                "He leans in, whispering that certain districts are supposedly higher-risk. He doesn't elaborate, just shrugs and straightens up again. Nobody in the bar looks comfortable hearing it said out loud."
            );
            options.addOption("Leave the bar", OptionID.LEAVE);
            break;

        default: case LEAVE:
            text.addPara("You decide it's best not to get involved and step away, leaving the uneasy patrons to their whispers.");
            BarEventManager.getInstance().notifyWasInteractedWith(this);
            noContinue = true;
            done = true;
        }
    }   

    public static class WellnessComplianceBarEventCreator extends BaseBarEventCreator {
        @Override
        public PortsideBarEvent createBarEvent() {
            return new WellnessComplianceBarEvent();
        }
    }
}
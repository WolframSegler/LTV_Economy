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

public class ConvergenceFestivalBarEvent extends BaseBarEvent {
    private enum OptionID {
        APPROACH_PARTICIPANT,
        INTRO_QUESTION,
        COMPLIMENT,
        COACH,
        FINAL_ADVICE,
        APOLOGIZE,
        LEAVE
    }

    private static final Random rand = new Random();

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        final PlayerMarketData data = EconomyEngine.getInstance().getPlayerMarketData(market.getId());
        if (data == null || !data.getPolicy("convergence_festival").isActive()) return false;

        return rand.nextFloat() < 0.5f || DebugFlags.BAR_DEBUG;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        final TextPanelAPI text = dialog.getTextPanel();
        final OptionPanelAPI options = dialog.getOptionPanel();

        text.addPara("The bar is decorated with colorful banners and holographic displays celebrating The Convergence Festival. Patrons cheer and participate in the festive mood.");

        if (rand.nextFloat() < 0.2f || DebugFlags.BAR_DEBUG) {
            text.addPara("Your eyes lock onto a festival participant among the crowd. He looks tense and exhausted, glancing nervously at the crowd.");
            options.addOption("Approach the participant", OptionID.APPROACH_PARTICIPANT);
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionID)) return;

        final OptionID option = (OptionID) optionData;
        final TextPanelAPI text = dialog.getTextPanel();
        final OptionPanelAPI options = dialog.getOptionPanel();

        options.clearOptions();

        switch (option) {
        case APPROACH_PARTICIPANT:
            text.addPara("You walk over to the participant, who stares silently at his drink, avoiding your gaze.");
            options.addOption("Ask if he is a participant in the festival", OptionID.INTRO_QUESTION);
            break;

        case INTRO_QUESTION:
            text.addPara("You ask if he is a participant in the festival. He doesn't answer, just lets out a quiet grunt and keeps staring at his drink.");
            options.addOption("'With your efforts, the colony is more unified than ever. Your sweat wasn't wasted.'", OptionID.COMPLIMENT);
            options.addOption("'This machine cannot run if a single cog falters. Pull through, and the whole festival stands.'", OptionID.COACH);
            break;

        case COMPLIMENT:
            text.addPara("The participant finally looks at you, eyes filled with silent anger and fear. 'The sweat wasn't from the festival itself,' he says quietly. 'It was from the shock when security knocked down my door to make sure I attended the morning preparations.'");
            options.addOption("'Choosing to join the events is voluntary, but once in, you must fulfill your promise'", OptionID.FINAL_ADVICE);
            options.addOption("Apologize on behalf of the authorities and leave", OptionID.APOLOGIZE);
            break;

        case COACH:
            text.addPara("The participant finally looks at you, eyes filled with silent anger and fear. 'Not actually tired,' he says in a strained, anxious voice. 'The security already helped me get energetic and motivated for the training.'");
            options.addOption("Reming him that choosing to join the events is voluntary, but once, mandatory", OptionID.FINAL_ADVICE);
            options.addOption("Apologize on behalf of the authorities and leave", OptionID.APOLOGIZE);
            break;

        case FINAL_ADVICE:
            text.addPara("You remind him quietly: 'Choosing to join the events is voluntary, but once in, you must fulfill your promise. Otherwise, the whole festival would crumble.'");

            text.addPara("The participant exhales sharply, eyes darting to the holographic banners spiraling above the plaza outside. Somewhere beyond the bar, drums beat in rhythm with the thousands of performers still rehearsing. The echo of the crowd feels almost mechanical, a precise symphony of movement and sound, as if the festival itself has become a single, living organism.");

            text.addPara("You leave him for a moment, feeling the pulse of the festival around you. The lights, banners, and synchronized cheers wash over the bar like a tide. Even from here, it is clear that the Convergence Festival is not merely entertainment; it is an assertion, a demonstration of unity, and a being of its own.");

            options.addOption("continue", OptionID.LEAVE);
            break;

        case APOLOGIZE:
            text.addPara("You apologize quietly and step away. The participant watches you go, muttering a curse under his breath as he chugs his drink.");
            endEvent(); break;

        default: case LEAVE:
            text.addPara("Suddenly the festival cheers go quiet and you find yourself back at the bar.");
            endEvent(); break;
        }
    }

    private final void endEvent() {
        BarEventManager.getInstance().notifyWasInteractedWith(this);
        noContinue = true;
        done = true;
    }

    public static class ConvergenceFestivalBarEventCreator extends BaseBarEventCreator {
        @Override
        public PortsideBarEvent createBarEvent() {
            return new ConvergenceFestivalBarEvent();
        }
    }
}
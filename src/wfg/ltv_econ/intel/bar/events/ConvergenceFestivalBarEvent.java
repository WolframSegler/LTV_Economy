package wfg.ltv_econ.intel.bar.events;

import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepRewards;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;

import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class ConvergenceFestivalBarEvent extends BaseBarEventWithPerson {
    private enum OptionID {
        APPROACH_PERFORMER,
        INTRO_QUESTION,
        COMPLIMENT,
        COACH,
        CLARIFICATION_REQUEST,
        FINAL_ADVICE,
        APOLOGIZE,
        LEAVE
    }

    private static final Random rand = new Random();

    public ConvergenceFestivalBarEvent() { super(); }

    @Override
    protected String getPersonFaction() {
		return Factions.PLAYER;
	}

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        final PlayerMarketData data = EconomyEngine.getInstance().getPlayerMarketData(market.getId());
        if (data == null || !data.getPolicy("convergence_festival").isActive()) return false;

        return rand.nextFloat() < 0.5f || DebugFlags.BAR_DEBUG;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        dialog.getTextPanel().addPara("The bar is decorated with colorful banners and holographic displays celebrating The Convergence Festival. Patrons cheer and participate in the festive mood.");

        if (rand.nextFloat() < 0.2f || DebugFlags.BAR_DEBUG) {
            regen(dialog.getInteractionTarget().getMarket());

            dialog.getTextPanel().addPara("Your eyes lock onto a festival performer among the crowd. He looks tense and exhausted, glancing nervously at the crowd from his booth.");
            dialog.getOptionPanel().addOption("Approach the convergence festival performer", this);
        }
    }

    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);

        dialog.getVisualPanel().showPersonInfo(person, true);

        optionSelected(null, OptionID.APPROACH_PERFORMER);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionID)) return;

        final OptionID option = (OptionID) optionData;

        options.clearOptions();

        switch (option) {
        case APPROACH_PERFORMER:
            text.addPara("You walk over to the performer, who stares silently at his drink, avoiding your gaze.");
            options.addOption("Make small talk", OptionID.INTRO_QUESTION);
            break;

        case INTRO_QUESTION:
            text.addPara("You ask if he is a performer in the festival. He doesn't answer, just lets out a quiet grunt and keeps staring at his drink.");
            options.addOption("\"With your efforts, the colony is more unified than ever. Your sweat wasn't wasted.\"", OptionID.COMPLIMENT);
            options.addOption("\"This machine cannot run if a single cog falters. Pull through, and the whole festival stands.\"", OptionID.COACH);
            break;

        case COMPLIMENT:
            text.addPara("The performer finally looks at you, eyes filled with silent anger. \"The sweat wasn't from the festival itself,\" he says quietly. \"It was from the shock when security knocked down my door to make sure I attended the morning preparations.\"");
            options.addOption("\"Choosing to join the events is voluntary, but once in, you must fulfill your duty\"", OptionID.FINAL_ADVICE);
            options.addOption("Apologize on behalf of the authorities and leave", OptionID.APOLOGIZE);
            break;

        case COACH:
            text.addPara("The performer finally looks at you, eyes filled with silent frustration. \"Not actually tired,\" he says in a strained, anxious voice. \"The security already helped me get energetic and motivated for the training.\"");
            options.addOption("Reming him that choosing to join makes participation mandatory", OptionID.FINAL_ADVICE);
            options.addOption("Ask what he means by that", OptionID.CLARIFICATION_REQUEST);
            break;

        case CLARIFICATION_REQUEST:
            text.addPara("He narrows his eyes, voice tight. \"The guards practically dragged me out. They made sure I was ready for the drills. I barely had a moment to catch my breath.\"");

            options.addOption("Remind him that choosing to join makes participation mandatory", OptionID.FINAL_ADVICE);
            options.addOption("Apologize on behalf of the authorities and leave", OptionID.APOLOGIZE);
            break;

        case FINAL_ADVICE:
            text.addPara(
                "You tell him quietly: \"Choosing to join the events is voluntary, but once in, you must fulfill your duty. Otherwise, the whole festival would crumble.\""
            );

            final CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
            impact.delta = -RepRewards.HIGH;

            Global.getSector().adjustPlayerReputation(
                new CoreReputationPlugin.RepActionEnvelope(
                    RepActions.CUSTOM, impact, text, true
                ), person
            );

            text.addPara(
                "The performer exhales sharply, eyes flicking to the holographic banners above the plaza. " +
                "Beyond the bar, drums beat in rhythm with the thousands still rehearsing, the crowd echoing rhythmically, as if the festival has become a living organism. " +
                "You leave him for a moment, sensing its pulse; lights, banners, and cheers flood the bar, and in your mind, the festival takes on a life of its own."
            );

            options.addOption("Snap out of it", OptionID.LEAVE);
            break;

        case APOLOGIZE:
            text.addPara("You apologize quietly and step away. The performer watches you go, muttering a curse under his breath as he sips his drink.");
            endEvent(); break;

        default: case LEAVE:
            text.addPara("Suddenly the festival cheers go quiet and you find yourself back at the bar.");
            endEvent(); break;
        }
    }

    private final void endEvent() {
        BarEventManager.getInstance().notifyWasInteractedWith(this);
        done = true;
    }

    public static class ConvergenceFestivalBarEventCreator extends BaseBarEventCreator {
        @Override
        public PortsideBarEvent createBarEvent() {
            return new ConvergenceFestivalBarEvent();
        }
    }
}
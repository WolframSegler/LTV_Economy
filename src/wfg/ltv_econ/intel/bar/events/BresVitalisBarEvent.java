package wfg.ltv_econ.intel.bar.events;

import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;

public class BresVitalisBarEvent extends BaseBarEvent {

    private enum OptionID {
        INSPECT_MAN,
        CALL_HOSPITAL,
        LEAVE
    }

    private static final Random rand = new Random();

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        final PlayerMarketData data = EconomyEngine.getInstance().getPlayerMarketData(market.getId());
        if (data == null || !data.getPolicy("bres_vitalis").isActive()) return false;
        return rand.nextFloat() < 0.15f || DebugFlags.BAR_DEBUG;
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        dialog.getTextPanel().addPara("From the corner of your eye, you notice a man passed out in a table full of cans. The staff seem to ignore him.");

        dialog.getOptionPanel().addOption("Inspect the sleeping man", OptionID.INSPECT_MAN);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionID)) return;

        final OptionID option = (OptionID) optionData;
        final TextPanelAPI text = dialog.getTextPanel();
        final OptionPanelAPI options = dialog.getOptionPanel();

        options.clearOptions();

        switch (option) {
        case INSPECT_MAN:
            text.addPara("You approach the man and notice the table, stacked with dozens of glowing cans of Bres Vitalis.");
            options.addOption("Call the medbay to help him", OptionID.CALL_HOSPITAL);
            options.addOption("Leave him be", OptionID.LEAVE);
            break;

        case CALL_HOSPITAL:
            text.addPara("You wake the man up and call for medical assistance. The medics arrive quickly and take him to the nearest hospital.");
            Global.getSector().getFaction(Factions.INDEPENDENT).adjustRelationship(Factions.PLAYER, 1f);
            endEvent(); break;

        default: case LEAVE:
            text.addPara("You decide it's best not to get involved and leave the man to his nap.");
            endEvent();
        }
    }

    private final void endEvent() {
        BarEventManager.getInstance().notifyWasInteractedWith(this);
        noContinue = true;
        done = true;
    }

    public static class BresVitalisBarEventCreator extends BaseBarEventCreator {
        @Override
        public PortsideBarEvent createBarEvent() {
            return new BresVitalisBarEvent();
        }
    }
}
package wfg.ltv_econ.ui.dialogs;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.panels.FactionSelectionPanel.RowPanel;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.UIContext;
import wfg.native_ui.ui.UIContext.Context;
import wfg.native_ui.ui.dialogs.DialogPanel;

public class ConfirmEmbargoDialog extends DialogPanel  {

    final String factionID;
    final boolean alreadyEmbargoed;
    final RowPanel caller;

    public ConfirmEmbargoDialog(FactionSpecAPI faction, RowPanel caller, boolean alreadyEmbargoed) {
        super(Attachments.getScreenPanel(),
            null,
            alreadyEmbargoed ? "Lift the embargo on "+faction.getDisplayName()+"? Colonies may resume trading with the faction." : "Impose an embargo on "+faction.getDisplayName()+"? Colonies cannot trade with this faction while embargo is active. This will negatively impact relations.",
            alreadyEmbargoed ? "Lift" : "Impose",
            "Cancel"
        );

        factionID = faction.getId();
        this.alreadyEmbargoed = alreadyEmbargoed;
        this.caller = caller;

        backgroundDimAmount = 0f;
        holo.setBackgroundAlpha(150, 175);

        holo.borderAlpha = 0.85f;

        UIContext.setContext(Context.DIALOG);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);
        UIContext.setContext(Context.NONE);

        if (option == 1) return;

        final PlayerFactionSettings settings = EconomyEngine.getInstance().playerFactionSettings;
        if (alreadyEmbargoed) {
            settings.embargoedFactions.remove(factionID);
        } else {
            settings.embargoedFactions.add(factionID);
            final FactionAPI faction = Global.getSector().getFaction(factionID);
            faction.adjustRelationship(Factions.PLAYER, -EconomyConfig.EMBARGO_REP_DROP);
            Global.getSoundPlayer().playUISound("ui_rep_drop", 1f, 1f);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                "Relations with "+faction.getDisplayName()+" decreased by " +
                Math.round(EconomyConfig.EMBARGO_REP_DROP * 100f),
                faction.getDisplayName(),
                faction.getBaseUIColor()
            );
        }

        caller.alreadyEmbargoed = !alreadyEmbargoed;
        caller.createPanel();
    }
}
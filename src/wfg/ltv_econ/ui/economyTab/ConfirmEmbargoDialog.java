package wfg.ltv_econ.ui.economyTab;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.ui.economyTab.FactionSelectionPanel.RowPanel;
import wfg.native_ui.ui.dialog.DialogPanel;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class ConfirmEmbargoDialog extends DialogPanel  {

    final String factionID;
    final boolean alreadyEmbargoed;
    final RowPanel caller;

    public ConfirmEmbargoDialog(FactionSpecAPI faction, RowPanel caller, boolean alreadyEmbargoed) {
        super(null,
            strf(alreadyEmbargoed ? "uiLiftEmbargoTxt" : "uiImposeEmbargoTxt", faction.getDisplayName()),
            alreadyEmbargoed ? str("uiLiftEmbargoVerb") : str("uiImposeEmbargoVerb"),
            str("uiCancel")
        );

        factionID = faction.getId();
        this.alreadyEmbargoed = alreadyEmbargoed;
        this.caller = caller;

        backgroundDimAmount = 0f;
        holo.setBackgroundAlpha(150, 175);

        holo.borderAlpha = 0.85f;
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);
        if (option == 1) return;

        final PlayerFactionSettings settings = LtvEconSaveData.instance().playerFactionSettings;
        if (alreadyEmbargoed) {
            settings.embargoedFactions.remove(factionID);
        } else {
            settings.embargoedFactions.add(factionID);
            final FactionAPI faction = Global.getSector().getFaction(factionID);
            faction.adjustRelationship(Factions.PLAYER, -EconConfig.EMBARGO_REP_DROP);
            Global.getSoundPlayer().playUISound("ui_rep_drop", 1f, 1f);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                strf("uiRelationshipDecreasedByTxt", faction.getDisplayName(), Math.round(EconConfig.EMBARGO_REP_DROP * 100f)),
                faction.getDisplayName(),
                faction.getBaseUIColor()
            );
        }

        caller.alreadyEmbargoed = !alreadyEmbargoed;
        caller.buildUI();
    }
}
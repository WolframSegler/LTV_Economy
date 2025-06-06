package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.Map;


public class AssignWorkersDialoga implements InteractionDialogPlugin {

     private static final Object SLIDER_ID = "assign_workers_slider";
    private static final Object CONFIRM_ID = "assign_workers_confirm";
    private static final Object CANCEL_ID = "assign_workers_cancel";

    private InteractionDialogAPI dialog;
    private OptionPanelAPI options;

    private int minWorkers = 0;
    private int maxWorkers = 100;

    private String industryName = "Example Industry"; // You may want to pass this in

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.options = dialog.getOptionPanel();

        dialog.setPromptText("Assign workers to " + industryName + ":");

        // Add slider
        options.addSelector(
            "Select number of workers to assign:", 
            SLIDER_ID, 
            Misc.getBasePlayerColor(), 
            300f, 60f, 
            minWorkers, maxWorkers, 
            ValueDisplayMode.VALUE, 
            "Use this slider to choose how many workers to assign."
        );

        // Add confirm/cancel options
        options.addOption("Confirm", CONFIRM_ID);
        options.addOption("Cancel", CANCEL_ID);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (CONFIRM_ID.equals(optionData)) {
            int selected = Math.round(options.getSelectorValue(SLIDER_ID));
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Assigned " + selected + " workers to " + industryName + ".");
            dialog.dismiss();
        } else if (CANCEL_ID.equals(optionData)) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
        // Optional: highlight text or display extra info
    }

    @Override
    public void advance(float amount) {
        // Optional: real-time updates or animations
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
        // Not needed for this case
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
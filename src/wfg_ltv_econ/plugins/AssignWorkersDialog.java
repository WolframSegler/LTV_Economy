package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.industry.LtvBaseIndustry;

import javax.swing.plaf.SliderUI;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;

public class AssignWorkersDialog implements CustomDialogDelegate {

    private final Industry industry;
    private int assignedWorkers;
    private int maxWorkers;

    public AssignWorkersDialog(Industry industry) {
        this.industry = industry;
        this.assignedWorkers = ((LtvBaseIndustry) industry).getWorkerAssigned();
        this.maxWorkers = ((LtvBaseIndustry) industry).getWorkerCap();
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {

    }

    @Override
    public void customDialogConfirm() {
        // Called when the confirm button of the whole dialog is pressed (not custom buttons)
    }

    @Override
    public void customDialogCancel() {
        // Called when dialog is cancelled (e.g. ESC or close)
    }

    public float getCustomDialogWidth() {
        return 520f;
    }

    public float getCustomDialogHeight() {
        return 320f;
    }
    public String getCancelText() {
        return "Cancel";
    }
    public String getConfirmText() {
        return "Confirm";
    }
    public boolean hasCancelButton() {
        return true;
    }
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }
}
package wfg.ltv_econ.ui.plugins;

import java.util.Map;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.dialogs.AssignWorkersDialog;
import wfg.wrap_ui.ui.dialogs.CustomDetailDialogPanel;
import wfg.wrap_ui.ui.panels.Slider;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class AssignWorkersDialogPlugin extends CustomPanelPlugin<
CustomDetailDialogPanel<AssignWorkersDialogPlugin>, AssignWorkersDialogPlugin
> {

    private final AssignWorkersDialog dialog;
    private final float initialFreeWorkerRatio;

    public AssignWorkersDialogPlugin(AssignWorkersDialog dialog) {
        this.dialog = dialog;

        final WorkerPoolCondition pool = WorkerIndustryData.getPoolCondition(dialog.market);
        initialFreeWorkerRatio = pool.getFreeWorkerRatio();
    }

    private float getNewFreeWorkerRatio() {
        return initialFreeWorkerRatio + (dialog.data.getWorkerAssignedRatio(false) -
            dialog.previewData.getWorkerAssignedRatio(false));
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        boolean update = false;

        for (Map.Entry<String, Slider> entry : dialog.outputSliders.entrySet()) {
            final String comID = entry.getKey();
            final Slider slider = entry.getValue();

            final float sliderValue = slider.getProgress() / 100f;

            if (dialog.previewData.getAssignedRatioForOutput(comID) != sliderValue) {
                dialog.previewData.setRatioForOutput(comID, sliderValue);
                update = true;
            }

            final float max = Math.max(0,
                sliderValue + getNewFreeWorkerRatio()
            );

            slider.maxValue = Math.min(
                max,
                IndustryIOs.getIndConfig(dialog.industry).outputs.get(comID).workerAssignableLimit
            ) * 100;
        }

        if (update) {
            dialog.inputOutputContainer.clearChildren();
            dialog.drawProductionAndConsumption(dialog.inputOutputContainer.getPanel(), 3, 10);
        }
    }
}

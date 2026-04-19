package wfg.ltv_econ.ui.factionTab.dialog;

import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.ltv_econ.serializable.StaticData;

public class ClearAllDialog extends DialogPanel {
    private final UIBuildableAPI content;

    public ClearAllDialog(UIBuildableAPI content) {
        super(500, 100, null, "Clear all orders?", "Confirm", "Cancel");

        this.content = content;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) {
            StaticData.inv.clearPlannedOrders();
            content.buildUI();
        }
    }
}
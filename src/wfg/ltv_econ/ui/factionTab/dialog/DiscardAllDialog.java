package wfg.ltv_econ.ui.factionTab.dialog;

import wfg.ltv_econ.serializable.StaticData;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.dialog.DialogPanel;

// TODO make discarding hulls return a portion of the resources
public class DiscardAllDialog extends DialogPanel {
    private final UIBuildableAPI content;

    public DiscardAllDialog(UIBuildableAPI content) {
        super(500, 150, null, "Discard all hulls? Allocated resources will be lost.", "Confirm", "Cancel");

        this.content = content;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) {
            StaticData.inv.clearActiveOrders();
            content.buildUI();
        }
    }
}
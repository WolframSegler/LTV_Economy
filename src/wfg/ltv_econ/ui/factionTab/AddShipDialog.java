package wfg.ltv_econ.ui.factionTab;

import wfg.ltv_econ.ui.fleet.InventoryShipWidget;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.widget.Slider;

public class AddShipDialog extends DialogPanel {
    private final InventoryShipWidget widget; 
    private final Slider slider;

    public AddShipDialog(InventoryShipWidget widget) {
        super(null, "Debug add ship amount", "Confirm", "Cancel");

        this.widget = widget;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();

        slider = new Slider(m_panel, null, 0f, 1000f, 450, 32);
        slider.roundBarValue = true;
        slider.roundingIncrement = 1;
        slider.showValueOnly = true;
        add(slider).inBL(25, 100);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) {
            widget.data.addShip(Math.round(slider.getProgress()));
            widget.buildUI();
        }
    }
}
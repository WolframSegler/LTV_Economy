package wfg.ltv_econ.ui.factionTab;

import wfg.ltv_econ.ui.fleet.InventoryShipWidget;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.widget.Slider;

public class RemoveShipDialog extends DialogPanel {
    
    private final InventoryShipWidget widget; 
    private final Slider slider;

    public RemoveShipDialog(InventoryShipWidget widget) {
        super(null, "Scuttle Vessels", "Confirm", "Cancel");

        this.widget = widget;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();

        slider = new Slider(m_panel, null, 0f, widget.data.getIdle(), 450, 32);
        slider.roundBarValue = true;
        slider.roundingIncrement = 1;
        slider.showValueOnly = true;
        add(slider).inBL(25, 100);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) {
            // TODO modify to return to the capital stockpiles the resources from producing the ships plus credits (maybe).
            widget.data.addShip(-Math.round(slider.getProgress()));
            widget.buildUI();
        }
    }
}
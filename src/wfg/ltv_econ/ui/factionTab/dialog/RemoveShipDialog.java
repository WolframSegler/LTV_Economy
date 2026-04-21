package wfg.ltv_econ.ui.factionTab.dialog;

import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.serializable.StaticData;
import wfg.ltv_econ.ui.fleet.InventoryShipWidget;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.widget.Slider;

public class RemoveShipDialog extends DialogPanel {
    private final InventoryShipWidget widget;
    private final Slider slider;

    public RemoveShipDialog(InventoryShipWidget widget) {
        super(500, 150, null, "Scuttle vessels? A portion of the hull materials will be recovered as scrap metal.", "Confirm", "Cancel");

        this.widget = widget;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();

        slider = new Slider(m_panel, null, 0f, widget.data.getIdle(), 450, 32);
        slider.roundBarValue = true;
        slider.roundingIncrement = 1;
        slider.showValueOnly = true;
        add(slider).inBL(25, 75);
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);
        if (option != 0) return;

        final int amount = Math.round(slider.getProgress());
        if (amount <= 0) return;

        ShipProductionManager.addScrapsToCapital(StaticData.inv, amount, widget.data.spec);
        widget.data.addShip(-amount);
        widget.buildUI();
    }
}
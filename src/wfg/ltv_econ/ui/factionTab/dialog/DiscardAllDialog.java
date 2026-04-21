package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.economy.fleet.ShipProductionManager;
import wfg.ltv_econ.economy.fleet.ShipProductionOrder;
import wfg.ltv_econ.serializable.StaticData;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.dialog.DialogPanel;

public class DiscardAllDialog extends DialogPanel {
    private final UIBuildableAPI content;

    public DiscardAllDialog(UIBuildableAPI content) {
        super(500, 150, null, "Discard all hulls? A portion of allocated resources will be recovered as scrap metal.", "Confirm", "Cancel");

        this.content = content;

        backgroundDimAmount = 0.1f;
        holo.borderAlpha = 0.66f;

        setConfirmShortcut();
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);
        if (option != 0) return;
        
        final MarketAPI capital = StaticData.inv.getCapital();
        if (capital != null) {
            float totalValue = 0f;
            for (ShipProductionOrder order : StaticData.inv.getActiveProductionQueue()) {
                totalValue += settings.getHullSpec(order.hullId).getBaseValue();
            }

            ShipProductionManager.addScrapsToCapital(StaticData.inv, totalValue);
        }

        StaticData.inv.clearActiveOrders();
        content.buildUI();
    }
}
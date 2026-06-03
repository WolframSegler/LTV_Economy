package wfg.ltv_econ.ui.fleetTab.dialog;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.str;

import wfg.native_ui.ui.dialog.DialogPanel;

public class TransferToFactionInventoryDialog extends DialogPanel {
    
    public TransferToFactionInventoryDialog() {
        super(450, 120, null, str("uiDialogTransferToFactionInventoryTxt"), str("confirmTxt"), str("uiCancel"));

        backgroundDimAmount = 0.5f;
        holo.borderAlpha = 0.66f;
        setConfirmShortcut();

        buildUI();
    }

    @Override
    public void buildUI() {
        
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option != 0) return;

        // TODO implement ship stripping of armaments and hull mods
        // TODO implement ship bonus xp gain
        // TODO implement ship transfer to faction inventory
        // TODO Notify the player that they earned bonus xp.
    }
}
package wfg.ltv_econ.ui.dialogs;

import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.dialogs.DialogPanel;

public class ColonyInvDialog2 extends DialogPanel {

    public ColonyInvDialog2() {
        super(Attachments.getScreenPanel(), null, "Rahhhh", "Button1", "Button2");

        setConfirmShortcut();

        show(0.5f, 0.5f);
    }
}
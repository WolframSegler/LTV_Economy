package wfg.ltv_econ.ui.plugins;

import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.wrap_ui.ui.dialogs.CustomDetailDialogPanel;
import wfg.wrap_ui.ui.plugins.CustomPanelPlugin;

public class ComDetailDialogPlugin extends CustomPanelPlugin<
    CustomDetailDialogPanel<ComDetailDialogPlugin>, ComDetailDialogPlugin
> {
    protected ComDetailDialog m_dialog;
    protected boolean isFooterButtonChecked = false;

    public ComDetailDialogPlugin(ComDetailDialog dialog) {
        m_dialog = dialog;
    }

    public void advance(float amount) {
        super.advance(amount);

        if (m_dialog.footerPanel.m_checkbox.isChecked() != isFooterButtonChecked) {
            isFooterButtonChecked = !isFooterButtonChecked;

            final int mode = m_dialog.producerButton.checked ? 0 : 1;

            m_dialog.updateSection3(mode);
        }
    }
}
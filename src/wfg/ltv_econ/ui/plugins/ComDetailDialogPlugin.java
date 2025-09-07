package wfg.ltv_econ.ui.plugins;

import java.util.List;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg.ltv_econ.ui.dialogs.ComDetailDialog;
import wfg.ltv_econ.ui.dialogs.CustomDetailDialogPanel;

public class ComDetailDialogPlugin extends LtvCustomPanelPlugin<
    CustomDetailDialogPanel<ComDetailDialogPlugin>, ComDetailDialogPlugin
> {
    protected ComDetailDialog m_dialog;

    protected boolean isFooterButtonChecked = false;
    protected boolean isProducerButtonChecked = true;
    protected boolean isConsumerButtonChecked = false;

    public ComDetailDialogPlugin(ComDetailDialog dialog) {
        m_dialog = dialog;
    }

    public void init(CustomDetailDialogPanel<ComDetailDialogPlugin> panel) {
        super.init(m_panel);
    }

    public void positionChanged(PositionAPI position) {}

    public void render(float alphaMult) {}

    public void advance(float amount) {
        super.advance(amount);

        if (m_dialog.footerPanel.m_checkbox.isChecked() != isFooterButtonChecked) {
            isFooterButtonChecked = !isFooterButtonChecked;

            final int mode = m_dialog.producerButton.isChecked() ? 0 : 1;

            m_dialog.updateSection3(mode);
        }

        if (m_dialog.producerButton != null && m_dialog.producerButton.isChecked() != isProducerButtonChecked) {
            isProducerButtonChecked = m_dialog.producerButton.isChecked();
            
            // At least one button must be checked
            if (!isProducerButtonChecked && !m_dialog.consumerButton.isChecked()) {
                isProducerButtonChecked = true;
                m_dialog.producerButton.setChecked(true);
            } else {
                m_dialog.updateSection3(0);
            }
        }

        if (m_dialog.consumerButton != null && m_dialog.consumerButton.isChecked() != isConsumerButtonChecked) {
            isConsumerButtonChecked = m_dialog.consumerButton.isChecked();
            
            // At least one button must be checked
            if (!isConsumerButtonChecked && !m_dialog.producerButton.isChecked()) {
                isConsumerButtonChecked = true;
                m_dialog.consumerButton.setChecked(true);
            } else {
                m_dialog.updateSection3(1);
            }            
        }
    }

    public void processInput(List<InputEventAPI> events) {}

    public void buttonPressed(Object buttonId) {}
}

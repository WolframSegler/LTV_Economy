package wfg_ltv_econ.plugins;

import java.util.List;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import wfg_ltv_econ.ui.LtvCommodityDetailDialog;
import wfg_ltv_econ.ui.LtvCustomPanel;
import wfg_ltv_econ.util.RenderUtils;

public class LtvCommodityDetailDialogPlugin implements CustomUIPanelPlugin {
    protected CustomPanelAPI m_panel;
    protected LtvCustomPanel m_parent;
    protected LtvCommodityDetailDialog m_dialog;

    protected boolean hasBackground = false;
    protected boolean hasOutline = false;

    protected boolean isFooterButtonChecked = false;
    protected boolean isProducerButtonChecked = true;
    protected boolean isConsumerButtonChecked = false;

    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetW = 0;
    protected int offsetH = 0;

    public LtvCommodityDetailDialogPlugin(LtvCustomPanel parent, LtvCommodityDetailDialog dialog) {
        m_parent = parent;
        m_dialog = dialog;
    }

    public void init(boolean hasBackground, boolean hasOutline, CustomPanelAPI panel) {
        m_panel = panel;
        this.hasBackground = hasBackground;
        this.hasOutline = hasOutline;
    }

    public void positionChanged(PositionAPI position) {}

    public void setOffsets(int x, int y, int width, int height) {
        offsetX = x;
        offsetY = y;
        offsetW = width;
        offsetH = height;
    }

    public void renderBelow(float alphaMult) {
        PositionAPI pos = m_panel.getPosition();

        if (hasBackground) {
            int x = (int)pos.getX() + offsetX;
            int y = (int)pos.getY() + offsetY;
            int w = (int)pos.getWidth() + offsetW;
            int h = (int)pos.getHeight() + offsetH;
            RenderUtils.drawQuad(x, y, w, h, m_parent.BgColor, alphaMult*0.65f, false);
            // Looks vanilla like with 0.65f
        }
        if (hasOutline) {
            RenderUtils.drawFramedBorder(
                pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
                1, m_parent.getFaction().getGridUIColor(), alphaMult
            );
        }
    }

    public void render(float alphaMult) {}

    public void advance(float amount) {
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

package wfg_ltv_econ.ui.plugins;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.input.InputEventAPI;

import wfg_ltv_econ.ui.dialogs.LtvCommodityDetailDialog;
import wfg_ltv_econ.ui.panels.LtvCommodityRowPanel;
import wfg_ltv_econ.util.UiUtils;

public class LtvCommodityRowPanelPlugin extends LtvCustomPanelPlugin {

    private boolean displayPrices = false;

    public void setDisplayPrices(boolean displayPrices) {
        this.displayPrices = displayPrices;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        LtvCommodityRowPanel panel = (LtvCommodityRowPanel)m_panel;

        if (LMBDownLastFrame && panel.getParentWrapper().isRowSelectable) {
            panel.getParentWrapper().selectRow(panel);

            if (panel.getParentWrapper().selectionListener != null) {
                panel.getParentWrapper().selectionListener.onCommoditySelected(panel.getCommodity());
            }
        }

        if (displayPrices && hoveredLastFrame && LMBUpLastFrame) {
            InteractionDialogAPI dialog = Global.getSector().getCampaignUI()
                    .getCurrentInteractionDialog();

            if (dialog != null) {
                LtvCommodityDetailDialog dialogPanel = new LtvCommodityDetailDialog(m_panel,
                panel.getCommodity());

                dialog.showCustomDialog(dialogPanel.PANEL_W, dialogPanel.PANEL_H, dialogPanel);
            }

            hasClickedBefore = false;
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        super.processInput(events);

        if (m_panel instanceof LtvCommodityRowPanel) {
            if (!m_hasTooltip || !hoveredLastFrame) {
                ((LtvCommodityRowPanel) m_panel).isExpanded = false;

                return;
            }

            for (InputEventAPI event : events) {
                if (event.isMouseEvent()) {
                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F1) {
                    ((LtvCommodityRowPanel) m_panel).isExpanded = !((LtvCommodityRowPanel) m_panel).isExpanded;
                    hideTooltip();

                    event.consume();

                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_F2) {
                    CommodityOnMarketAPI com = ((LtvCommodityRowPanel) m_panel).getCommodity();
                    String codexID = CodexDataV2.getCommodityEntryId(com.getId());
                    UiUtils.openCodexPage(codexID);
                    hideTooltip();

                    event.consume();
                }
            }
        }
    }
}

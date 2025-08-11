package wfg_ltv_econ.ui.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import wfg_ltv_econ.ui.dialogs.ComDetailDialog;
import wfg_ltv_econ.ui.panels.LtvCommodityRowPanel;

public class LtvCommodityRowPanelPlugin extends LtvCustomPanelPlugin<
    LtvCommodityRowPanel, LtvCommodityRowPanelPlugin
> {

    private boolean displayPrices = false;

    public void setDisplayPrices(boolean displayPrices) {
        this.displayPrices = displayPrices;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        LtvCommodityRowPanel panel = (LtvCommodityRowPanel)m_panel;

        if (inputSnapshot.LMBDownLastFrame && panel.getParentWrapper().isRowSelectable) {
            panel.getParentWrapper().selectRow(panel);

            if (panel.getParentWrapper().selectionListener != null) {
                panel.getParentWrapper().selectionListener.onCommoditySelected(panel.getCommodity());
            }
        }

        if (displayPrices && inputSnapshot.hoveredLastFrame && inputSnapshot.LMBUpLastFrame) {
            InteractionDialogAPI dialog = Global.getSector().getCampaignUI()
                    .getCurrentInteractionDialog();

            if (dialog != null) {
                ComDetailDialog dialogPanel = new ComDetailDialog(m_panel,
                panel.getCommodity());

                dialog.showCustomDialog(dialogPanel.PANEL_W, dialogPanel.PANEL_H, dialogPanel);
            }

            inputSnapshot.hasClickedBefore = false;
        }
    }
}

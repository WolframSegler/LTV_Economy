package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import wfg_ltv_econ.ui.LtvCommodityDetailDialog;
import wfg_ltv_econ.ui.LtvCommodityRowPanel;

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
            setPersistentGlow(!persistentGlow);
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
}

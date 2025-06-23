package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;

import wfg_ltv_econ.ui.LtvCommodityDetailDialog;
import wfg_ltv_econ.ui.LtvCommodityRowPanel;

public class LtvCommodityRowPanelPlugin extends LtvCustomPanelPlugin {

    private boolean displayPrices = false;
    private boolean hasClickedBefore = false;

    public void setDisplayPrices(boolean displayPrices) {
        this.displayPrices = displayPrices;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (clickedThisFrame) {
            hasClickedBefore = true;
        }
        if (!hoveredLastFrame) {
            hasClickedBefore = false;
        }

        if (displayPrices && hoveredLastFrame && !clickedThisFrame && hasClickedBefore) {
            InteractionDialogAPI dialog = Global.getSector().getCampaignUI()
                    .getCurrentInteractionDialog();

            if (dialog != null) {
                CommodityOnMarketAPI com = ((LtvCommodityRowPanel)m_panel).getCommodity();
                LtvCommodityDetailDialog dialogPanel = new LtvCommodityDetailDialog(m_panel, com);

                dialog.showCustomDialog(dialogPanel.PANEL_W, dialogPanel.PANEL_H, dialogPanel);
            }

            hasClickedBefore = false;
        }
    }
}

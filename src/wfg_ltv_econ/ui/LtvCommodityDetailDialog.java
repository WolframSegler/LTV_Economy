package wfg_ltv_econ.ui;

import java.util.Map;

import org.lwjgl.util.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.marketinfo.CommodityDetailDialog;

import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.util.LtvNumFormat;

public class LtvCommodityDetailDialog implements CustomDialogDelegate {
//     // The window that opens when you click on a commodity while inside the range of a comms network
    
//     // Notes about the original window. Using VisualVM
//     // new Color(181, 230, 255, 255)
//     // new Color(70, 200, 255, 255)
//     // new Color(31, 94, 112, 255)
//     // new Color(170, 222, 255, 255)
//     // int 1756
    private final int panelWidth;
    private final int panelHeight;

    public LtvCommodityDetailDialog(int panelWidth, int panelHeight) {
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        TooltipMakerAPI tooltip = panel.createUIElement(panelWidth, panelHeight, true);

        final int pad = 3;
        final int opad = 10;

        // Add the tooltip to the main Panel at the end
        panel.addUIElement(tooltip);
    }

    @Override
    public void customDialogConfirm() {

    }

    @Override
    public void customDialogCancel() {
        // Called when dialog is cancelled (e.g. ESC or close)
    }

    public float getCustomDialogWidth() {
        return panelWidth;
    }

    public float getCustomDialogHeight() {
        return panelHeight;
    }

    public String getCancelText() {
        return "Cancel";
    }

    public String getConfirmText() {
        return "Confirm";
    }

    public boolean hasCancelButton() {
        return true;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }
}

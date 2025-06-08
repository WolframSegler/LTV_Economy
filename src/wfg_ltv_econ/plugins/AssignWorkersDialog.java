package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.industry.LtvBaseIndustry;
import wfg_ltv_econ.util.LtvNumFormat;

import java.awt.Color;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.campaign.ui.N; //Current slider class (v.0.98 R8).
// Here is a unique method it has: public float getShowNotchOnIfBelowProgress()

public class AssignWorkersDialog implements CustomDialogDelegate {

    private final Industry industry;
    private final MarketAPI market;
    private final int panelWidth;
    private final int panelHeight;

    // public N(String LabelText, float MinValue, float MaxValue)
    private N slider = new N(null, 0, 100);

    public AssignWorkersDialog(Industry industry, int panelWidth, int panelHeight) {
        this.industry = industry;
        this.market = ((LtvBaseIndustry) industry).getMarket();
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        TooltipMakerAPI tooltip = panel.createUIElement(panelWidth, panelHeight, true);

        final int pad = 3;
        final int opad = 10;
        final int sliderHeight = 32;
        final int sliderWidth = 380;
        final int sliderY = 250;

        // Draw Titel
        tooltip.setParaOrbitronLarge();
        String txt = "Assign workers to " + industry.getCurrentName();
        LabelAPI lbl = tooltip.addPara(txt, 0f);

        UIComponentAPI lblComp = tooltip.getPrev();
        float textX = (panelWidth - lbl.computeTextWidth(txt)) / 2;
        lblComp.getPosition().inTL(textX, opad);
        tooltip.setParaFontDefault();

        // Draw Production
        drawProductionAndConsumption(panel, pad, opad, (int)(tooltip.getHeightSoFar() + lbl.computeTextHeight(txt)));

        // Draw text left of the slider
        tooltip.setParaInsigniaLarge();
        txt = "Workers:";
        lbl = tooltip.addPara(txt, 0f);

        lblComp = tooltip.getPrev();
        float textH = lbl.computeTextHeight(txt);
        float textY = sliderY + (sliderHeight - textH) * 0.5f;
        lblComp.getPosition().inTL(pad, textY);
        tooltip.setParaFontDefault();

        // Create the slider
        slider.setHighlightOnMouseover(true);
        slider.setUserAdjustable(true);
        slider.setShowAdjustableIndicator(true);
        slider.setShowValueOnly(true);
        slider.setRoundBarValue(true);
        slider.setClampCurrToMax(true);

        slider.setRoundingIncrement(5);
        slider.setBarColor(new Color(20, 125, 200));
        slider.setHeight(sliderHeight);
        slider.setWidth(sliderWidth);
        slider.setProgress(((LtvBaseIndustry) industry).getWorkerAssignedRatio() * 100);
        slider.setMax((getFreeWorkerRatio() * 100f) + ((LtvBaseIndustry) industry).getWorkerAssignedRatio() * 100);
        // Do not let assignation above capacity minus the current industry

        panel.addComponent((UIPanelAPI) slider).inTL((panelWidth - sliderWidth - opad), sliderY);

        // At the end
        panel.addUIElement(tooltip);
    }

    @Override
    public void customDialogConfirm() {
        ((LtvBaseIndustry) industry).setWorkersAssigned(slider.getProgress() / 100);
    }

    public float getFreeWorkerRatio() {
        MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
        if (workerPoolCondition == null) {
            return 0;
        }
        WorkerPoolCondition pool = (WorkerPoolCondition) workerPoolCondition.getPlugin();
        return pool.getFreeWorkerRatio();
    }

    public void drawProductionAndConsumption(CustomPanelAPI panel, int pad, int opad, int lastHeight) {
        final float iconSize = 32f;
        final int itemsPerRow = 2;
        final float sectionWidth = ((panelWidth / 2) / itemsPerRow) - opad;
        final Color highlight = Misc.getHighlightColor();
        Map<String, MutableCommodityQuantity> supply = ((LtvBaseIndustry) industry).getSupply();
        Map<String, MutableCommodityQuantity> demand = ((LtvBaseIndustry) industry).getDemand();
        final FactionAPI faction = market.getFaction();
        final Color color = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();

        TooltipMakerAPI tooltip = panel.createUIElement((panelWidth/2) - opad, panelHeight, false);
        tooltip.addSectionHeading("Production", color, dark, Alignment.MID, opad);
        float startY = tooltip.getHeightSoFar() + opad;

        // Supply
        float x = opad;
        float y = startY;
        int count = -1;

        for (MutableCommodityQuantity curr : supply.values()) {
            CommoditySpecAPI commodity = market.getCommodityData(curr.getCommodityId()).getCommodity();
            CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(curr.getCommodityId());
            int pAmount = curr.getQuantity().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }

            // draw icon
            tooltip.beginIconGroup();
            tooltip.setIconSpacingMedium();
            tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
            tooltip.addIconGroup(0f);
            UIComponentAPI iconComp = tooltip.getPrev();

            // Add extra padding for thinner icons
            float actualIconWidth = iconSize * commoditySpec.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            String txt = Strings.X + LtvNumFormat.formatWithMaxDigits(pAmount);
            LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

            UIComponentAPI lblComp = tooltip.getPrev();
            float textH = lbl.computeTextHeight(txt);
            float textX = x + iconSize + pad;
            float textY = y + (iconSize - textH) * 0.5f;
            lblComp.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tooltip.setHeightSoFar(y);
        panel.addUIElement(tooltip).inTL(opad/2, lastHeight + opad);

        tooltip = panel.createUIElement((panelWidth/2) - opad, panelHeight, false);
        tooltip.addSectionHeading("Demand", color, dark, Alignment.MID, opad);

        // Demand
        x = opad;
        y = startY;
        count = -1;

        for (MutableCommodityQuantity curr : demand.values()) {
            CommodityOnMarketAPI commodity = market.getCommodityData(curr.getCommodityId());
            CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(curr.getCommodityId());
            int dAmount = curr.getQuantity().getModifiedInt();
            int allDeficit = commodity.getDeficitQuantity();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }

            // draw icon
            tooltip.beginIconGroup();
            tooltip.setIconSpacingMedium();
            if (allDeficit > 0) {
                tooltip.addIcons(commodity, 1, IconRenderMode.DIM_RED);
            } else {
                tooltip.addIcons(commodity, 1, IconRenderMode.NORMAL);
            }
            tooltip.addIconGroup(0f);
            UIComponentAPI iconComp = tooltip.getPrev();

            // Add extra padding for thinner icons
            float actualIconWidth = iconSize * commoditySpec.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            String txt = Strings.X + LtvNumFormat.formatWithMaxDigits(dAmount);
            LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

            UIComponentAPI lblComp = tooltip.getPrev();
            float textH = lbl.computeTextHeight(txt);
            float textX = x + iconSize + pad;
            float textY = y + (iconSize - textH) * 0.5f;
            lblComp.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tooltip.setHeightSoFar(y);
        panel.addUIElement(tooltip).inTR(opad/2, lastHeight + opad);

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
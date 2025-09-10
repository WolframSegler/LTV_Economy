package wfg.ltv_econ.ui.dialogs;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.campaign.ui.N; //Current slider class (v.0.98 R8).
// Here is a unique method it has: public float getShowNotchOnIfBelowProgress()

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.IndustryConfigLoader.IndustryConfig;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.wrap_ui.util.NumFormat;

public class AssignWorkersDialog implements CustomDialogDelegate {

    private final Industry industry;
    private final MarketAPI market;
    private final int panelWidth;
    private final int panelHeight;

    private final WorkerIndustryData data;

    // public N(String LabelText, float MinValue, float MaxValue)
    private N slider = new N(null, 0, 100);

    public AssignWorkersDialog(Industry ind, int panelWidth, int panelHeight) {
        this.industry = ind;
        this.market = ind.getMarket();
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.data = WorkerRegistry.getInstance().getData(ind.getMarket().getId(), ind.getId());
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        final TooltipMakerAPI tooltip = panel.createUIElement(panelWidth, panelHeight, true);
        final EconomyEngine engine = EconomyEngine.getInstance();

        final int pad = 3;
        final int opad = 10;
        final int sliderHeight = 32;
        final int sliderWidth = 380;
        final int sliderY = 250;

        // Draw Titel
        tooltip.setParaOrbitronLarge();
        String txt = "Assign workers to " + industry.getCurrentName();
        LabelAPI lbl = tooltip.addPara(txt, 0f);

        final float textX = (panelWidth - lbl.computeTextWidth(txt)) / 2;
        lbl.getPosition().inTL(textX, opad);
        tooltip.setParaFontDefault();

        // Draw Production
        drawProductionAndConsumption(panel, pad, opad, (int) (tooltip.getHeightSoFar() + lbl.computeTextHeight(txt)));

        // Draw separator line
        final Color gray = new Color(100, 100, 100);
        LabelAPI separator = tooltip.addSectionHeading(null, gray, gray, Alignment.MID, 0);
        separator.getPosition().inTL(0, sliderY - sliderHeight);
        separator.getPosition().setSize(panelWidth, 1);

        // Draw text left of the slider
        tooltip.setParaInsigniaLarge();
        txt = "Workers:";
        lbl = tooltip.addPara(txt, 0f);

        final float textH = lbl.computeTextHeight(txt);
        final float textY = sliderY + (sliderHeight - textH) * 0.5f;
        lbl.getPosition().inTL(pad, textY);
        tooltip.setParaFontDefault();

        // Create the slider
        slider.setHighlightOnMouseover(true);
        slider.setUserAdjustable(true);
        slider.setShowAdjustableIndicator(true);
        slider.setShowValueOnly(true);
        slider.setRoundBarValue(true);
        slider.setClampCurrToMax(true);

        slider.setRoundingIncrement(2);
        slider.setBarColor(new Color(20, 125, 200));
        slider.setHeight(sliderHeight);
        slider.setWidth(sliderWidth);
        slider.setProgress(data.getWorkerAssignedRatio() * 100);

        final IndustryConfig config = engine.ind_config.get(industry.getId());
        final float limit = config == null ? WorkerRegistry.DEFAULT_WORKER_CAP
            : config.workerAssignableLimit;
        final float max = Math.min(limit, getFreeWorkerRatio() + data.getWorkerAssignedRatio());
        
        slider.setMax(max * 100);

        panel.addComponent((UIPanelAPI) slider).inTL((panelWidth - sliderWidth - opad), sliderY);
        panel.addUIElement(tooltip);
    }

    @Override
    public void customDialogConfirm() {
        data.setWorkersAssigned(slider.getProgress() / 100f);
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

        final EconomyEngine engine = EconomyEngine.getInstance();

        final FactionAPI faction = market.getFaction();
        final Color color = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();

        TooltipMakerAPI tooltip = panel.createUIElement((panelWidth / 2) - opad, panelHeight, false);
        tooltip.addSectionHeading("Production", color, dark, Alignment.MID, opad);
        float startY = tooltip.getHeightSoFar() + opad;

        // Supply
        float x = opad;
        float y = startY;
        int count = -1;

        for (CommoditySpecAPI com : EconomyEngine.getEconCommodities()) {
            CommodityStats stats = engine.getComStats(com.getId(), market.getId());
            long pAmount = stats.getLocalProductionStat(industry.getId()).getModifiedInt();

            if (pAmount < 1) {
                continue;
            }

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }

            // draw icon
            tooltip.beginIconGroup();
            tooltip.setIconSpacingMedium();
            tooltip.addIcons(com, 1, IconRenderMode.NORMAL);
            tooltip.addIconGroup(0f);
            UIComponentAPI iconComp = tooltip.getPrev();

            // Add extra padding for thinner icons
            float actualIconWidth = iconSize * com.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            String txt = Strings.X + NumFormat.engNotation(pAmount);
            LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

            float textH = lbl.computeTextHeight(txt);
            float textX = x + iconSize + pad;
            float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tooltip.setHeightSoFar(y);
        panel.addUIElement(tooltip).inTL(opad / 2, lastHeight + opad);

        tooltip = panel.createUIElement((panelWidth / 2) - opad, panelHeight, false);
        tooltip.addSectionHeading("Demand", color, dark, Alignment.MID, opad);

        // Demand
        x = opad;
        y = startY;
        count = -1;

        for (CommoditySpecAPI com : EconomyEngine.getEconCommodities()) {
            CommodityStats stats = engine.getComStats(com.getId(), market.getId());
            long dAmount = stats.getBaseDemandStat(industry.getId()).getModifiedInt();
            long allDeficit = stats.getDeficit();            

            if (dAmount < 1) {
                continue;
            }

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
                tooltip.addIcons(com, 1, IconRenderMode.DIM_RED);
            } else {
                tooltip.addIcons(com, 1, IconRenderMode.NORMAL);
            }
            tooltip.addIconGroup(0f);
            UIComponentAPI iconComp = tooltip.getPrev();

            // Add extra padding for thinner icons
            float actualIconWidth = iconSize * com.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            String txt = Strings.X + NumFormat.engNotation(dAmount);
            LabelAPI lbl = tooltip.addPara(txt + " / Day", 0f, highlight, txt);

            float textH = lbl.computeTextHeight(txt);
            float textX = x + iconSize + pad;
            float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tooltip.setHeightSoFar(y);
        panel.addUIElement(tooltip).inTR(opad / 2, lastHeight + opad);

    }

    @Override
    public void customDialogCancel() {}

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
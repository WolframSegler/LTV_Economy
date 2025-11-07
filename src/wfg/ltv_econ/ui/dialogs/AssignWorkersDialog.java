package wfg.ltv_econ.ui.dialogs;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.campaign.ui.N; //Current slider class (v.0.98 R8).
// Here is a unique method it has: public float getShowNotchOnIfBelowProgress()

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityStats;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.plugins.AssignWorkersDialogPlugin;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.CustomDetailDialogPanel;
import wfg.wrap_ui.ui.panels.BasePanel;
import wfg.wrap_ui.ui.panels.SpritePanelWithTp;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.plugins.SpritePanelPlugin;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class AssignWorkersDialog implements CustomDialogDelegate {

    public static final String WARNING_BUTTON_PATH = Global.getSettings()
        .getSpriteName("ui", "warning_button");

    public final WorkerRegistry reg;

    public final Industry industry;
    public final MarketAPI market;
    public final int panelWidth;
    public final int panelHeight;

    public final WorkerIndustryData data;
    public final WorkerIndustryData previewData;
    public final Map<String, N> outputSliders;

    public BasePanel inputOutputContainer;

    public AssignWorkersDialog(Industry ind, int panelWidth, int panelHeight) {
        this.reg = WorkerRegistry.getInstance();

        this.industry = ind;
        this.market = ind.getMarket();
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.data = reg.getData(ind.getMarket().getId(), ind.getSpec());
        this.previewData = new WorkerIndustryData(data);
        reg.setData(previewData);

        this.outputSliders = new HashMap<>();
    }

    @Override
    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        UIState.setState(State.DIALOG);

        CustomDetailDialogPanel<AssignWorkersDialogPlugin> m_panel = new CustomDetailDialogPanel<>(
            null,
            panel,
            market,
            panelWidth, panelHeight,
            new AssignWorkersDialogPlugin(this)
        ) {
            @Override
            public float getBgTransparency() {
                return 0f;
            }
        };

        panel.addComponent(m_panel.getPanel()).inBL(0, 0);

        final int pad = 3;
        final int opad = 10;
        final int sliderHeight = 32;
        final int sliderWidth = 380;
        final int sliderY = 225;
        final int iconSize = 26;

        // Draw Titel
        String txt = "Assign workers to " + industry.getCurrentName();
        LabelAPI lbl = Global.getSettings().createLabel(txt, Fonts.ORBITRON_20AA);

        final float textX = (panelWidth - lbl.computeTextWidth(txt)) / 2;
        m_panel.add(lbl).inTL(textX, pad*2);

        inputOutputContainer = new BasePanel(
            null, m_panel.getPanel(), market, (int) m_panel.getPos().getWidth(),
            180, new BasePanelPlugin<>()
        ) {
            @Override
            public float getBgTransparency() {
                return 0f;
            }
        };

        // Draw Production
        drawProductionAndConsumption(inputOutputContainer.getPanel(), pad, opad);

        m_panel.add(inputOutputContainer).inTL(0, lbl.computeTextHeight(txt) + opad);

        // Draw separator line
        final BasePanel separator = new BasePanel(
            null, m_panel.getPanel(), market, panelWidth, 1, new BasePanelPlugin<>()
        ) {
            @Override
            public Color getBgColor() {
                return new Color(100, 100, 100);
            }
        };
        separator.getPos().inTL(0, sliderY - opad);
        m_panel.add(separator);

        SpritePanelWithTp help_button = new SpritePanelWithTp(
            null, m_panel.getPanel(), market, 20 , 20, new SpritePanelPlugin<>(),
            WARNING_BUTTON_PATH, null, null, false
        ) {
            @Override
            public void initializePlugin(boolean hasPlugin) {
                super.initializePlugin(hasPlugin);
                
                getPlugin().setIgnoreUIState(true);
            }

            @Override
            public CustomPanelAPI getTpParent() {
                return getPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = getPanel().createUIElement(300, 1, false);

                tp.addPara(
                    "Adjust each output's slider to allocate a portion of the market's total workforce. " +
                    "The values represent the percentage of available workers assigned to that output.",
                    pad
                );

                add(tp);

                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.TopLeft, 0);

                return tp;
            }
        };

        m_panel.add(help_button).inTR(pad, sliderY + pad);

        final CustomPanelAPI outputsPanel = Global.getSettings().createCustom(
            panelWidth,
            300,
            null
        );
        final TooltipMakerAPI outputsTp = outputsPanel.createUIElement(panelWidth, 180, true);

        final SettingsAPI settings = Global.getSettings();

        int cumulativeYOffset = pad;
        for (String comID : data.getRegisteredOutputs()) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(comID);
            outputsTp.addImage(spec.getIconName(), iconSize, iconSize, pad);
            outputsTp.getPrev().getPosition().inTL(pad, cumulativeYOffset);

            N outputSlider = new N(null, 0, 100);
            outputSliders.put(comID, outputSlider);

            // Create the slider
            outputSlider.setHighlightOnMouseover(true);
            outputSlider.setUserAdjustable(true);
            outputSlider.setShowValueOnly(true);
            outputSlider.setRoundBarValue(true);
            outputSlider.setClampCurrToMax(true);

            outputSlider.setRoundingIncrement(1);
            outputSlider.setBarColor(new Color(20, 125, 200));
            outputSlider.setHeight(sliderHeight);
            outputSlider.setWidth(sliderWidth);

            final WorkerPoolCondition pool = WorkerIndustryData.getPoolCondition(market);
            pool.recalculateWorkerPool();

            final float max = Math.max(0,
                data.getAssignedRatioForOutput(comID) + pool.getFreeWorkerRatio()
            );
            
            outputSlider.setMax(max * 100);

            outputSlider.setProgress(data.getAssignedRatioForOutput(comID) * 100);

            outputsTp.addComponent(outputSlider).inTL(iconSize + pad*2, cumulativeYOffset);
            cumulativeYOffset += pad + sliderHeight;
        }

        outputsTp.setHeightSoFar(cumulativeYOffset);

        outputsPanel.addUIElement(outputsTp).inTL(-pad, 0);
        m_panel.add(outputsPanel).inTL(opad, sliderY);
    }

    @Override
    public void customDialogConfirm() {
        UIState.setState(State.NONE);
    }

    @Override
    public void customDialogCancel() {
        reg.setData(data);

        UIState.setState(State.NONE);
    }

    public void drawProductionAndConsumption(CustomPanelAPI panel, int pad, int opad) {
        final float iconSize = 32f;
        final int itemsPerRow = 2;
        final float sectionWidth = ((panelWidth / 2) / itemsPerRow) - opad;
        final Color highlight = Misc.getHighlightColor();

        final EconomyEngine engine = EconomyEngine.getInstance();

        final FactionAPI faction = market.getFaction();
        final Color color = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();

        final Map<String, MutableStat> supplyList = new HashMap<>();
        final Map<String, MutableStat> demandList = new HashMap<>();

        final boolean importing = IndustryIOs.getIndConfig(industry).ignoreLocalStockpiles;

        if (!importing) {
            for (String comID : IndustryIOs.getRealOutputs(industry, false).keySet()) {
                supplyList.put(comID, CompatLayer.convertIndSupplyStat(industry, comID));
            }
        }

        for (String comID : IndustryIOs.getRealInputs(industry, false)) {
            demandList.put(comID, CompatLayer.convertIndDemandStat(industry, comID));
        }

        TooltipMakerAPI tooltip = panel.createUIElement((panelWidth / 2) - opad, panelHeight, false);
        tooltip.addSectionHeading(importing ? "---" : "Production", color, dark, Alignment.MID, opad);
        final float startY = tooltip.getHeightSoFar() + pad;

        // Supply
        float x = opad;
        float y = startY;
        int count = -1;

        for (Map.Entry<String, MutableStat> entry : supplyList.entrySet()) {
            final CommoditySpecAPI com = market.getCommodityData(entry.getKey()).getCommodity();
            final long pAmount = entry.getValue().getModifiedInt();

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
        panel.addUIElement(tooltip).inTL(opad / 2, 0);

        tooltip = panel.createUIElement((panelWidth / 2) - opad, panelHeight, false);
        tooltip.addSectionHeading(importing ? "Import" : "Demand", color, dark, Alignment.MID, opad);

        // Demand
        x = opad;
        y = startY;
        count = -1;

        for (Map.Entry<String, MutableStat> entry : demandList.entrySet()) {
            final CommoditySpecAPI com = market.getCommodityData(entry.getKey()).getCommodity();
            final long dAmount = entry.getValue().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }

            final CommodityStats stats = engine.getComStats(entry.getKey(), market.getId());
            final float oldDemand = stats.getBaseDemandStat(industry.getId()).getModifiedValue();

            final float baseDemand = stats.getBaseDemand(false) + (long) (dAmount - oldDemand);
            final float demandMet = Math.min(stats.getLocalProduction(false), baseDemand)
                + stats.getDeficitMetViaTrade();
            final float availability = baseDemand == 0 ? 1f : (float)demandMet / baseDemand;

            // draw icon
            tooltip.beginIconGroup();
            tooltip.setIconSpacingMedium();
            IconRenderMode renderMode = availability < 0.99f && !importing ?
                IconRenderMode.DIM_RED : IconRenderMode.NORMAL;
            tooltip.addIcons(com, 1, renderMode);
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
        panel.addUIElement(tooltip).inTR(opad / 2, 0);

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
package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.UIColors.SLIDER_BASE;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.panel.BasePanel;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.visual.SpritePanelWithTp;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class AssignWorkersDialog extends DialogPanel {
    private static final SpriteAPI WARNING_BUTTON = settings.getSprite("ui", "warning_button");
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 400;

    private final WorkerRegistry reg;
    private final Industry industry;
    private final MarketAPI market;
    private final WorkerIndustryData data;
    private final WorkerIndustryData previewData;
    private final ArrayMap<String, Slider> outputSliders;

    private BasePanel inputOutputContainer;

    private final float initialFreeWorkerRatio;

    public AssignWorkersDialog(Industry ind) {
        super(PANEL_W, PANEL_H, null, null, "Confirm", "Cancel");
        setConfirmShortcut();

        reg = WorkerRegistry.instance();
        industry = ind;
        market = ind.getMarket();
        data = reg.getData(ind);
        previewData = new WorkerIndustryData(data);
        outputSliders = new ArrayMap<>(IndustryConfigManager.getIndConfig(industry).outputs.size());

        reg.setData(previewData);
        initialFreeWorkerRatio = WorkerPoolCondition.getPoolCondition(market).getFreeWorkerRatio();

        holo.borderAlpha = 0.7f;
        backgroundDimAmount = 0.2f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final int sliderHeight = 32;
        final int sliderWidth = 380;
        final int sliderY = 225;
        final int iconSize = 26;

        final String txt = "Assign workers to " + industry.getCurrentName();
        final LabelAPI lbl = settings.createLabel(txt, Fonts.ORBITRON_20AA);

        final float textX = (PANEL_W - lbl.computeTextWidth(txt)) / 2;
        add(lbl).inTL(textX, pad*2);

        inputOutputContainer = new BasePanel(
            m_panel, (int) pos.getWidth(), 180
        ) {{ bg.alpha = 0f;}};

        drawProductionAndConsumption(inputOutputContainer.getPanel());

        add(inputOutputContainer.getPanel())
            .inTL(0, lbl.computeTextHeight(txt) + opad);

        final BasePanel separator = new BasePanel(
            m_panel, PANEL_W, 1
        ) {{ bg.color = gray;}};
        
        separator.getPos().inTL(0, sliderY - opad);
        add(separator.getPanel());

        final SpritePanelWithTp help_button = new SpritePanelWithTp(m_panel, 20 , 20,
            WARNING_BUTTON, null, null
        ) {{
            tooltip.builder = (tp, exp) -> {
                tp.addPara(
                    "Adjust each output's slider to allocate a portion of the market's total workforce. " +
                    "The values represent the percentage of available workers assigned to that output.",
                    pad
                );
            };
            tooltip.positioner = (tp, exp) -> {
                NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.TopLeft, 0);
            };

            glow.type = GlowType.ADDITIVE;
            glow.additiveSprite = m_sprite;
        }};

        add(help_button.getPanel()).inTR(pad, sliderY + pad);

        final UIPanelAPI outputsPanel = settings.createCustom(
            PANEL_W,
            PANEL_H - (sliderY + pad * 2),
            null
        );
        final TooltipMakerAPI outputsTp = ComponentFactory.createTooltip(PANEL_W, true);
        
        int cumulativeYOffset = pad;
        for (OutputConfig output : IndustryConfigManager.getIndConfig(industry).outputs.values()) {
            if (!output.usesWorkers) continue;

            final CommoditySpecAPI spec = settings.getCommoditySpec(output.comID);
            outputsTp.addImage(spec.getIconName(), iconSize, iconSize, pad);
            outputsTp.getPrev().getPosition().inTL(pad, cumulativeYOffset);

            final Slider outputSlider = new Slider(
                m_panel, null, 0, 100, sliderWidth, sliderHeight
            );
            outputSliders.put(output.comID, outputSlider);

            outputSlider.setHighlightOnMouseover(true);
            outputSlider.setBarColor(SLIDER_BASE);
            outputSlider.showValueOnly = true;
            outputSlider.roundBarValue = true;
            outputSlider.clampCurrToMax = true;
            outputSlider.roundingIncrement = 1;

            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);
            pool.recalculateWorkerPool();

            final float max = Math.max(0,
                data.getAssignedRatioForOutput(output.comID) + pool.getFreeWorkerRatio()
            );
            
            outputSlider.maxValue = Math.min(
                max,
                IndustryConfigManager.getIndConfig(industry).outputs.get(output.comID).workerAssignableLimit
            ) * 100;

            outputSlider.setProgress(data.getAssignedRatioForOutput(output.comID) * 100);

            outputsTp.addComponent(outputSlider.getPanel()).inTL(iconSize + pad*2, cumulativeYOffset);
            cumulativeYOffset += pad + sliderHeight;
        }

        outputsTp.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(outputsTp, 180, true, outputsPanel).inTL(-pad, 0);
        add(outputsPanel).inTL(opad, sliderY);
    }

    public void drawProductionAndConsumption(UIPanelAPI panel) {
        final float iconSize = 32f;
        final int itemsPerRow = 2;
        final float sectionWidth = ((PANEL_W / 2) / itemsPerRow) - opad;
        final EconomyEngine engine = EconomyEngine.instance();

        final FactionAPI faction = market.getFaction();
        final Color color = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();

        final Map<String, Float> outputs = IndustryIOs.getRealOutputs(industry, false);
        final Set<String> inputs = IndustryIOs.getRealInputs(industry, false);

        final ArrayMap<String, MutableStat> supplyList = new ArrayMap<>(outputs.size());
        final ArrayMap<String, MutableStat> demandList = new ArrayMap<>(inputs.size());

        final boolean importing = IndustryConfigManager.getIndConfig(industry).demandOnly;

        if (!importing) {
            for (String comID : outputs.keySet()) {
                final var stat = CompatLayer.convertIndSupplyStat(industry, comID);
                if (stat.getModifiedValue() > 0f) supplyList.put(comID, stat);
            }
        }

        for (String comID : inputs) {
            final var stat = CompatLayer.convertIndDemandStat(industry, comID);
            if (stat.getModifiedValue() > 0f) demandList.put(comID, stat);
        }

        TooltipMakerAPI tp = ComponentFactory.createTooltip((PANEL_W / 2) - opad, false);
        tp.addSectionHeading(importing ? "---" : "Production", color, dark, Alignment.MID, opad);
        final float startY = tp.getHeightSoFar() + pad;

        // Supply
        float x = opad;
        float y = startY;
        int count = -1;

        for (Map.Entry<String, MutableStat> entry : supplyList.singleEntrySet()) {
            final CommoditySpecAPI com = settings.getCommoditySpec(entry.getKey());
            final long pAmount = entry.getValue().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + hpad;
            }

            // draw icon
            tp.beginIconGroup();
            tp.setIconSpacingMedium();
            tp.addIcons(com, 1, IconRenderMode.NORMAL);
            tp.addIconGroup(0f);
            final UIComponentAPI iconComp = tp.getPrev();

            // Add extra padding for thinner icons
            final float actualIconWidth = iconSize * com.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            final String txt = Strings.X + NumFormat.engNotate(pAmount);
            final LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

            final float textH = lbl.computeTextHeight(txt);
            final float textX = x + iconSize + pad;
            final float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + hpad;
        }
        tp.setHeightSoFar(y);
        ComponentFactory.addTooltip(tp, PANEL_H, false, panel).inTL(hpad, 0);

        tp = ComponentFactory.createTooltip((PANEL_W / 2) - opad, false);
        tp.addSectionHeading(importing ? "Import" : "Demand", color, dark, Alignment.MID, opad);

        // Demand
        x = opad;
        y = startY;
        count = -1;

        for (Map.Entry<String, MutableStat> entry : demandList.singleEntrySet()) {
            final CommodityCell cell = engine.getComCell(entry.getKey(), market.getId());
            final long dAmount = entry.getValue().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + hpad;
            }
            final float availability = cell.getStoredAvailabilityRatio();

            // draw icon
            tp.beginIconGroup();
            tp.setIconSpacingMedium();
            final IconRenderMode renderMode = availability < 0.9f && !importing ?
                IconRenderMode.DIM_RED : IconRenderMode.NORMAL;
            tp.addIcons(cell.spec, 1, renderMode);
            tp.addIconGroup(0f);
            final UIComponentAPI iconComp = tp.getPrev();

            // Add extra padding for thinner icons
            final float actualIconWidth = iconSize * cell.spec.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            final String txt = Strings.X + NumFormat.engNotate(dAmount);
            final LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

            final float textH = lbl.computeTextHeight(txt);
            final float textX = x + iconSize + pad;
            final float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + hpad;
        }
        tp.setHeightSoFar(y);
        ComponentFactory.addTooltip(tp, PANEL_H, false, panel).inTR(hpad, 0);

    }

    private final float getNewFreeWorkerRatio() {
        return initialFreeWorkerRatio + (data.getWorkerAssignedRatio(false) -
            previewData.getWorkerAssignedRatio(false));
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        boolean update = false;

        for (Map.Entry<String, Slider> entry : outputSliders.singleEntrySet()) {
            final String comID = entry.getKey();
            final Slider slider = entry.getValue();

            final float sliderValue = slider.getProgress() / 100f;

            if (previewData.getAssignedRatioForOutput(comID) != sliderValue) {
                previewData.setRatioForOutput(comID, sliderValue);
                update = true;
            }

            final float max = Math.max(0,
                sliderValue + getNewFreeWorkerRatio()
            );

            slider.maxValue = Math.min(
                max,
                IndustryConfigManager.getIndConfig(industry).outputs.get(comID).workerAssignableLimit
            ) * 100;
        }

        if (update) {
            inputOutputContainer.clearChildren();
            drawProductionAndConsumption(inputOutputContainer.getPanel());
        }
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 1) reg.setData(data);

        market.reapplyConditions();
        LtvIndustryListPanel.refreshPanel();
    }
}
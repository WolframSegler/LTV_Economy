package wfg.ltv_econ.ui.dialogs;

import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.IconRenderMode;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.panels.LtvIndustryListPanel;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.DialogPanel;
import wfg.wrap_ui.ui.panels.BasePanel;
import wfg.wrap_ui.ui.panels.Slider;
import wfg.wrap_ui.ui.panels.SpritePanelWithTp;
import wfg.wrap_ui.ui.plugins.BasePanelPlugin;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class AssignWorkersDialog extends DialogPanel {

    public static final String WARNING_BUTTON_PATH = Global.getSettings()
        .getSpriteName("ui", "warning_button");
    public static final int panelWidth = 540;
    public static final int panelHeight = 400;

    public final WorkerRegistry reg;
    public final Industry industry;
    public final MarketAPI market;
    public final WorkerIndustryData data;
    public final WorkerIndustryData previewData;
    public final Map<String, Slider> outputSliders;

    public BasePanel inputOutputContainer;

    private final float initialFreeWorkerRatio;

    public AssignWorkersDialog(Industry ind) {
        super(Attachments.getScreenPanel(), panelWidth, panelHeight, null, null, "Confirm", "Cancel");
        setConfirmShortcut();

        reg = WorkerRegistry.getInstance();
        industry = ind;
        market = ind.getMarket();
        data = reg.getData(ind);
        previewData = new WorkerIndustryData(data);
        outputSliders = new HashMap<>();

        reg.setData(previewData);
        initialFreeWorkerRatio = WorkerPoolCondition.getPoolCondition(market).getFreeWorkerRatio();

        getHolo().setBackgroundAlpha(1, 1);

        createPanel();
    }

    @Override
    public void createPanel() {
        UIState.setState(State.DIALOG);

        final int sliderHeight = 32;
        final int sliderWidth = 380;
        final int sliderY = 225;
        final int iconSize = 26;

        // Draw Titel
        final String txt = "Assign workers to " + industry.getCurrentName();
        final LabelAPI lbl = Global.getSettings().createLabel(txt, Fonts.ORBITRON_20AA);

        final float textX = (panelWidth - lbl.computeTextWidth(txt)) / 2;
        innerPanel.addComponent((UIComponentAPI)lbl).inTL(textX, pad*2);

        inputOutputContainer = new BasePanel(
            innerPanel, (int) innerPanel.getPosition().getWidth(),
            180, new BasePanelPlugin<>()
        ) {
            @Override
            public float getBgAlpha() {
                return 0f;
            }
        };

        // Draw Production
        drawProductionAndConsumption(inputOutputContainer.getPanel());

        innerPanel.addComponent(inputOutputContainer.getPanel())
            .inTL(0, lbl.computeTextHeight(txt) + opad);

        // Draw separator line
        final BasePanel separator = new BasePanel(
            innerPanel, panelWidth, 1, new BasePanelPlugin<>()
        ) {
            @Override
            public Color getBgColor() {
                return new Color(100, 100, 100);
            }
        };
        separator.getPos().inTL(0, sliderY - opad);
        innerPanel.addComponent(separator.getPanel());

        final SpritePanelWithTp help_button = new SpritePanelWithTp(innerPanel, 20 , 20,
            WARNING_BUTTON_PATH, null, null
        ) {
            {
                getPlugin().setIgnoreUIState(true);
            }

            @Override
            public UIPanelAPI getTpParent() {
                return Attachments.getScreenPanel();
            }

            @Override  
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = ComponentFactory.createTooltip(300f, false);

                tp.addPara(
                    "Adjust each output's slider to allocate a portion of the market's total workforce. " +
                    "The values represent the percentage of available workers assigned to that output.",
                    pad
                );

                ComponentFactory.addTooltip(tp, 0f, false);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.TopLeft, 0);

                return tp;
            }
        
            @Override 
            public Glow getGlowType() {
                return Glow.ADDITIVE;
            }

            @Override 
            public Optional<SpriteAPI> getAdditiveSprite() {
                return Optional.of(m_sprite);
            }
        };

        innerPanel.addComponent(help_button.getPanel()).inTR(pad, sliderY + pad);

        final CustomPanelAPI outputsPanel = Global.getSettings().createCustom(
            panelWidth,
            panelHeight - (sliderY + pad * 2),
            null
        );
        final TooltipMakerAPI outputsTp = ComponentFactory.createTooltip(panelWidth, true);

        final SettingsAPI settings = Global.getSettings();
        
        int cumulativeYOffset = pad;
        for (String comID : data.getRegisteredOutputs()) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(comID);
            outputsTp.addImage(spec.getIconName(), iconSize, iconSize, pad);
            outputsTp.getPrev().getPosition().inTL(pad, cumulativeYOffset);

            final Slider outputSlider = new Slider(
                innerPanel, null, 0, 100, sliderWidth, sliderHeight
            );
            outputSliders.put(comID, outputSlider);

            // Configure the slider
            outputSlider.setHighlightOnMouseover(true);
            outputSlider.setUserAdjustable(true);
            outputSlider.setBarColor(new Color(20, 125, 200));
            outputSlider.showValueOnly = true;
            outputSlider.roundBarValue = true;
            outputSlider.clampCurrToMax = true;
            outputSlider.roundingIncrement = 1;

            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);
            pool.recalculateWorkerPool();

            final float max = Math.max(0,
                data.getAssignedRatioForOutput(comID) + pool.getFreeWorkerRatio()
            );
            
            outputSlider.maxValue = Math.min(
                max,
                IndustryIOs.getIndConfig(industry).outputs.get(comID).workerAssignableLimit
            ) * 100;

            outputSlider.setProgress(data.getAssignedRatioForOutput(comID) * 100);

            outputsTp.addComponent(outputSlider.getPanel()).inTL(iconSize + pad*2, cumulativeYOffset);
            cumulativeYOffset += pad + sliderHeight;
        }

        outputsTp.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(outputsTp, 180, true, outputsPanel).inTL(-pad, 0);
        innerPanel.addComponent(outputsPanel).inTL(opad, sliderY);
    }

    public void drawProductionAndConsumption(CustomPanelAPI panel) {
        final float iconSize = 32f;
        final int itemsPerRow = 2;
        final float sectionWidth = ((panelWidth / 2) / itemsPerRow) - opad;

        final SettingsAPI settings = Global.getSettings();
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

        TooltipMakerAPI tp = ComponentFactory.createTooltip((panelWidth / 2) - opad, false);
        tp.addSectionHeading(importing ? "---" : "Production", color, dark, Alignment.MID, opad);
        final float startY = tp.getHeightSoFar() + pad;

        // Supply
        float x = opad;
        float y = startY;
        int count = -1;

        for (Map.Entry<String, MutableStat> entry : supplyList.entrySet()) {
            final CommoditySpecAPI com = settings.getCommoditySpec(entry.getKey());
            final long pAmount = entry.getValue().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }

            // draw icon
            tp.beginIconGroup();
            tp.setIconSpacingMedium();
            tp.addIcons(com, 1, IconRenderMode.NORMAL);
            tp.addIconGroup(0f);
            UIComponentAPI iconComp = tp.getPrev();

            // Add extra padding for thinner icons
            float actualIconWidth = iconSize * com.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            String txt = Strings.X + NumFormat.engNotation(pAmount);
            LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

            float textH = lbl.computeTextHeight(txt);
            float textX = x + iconSize + pad;
            float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tp.setHeightSoFar(y);
        ComponentFactory.addTooltip(tp, panelHeight, false, panel).inTL(opad / 2, 0);

        tp = ComponentFactory.createTooltip((panelWidth / 2) - opad, false);
        tp.addSectionHeading(importing ? "Import" : "Demand", color, dark, Alignment.MID, opad);

        // Demand
        x = opad;
        y = startY;
        count = -1;

        for (Map.Entry<String, MutableStat> entry : demandList.entrySet()) {
            final CommodityCell cell = engine.getComCell(entry.getKey(), market.getId());
            final long dAmount = entry.getValue().getModifiedInt();

            // wrap to next line if needed
            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + 5f; // line height + padding between rows
            }
            final float availability = cell.getStoredAvailabilityRatio();

            // draw icon
            tp.beginIconGroup();
            tp.setIconSpacingMedium();
            IconRenderMode renderMode = availability < 0.9f && !importing ?
                IconRenderMode.DIM_RED : IconRenderMode.NORMAL;
            tp.addIcons(cell.spec, 1, renderMode);
            tp.addIconGroup(0f);
            final UIComponentAPI iconComp = tp.getPrev();

            // Add extra padding for thinner icons
            final float actualIconWidth = iconSize * cell.spec.getIconWidthMult();
            iconComp.getPosition().inTL(x + ((iconSize - actualIconWidth) * 0.5f), y);

            // draw text
            final String txt = Strings.X + NumFormat.engNotation(dAmount);
            final LabelAPI lbl = tp.addPara(txt + " / Day", 0f, highlight, txt);

            final float textH = lbl.computeTextHeight(txt);
            final float textX = x + iconSize + pad;
            final float textY = y + (iconSize - textH) * 0.5f;
            lbl.getPosition().inTL(textX, textY);

            // advance X
            x += sectionWidth + 5f;
        }
        tp.setHeightSoFar(y);
        ComponentFactory.addTooltip(tp, panelHeight, false, panel).inTR(opad / 2, 0);

    }

    private final float getNewFreeWorkerRatio() {
        return initialFreeWorkerRatio + (data.getWorkerAssignedRatio(false) -
            previewData.getWorkerAssignedRatio(false));
    }

    @Override
    public void advanceImpl(float amount) {
        super.advanceImpl(amount);

        boolean update = false;

        for (Map.Entry<String, Slider> entry : outputSliders.entrySet()) {
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
                IndustryIOs.getIndConfig(industry).outputs.get(comID).workerAssignableLimit
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

        UIState.setState(State.NONE);
        LtvIndustryListPanel.refreshPanel();
    }
}
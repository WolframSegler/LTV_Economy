package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constant.UIColors.SLIDER_BASE;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry.WorkerPool;
import wfg.ltv_econ.economy.registry.WorkerRegistry.IndustryWorkerData;
import wfg.ltv_econ.industry.CompatLayer;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.native_ui.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.GlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.table.ListTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.container.BaseContainer;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.visual.InteractiveSprite;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public final class AssignWorkersDialog extends DialogPanel {
    private static final SpriteAPI WARNING_BUTTON = settings.getSprite("ui", "warning_button");
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 400;

    private final Industry industry;
    private final MarketAPI market;
    private final IndustryWorkerData data;
    private final IndustryWorkerData previewData;
    private final ArrayMap<String, Slider> outputSliders;

    private BaseContainer inputOutputContainer;

    private final float initialFreeWorkerRatio;

    public AssignWorkersDialog(Industry ind) {
        super(PANEL_W, PANEL_H, null, null, str("uiConfirm"), str("uiCancel"));
        setConfirmShortcut();

        final WorkerRegistry reg = WorkerRegistry.instance();
        industry = ind;
        market = ind.getMarket();
        data = reg.getRegisterData(ind);
        previewData = new IndustryWorkerData(data);
        outputSliders = new ArrayMap<>(IndustryConfigManager.getIndConfig(industry).outputs.size());

        initialFreeWorkerRatio = WorkerPoolRegistry.get(market).getFreeWorkerRatio();

        mHolo.borderAlpha = 0.7f;
        backgroundDimAmount = 0.2f;

        buildUI();
    }

    @Override
    public void buildUI() {
        
        final int sliderY = 225;

        final String txt = strf("uiTitleAssignWorkersTo", industry.getCurrentName());
        final LabelAPI titleLbl = settings.createLabel(txt, Fonts.ORBITRON_20AA);
        titleLbl.setAlignment(Alignment.MID);
        titleLbl.autoSizeToWidth(PANEL_W);
        add(titleLbl).inTL(0f, pad*2);

        inputOutputContainer = new BaseContainer(PANEL_W, 180) {{ bg.alpha = 0f;}};

        drawProductionAndConsumption(inputOutputContainer);

        add(inputOutputContainer).inTL(0, titleLbl.computeTextHeight(txt) + opad);

        final BaseContainer separator = new BaseContainer(
            PANEL_W, 1
        ) {{ bg.color = gray;}};
        
        separator.pos().inTL(0, sliderY - opad);
        add(separator);

        final InteractiveSprite help_button = new InteractiveSprite(20 , 20,
            WARNING_BUTTON, null, null
        ) {{
            tooltip.builder = (tp, exp) -> {
                tp.addPara(str("uiDialogTpTxtAssignWorkers"), pad);
            };
            tooltip.positioner = (tp, exp) -> {
                NativeUiUtils.anchorPanelWithBounds(tp, this, AnchorType.TopLeft, 0);
            };

            glow.type = GlowType.ADDITIVE;
            glow.additiveSprite = mSprite;
        }};

        add(help_button).inTR(pad, sliderY + pad);

        final float listWidth = PANEL_W - opad*2;
        add(new OutputList(listWidth, 180f)).inTL(opad, sliderY);
    }

    public void drawProductionAndConsumption(UIPanelAPI panel) {
        final FactionAPI faction = market.getFaction();
        final Color color = faction.getBaseUIColor();
        final Color dark = faction.getDarkUIColor();

        final Map<String, Float> outputs = IndustryIOs.getRealOutputs(industry, previewData, false, false);
        final Set<String> inputs = IndustryIOs.getRealInputs(industry, false);

        final ArrayMap<String, MutableStat> supplyList = new ArrayMap<>(outputs.size());
        final ArrayMap<String, MutableStat> demandList = new ArrayMap<>(inputs.size());

        final boolean importing = IndustryConfigManager.getIndConfig(industry).demandOnly;

        if (!importing) {
            for (String comID : outputs.keySet()) {
                final var stat = CompatLayer.convertIndSupplyStat(industry, comID, previewData, false);
                if (stat.getModifiedValue() > 0f) supplyList.put(comID, stat);
            }
        }

        for (String comID : inputs) {
            final var stat = CompatLayer.convertIndDemandStat(industry, comID, previewData, false);
            if (stat.getModifiedValue() > 0f) demandList.put(comID, stat);
        }

        final float sectionHeadW = (PANEL_W / 2) - opad;

        final UIContainer supplySection = new UIContainer(sectionHeadW, 0f);
        final LabelAPI supplyHead = createSectionLabel(
            importing ? "---" : str("uiTableProductionTitle"), color, dark, sectionHeadW);
        supplySection.add(supplyHead).inTL(0f, 0f);

        final float startY = supplyHead.getPosition().getHeight() + hpad;
        final float supplyBottom = layoutSectionRows(supplySection, supplyList, startY);
        supplySection.setHeight(supplyBottom);
        supplySection.attachTo(panel).inTL(hpad, 0);

        final UIContainer demandSection = new UIContainer(sectionHeadW, 0f);
        final LabelAPI demandHead = createSectionLabel(
            importing ? str("uiTableImport") : str("uiTableDemandTitle"), color, dark, sectionHeadW);
        demandSection.add(demandHead).inTL(0f, 0f);

        final float demandBottom = layoutSectionRows(demandSection, demandList, startY);
        demandSection.setHeight(demandBottom);
        demandSection.attachTo(panel).inTR(hpad, 0);
    }

    private static final LabelAPI createSectionLabel(String text, Color color, Color dark, float width) {
        final LabelAPI head = settings.createLabel(text, Fonts.ORBITRON_12);
        head.setColor(color);
        head.setAlignment(Alignment.MID);
        final int headLblH = (int) head.getPosition().getHeight() + 3;
        head.getPosition().setSize(width, headLblH);
        head.setBackgroundColor(dark);
        return head;
    }

    private static final float layoutSectionRows(
        UIContainer section, ArrayMap<String, MutableStat> items, float startY
    ) {
        final int itemsPerRow = 2;
        final float sectionW = ((PANEL_W / 2) / itemsPerRow) - opad;
        final float iconSize = 32f;

        float x = opad;
        float y = startY;
        int count = -1;

        for (Map.Entry<String, MutableStat> entry : items.singleEntrySet()) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(entry.getKey());
            final long amount = entry.getValue().getModifiedInt();

            count++;
            if (count % itemsPerRow == 0 && count != 0) {
                x = opad;
                y += iconSize + hpad;
            }

            final float actualIconW = iconSize * spec.getIconWidthMult();
            final SpriteElement icon = new SpriteElement(actualIconW, iconSize, spec.getIconName(), null, null);
            section.add(icon).inTL(x + (iconSize - actualIconW) * 0.5f, y);

            final String txt = Strings.X + NumFormat.engNotate(amount);
            final LabelAPI lbl = settings.createLabel(
                txt + str("industryPerDayProductionSuffix"), Fonts.DEFAULT_SMALL);
            lbl.setColor(Global.getSettings().getColor("standardTextColor"));
            lbl.setHighlightColor(highlight);
            lbl.setHighlight(txt);

            final float textH = lbl.computeTextHeight(txt);
            section.add(lbl).inTL(x + iconSize + pad, y + (iconSize - textH) * 0.5f);

            x += sectionW + hpad;
        }

        return y + iconSize;
    }

    private final float getNewFreeWorkerRatio() {
        return initialFreeWorkerRatio + (data.getWorkerAssignedRatio(false) -
            previewData.getWorkerAssignedRatio(false));
    }

    @Override
    public void advanceImpl(float amount) {
        super.advanceImpl(amount);

        boolean update = false;

        for (Map.Entry<String, Slider> entry : outputSliders.singleEntrySet()) {
            final String comID = entry.getKey();
            final Slider slider = entry.getValue();

            final float sliderValue = slider.getProgress() / 100f;

            if (previewData.getAssignedRatioForOutput(comID) != sliderValue) {
                previewData.setRatioForOutput(comID, sliderValue);
                update = true;
            }

            slider.maxValue = Arithmetic.clamp(sliderValue + getNewFreeWorkerRatio(), 0f,
                IndustryConfigManager.getIndConfig(industry).outputs.get(comID).workerAssignableLimit
            ) * 100;
        }

        if (update) {
            inputOutputContainer.clearChildren();
            drawProductionAndConsumption(inputOutputContainer);
        }
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) WorkerRegistry.instance().setData(previewData);

        WorkerPoolRegistry.get(market).recalculate();
        LtvIndustryListPanel.refreshPanel();
    }

    private final class OutputList extends ListTable<OutputConfig, OutputRow> {
        private static final int SLIDER_W = 380;
        private static final int SLIDER_H = 32;

        public OutputList(float width, float height) {
            super(width, height, SLIDER_H, pad, pad);
            mIsSelectionEnabled = false;
            mIsHighlightingEnabled = false;
            buildUI();
        }

        @Override
        protected List<OutputConfig> getDataList() {
            final List<OutputConfig> result = new ArrayList<>();
            for (OutputConfig o : IndustryConfigManager.getIndConfig(industry).outputs.values()) {
                if (o.usesWorkers) result.add(o);
            }
            return result;
        }

        @Override
        protected OutputRow createWidget(OutputConfig output, int index) {
            final Slider s = new Slider(null, 0, 100, SLIDER_W, SLIDER_H);
            outputSliders.put(output.comID, s);

            s.setHighlightOnMouseover(true);
            s.setBarColor(SLIDER_BASE);
            s.showValueOnly = true;
            s.roundBarValue = true;
            s.clampCurrToMax = true;
            s.roundingIncrement = 1;

            final WorkerPool pool = WorkerPoolRegistry.get(market);
            pool.recalculate();

            final float max = Math.max(0f,
                data.getAssignedRatioForOutput(output.comID) + pool.getFreeWorkerRatio()
            );
            s.maxValue = Math.min(max, output.workerAssignableLimit) * 100;
            s.setProgress(data.getAssignedRatioForOutput(output.comID) * 100);

            return new OutputRow(settings.getCommoditySpec(output.comID), s);
        }

        protected void onWidgetClicked(OutputRow source, boolean isLeft, boolean isKeyboard) {}
        protected String getEmptyMessage() { return null; }
    }

    private static final class OutputRow extends UIContainer implements WidgetAPI<OutputRow> {
        private static final int ICON_SIZE = 26;

        public final SpriteElement mSprite;
        public final Slider mSlider;

        public OutputRow(CommoditySpecAPI spec, Slider slider) {
            mSprite = new SpriteElement(ICON_SIZE, ICON_SIZE, spec.getIconName(), null, null);
            mSlider = slider;
        }

        @Override public InteractionComp<OutputRow> getInteraction() { return null; }

        @Override
        public void buildUI() {
            clearChildren();
            add(mSprite).inLMid(0f);
            add(mSlider).rightOfMid(mSprite, pad);
        }
    }
}
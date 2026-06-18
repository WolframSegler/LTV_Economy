package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.ltv_econ.constant.CommoditiesID.*;
import static wfg.ltv_econ.constant.Sprites.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.ArrayList;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.ui.marketInfo.LtvIndustryListPanel;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.util.Arithmetic;

public class ServiceSectorDialog extends DialogPanel {
    private static final int PANEL_W = 700;
    private static final int PANEL_H = 500;
    private static final Color MED_GREEN = new Color(1, 110, 52);

    public static final float LOGISTICS_LIM = 0.1f;
    public static final float HEALTHCARE_LIM = 0.2f;
    public static final float SECURITY_LIM = 0.05f;
    public static final float PUBLIC_INFO_LIM = 0.1f;
    public static final float CULTURE_LIM = 0.1f;

    private final MarketAPI market;
    private final WorkerIndustryData data;
    private final WorkerIndustryData previewData;
    private final ArrayList<SectorCard> sectorCards = new ArrayList<>(5);

    private final float initialFreeWorkerRatio;

    public ServiceSectorDialog(Industry ind) {
        super(PANEL_W, PANEL_H, null, null, str("uiConfirm"), str("uiCancel"));
        setConfirmShortcut();

        final WorkerRegistry reg = WorkerRegistry.instance();
        market = ind.getMarket();
        data = reg.getRegisterData(ind);
        previewData = new WorkerIndustryData(data);

        reg.setData(previewData);
        initialFreeWorkerRatio = WorkerPoolRegistry.get(market).getFreeWorkerRatio();

        holo.borderAlpha = 0.7f;
        backgroundDimAmount = 0.2f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final LabelAPI titleLbl = settings.createLabel(str("uiPublicServicesTitle"), Fonts.ORBITRON_20AA);
        titleLbl.setAlignment(Alignment.MID);
        titleLbl.autoSizeToWidth(PANEL_W);
        add(titleLbl).inTL(0f, pad*2);

        final TooltipMakerAPI sectorsCont = ComponentFactory.createTooltip(PANEL_W, true);

        final float workerPool = WorkerPoolRegistry.get(market).getWorkerPool();
        
        int cumulativeYOffset = opad;
        { // Sectors
            final SectorCard logistics = new SectorCard(sectorsCont, SERVICE_LOGISTICS, LOGISTICS_LIM, LOGISTICS,
                str("uiLogisticsTitle"), (slider) -> {
                    final String eff1 = Integer.toString(Math.round(slider.getProgress()));
                    final String max1 = Integer.toString(Math.round(LOGISTICS_LIM * 100f));
                    final String txt = strf("uiLogisticsTpTxt", eff1, max1);

                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base);
                    lbl.setHighlight(eff1, max1);

                    return lbl;
                }, null
            );
            final SectorCard healthcare = new SectorCard(sectorsCont, SERVICE_HEALTHCARE, HEALTHCARE_LIM, HEALTHCARE,
                str("uiHealthcareTitle"), (slider) -> {
                    final String eff1 = Integer.toString((int) (slider.getProgress() / 5f));
                    final String max1 = Integer.toString(Math.round(HEALTHCARE_LIM * 20f));
                    final String eff2 = String.format("%.2f", slider.getProgress() / 200f);
                    final String max2 = String.format("%.2f", HEALTHCARE_LIM / 2f);
                    final String txt = strf("uiHealthcareTpTxt", eff1, max1, eff2, max2);

                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

                    return lbl;
                }, MED_GREEN
            );
            final SectorCard security = new SectorCard(sectorsCont, SERVICE_SECURITY, SECURITY_LIM, SECURITY,
                str("uiSecurityTitle"), (slider) -> {
                    final String eff1 = String.format("%.3f", slider.getProgress() / 1000f);
                    final String max1 = String.format("%.3f", SECURITY_LIM / 10f);
                    final String eff2 = Integer.toString((int) (slider.getProgress()));
                    final String max2 = Integer.toString(Math.round(SECURITY_LIM * 100f));
                    final String txt = strf("uiSecurityTpTxt", eff1, max1, eff2, max2);

                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

                    return lbl;
                }, grid
            );
            final SectorCard publicInfo = new SectorCard(sectorsCont, SERVICE_PUBLIC_INFO, PUBLIC_INFO_LIM, PUBLIC_INFO,
                str("uiPublicInfoTitle"), (slider) -> {
                    final String eff1 = String.format("%.3f", slider.getProgress() / 200f);
                    final String max1 = String.format("%.3f", PUBLIC_INFO_LIM / 2f);
                    final String eff2 = Integer.toString((int) (slider.getProgress()));
                    final String max2 = Integer.toString(Math.round(PUBLIC_INFO_LIM * 100f));
                    final String txt = strf("uiPublicInfoTpTxt", eff1, max1, eff2, max2);
                    
                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

                    return lbl;
                }, grid
            );
            final SectorCard culture = new SectorCard(sectorsCont, SERVICE_CULTURE, CULTURE_LIM, CULTURE,
                str("uiCultureTitle"), (slider) -> {
                    final String eff1 = String.format("%.3f", slider.getProgress() / 400f);
                    final String max1 = String.format("%.3f", CULTURE_LIM / 4f);
                    final String eff2 = String.format("%.2f", slider.getProgress() / 50f);
                    final String max2 = String.format("%.2f", CULTURE_LIM * 2f);
                    final String eff3 = String.format("%.0f", slider.getProgress() * workerPool * LaborConfig.LPV_month * 0.5f / 100f);
                    final String max3 = String.format("%.0f", CULTURE_LIM * workerPool * LaborConfig.LPV_month * 0.5f);
                    final String txt = strf("uiCultureTpTxt", eff1, max1, eff2, max2, eff3, max3);

                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2, eff3, max3);

                    return lbl;
                }, grid
            );

            sectorsCont.addComponent(logistics.getPanel()).inTL(SectorCard.borderMargin, cumulativeYOffset);
            cumulativeYOffset += SectorCard.borderMargin*2 + SectorCard.CARD_H + hpad;
            sectorsCont.addComponent(healthcare.getPanel()).inTL(SectorCard.borderMargin, cumulativeYOffset);
            cumulativeYOffset += SectorCard.borderMargin*2 + SectorCard.CARD_H + hpad;
            sectorsCont.addComponent(security.getPanel()).inTL(SectorCard.borderMargin, cumulativeYOffset);
            cumulativeYOffset += SectorCard.borderMargin*2 + SectorCard.CARD_H + hpad;
            sectorsCont.addComponent(publicInfo.getPanel()).inTL(SectorCard.borderMargin, cumulativeYOffset);
            cumulativeYOffset += SectorCard.borderMargin*2 + SectorCard.CARD_H + hpad;
            sectorsCont.addComponent(culture.getPanel()).inTL(SectorCard.borderMargin, cumulativeYOffset);
            cumulativeYOffset += SectorCard.borderMargin*2 + SectorCard.CARD_H + hpad;

            logistics.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_LOGISTICS) * 100f);
            healthcare.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_HEALTHCARE) * 100f);
            security.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_SECURITY) * 100f);
            publicInfo.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_PUBLIC_INFO) * 100f);
            culture.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_CULTURE) * 100f);

            logistics.buildUI();
            healthcare.buildUI();
            security.buildUI();
            publicInfo.buildUI();
            culture.buildUI();

            sectorCards.add(logistics);
            sectorCards.add(healthcare);
            sectorCards.add(security);
            sectorCards.add(publicInfo);
            sectorCards.add(culture);
        }

        sectorsCont.setHeightSoFar(cumulativeYOffset);
        ComponentFactory.addTooltip(sectorsCont, PANEL_H - 20 - BUTTON_H - opad, true, m_panel).inBL(0f, BUTTON_H + opad);
    }

    private final float getNewFreeWorkerRatio() {
        return initialFreeWorkerRatio + (data.getWorkerAssignedRatio(false) -
            previewData.getWorkerAssignedRatio(false));
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        for (SectorCard card : sectorCards) {
            final String sectorID = card.sectorID;
            final Slider slider = card.slider;

            final float sliderValue = slider.getProgress() / 100f;

            if (previewData.getAssignedRatioForOutput(sectorID) != sliderValue) {
                previewData.setRatioForOutput(sectorID, sliderValue);
                card.buildUI();
            }

            slider.maxValue = Arithmetic.clamp(sliderValue + getNewFreeWorkerRatio(), 0f, card.workerLimit) * 100;
        }
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        if (option == 0) {
            EconomyEngine.instance().applyServiceSectorEffectsToMarket(market);
        } else {
            WorkerRegistry.instance().setData(data);
        }

        market.reapplyConditions();
        LtvIndustryListPanel.refreshPanel();
    }

    private static class SectorCard extends CustomPanel implements UIBuildableAPI {
        private static final int CARD_W = PANEL_W - opad*4;
        private static final int CARD_H = 150;
        private static final int borderMargin = opad;
        private static final Color borderBgColor = new Color(0, 0, 0, 150);
        
        private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, false);
        private final SpriteAPI icon;
        private final String title;
        private final LabelSupplier<Slider> desc;
        private final Color iconColor;

        public final String sectorID;
        public final float workerLimit;
        public final Slider slider;

        public SectorCard(UIPanelAPI parent, String sectorID, float limit, SpriteAPI icon, String title,
            LabelSupplier<Slider> desc, Color iconColor
        ) {
            super(parent, CARD_W, CARD_H);

            border.setSize(CARD_W + borderMargin*2, CARD_H + borderMargin*2);
            border.centerColor = borderBgColor;

            this.sectorID = sectorID;
            this.workerLimit = limit;
            this.icon = icon;
            this.title = title;
            this.desc = desc;
            this.iconColor = iconColor;

            slider = new Slider(m_panel, null, 0, limit * 100, 200, 16);
            slider.setHighlightOnMouseover(true);
            slider.setBarColor(UIColors.SLIDER_BASE);
            slider.showNoText = true;
            slider.roundBarValue = true;
            slider.clampCurrToMax = true;
            slider.roundingIncrement = 1;
        }

        @Override
        public void buildUI() {
            clearChildren();
            final int iconS = 32;

            final Base iconElement = new Base(m_panel, iconS, iconS, icon, iconColor, null);
            add(iconElement).inTL(0f, 0f);

            final LabelAPI titleLbl = settings.createLabel(title, Fonts.INSIGNIA_LARGE);
            add(titleLbl).inTL(iconS + hpad, 0f).setSize(400f, iconS);

            final String valueStr = Integer.toString((int) slider.getProgress()) + "%";
            final LabelAPI valueLbl = settings.createLabel(valueStr, Fonts.INSIGNIA_LARGE);
            valueLbl.setAlignment(Alignment.RMID);
            add(valueLbl).inTR(0f, 0f).setSize(50f, iconS);

            add(slider).inTR(50f, ((iconS - 16) / 2) + 1);

            final int SECOND_ROW = 45;

            final LabelAPI descLbl = desc.getLabel(slider);
            descLbl.setAlignment(Alignment.TL);
            add(descLbl).inTL(0f, SECOND_ROW).setSize(CARD_W, CARD_H - SECOND_ROW);
        }

        @Override
        public void renderBelow(float alpha) {
            super.renderBelow(alpha);

            border.render(pos.getX() - borderMargin, pos.getY() - borderMargin, alpha);
        }
    }

    @FunctionalInterface
    private interface LabelSupplier<T> {
        LabelAPI getLabel(T slider);
    }
}
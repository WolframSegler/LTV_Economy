package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.ltv_econ.constants.CommoditiesID.*;
import static wfg.ltv_econ.constants.Sprites.*;

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

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.engine.EconomyEngine;
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

    private final MarketAPI market;
    private final WorkerIndustryData data;
    private final WorkerIndustryData previewData;
    private final ArrayList<SectorCard> sectorCards = new ArrayList<>(5);

    private final float initialFreeWorkerRatio;

    public ServiceSectorDialog(Industry ind) {
        super(PANEL_W, PANEL_H, null, null, "Confirm", "Cancel");
        setConfirmShortcut();

        final WorkerRegistry reg = WorkerRegistry.instance();
        market = ind.getMarket();
        data = reg.getRegisterData(ind);
        previewData = new WorkerIndustryData(data);

        reg.setData(previewData);
        initialFreeWorkerRatio = WorkerPoolCondition.getPoolCondition(market).getFreeWorkerRatio();

        holo.borderAlpha = 0.7f;
        backgroundDimAmount = 0.2f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final LabelAPI titleLbl = settings.createLabel("Manage Public Services", Fonts.ORBITRON_20AA);
        titleLbl.setAlignment(Alignment.MID);
        titleLbl.autoSizeToWidth(PANEL_W);
        add(titleLbl).inTL(0f, pad*2);

        final TooltipMakerAPI sectorsCont = ComponentFactory.createTooltip(PANEL_W, true);
        
        int cumulativeYOffset = opad;
        { // Sectors
            final float logisticsLim = 0.1f;
            final float healthcareLim = 0.2f;
            final float securityLim = 0.05f;
            final float publicInfoLim = 0.1f;

            final SectorCard logistics = new SectorCard(sectorsCont, SERVICE_LOGISTICS, logisticsLim, LOGISTICS,
                "Logistics", (slider) -> {
                    final String eff1 = Integer.toString(Math.round(slider.getProgress()));
                    final String max1 = Integer.toString(Math.round(logisticsLim * 100f));
                    final String txt = String.format("Tasked with the efficient allocation and transportation of resources, " +
                        "the logistics sector reduces overall resource consumption by %s%% (max %s%%).", eff1, max1
                    );
                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base);
                    lbl.setHighlight(eff1, max1);

                    return lbl;
                }, null
            );
            final SectorCard healthcare = new SectorCard(sectorsCont, SERVICE_HEALTHCARE, healthcareLim, HEALTHCARE,
                "Healthcare", (slider) -> {
                    final String eff1 = Integer.toString((int) (slider.getProgress() / 5f));
                    final String max1 = Integer.toString(Math.round(healthcareLim * 20f));
                    final String eff2 = String.format("%.2f", slider.getProgress() / 200f);
                    final String max2 = String.format("%.2f", healthcareLim / 2f);
                    final String txt = String.format("The healthcare sector increases life expectancy, which contributes %s (max %s) growth points. " +
                        "The health of the population is also increased by %s (max %s) per-day.", eff1, max1, eff2, max2
                    );
                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

                    return lbl;
                }, MED_GREEN
            );
            final SectorCard security = new SectorCard(sectorsCont, SERVICE_SECURITY, securityLim, SECURITY,
                "Security", (slider) -> {
                    final String eff1 = String.format("%.3f", slider.getProgress() / 1000f);
                    final String max1 = String.format("%.3f", securityLim / 10f);
                    final String eff2 = Integer.toString((int) (slider.getProgress()));
                    final String max2 = Integer.toString(Math.round(securityLim * 100f));
                    final String txt = String.format("The ministry of security is distinct from COMSEC in that it is tasked with ensuring public order, subduing \"troublemakers\" among the populace and creating a secondary line against invaders. " +
                        "Decreases class consciousness by %s (max %s) and boosts ground defenses by %s (max %s).", eff1, max1, eff2, max2
                    );
                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

                    return lbl;
                }, grid
            );
            final SectorCard publicInfo = new SectorCard(sectorsCont, SERVICE_PUBLIC_INFO, publicInfoLim, PUBLIC_INFO,
                "Public Information", (slider) -> {
                    final String eff1 = String.format("%.3f", slider.getProgress() / 200f);
                    final String max1 = String.format("%.3f", publicInfoLim / 2f);
                    final String eff2 = Integer.toString((int) (slider.getProgress()));
                    final String max2 = Integer.toString(Math.round(publicInfoLim * 100f));
                    final String txt = String.format("The public information department broadcasts daily news and educational programmes. " +
                        "Increases social cohesion by %s (max %s) and accessibility by %s (max %s).", eff1, max1, eff2, max2
                    );
                    final LabelAPI lbl = settings.createLabel(txt, Fonts.DEFAULT_SMALL);
                    lbl.setHighlightColors(highlight, base, highlight, base);
                    lbl.setHighlight(eff1, max1, eff2, max2);

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

            logistics.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_LOGISTICS) * 100f);
            healthcare.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_HEALTHCARE) * 100f);
            security.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_SECURITY) * 100f);
            publicInfo.slider.setProgress(data.getAssignedRatioForOutput(SERVICE_PUBLIC_INFO) * 100f);

            logistics.buildUI();
            healthcare.buildUI();
            security.buildUI();
            publicInfo.buildUI();

            sectorCards.add(logistics);
            sectorCards.add(healthcare);
            sectorCards.add(security);
            sectorCards.add(publicInfo);
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

            add(slider).inTR(50f, (iconS - 16) / 2);

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
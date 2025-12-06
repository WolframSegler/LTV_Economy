package wfg.ltv_econ.ui.dialogs;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.WrapDialogDelegate;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.Slider;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class ManageWorkersDialog implements WrapDialogDelegate {
    public static final int PANEL_W = 950;
    public static final int PANEL_H = 650;

    private final MarketAPI m_market;
    private InteractionDialogAPI interactionDialog;
    private static final Color negativeColor = new Color(210, 115, 90);
    private static final Color positiveColor = new Color(90, 150, 110);
    public static final String WORKER_ICON = Global.getSettings()
        .getSpriteName("ui", "three_workers");
    public static final String HEALTH_ICON = Global.getSettings()
        .getSpriteName("ui", "health");
    public static final String SMILING_ICON = Global.getSettings()
        .getSpriteName("ui", "smiling_face");
    public static final String SOCIETY_ICON = Global.getSettings()
        .getSpriteName("ui", "society");
    public static final String SOLIDARITY_ICON = Global.getSettings()
        .getSpriteName("ui", "solidarity_colored");

    public Slider exploitationSlider = null;

    public ManageWorkersDialog(MarketAPI market) {
        m_market = market;
    }

    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        UIState.setState(State.DIALOG);
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final PlayerMarketData mData = engine.getPlayerMarketData(m_market.getId());
        final WorkerPoolCondition cond = WorkerIndustryData.getPoolCondition(m_market);

        final int SECT_I_H = 30;
        final int SECT_II_H = 185;

        final Color baseColor = m_market.getFaction().getBaseUIColor();
        final Color highlight = Misc.getHighlightColor();
        final Color negative = Misc.getNegativeHighlightColor();
        final int LABEL_W = 140;
        final int LABEL_H = 40;
        final int opad = 10;
        final int pad = 3;
        final int sliderH = 32;
        final int sliderW = 300;
        final int buttonH = 28;
        final int buttonW = 70;
        final int ICON_S = 28;

        final LabelAPI title = settings.createLabel("Manage Workers", Fonts.INSIGNIA_VERY_LARGE);
        title.autoSizeToWidth(PANEL_W);
        title.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI)title).inTL(0, 0);

        final TooltipMakerAPI mainCont = panel.createUIElement(PANEL_W, PANEL_H - SECT_I_H, true);
        panel.addUIElement(mainCont).inBL(0, 0);

        { // SECTION I
        final LabelAPI subtitle = settings.createLabel("Income", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        mainCont.addComponent((UIComponentAPI)subtitle).inTL(opad, 0);

        final TextPanel RoSVLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Rate of Exploitation";
                final String valueTxt = ((int)mData.getRoSV())+"";

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(baseColor);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();

                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return m_panel;
            }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);

                tp.addPara(
                    "Controls the proportion of worker output the colony retains as profit." +
                    "A higher value means the colony keeps more of the workers' production," +
                    "increasing colony income but reducing worker wages." +
                    "A lower value increases wages but reduces the colony's net income." +
                    "Adjust carefully — extreme values might have unintended consequences.", pad
                );

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel wagesLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Monthly Wages";
                final String valueTxt = NumFormat.formatCredit((int)(engine.getWagesForMarket(m_market.getId())*30));

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(negative);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();

                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return m_panel;
            }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);

                tp.addPara(
                    "Total wages paid to workers this month.", pad
                );

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel avgWageLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Average Wage";
                final float value = LaborConfig.LPV_month / mData.getRoSV();
                final String valueTxt = String.format("%.2f%s", value, Strings.C);

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(negative);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();

                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());
            }

            public CustomPanelAPI getTpParent() {
                return m_panel;
            }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);

                tp.addPara(
                    "The average monthly income of workers in the colony. Each person spends 1" +
                    Strings.C + " each month to purchase food.",
                    pad
                );

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        mainCont.addComponent(RoSVLabel.getPanel()).inTL(opad, opad*3);
        mainCont.addComponent(wagesLabel.getPanel()).inTL(opad, LABEL_H + opad*4);
        mainCont.addComponent(avgWageLabel.getPanel()).inTL(opad, LABEL_H*2 + opad*5);

        exploitationSlider = new Slider(
            mainCont, "", 1, LaborConfig.MAX_RoSV, sliderW, sliderH
        );
        exploitationSlider.setHighlightOnMouseover(true);
        exploitationSlider.setUserAdjustable(true);
        exploitationSlider.setProgress(mData.getRoSV());
        exploitationSlider.maxValue = LaborConfig.MAX_RoSV;
        exploitationSlider.clampCurrToMax = true;
        exploitationSlider.roundBarValue = true;
        exploitationSlider.setBarColor(UiUtils.lerpColor(
            positiveColor, negativeColor, mData.getRoSV()/(float)(LaborConfig.MAX_RoSV - 1)
        ));
        exploitationSlider.showValueOnly = true;
        mainCont.addComponent(exploitationSlider.getPanel()).inTL(opad*2 + LABEL_W, opad*3);

        final CallbackRunnable<Button> exploitationRunnable = (btn) -> {
            mData.setRoSV(exploitationSlider.getProgress());

            RoSVLabel.label2.setText(
                String.format("%d", Math.round(exploitationSlider.getProgress()))
            );

            wagesLabel.label2.setText(
                NumFormat.formatCredit((int)(engine.getWagesForMarket(m_market.getId())*30))
            );

            avgWageLabel.label2.setText(
                String.format("%.2f%s", LaborConfig.LPV_month / mData.getRoSV(), Strings.C)
            );
        };

        final Button exploitationBtn = new Button(
            mainCont, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, exploitationRunnable
        );

        exploitationBtn.quickMode = true;
        exploitationBtn.setCutStyle(CutStyle.ALL);
        mainCont.addComponent(exploitationBtn.getPanel()).inTL(opad*3 + LABEL_W + sliderW, opad*3);

        final TextPanel workerAmount = new TextPanel(mainCont, LABEL_W+100, LABEL_H) {
            public void createPanel() {
                final String txt = "Workforce: Employed / Total";
                final long value2 = cond.getWorkerPool();
                final long value1 = (long) (value2*((double)(1f - cond.getFreeWorkerRatio())));
                final String valueTxt = NumFormat.engNotation(value1) + " / " + NumFormat.engNotation(value2);

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(baseColor);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);
                label2.setHighlight(NumFormat.engNotation(value1));
                label2.setHighlightColor(highlight);

                final float textH1 = label1.getPosition().getHeight();

                add(label1).inTL(0, 0).setSize(LABEL_W+100, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W+100, label2.getPosition().getHeight());

                final Base workerIcon = new Base(
                    mainCont, ICON_S, ICON_S, WORKER_ICON, Misc.getBasePlayerColor(), null, false
                );
                add(workerIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() {
                return m_panel;
            }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);

                tp.addPara(
                    "Shows how many workers are currently employed compared to the colony's total labor capacity.",
                    pad
                );

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.LeftTop, opad);
                return tp;
            }
        };

        mainCont.addComponent(workerAmount.getPanel()).inTR(opad, opad*3);
        }
    
        { // SECTION II
        final LabelAPI subtitle = settings.createLabel("Population", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        mainCont.addComponent((UIComponentAPI)subtitle).inTL(opad, SECT_II_H);

        final TextPanel healthLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Health";
                final String valueTxt = String.format("%.0f", mData.getHealth());

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(highlight);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();
                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());

                final Base healthIcon = new Base(
                    mainCont, ICON_S, ICON_S, HEALTH_ICON, Misc.getBasePlayerColor(), null, false
                );
                add(healthIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Overall health of the population. A higher value indicates better living conditions, food availability, and lower hazard exposure.", pad);
                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel happinessLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Happiness";
                final String valueTxt = String.format("%.0f", mData.getHappiness());

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(highlight);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();
                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());

                final Base happinessIcon = new Base(
                    mainCont, ICON_S, ICON_S, SMILING_ICON, Misc.getBasePlayerColor(), null, false
                );
                add(happinessIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Overall happiness and morale of the population. Influenced by health, wages, stability, and cultural cohesion.", pad);
                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel cohesionLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Cohesion";
                final String valueTxt = String.format("%.0f", mData.getCulturalCohesion());

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(highlight);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();
                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());

                final Base cohesionIcon = new Base(
                    mainCont, ICON_S, ICON_S, SOCIETY_ICON, Misc.getBasePlayerColor(), null, false
                );
                add(cohesionIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Degree of cultural cohesion within the population. High cohesion reduces conflict and increases social stability.", pad);
                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel consciousnessLabel = new TextPanel(mainCont, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Class Consc.";
                final String valueTxt = String.format("%.0f", mData.getClassConsciousness());

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(highlight);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);

                final float textH1 = label1.getPosition().getHeight();
                add(label1).inTL(0, 0).setSize(LABEL_W, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W, label2.getPosition().getHeight());

                final Base classConsciousnessIcon = new Base(
                    mainCont, ICON_S, ICON_S, SOLIDARITY_ICON, Misc.getBasePlayerColor(), null, false
                );
                add(classConsciousnessIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara(
                    "The population's awareness of exploitation and social hierarchy. Higher values indicate a greater likelihood of collective action. " +
                    "Can be lowered by increasing wages, improving health, raising happiness, or implementing policies that reduce perceived inequities."
                    , pad
                );
                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, getPanel(), AnchorType.RightTop, opad);
                return tp;
            }
        };

        mainCont.addComponent(healthLabel.getPanel()).inTL(opad, SECT_II_H + opad*3);
        mainCont.addComponent(happinessLabel.getPanel()).inTL(opad + LABEL_W + pad, SECT_II_H + opad*3);
        mainCont.addComponent(cohesionLabel.getPanel()).inTL(opad + LABEL_W*2 + pad*2, SECT_II_H + opad*3);
        mainCont.addComponent(consciousnessLabel.getPanel()).inTL(opad + LABEL_W*3 + pad*3, SECT_II_H + opad*3);
        }
    }

    @Override
    public void setInteractionDialog(InteractionDialogAPI a) {
        interactionDialog = a;
    }

    @Override
    public void customDialogConfirm() {
        customDialogCancel();
    }

    @Override
    public void customDialogCancel() {
        UIState.setState(State.NONE);

        if (interactionDialog != null) {
            interactionDialog.dismiss();
        }
    }

    public String getCancelText() {
        return "Dismiss";
    }

    public String getConfirmText() {
        return "Dismiss";
    }

    public boolean hasCancelButton() {
        return false;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return new CustomUIPanelPlugin() {
            final ManageWorkersDialog dialog = ManageWorkersDialog.this;
            float sliderValue = 0f;
            
            public void advance(float arg0) {
                if (dialog.exploitationSlider.getProgressInterpolated() == sliderValue) return;

                sliderValue = dialog.exploitationSlider.getProgressInterpolated();
                dialog.exploitationSlider.setBarColor(UiUtils.lerpColor(
                    positiveColor, negativeColor, sliderValue/(float)(LaborConfig.MAX_RoSV - 1)
                ));
            }
            public void processInput(List<InputEventAPI> arg0) {}
            public void positionChanged(PositionAPI arg0) {}
            public void renderBelow(float arg0) {}
            public void render(float arg0) {}
            public void buttonPressed(Object arg0) {}
        };
    }   
}
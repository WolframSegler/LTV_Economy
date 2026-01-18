package wfg.ltv_econ.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static wfg.ltv_econ.constants.economyValues.*;
import static wfg.wrap_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.ui.UIComponentAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;
import wfg.ltv_econ.util.UiUtils;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.ComponentFactory;
import wfg.wrap_ui.ui.UIState;
import wfg.wrap_ui.ui.UIState.State;
import wfg.wrap_ui.ui.dialogs.DialogPanel;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.ListenerProviderPanel;
import wfg.wrap_ui.ui.panels.PieChart;
import wfg.wrap_ui.ui.panels.PieChart.PieSlice;
import wfg.wrap_ui.ui.panels.ScrollPanel;
import wfg.wrap_ui.ui.panels.ScrollPanel.ScrollType;
import wfg.wrap_ui.ui.panels.Slider;
import wfg.wrap_ui.ui.panels.SpritePanel.Base;
import wfg.wrap_ui.ui.panels.SpritePanelWithTp;
import wfg.wrap_ui.ui.panels.TextPanel;
import wfg.wrap_ui.ui.systems.FaderSystem.Glow;
import wfg.wrap_ui.util.CallbackRunnable;
import wfg.wrap_ui.util.NumFormat;
import wfg.wrap_ui.util.WrapUiUtils;
import wfg.wrap_ui.util.WrapUiUtils.AnchorType;

public class ManageWorkersDialog extends DialogPanel {

    public static final int PANEL_W = 950;
    public static final int PANEL_H = 680;
    public static final int SELECTED_P_H = 230;

    public static boolean showPolicies = true;

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
    private final MarketAPI m_market;
    private static final Color negativeColor = new Color(210, 115, 90);
    private static final Color positiveColor = new Color(90, 150, 110);

    public Slider exploitationSlider = null;

    private MarketPolicy selectedPolicy = null;
    private CustomPanelAPI selectedPolicyCont = null;
    
    public ManageWorkersDialog(MarketAPI market) {
        super(Attachments.getScreenPanel(), PANEL_W, PANEL_H, null, null, "Dismiss");
        setConfirmShortcut();

        m_market = market;

        getHolo().setBackgroundAlpha(1, 1);
        backgroundDimAmount = 0.2f;

        createPanel();
    }

    @Override
    public void createPanel() {
        UIState.setState(State.DIALOG);
        final SettingsAPI settings = Global.getSettings();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final PlayerMarketData mData = engine.getPlayerMarketData(m_market.getId());
        final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(m_market);

        final int SECT_I_H = 30;
        final int SECT_II_H = 160;
        final int SECT_III_H = 245;

        final Color baseColor = m_market.getFaction().getBaseUIColor();
        final int LABEL_W = 140;
        final int LABEL_H = 40;
        final int sliderH = 32;
        final int sliderW = 300;
        final int buttonH = 28;
        final int buttonW = 70;
        final int ICON_S = 28;
        final int gridWidth = 350;
        final int valueWidth = 60;
        final int policyWidth = 100;
        final int policyHeight = 141;

        final LabelAPI title = settings.createLabel("Manage Workers", Fonts.INSIGNIA_VERY_LARGE);
        title.autoSizeToWidth(PANEL_W);
        title.setAlignment(Alignment.MID);
        innerPanel.addComponent((UIComponentAPI)title).inTL(0, 0);

        { // SECTION I
        final LabelAPI subtitle = settings.createLabel("Income", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        innerPanel.addComponent((UIComponentAPI)subtitle).inTL(opad, SECT_I_H);

        final TextPanel RoSVLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
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

        final TextPanel wagesLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Monthly Wages";
                final String valueTxt = NumFormat.formatCredit((int)(engine.info.getWagesForMarket(m_market)*MONTH));

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

        final TextPanel avgWageLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
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

        innerPanel.addComponent(RoSVLabel.getPanel()).inTL(opad, opad*3 + SECT_I_H);
        innerPanel.addComponent(wagesLabel.getPanel()).inTL(opad, LABEL_H + opad*4 + SECT_I_H);
        innerPanel.addComponent(avgWageLabel.getPanel()).inTL(opad + LABEL_W, LABEL_H + opad*4 + SECT_I_H);

        exploitationSlider = new Slider(
            innerPanel, "", 1, LaborConfig.MAX_RoSV, sliderW, sliderH
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
        innerPanel.addComponent(exploitationSlider.getPanel()).inTL(opad*2 + LABEL_W, opad*3 + SECT_I_H);

        final CallbackRunnable<Button> exploitationRunnable = (btn) -> {
            mData.setRoSV(exploitationSlider.getProgress());

            RoSVLabel.label2.setText(
                String.format("%d", Math.round(exploitationSlider.getProgress()))
            );

            wagesLabel.label2.setText(
                NumFormat.formatCredit((int)(engine.info.getWagesForMarket(m_market)*30))
            );

            avgWageLabel.label2.setText(
                String.format("%.2f%s", LaborConfig.LPV_month / mData.getRoSV(), Strings.C)
            );
        };

        final Button exploitationBtn = new Button(
            innerPanel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, exploitationRunnable
        );

        exploitationBtn.quickMode = true;
        exploitationBtn.setCutStyle(CutStyle.ALL);
        innerPanel.addComponent(exploitationBtn.getPanel()).inTL(opad*3 + LABEL_W + sliderW, opad*3 + SECT_I_H);

        final TextPanel workerAmount = new TextPanel(innerPanel, LABEL_W+100, LABEL_H) {
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
                    innerPanel, ICON_S, ICON_S, WORKER_ICON, base, null
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

        innerPanel.addComponent(workerAmount.getPanel()).inTR(opad, opad*3 + SECT_I_H);
        }
    
        { // SECTION II
        final LabelAPI subtitle = settings.createLabel("Population", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        innerPanel.addComponent((UIComponentAPI)subtitle).inTL(opad, SECT_II_H);

        final StatModValueGetter tpGridGetter = new StatModValueGetter() {
            public String getFlatValue(StatMod mod) {
                return String.format("%.3f", mod.value);
            }
            public String getPercentValue(StatMod mod) {
                return mod.value + "%";
            }
            public String getMultValue(StatMod mod) {
                return Strings.X + String.format("%.1f", mod.value);
            }
            public Color getModColor(StatMod mod) {
                return highlight;
            }
        };

        final TextPanel healthLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
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
                    innerPanel, ICON_S, ICON_S, HEALTH_ICON, base, null
                );
                add(healthIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Overall health of the population. A higher value indicates better living conditions, food availability, and lower hazard exposure.", pad);
                
                final float value = mData.healthDelta
                    .computeEffective(mData.getHealth()) - mData.getHealth();
                tp.addPara("Daily Change: %s", 3, highlight, String.format("%.2f", value));
                tp.addStatModGrid(gridWidth, valueWidth, pad, pad, mData.healthDelta, tpGridGetter);

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel happinessLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
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
                    innerPanel, ICON_S, ICON_S, SMILING_ICON, base, null
                );
                add(happinessIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Overall happiness and morale of the population. Influenced by health, wages, stability, and social cohesion.", opad);

                final float value = mData.happinessDelta
                    .computeEffective(mData.getHappiness()) - mData.getHappiness();
                tp.addPara("Daily Change: %s", 3, highlight, String.format("%.2f", value));
                tp.addStatModGrid(gridWidth, valueWidth, pad, pad, mData.happinessDelta, tpGridGetter);

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel cohesionLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
            public void createPanel() {
                final String txt = "Cohesion";
                final String valueTxt = String.format("%.0f", mData.getSocialCohesion());

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
                    innerPanel, ICON_S, ICON_S, SOCIETY_ICON, base, null
                );
                add(cohesionIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(400, 0, false);
                tp.addPara("Degree of social cohesion within the population. High cohesion reduces conflict and increases stability.", opad);

                final float value = mData.socialCohesionDelta.computeEffective(
                    mData.getSocialCohesion()) - mData.getSocialCohesion();
                tp.addPara("Daily Change: %s", 3, highlight, String.format("%.2f", value));
                tp.addStatModGrid(gridWidth, valueWidth, pad, pad, mData.socialCohesionDelta, tpGridGetter);

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                return tp;
            }
        };

        final TextPanel consciousnessLabel = new TextPanel(innerPanel, LABEL_W, LABEL_H) {
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
                    innerPanel, ICON_S, ICON_S, SOLIDARITY_ICON, base, null
                );
                add(classConsciousnessIcon).inBL(-pad, (LABEL_H - ICON_S)/2f);
            }

            public CustomPanelAPI getTpParent() { return m_panel; }

            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = m_panel.createUIElement(380, 0, false);
                tp.addPara(
                    "The population's awareness of exploitation and social hierarchy. Higher values indicate a greater likelihood of collective action. " +
                    "Can be lowered by increasing wages, improving health, raising happiness, or implementing policies that reduce perceived inequities."
                    , opad
                );

                final float value = mData.classConsciousnessDelta.computeEffective(
                    mData.getClassConsciousness()) - mData.getClassConsciousness();
                tp.addPara("Daily Change: %s", 3, highlight, String.format("%.2f", value));
                tp.addStatModGrid(gridWidth, valueWidth, pad, pad, mData.classConsciousnessDelta, tpGridGetter);

                add(tp);
                WrapUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, 0);
                return tp;
            }
        };

        innerPanel.addComponent(healthLabel.getPanel()).inTL(opad, SECT_II_H + opad*3);
        innerPanel.addComponent(happinessLabel.getPanel()).inTL(opad + LABEL_W + pad, SECT_II_H + opad*3);
        innerPanel.addComponent(cohesionLabel.getPanel()).inTL(opad + LABEL_W*2 + pad*2, SECT_II_H + opad*3);
        innerPanel.addComponent(consciousnessLabel.getPanel()).inTL(opad + LABEL_W*3 + pad*3, SECT_II_H + opad*3);
        }
    
        if (showPolicies && EconomyConfig.SHOW_MARKET_POLICIES) { // SECTION III
        final LabelAPI subtitle = settings.createLabel("Policies", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        innerPanel.addComponent((UIComponentAPI)subtitle).inTL(opad, SECT_III_H);
        final int subtitleH = (int) subtitle.computeTextHeight(subtitle.getText());

        final ScrollPanel policyContainer = new ScrollPanel(innerPanel, PANEL_W - opad, policyHeight + opad);
        policyContainer.scrollType = ScrollType.HORIZONTAL;
        innerPanel.addComponent(policyContainer.getPanel()).inTL(opad/2f, SECT_III_H + subtitleH + pad*2);

        int posterCount = 0;
        for (MarketPolicy policy : mData.getPolicies()) {
            if (!policy.isEnabled(mData)) continue;

            final int posterIndex = posterCount;
            final HasActionListener listener = new HasActionListener() {
                public void onClicked(CustomPanel<?, ?, ?> source, boolean isLeftClick) {
                    if (selectedPolicy == policy) return;

                    selectedPolicy = policy;
                    innerPanel.removeComponent(selectedPolicyCont);
                    selectedPolicyCont = settings.createCustom(
                        PANEL_W, SELECTED_P_H, null
                    );
                    innerPanel.addComponent(selectedPolicyCont).inTL(
                        opad, SECT_III_H + subtitleH + opad*2 + policyHeight
                    );

                    final HasActionListener self = this;
                    final CallbackRunnable<Button> activateRun = new CallbackRunnable<>() {
                        public void run(Button btn) {
                            policy.activate(mData);
                            buildPoster(policyContainer.getContentPanel(), policy, mData,
                                self, policyWidth, policyHeight
                            ).inBL(pad + posterIndex*(policyWidth + pad), opad/2f);

                            innerPanel.removeComponent(selectedPolicyCont);
                            selectedPolicyCont = settings.createCustom(
                                PANEL_W, SELECTED_P_H, null
                            );
                            innerPanel.addComponent(selectedPolicyCont).inTL(
                                opad, SECT_III_H + subtitleH + opad*2 + policyHeight
                            );

                            source.getParent().removeComponent(source.getPanel());
                            buildSelectedPosterMenu(selectedPolicyCont, policy, mData, this);
                        };
                    };

                    buildSelectedPosterMenu(selectedPolicyCont, policy, mData, activateRun);
                }
            };

            buildPoster(policyContainer.getContentPanel(), policy, mData, listener,  
                policyWidth, policyHeight
            ).inBL(pad + posterCount*(policyWidth + pad), opad/2f);

            posterCount++;
        }
        policyContainer.setContentWidth(pad + (policyWidth+pad)*posterCount);
        }
    }

    private final PositionAPI buildPoster(CustomPanelAPI cont, MarketPolicy policy,
        PlayerMarketData mData, HasActionListener listener, int width, int height
    ) {
        final SettingsAPI settings = Global.getSettings();
        
        try {
            settings.loadTexture(policy.spec.posterPath);
        } catch (Exception e) {
            Global.getLogger(getClass()).warn(e);
        }

        final ListenerProviderPanel posterWrap = new ListenerProviderPanel(
            cont, width, height
        ) {
            @Override
            public Optional<HasActionListener> getActionListener() {
                return Optional.ofNullable(listener);
            }
        };

        final SpritePanelWithTp poster = new SpritePanelWithTp(posterWrap.getPanel(), width, height,
            policy.spec.posterPath, policy.isOnCooldown() ? Color.GRAY : null, null
        ) {
            {
                getPlugin().setTargetUIState(State.DIALOG);
                outlineColor = Color.ORANGE;
                createPanel();
            }
            @Override
            public void createPanel() {
                if (!policy.isOnCooldown()) return;

                final float cooledRatio = (float) policy.cooldownDaysRemaining/policy.spec.cooldownDays;
                final ArrayList<PieSlice> data = new ArrayList<>(
                    List.of(
                        new PieSlice(null, gray, cooledRatio),
                        new PieSlice(null, Color.ORANGE, 1f - cooledRatio)
                    )
                );
                final int clockD = 30;
                final PieChart cooldownClock = new PieChart(
                    getPanel(), clockD, clockD, data
                );

                add(cooldownClock).inBL(
                    (width - clockD) / 2f,
                    (height - clockD) / 2f
                );
            }

            @Override
            public CustomPanelAPI getTpParent() {
                return ManageWorkersDialog.this.m_panel;
            }

            @Override
            public TooltipMakerAPI createAndAttachTp() {
                final TooltipMakerAPI tp = ManageWorkersDialog.this.m_panel.createUIElement(400, 0, false);

                policy.createTooltip(mData, tp);

                ManageWorkersDialog.this.m_panel.addUIElement(tp);
                WrapUiUtils.mouseCornerPos(tp, opad);
                return tp;
            }
        
            @Override
            public Glow getGlowType() { return Glow.ADDITIVE; }

            @Override
            public Optional<SpriteAPI> getAdditiveSprite() {
                return Optional.ofNullable(m_sprite);
            }
        };
        poster.drawBorder = policy.isActive();

        posterWrap.add(poster).inBL(0, 0);
        return cont.addComponent(posterWrap.getPanel());
    }

    private final void buildSelectedPosterMenu(CustomPanelAPI cont,
        MarketPolicy policy, PlayerMarketData mData, CallbackRunnable<Button> activateRun
    ) {
        final SettingsAPI settings = Global.getSettings();
        final int posterW = 163;
        final int buttonW = 140;
        final int buttonH = 30;

        buildPoster(cont, policy, mData, null,  
            posterW, SELECTED_P_H
        ).inTL(opad*2, 0);

        final String buttonTxt;
        final String buttonSideTxt;
        final String buttonSideHighlight;
        switch (policy.state) {
        case COOLDOWN:
            buttonTxt = "On Cooldown";
            buttonSideTxt = "Available in " + policy.cooldownDaysRemaining + " days";
            buttonSideHighlight = Integer.toString(policy.cooldownDaysRemaining);
            break;
        case ACTIVE:
            buttonTxt = "Already Active";
            buttonSideTxt = "Effect lasts for " + policy.activeDaysRemaining + " days";
            buttonSideHighlight = Integer.toString(policy.activeDaysRemaining);
            break;
        default:
            buttonTxt = "Activate";
            buttonSideTxt = "Active for " + policy.spec.durationDays + " days";
            buttonSideHighlight = Integer.toString(policy.spec.durationDays);
            break;
        }
        final Button activateButton = new Button(
            cont, buttonW, buttonH, buttonTxt, Fonts.ORBITRON_12, activateRun
        );
        activateButton.quickMode = true;
        activateButton.setCutStyle(CutStyle.TL_BR);
        activateButton.bgAlpha = 1f;
        final long marketCredits = EconomyEngine.getInstance().getCredits(m_market.getId());
        final boolean hasSufficientCredits = Math.max(0, marketCredits) >= policy.spec.cost;
        if ((!policy.isAvailable() || !hasSufficientCredits) && !DebugFlags.COLONY_DEBUG) {
            activateButton.disabled = true;

            activateButton.setTooltipFactory(() -> {
                final TooltipMakerAPI tp = activateButton.getPanel().createUIElement(400, 0, false);

                tp.addPara("Not enough market credits to activate this policy", pad);

                activateButton.add(tp);
                WrapUiUtils.anchorPanel(tp, activateButton.getPanel(), AnchorType.TopLeft, pad);
                return tp;
            });
            activateButton.setParentSupplier(() -> { return activateButton.getPanel();});
            activateButton.showTooltipWhileInactive = true;
        }

        cont.addComponent(activateButton.getPanel()).inBR(opad + PANEL_W/2f, pad);

        final String costTxt = policy.spec.cost > 0 ? " - "+NumFormat.formatCredit(policy.spec.cost) : "";
        final LabelAPI title = settings.createLabel(policy.spec.name + costTxt, Fonts.ORBITRON_12);

        title.setHighlightColor(hasSufficientCredits ? highlight : negative);
        title.setHighlight(NumFormat.formatCredit(policy.spec.cost));
        title.setColor(base);
        cont.addComponent((UIComponentAPI)title).inTL(posterW + opad*3, pad);
        
        final LabelAPI desc = settings.createLabel(policy.spec.description, Fonts.DEFAULT_SMALL);
        desc.autoSizeToWidth(PANEL_W/1.5f - 4*opad - posterW);
        cont.addComponent((UIComponentAPI)desc);
        WrapUiUtils.anchorPanel((UIComponentAPI)desc, (UIComponentAPI)title, AnchorType.BottomLeft, pad*2);

        final LabelAPI buttonSide = settings.createLabel(buttonSideTxt, Fonts.DEFAULT_SMALL);
        buttonSide.setHighlightColor(highlight);
        buttonSide.setHighlight(buttonSideHighlight);
        buttonSide.setColor(base);
        buttonSide.getPosition().setSize(posterW/2f + opad, 30);
        cont.addComponent((UIComponentAPI)buttonSide).inBR(opad*2 + buttonW + PANEL_W/2f, pad);

        // OPTION BUTTONS
        final CallbackRunnable<Button> availableRn = (btn) -> {
            btn.checked = !btn.checked;
            policy.notifyWhenAvailable = btn.checked;
        };
        final CallbackRunnable<Button> finishedRn = (btn) -> {
            btn.checked = !btn.checked;
            policy.notifyWhenFinished = btn.checked;
        };
        final CallbackRunnable<Button> repeatRn = (btn) -> {
            btn.checked = !btn.checked;
            policy.repeatAfterCooldown = btn.checked;
        };

        final Button notifyAvailableBtn = ComponentFactory.createCheckboxWithText(
            cont, 18, "Notify when available",
            Fonts.DEFAULT_SMALL, availableRn, base, pad
        );
        final Button notifyFinishedBtn = ComponentFactory.createCheckboxWithText(
            cont, 18, "Notify when finished",
            Fonts.DEFAULT_SMALL, finishedRn, base, pad
        );
        final Button repeatAfterCooldownBtn = ComponentFactory.createCheckboxWithText(
            cont, 18, "Repeat after cooldown",
            Fonts.DEFAULT_SMALL, repeatRn, base, pad
        );

        notifyAvailableBtn.checked = policy.notifyWhenAvailable;
        notifyFinishedBtn.checked = policy.notifyWhenFinished;
        repeatAfterCooldownBtn.checked = policy.repeatAfterCooldown;
        cont.addComponent(notifyAvailableBtn.getPanel()).inBL(PANEL_W/2f + opad, pad + opad*4);
        cont.addComponent(notifyFinishedBtn.getPanel()).inBL(PANEL_W/2f + opad, pad + opad*2);
        cont.addComponent(repeatAfterCooldownBtn.getPanel()).inBL(PANEL_W/2f + opad, pad);
    }

    float sliderValue = 0f;

    @Override
    public void advanceImpl(float delta) {
        super.advanceImpl(delta);

        if (exploitationSlider.getProgressInterpolated() == sliderValue) return;

        sliderValue = exploitationSlider.getProgressInterpolated();
        exploitationSlider.setBarColor(UiUtils.lerpColor(
            positiveColor, negativeColor, sliderValue/(float)(LaborConfig.MAX_RoSV - 1)
        ));
    }

    @Override
    public void dismiss(int option) {
        super.dismiss(option);

        UIState.setState(State.NONE);
    }
}
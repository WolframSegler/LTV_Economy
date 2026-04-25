package wfg.ltv_econ.ui.marketInfo.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import static wfg.ltv_econ.constants.EconomyConstants.*;
import static wfg.native_ui.util.UIConstants.*;
import static wfg.native_ui.util.Globals.settings;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.constants.SubmarketsID;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.intel.market.policies.MarketPolicy;
import wfg.ltv_econ.ui.marketInfo.population.CohesionPair;
import wfg.ltv_econ.ui.marketInfo.population.ConsciousnessPair;
import wfg.ltv_econ.ui.marketInfo.population.HappinessPair;
import wfg.ltv_econ.ui.marketInfo.population.HealthPair;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp.ClickHandler;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.functional.ListenerProviderPanel;
import wfg.native_ui.ui.visual.PieChart;
import wfg.native_ui.ui.visual.PieChart.PieSlice;
import wfg.native_ui.ui.container.ScrollPanel;
import wfg.native_ui.ui.container.ScrollPanel.ScrollType;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.visual.SpritePanelWithTp;
import wfg.native_ui.ui.visual.TextPanel;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NumFormat;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class ManagePopulationDialog extends DialogPanel {
    private static final Logger log = Global.getLogger(ManagePopulationDialog.class);
    private static final SpriteAPI WORKER_ICON = settings.getSprite("ui", "three_workers");
    private static final Color negativeColor = new Color(210, 115, 90);
    private static final Color positiveColor = new Color(90, 150, 110);

    private static final int PANEL_W = 950;
    private static final int PANEL_H = 680;
    private static final int SELECTED_P_H = 230;

    public static boolean showPolicies = true;

    private final MarketAPI m_market;

    public Slider exploitationSlider = null;
    private float sliderValue = 0f;

    private MarketPolicy selectedPolicy = null;
    private UIPanelAPI selectedPolicyCont = null;
    
    public ManagePopulationDialog(MarketAPI market) {
        super(PANEL_W, PANEL_H, null, null, "Dismiss");
        getButton(0).setShortcutAndAppendToText(Keyboard.KEY_4);

        m_market = market;

        holo.borderAlpha = 0.8f;
        backgroundDimAmount = 0.2f;

        buildUI();
    }

    @Override
    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final PlayerMarketData data = engine.getPlayerMarketData(m_market.getId());
        final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(m_market);

        final int SECT_I_H = 30;
        final int SECT_II_H = 160;
        final int SECT_III_H = 245;

        final Color baseColor = m_market.getFaction().getBaseUIColor();
        final int LABEL_W = 140;
        final int LABEL_H = 35;
        final int sliderH = 32;
        final int sliderW = 300;
        final int buttonH = 28;
        final int buttonW = 70;
        final int ICON_S = 28;
        final int policyWidth = 100;
        final int policyHeight = 141;

        final LabelAPI title = settings.createLabel("Manage Workers", Fonts.INSIGNIA_VERY_LARGE);
        title.autoSizeToWidth(PANEL_W);
        title.setAlignment(Alignment.MID);
        add(title).inTL(0, 0);

        { // SECTION I
        final LabelAPI subtitle = settings.createLabel("Income", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        add(subtitle).inTL(opad, SECT_I_H);

        final TextPanel RoSVLabel = new TextPanel(m_panel, LABEL_W, LABEL_H) {
            public void buildUI() {
                final String txt = "Rate of Exploitation";
                final String valueTxt = ((int)data.getRoSV())+"";

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

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Controls the proportion of worker output the colony retains as profit." +
                        "A higher value means the colony keeps more of the workers' production," +
                        "increasing colony income but reducing worker wages." +
                        "A lower value increases wages but reduces the colony's net income." +
                        "Adjust carefully — extreme values might have unintended consequences.", pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                };
            }
        };

        final TextPanel wagesLabel = new TextPanel(m_panel, LABEL_W, LABEL_H) {
            public void buildUI() {
                final String txt = "Monthly Wages";
                final String valueTxt = NumFormat.formatCredit((int)(engine.info.getDailyWages(m_market)*MONTH));

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

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Total wages paid to workers this month.", pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                };
            }
        };

        final TextPanel avgWageLabel = new TextPanel(m_panel, LABEL_W, LABEL_H) {
            public void buildUI() {
                final String txt = "Average Wage";
                final float value = LaborConfig.LPV_month / data.getRoSV();
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

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "The average monthly income of workers in the colony. Each person spends 1" +
                        Strings.C + " each month to purchase food.",
                        pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.RightTop, opad);
                };
            }
        };

        add(RoSVLabel.getPanel()).inTL(opad, opad*3 + SECT_I_H);
        add(wagesLabel.getPanel()).inTL(opad, LABEL_H + opad*4 + SECT_I_H);
        add(avgWageLabel.getPanel()).inTL(opad + LABEL_W, LABEL_H + opad*4 + SECT_I_H);

        exploitationSlider = new Slider(
            m_panel, "", 1, LaborConfig.MAX_RoSV, sliderW, sliderH
        );
        exploitationSlider.setHighlightOnMouseover(true);
        exploitationSlider.setProgress(data.getRoSV());
        exploitationSlider.maxValue = LaborConfig.MAX_RoSV;
        exploitationSlider.clampCurrToMax = true;
        exploitationSlider.roundBarValue = true;
        exploitationSlider.setBarColor(NativeUiUtils.lerpColor(
            positiveColor, negativeColor, data.getRoSV()/(float)(LaborConfig.MAX_RoSV - 1)
        ));
        exploitationSlider.showValueOnly = true;
        add(exploitationSlider.getPanel()).inTL(opad*2 + LABEL_W, opad*3 + SECT_I_H);

        final CallbackRunnable<Button> exploitationRunnable = (btn) -> {
            data.setRoSV(exploitationSlider.getProgress());

            RoSVLabel.label2.setText(
                String.format("%d", Math.round(exploitationSlider.getProgress()))
            );

            wagesLabel.label2.setText(
                NumFormat.formatCredit((int)(engine.info.getDailyWages(m_market)*30))
            );

            avgWageLabel.label2.setText(
                String.format("%.2f%s", LaborConfig.LPV_month / data.getRoSV(), Strings.C)
            );
        };

        final Button exploitationBtn = new Button(
            m_panel, buttonW, buttonH, "Confirm", Fonts.ORBITRON_12, exploitationRunnable
        );

        exploitationBtn.setQuickMode(true);
        exploitationBtn.cutStyle = CutStyle.ALL;
        add(exploitationBtn.getPanel()).inTL(opad*3 + LABEL_W + sliderW, opad*3 + SECT_I_H);

        final TextPanel workerAmount = new TextPanel(m_panel, LABEL_W+100, LABEL_H) {
            public void buildUI() {
                final String txt = "Workforce: Employed / Total";
                final long value2 = cond.getWorkerPool();
                final long value1 = (long) (value2*((double)(1f - cond.getFreeWorkerRatio())));
                final String valueTxt = NumFormat.engNotate(value1) + " / " + NumFormat.engNotate(value2);

                label1 = settings.createLabel(txt, Fonts.ORBITRON_12);
                label1.setColor(baseColor);
                label1.setHighlightOnMouseover(true);
                label1.setAlignment(Alignment.MID);

                label2 = settings.createLabel(valueTxt, Fonts.INSIGNIA_VERY_LARGE);
                label2.setColor(baseColor);
                label2.setHighlightOnMouseover(true);
                label2.setAlignment(Alignment.MID);
                label2.setHighlight(NumFormat.engNotate(value1));
                label2.setHighlightColor(highlight);

                final float textH1 = label1.getPosition().getHeight();

                add(label1).inTL(0, 0).setSize(LABEL_W+100, textH1);
                add(label2).inTL(0, textH1 + pad).setSize(LABEL_W+100, label2.getPosition().getHeight());

                final Base workerIcon = new Base(
                    m_panel, ICON_S, ICON_S, WORKER_ICON, base, null
                );
                add(workerIcon).inBL(0, (LABEL_H - ICON_S)/2f);
            }

            {
                tooltip.builder = (tp, exp) -> {
                    tp.addPara(
                        "Shows how many workers are currently employed compared to the colony's total labor capacity.",
                        pad
                    );
                };
                tooltip.positioner = (tp, exp) -> {
                    NativeUiUtils.anchorPanelWithBounds(tp, m_panel, AnchorType.LeftTop, opad);
                };
            }
        };

        add(workerAmount.getPanel()).inTR(opad, opad*3 + SECT_I_H);
        }
    
        { // SECTION II
        final LabelAPI subtitle = settings.createLabel("Population", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        add(subtitle).inTL(opad, SECT_II_H);

        final HealthPair healthPair = new HealthPair(m_panel, LABEL_W - opad*2, LABEL_H, data, null, Fonts.INSIGNIA_VERY_LARGE);
        final HappinessPair happinessPair = new HappinessPair(m_panel, LABEL_W - opad*2, LABEL_H, data, null, Fonts.INSIGNIA_VERY_LARGE);
        final CohesionPair cohesionPair = new CohesionPair(m_panel, LABEL_W - opad*2, LABEL_H, data, null, Fonts.INSIGNIA_VERY_LARGE);
        final ConsciousnessPair consciousnessPair = new ConsciousnessPair(m_panel, LABEL_W - opad*2, LABEL_H, data, null, Fonts.INSIGNIA_VERY_LARGE);

        add(healthPair).inTL(opad, SECT_II_H + opad*3);
        add(happinessPair).inTL(opad + LABEL_W + pad, SECT_II_H + opad*3);
        add(cohesionPair).inTL(opad + LABEL_W*2 + pad*2, SECT_II_H + opad*3);
        add(consciousnessPair).inTL(opad + LABEL_W*3 + pad*3, SECT_II_H + opad*3);
        }
    
        if (showPolicies && EconConfig.SHOW_MARKET_POLICIES) { // SECTION III
        final LabelAPI subtitle = settings.createLabel("Policies", Fonts.INSIGNIA_LARGE);
        subtitle.autoSizeToWidth(PANEL_W - opad);
        subtitle.setAlignment(Alignment.LMID);
        add(subtitle).inTL(opad, SECT_III_H);
        final int subtitleH = (int) subtitle.computeTextHeight(subtitle.getText());

        final ScrollPanel policyContainer = new ScrollPanel(m_panel, PANEL_W - opad, policyHeight + opad);
        policyContainer.scrollType = ScrollType.HORIZONTAL;
        add(policyContainer.getPanel()).inTL(hpad, SECT_III_H + subtitleH + pad*2);

        int posterCount = 0;
        for (MarketPolicy policy : data.getPolicies()) {
            if (!policy.isEnabled(data)) continue;

            final int posterIndex = posterCount;
            final ClickHandler<ListenerProviderPanel> listener = (source, isLeftClick) -> {
                if (selectedPolicy == policy) return;

                selectedPolicy = policy;
                remove(selectedPolicyCont);
                selectedPolicyCont = settings.createCustom(
                    PANEL_W, SELECTED_P_H, null
                );
                add(selectedPolicyCont).inTL(
                    opad, SECT_III_H + subtitleH + opad*2 + policyHeight
                );

                final CallbackRunnable<Button> activateRun = (btn) -> {
                    if (!policy.isActive(data)) policy.activate(data);
                    m_market.getSubmarket(SubmarketsID.STOCKPILES).getPlugin().updateCargoPrePlayerInteraction();

                    buildPoster(policyContainer.getContentPanel(), policy, data,
                        source.interaction.onClicked, policyWidth, policyHeight
                    ).inBL(pad + posterIndex*(policyWidth + pad), hpad);

                    remove(selectedPolicyCont);
                    selectedPolicyCont = settings.createCustom(
                        PANEL_W, SELECTED_P_H, null
                    );
                    add(selectedPolicyCont).inTL(
                        opad, SECT_III_H + subtitleH + opad*2 + policyHeight
                    );

                    source.getParent().removeComponent(source.getPanel());
                    buildSelectedPosterMenu(selectedPolicyCont, policy, data, this);
                };

                buildSelectedPosterMenu(selectedPolicyCont, policy, data, activateRun);
            };

            buildPoster(policyContainer.getContentPanel(), policy, data, listener,  
                policyWidth, policyHeight
            ).inBL(pad + posterCount*(policyWidth + pad), hpad);

            posterCount++;
        }
        policyContainer.setContentWidth(pad + (policyWidth+pad)*posterCount);
        }
    }

    private final PositionAPI buildPoster(UIPanelAPI cont, MarketPolicy policy,
        PlayerMarketData mData, ClickHandler<ListenerProviderPanel> listener, int width, int height
    ) {
        try {
            settings.loadTexture(policy.spec.posterPath);
        } catch (Exception e) {
            log.warn(e);
        }

        final ListenerProviderPanel posterWrap = new ListenerProviderPanel(
            cont, width, height
        ) {{ interaction.onClicked = listener;}};

        final SpritePanelWithTp poster = new SpritePanelWithTp(posterWrap.getPanel(), width, height,
            policy.spec.posterPath, policy.isOnCooldown(mData) ? gray : null, null
        ) {
            public void buildUI() {
                if (policy.isOnCooldown(mData)) {
                    final float cooledRatio = (float) policy.cooldownDaysRemaining/policy.spec.cooldownDays;
                    final ArrayList<PieSlice> pieData = new ArrayList<>(
                        List.of(
                            new PieSlice(null, gray, cooledRatio),
                            new PieSlice(null, Color.ORANGE, 1f - cooledRatio)
                        )
                    );
                    final int clockD = 30;
                    final PieChart cooldownClock = new PieChart(
                        m_panel, clockD, clockD, pieData
                    );
    
                    add(cooldownClock).inBL(
                        (width - clockD) / 2f,
                        (height - clockD) / 2f
                    );
                }
            }

            {
                outline.color = Color.ORANGE;
                outline.enabled = policy.isActive(mData);

                glow.type = GlowType.ADDITIVE;
                glow.additiveSprite = m_sprite;

                tooltip.builder = (tp, exp) -> policy.createTooltip(mData, tp);

                buildUI();
            }
        };

        posterWrap.add(poster).inBL(0, 0);
        return cont.addComponent(posterWrap.getPanel());
    }

    private final void buildSelectedPosterMenu(UIPanelAPI cont,
        MarketPolicy policy, PlayerMarketData mData, CallbackRunnable<Button> activateRun
    ) {
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
        activateButton.setQuickMode(true);
        activateButton.cutStyle = CutStyle.TL_BR;
        activateButton.bgAlpha = 1f;
        final long marketCredits = EconomyEngine.instance().getCredits(m_market.getId());
        final boolean hasSufficientCredits = Math.max(0, marketCredits) >= policy.spec.cost;

        final boolean cantActivate = !policy.isAvailable(mData) || !hasSufficientCredits;
        final boolean shouldDisable = (cantActivate && !DebugFlags.COLONY_DEBUG) || policy.isActive(mData);

        if (shouldDisable) {
            activateButton.setEnabled(false);
            activateButton.setShowTooltipWhileInactive(true);

            activateButton.tooltip.builder = (tp, exp) -> {
                if (policy.isActive(mData)) {
                    tp.addPara("This policy is already active.", pad);
                } else if (!hasSufficientCredits) {
                    tp.addPara("Not enough market credits to activate this policy.", pad);
                } else {
                    tp.addPara("Policy requirements are not met.", pad);
                }
            };

            activateButton.tooltip.positioner = (tp, exp) ->
                NativeUiUtils.anchorPanel(tp, activateButton.getPanel(), AnchorType.TopLeft, pad);
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
        NativeUiUtils.anchorPanel((UIComponentAPI)desc, (UIComponentAPI)title, AnchorType.BottomLeft, pad*2);

        final LabelAPI buttonSide = settings.createLabel(buttonSideTxt, Fonts.DEFAULT_SMALL);
        buttonSide.setHighlightColor(highlight);
        buttonSide.setHighlight(buttonSideHighlight);
        buttonSide.setColor(base);
        buttonSide.getPosition().setSize(posterW/2f + opad, 30);
        cont.addComponent((UIComponentAPI)buttonSide).inBR(opad*2 + buttonW + PANEL_W/2f, pad);

        // OPTION BUTTONS
        final CallbackRunnable<Button> availableRn = (btn) -> {
            btn.setChecked(!btn.isChecked());
            policy.notifyWhenAvailable = btn.isChecked();
        };
        final CallbackRunnable<Button> finishedRn = (btn) -> {
            btn.setChecked(!btn.isChecked());
            policy.notifyWhenFinished = btn.isChecked();
        };
        final CallbackRunnable<Button> repeatRn = (btn) -> {
            btn.setChecked(!btn.isChecked());
            policy.repeatAfterCooldown = btn.isChecked();
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

        notifyAvailableBtn.setChecked(policy.notifyWhenAvailable);
        notifyFinishedBtn.setChecked(policy.notifyWhenFinished);
        repeatAfterCooldownBtn.setChecked(policy.repeatAfterCooldown);
        cont.addComponent(notifyAvailableBtn.getPanel()).inBL(PANEL_W/2f + opad, pad + opad*4);
        cont.addComponent(notifyFinishedBtn.getPanel()).inBL(PANEL_W/2f + opad, pad + opad*2);
        cont.addComponent(repeatAfterCooldownBtn.getPanel()).inBL(PANEL_W/2f + opad, pad);
    }

    @Override
    public void advance(float delta) {
        super.advance(delta);

        if (exploitationSlider.getProgressInterpolated() == sliderValue) return;

        sliderValue = exploitationSlider.getProgressInterpolated();
        exploitationSlider.setBarColor(NativeUiUtils.lerpColor(
            positiveColor, negativeColor, sliderValue/(float)(LaborConfig.MAX_RoSV - 1)
        ));
    }
}
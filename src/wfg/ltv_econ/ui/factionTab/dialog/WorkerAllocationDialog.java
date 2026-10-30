package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.config.PlanConfig;
import wfg.ltv_econ.config.PlanConfig.WorkerAllocationPlan;
import wfg.ltv_econ.config.loader.PlanConfigLoader;
import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.planning.custom.CustomGoal;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments.PiecewiseSegment;
import wfg.ltv_econ.economy.planning.custom.goalParams.BooleanParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.DoubleParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.MultiSelectParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.RadioParameter;
import wfg.ltv_econ.economy.registry.PlanningGoalRegistry;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.internal.ui.core.UIElement;
import wfg.native_ui.internal.util.BorderRenderer;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.component.HoverGlowComp;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.component.InteractionComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.component.TooltipComp;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.core.UIElementFlags.HasHoverGlow;
import wfg.native_ui.ui.core.UIElementFlags.HasOutline;
import wfg.native_ui.ui.core.UIElementFlags.HasTooltip;
import wfg.native_ui.ui.dialog.DialogPanel;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.CheckboxButton;
import wfg.native_ui.ui.interaction.UIClickable;
import wfg.native_ui.ui.widget.Button.CutStyle;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;
import wfg.native_ui.ui.widget.MultiSelect;
import wfg.native_ui.ui.widget.RadioPanel;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.widget.RadioPanel.LayoutMode;

public final class WorkerAllocationDialog extends DialogPanel {
    private static final Logger log = Global.getLogger(WorkerAllocationDialog.class);
    private static final SpriteAPI CUSTOM = settings.getSprite("ui", "customize");
    private static final SpriteAPI PRESET = settings.getSprite("ui", "blueprint");
    private static final int PANEL_W = 1100;
    private static final int PANEL_H = 690;
    private static final int TITLE_H = 30;
    private static final int LEFT_SIDE_W = 200;
    private static final int WIDGET_W =  LEFT_SIDE_W - pad * 2;
    private static final int CREATE_PLAN_BTN_H = 50;
    private static final int WIDGET_H = 40;
    private static final int CONTENT_PANEL_W = PANEL_W - LEFT_SIDE_W - hpad;
    private static final int CONTENT_PANEL_H = PANEL_H - TITLE_H;
    private static final float CONTENT_WIDTH = CONTENT_PANEL_W - 10f;
    private static final int SEGMENT_PANEL_W = CONTENT_PANEL_W - opad*2;
    private static final int SEGMENT_PANEL_H = 30;
    private static final int GOAL_PANEL_W = SEGMENT_PANEL_W;
    private static final int GOAL_PANEL_H = 70;

    private ContentPanel contentPanel;
    private PlanSelectionGrid gridPanel;

    public WorkerAllocationDialog() {
        super(PANEL_W, PANEL_H, null, null, str("uiDismiss"));

        backgroundDimAmount = 0.2f;
        holo.borderAlpha = 0.8f;

        setConfirmShortcut();

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final Button excludedBtn = new Button(200, BUTTON_H, str("uiBtnTitleClickToExcludeMarkets"), Fonts.DEFAULT_SMALL, (btn) -> {
            new ExcludedMarketsDialog().show(0.3f, 0.3f);
        });
        excludedBtn.setCutStyle(CutStyle.ALL);
        add(excludedBtn).inTR(pad, pad);

        final LabelAPI title = settings.createLabel(str("uiTitleWorkerAllocPlanner"), Fonts.INSIGNIA_VERY_LARGE);
        title.autoSizeToWidth(PANEL_W);
        title.setAlignment(Alignment.MID);
        add(title).inTL(0f, 0f).setSize(PANEL_W, TITLE_H);

        contentPanel = new ContentPanel(null); 
        add(contentPanel).inBR(0f, 0f);

        final Button createPlanBtn = new Button(LEFT_SIDE_W, CREATE_PLAN_BTN_H, str("uiBtnTitleCreateWorkerAllocationPlan"), Fonts.ORBITRON_20AA, (btn) -> {
            contentPanel.selectedPlan = new WorkerAllocationPlan();
            contentPanel.selectedPlan.isCustom = true;
            contentPanel.selectedPlan.id = Long.toString(Math.abs(Misc.genRandomSeed()));
            contentPanel.buildUI();
        });
        add(createPlanBtn).inTL(0f, TITLE_H);

        gridPanel = new PlanSelectionGrid(contentPanel);
        add(gridPanel).inBL(0f, 0f);
    }

    public class PlanSelectionGrid extends GridTable<WorkerAllocationPlan, PlanSelectionWidget> implements HasOutline {

        public PlanSelectionGrid(ContentPanel content) {
            super(LEFT_SIDE_W, PANEL_H - CREATE_PLAN_BTN_H, WIDGET_W, WIDGET_H, pad);

            uniformOuterGap = true;
            isSelectionEnabled = true;

            buildUI();
        }

        protected List<WorkerAllocationPlan> getDataList() {
            return PlanConfig.getPlansCopy();
        }

        protected PlanSelectionWidget createWidget(WorkerAllocationPlan item, int index) {
            return new PlanSelectionWidget(item);
        }

        protected void onWidgetClicked(PlanSelectionWidget source) {
            if (selectedWidget != null) {
                selectedWidget.isSelected = false;
                selectedWidget.buildUI();
            }
            selectedWidget = source;
            source.isSelected = true;
            contentPanel.selectedPlan = source.plan.copy();

            source.buildUI();
            contentPanel.buildUI();
        }

        protected String getEmptyMessage() {
            return str("uiTitleNoPlansToSelect");
        }
    }

    public static class PlanSelectionWidget extends UIClickable<PlanSelectionWidget> implements
        WidgetAPI<PlanSelectionWidget>, HasTooltip, HasHoverGlow
    {
        private final TooltipComp tooltip = comp().get(NativeComponents.TOOLTIP);
        private final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);
        private final BorderRenderer border = new BorderRenderer(UI_BORDER_4, true, WIDGET_W, WIDGET_H);
        private final WorkerAllocationPlan plan;

        public boolean isSelected = false;

        public PlanSelectionWidget(WorkerAllocationPlan plan) {
            super(WIDGET_W, WIDGET_H, null);
            this.plan = plan;

            glow.type = GlowType.UNDERLAY;
            glow.glowBrightness = 0.6f;
            glow.flashBrightness = 0.9f;
            glow.color = base;

            border.centerColor = UIColors.WIDGET_BG;

            tooltip.width = 500f;
            tooltip.builder = (tp, expanded) -> {
                tp.addTitle(plan.id, base);

                tp.addPara(plan.description, pad);

                tp.addPara(str(plan.isCustom ? "uiTpTxtCustomWorkerAllocationPlanNotice" : "uiTpTxtDefaultWorkerAllocationPlanNotice"), gray, pad);
            };

            buildUI();
        }

        @Override
        public void buildUI() {
            clearChildren();

            final int iconS = 32;

            border.centerColor = isSelected ? UIColors.WIDGET_BG_SELECTED : UIColors.WIDGET_BG;
            final SpriteElement icon = new SpriteElement(iconS, iconS, plan.isCustom ? CUSTOM : PRESET, base, null);
            add(icon).inLMid(pad);

            final LabelAPI title = settings.createLabel(plan.id, Fonts.ORBITRON_20AA);
            title.setAlignment(Alignment.MID);
            add(title).inRMid(pad).setSize(WIDGET_W - iconS - pad, WIDGET_H);
        }

        @Override
        public void renderBelowImpl(float alpha) {
            border.render(getX(), getY(), alpha);
        }

        public InteractionComp<PlanSelectionWidget> getInteraction() {
            return interaction;
        }
    }

    public class ContentPanel extends UIContainer implements UIBuildableAPI {
        private TextFieldAPI titleTextField = null;
        private TextFieldAPI descTextField = null;
        private boolean invalidId = false;

        private UIContainer content;
        private UIComponentAPI prev;
        private float yCoord;

        public WorkerAllocationPlan selectedPlan;

        public ContentPanel(WorkerAllocationPlan plan) {
            super(CONTENT_PANEL_W, CONTENT_PANEL_H);

            selectedPlan = plan;
            buildUI();
        }

        @Override
        public void buildUI() {
            clearChildren();
            content = new UIContainer(CONTENT_PANEL_W, 0f);
            prev = null;
            yCoord = 0f;

            if (selectedPlan == null) {
                final LabelAPI lbl = settings.createLabel(
                    str("uiTpTxtNoWorkerAllocationPlanSelected"), Fonts.DEFAULT_SMALL);
                lbl.setColor(gray);
                content.add(lbl).inMid();
            } else {
                final boolean editable = selectedPlan.isCustom;
                final int titleH = 30;
                final int descH = 100;

                addTitle(str("uiTpTitleWorkerAllocationPlanName"), base);

                if (editable) {
                    titleTextField = settings.createTextField(selectedPlan.id, Fonts.INSIGNIA_LARGE);
                    titleTextField.getPosition().setSize(300, titleH);
                    titleTextField.setLimitByStringWidth(true);
                    content.add(titleTextField).belowLeft(prev, pad);
                    prev = titleTextField;
                    yCoord += titleTextField.getPosition().getHeight() + pad;

                    if (invalidId) {
                        addPara(str("uiTpTxtDuplicateWorkerAllocationPlanName"), gray, pad);
                    }
                } else {
                    titleTextField = null;
                    addPara(selectedPlan.id, pad);
                }

                addPara(str("uiTpTitleWorkerAllocationPlanDesc"), base, pad);

                if (editable) {
                    descTextField = settings.createTextField(selectedPlan.description, Fonts.DEFAULT_SMALL);
                    descTextField.getPosition().setSize(CONTENT_PANEL_W - opad * 4, descH);
                    descTextField.setMaxChars(512);
                    descTextField.setLimitByStringWidth(false);
                    descTextField.setVerticalCursor(true);
                    content.add(descTextField).belowLeft(prev, pad);
                    prev = descTextField;
                    yCoord += descTextField.getPosition().getHeight() + pad;
                } else {
                    descTextField = null;
                    addPara(selectedPlan.description, pad);
                }

                if (editable) {
                    addSectionHeading(str("uiTpTitleWorkerAllocationObjectiveSettings"), Alignment.MID, opad);

                    final RadioPanel objectiveRadio = new RadioPanel(150, 45, LayoutMode.VERTICAL);
                    for (GoalType type : GoalType.values()) {
                        objectiveRadio.addOption(type.name(), selectedPlan.objConfig.goal == type);
                    }
                    objectiveRadio.optionSelected = (index) -> {
                        selectedPlan.objConfig.goal = GoalType.values()[index];
                    };
                    objectiveRadio.checkboxType = UICheckboxSize.SMALL;
                    objectiveRadio.checkboxSize = 20;
                    objectiveRadio.buildUI();

                    content.add(objectiveRadio).belowLeft(prev, opad);
                    prev = objectiveRadio;
                    yCoord += objectiveRadio.getHeight() + opad;

                    final Slider iterCountSlider = new Slider(null, 100, 100000, 300, 32);
                    iterCountSlider.roundBarValue = true;
                    iterCountSlider.roundingIncrement = 1;
                    iterCountSlider.showValueOnly = true;
                    iterCountSlider.setProgress(selectedPlan.objConfig.maxIter);
                    iterCountSlider.customText = () -> {
                        selectedPlan.objConfig.maxIter = (int) iterCountSlider.getProgress();
                        return String.format("%.0f", iterCountSlider.getProgress());
                    };
                    content.add(iterCountSlider).belowLeft(prev, pad);
                    iterCountSlider.getPosition().rightOfBottom(objectiveRadio, opad);
                    prev = iterCountSlider;
                    yCoord += iterCountSlider.getHeight() + pad;

                    final LabelAPI iterLbl = settings.createLabel(
                        str("uiTpTitleSimplexSolverIterCount"), Fonts.DEFAULT_SMALL);
                    iterLbl.setColor(base);
                    iterLbl.setAlignment(Alignment.TL);
                    iterLbl.autoSizeToWidth(CONTENT_WIDTH);
                    content.add(iterLbl);
                    iterLbl.getPosition().aboveLeft(iterCountSlider, pad);
                    prev = (UIComponentAPI) iterLbl;
                    yCoord += iterLbl.getPosition().getHeight() + pad;

                    yCoord -= 50f;
                    final UIElement flowReset = new UIElement(CONTENT_PANEL_W, 0f);
                    content.add(flowReset).inTL(hpad, yCoord);
                    prev = flowReset;

                    addSectionHeading(str("uiTpTitleWorkerAllocationPlanSegments"), Alignment.MID, opad);

                    final Button addSegmentBtn = new Button(100, 25,
                        str("uiBtnTitleAddWorkerAllocationPlanSegment"), Fonts.DEFAULT_SMALL, (btn) -> {
                        selectedPlan.segments.segments.put("segment", new PiecewiseSegment(1d, "segment"));
                        buildUI();
                    });
                    content.add(addSegmentBtn).belowLeft(prev, pad);
                    prev = addSegmentBtn;
                    yCoord += addSegmentBtn.getPosition().getHeight() + pad;

                    for (PiecewiseSegment seg : selectedPlan.segments.segments.values()) {
                        final SegmentPanel segPanel = new SegmentPanel(seg, selectedPlan.segments);
                        content.add(segPanel).belowLeft(prev, hpad);
                        prev = segPanel;
                        yCoord += segPanel.getPosition().getHeight() + hpad;
                    }

                    addSectionHeading(str("uiTpTitleWorkerAllocationPlanGoals"), Alignment.MID, opad);

                    final Button addGoalBtn = new Button(100, 25,
                        str("uiBtnTitleAddWorkerAllocationPlanGoal"), Fonts.DEFAULT_SMALL, (btn) -> {
                        new AddGoalDialog(this).show(0.3f, 0.3f);
                    });
                    content.add(addGoalBtn).belowLeft(prev, pad);
                    prev = addGoalBtn;
                    yCoord += addGoalBtn.getPosition().getHeight() + pad;

                    for (CustomGoal goal : selectedPlan.goals) {
                        final GoalPanel goalPanel = new GoalPanel(this, goal);
                        content.add(goalPanel).belowLeft(prev, opad);
                        prev = goalPanel;
                        yCoord += goalPanel.getPosition().getHeight() + opad;
                    }
                }

                if (editable) {
                    final Button savePlanBtn = new Button(100, 30,
                        str("uiBtnTitleSaveWorkerAllocationPlan"), null, null);
                    content.add(savePlanBtn);
                    savePlanBtn.getPosition().inBL(hpad, hpad);
                    yCoord += savePlanBtn.getPosition().getHeight();
                    savePlanBtn.onClicked = (btn) -> {
                        try {
                            PlanConfig.map.put(selectedPlan.id, selectedPlan);
                            PlanConfigLoader.serializeAndWriteToCommon();
                            gridPanel.buildUI();
                        } catch (Exception e) {
                            new DialogPanel(400, 100, null,
                                str("uiTitleFailedToSaveWorkerAllocationPlan") + e.toString(),
                                str("uiDismiss")).show(0.3f, 0.3f);
                            log.error(e.toString());
                        }
                    };

                    final Button deletePlanBtn = new Button(100, 30,
                        str("uiBtnTitleDeleteWorkerAllocationPlan"), null, null);
                    content.add(deletePlanBtn);
                    deletePlanBtn.getPosition().rightOfMid(savePlanBtn, hpad);
                    yCoord += deletePlanBtn.getPosition().getHeight();
                    deletePlanBtn.onClicked = (btn) -> {
                        try {
                            PlanConfig.map.remove(selectedPlan.id);
                            PlanConfigLoader.serializeAndWriteToCommon();
                            selectedPlan = null;
                            gridPanel.buildUI();
                            buildUI();
                        } catch (Exception e) {
                            new DialogPanel(400, 100, null,
                                str("uiTitleFailedToDeleteWorkerAllocationPlan") + e.toString(),
                                str("uiDismiss")).show(0.3f, 0.3f);
                            log.error(e.toString());
                        }
                    };
                }

                final Button runPlanBtn = new Button(100, 30,
                    str("uiBtnTitleRunWorkerAllocationPlan"), null, null);
                content.add(runPlanBtn);
                runPlanBtn.getPosition().inBR(hpad, hpad);
                yCoord += runPlanBtn.getPosition().getHeight();
                runPlanBtn.onClicked = (btn) -> {
                    try {
                        EconomyEngine.instance().assignPlayerWorkers(selectedPlan);
                        new DialogPanel(400, 100, null,
                            str("uiTitleWorkerAllocationPlanRunSuccess"),
                            str("uiAcknowledged")).show(0.3f, 0.3f);
                        LtvEconSaveData.instance().playerFactionSettings.factionPlan = selectedPlan;
                    } catch (Exception e) {
                        new DialogPanel(400, 100, null,
                            str("uiTitleFailedToRunWorkerAllocationPlan") + e.toString(),
                            str("uiDismiss")).show(0.3f, 0.3f);
                        log.error(e.toString());
                        LtvEconSaveData.instance().playerFactionSettings.factionPlan = null;
                    }
                };
            }

            content.setSize(CONTENT_PANEL_W, Math.max(yCoord, CONTENT_PANEL_H));
            add(ComponentFactory.wrapWithScrollPanel(content, CONTENT_PANEL_W, CONTENT_PANEL_H)).inBL(0f, 0f);
        }

        @Override
        public void advanceImpl(float delta) {
            super.advanceImpl(delta);

            if (selectedPlan == null || !selectedPlan.isCustom) return;
            if (titleTextField == null || descTextField == null) return;

            selectedPlan.description = descTextField.getText();

            final boolean wasInvalid = invalidId;
            invalidId = false;
            final String titleText = titleTextField.getText();
            for (WorkerAllocationPlan plan : PlanConfig.getPlansCopy()) {
                if (!plan.equals(selectedPlan) && plan.id.equals(titleText)) {
                    invalidId = true;
                    break;
                }
            }
            if (!selectedPlan.id.equals(titleText)) {
                if (invalidId && !wasInvalid) buildUI();
                if (!invalidId && wasInvalid) buildUI();
                else selectedPlan.id = titleText;
            }
        }
    
        private final LabelAPI addPara(String text, Color color, float gap) {
            final LabelAPI lbl = settings.createLabel(text, Fonts.DEFAULT_SMALL);
            lbl.setColor(color);
            lbl.setAlignment(Alignment.TL);
            lbl.autoSizeToWidth(CONTENT_WIDTH);
            if (prev != null) {
                content.add(lbl).belowLeft(prev, gap);
            } else {
                content.add(lbl).inTL(5f, gap);
            }
            prev = (UIComponentAPI) lbl;
            yCoord += lbl.getPosition().getHeight() + gap;
            return lbl;
        }

        private final LabelAPI addPara(String text, float gap) {
            return addPara(text, Global.getSettings().getColor("standardTextColor"), gap);
        }

        private final LabelAPI addTitle(String text, Color color) {
            final LabelAPI lbl = settings.createLabel(text, Fonts.ORBITRON_12);
            lbl.setColor(color);
            lbl.setAlignment(Alignment.LMID);
            lbl.autoSizeToWidth(CONTENT_WIDTH);
            content.add(lbl).inTL(5f, 0f);
            prev = (UIComponentAPI) lbl;
            yCoord += lbl.getPosition().getHeight();
            return lbl;
        }

        private final LabelAPI addSectionHeading(String text, Alignment alignment, float gap) {
            final LabelAPI headLbl = settings.createLabel(text, Fonts.ORBITRON_12);
            headLbl.setColor(base);
            headLbl.setAlignment(alignment);
            headLbl.autoSizeToWidth(CONTENT_PANEL_W);
            final int headLblH = (int) headLbl.getPosition().getHeight() + 3;
            headLbl.getPosition().setSize(CONTENT_PANEL_W, headLblH);
            RolfLectionUtil.getMethodAndInvokeDirectly("setBackgroundColor", headLbl, dark);

            final UIElement spacer = new UIElement(CONTENT_PANEL_W, headLblH);
            if (prev != null) {
                content.add(headLbl).belowLeft(prev, gap).setXAlignOffset(-5f);
                content.add(spacer).belowLeft(prev, gap);
            } else {
                content.add(headLbl).inTL(0f, 0f);
                content.add(spacer).inTL(5f, 0f);
            }
            prev = spacer;
            yCoord += headLbl.getPosition().getHeight() + gap;
            return headLbl;
        }
    }

    public static class SegmentPanel extends UIContainer {
        private final PiecewiseSegment segment;
        private final PiecewiseSegments segments;
        private final TextFieldAPI idField;
        private final TextFieldAPI valueField;

        public SegmentPanel(PiecewiseSegment seg, PiecewiseSegments segs) {
            super(SEGMENT_PANEL_W, SEGMENT_PANEL_H);

            segment = seg;
            segments = segs;

            idField = settings.createTextField(seg.id, Fonts.DEFAULT_SMALL);
            idField.getPosition().setSize(SEGMENT_PANEL_W / 2 - opad * 2, SEGMENT_PANEL_H - opad);
            idField.setMaxChars(64);
            add(idField).inBL(hpad, hpad);

            valueField = settings.createTextField(Double.toString(seg.cost), Fonts.DEFAULT_SMALL);
            valueField.getPosition().setSize(SEGMENT_PANEL_W / 2 - opad * 2, SEGMENT_PANEL_H - opad);
            valueField.setMaxChars(64);
            add(valueField).inBR(hpad, hpad);
        }

        @Override
        public void advanceImpl(float delta) {
            super.advanceImpl(delta);

            if (!idField.getText().isBlank() && !segment.id.equals(idField.getText())) {
                segments.segments.remove(segment.id);
                segment.id = idField.getText();
                segments.segments.put(segment.id, segment);
            }

            try {
                final Double val = Double.valueOf(valueField.getText());
                if (val != null) segment.cost = val.doubleValue();
            } catch (NumberFormatException e) {}
        }
    }

    public static class GoalPanel extends UIContainer {
        public GoalPanel(ContentPanel content, CustomGoal goal) {
            super(GOAL_PANEL_W, GOAL_PANEL_H);

            final LabelAPI title = settings.createLabel(goal.getSerializationId(), Fonts.INSIGNIA_LARGE);
            add(title).inTL(hpad, hpad);

            final RemoveGoalBtn removeBtn = new RemoveGoalBtn(content, goal);
            add(removeBtn).inTR(hpad, hpad);

            if (goal.getIcon() != null) {
                final SpriteElement icon = new SpriteElement(28, 28, goal.getIcon(), null, null);
                add(icon).rightOfMid((UIComponentAPI) title, opad);
            }

            float yStart = opad + title.getPosition().getHeight();
            final float settingTitleH = 20f;
            for (GoalParameter param : goal.getParameters()) {
                final LabelAPI settingTitle = settings.createLabel(param.name, Fonts.DEFAULT_SMALL);
                add(settingTitle).inTL(hpad, yStart);
                yStart += settingTitleH;

                switch (param.getParamType()) {
                case BOOLEAN:
                    final BooleanParameter boolParam = (BooleanParameter) param;
                    final CheckboxButton btn = new CheckboxButton(22, param.name, Fonts.DEFAULT_SMALL, null, UICheckboxSize.SMALL, false);
                    btn.onClicked = (button) -> {
                        button.setChecked(!button.isChecked());
                        boolParam.setter.accept(button.isChecked());
                    };
                    btn.setChecked(boolParam.getValue());
                    add(btn).inTL(hpad, yStart);
                    break;

                case DOUBLE:
                    final DoubleParameter doubleParam = (DoubleParameter) param;
                    final Slider slider = new Slider(null, (float)doubleParam.getMin(), (float)doubleParam.getMax(), GOAL_PANEL_W - opad*2, 32);
                    slider.customText = () -> {
                        doubleParam.setValue(slider.getProgress()); // using the custom text setter as a listener.
                        return String.format("%.2f", slider.getProgress());
                    };
                    slider.setProgress((float) doubleParam.getValue());
                    add(slider).inTL(hpad, yStart);
                    break;

                case MULTI_SELECT:
                    final MultiSelectParameter multiParam = (MultiSelectParameter) param;
                    final MultiSelect multiPanel = new MultiSelect(GOAL_PANEL_W - opad*2, GOAL_PANEL_H - 25, multiParam.getAllOptions(), LayoutMode.VERTICAL);
                    for (String val : multiParam.getValue()) {
                        multiPanel.selectFirst(val);
                    }
                    multiPanel.onSelected = (button) -> {
                        multiParam.setValue(new HashSet<>(multiPanel.getSelectedStrings()));
                    };
                    multiPanel.buildUI();
                    add(multiPanel).inTL(hpad, yStart);
                    break;

                case RADIO:
                    final RadioParameter radioParam = (RadioParameter) param;
                    final RadioPanel radioPanel = new RadioPanel(GOAL_PANEL_W - opad*2, GOAL_PANEL_H - 25, LayoutMode.VERTICAL);
                    for (String opt : radioParam.getAllOptions()) {
                        radioPanel.addOption(opt, opt.equals(radioParam.getter.get()));
                    }
                    radioPanel.optionSelected = (index) -> {
                        radioParam.setter.accept(radioParam.getAllOptions().get(index));
                    };
                    radioPanel.buildUI();
                    add(radioPanel).inTL(hpad, yStart);
                    break;
            
                default:
                    throw new IllegalArgumentException("Unhandled type: " + param.getParamType().name());
                }

                yStart += 50;
            }

            setHeight((int) Math.max(yStart, GOAL_PANEL_H));
        }
    }

    public static class AddGoalDialog extends DialogPanel {
        
        public AddGoalDialog(ContentPanel content) {
            super(460, 350, null, str("uiDialogTitleSelectWorkerAllocationPlanGoal"), str("uiCancel"));

            backgroundDimAmount = 0.1f;
            holo.borderAlpha = 0.66f;

            setConfirmShortcut();

            final GoalRegistryGrid grid = new GoalRegistryGrid(this, content);
            add(grid).inTL(0f, 25f);
        }
    }

    public static class GoalRegistryGrid extends GridTable<String, GoalRegistryWidget> {
        private final ContentPanel content;
        private final DialogPanel dialog;

        public GoalRegistryGrid(DialogPanel dialog, ContentPanel content) {
            super(460, 330, 450, 30, hpad);
            this.content = content;
            this.dialog = dialog;

            uniformOuterGap = true;
            isSelectionEnabled = true;
            buildUI();
        }

        protected List<String> getDataList() {
            return new ArrayList<>(PlanningGoalRegistry.getRegisteredIds());
        }

        protected GoalRegistryWidget createWidget(String id, int index) {
            return new GoalRegistryWidget(id);
        }

        @Override
        protected void onWidgetClicked(GoalRegistryWidget source) {
            content.selectedPlan.goals.add(PlanningGoalRegistry.createGoal(source.goalId));
            content.buildUI();
            dialog.dismiss(0);
        }

        protected String getEmptyMessage() {
            return str("uiTitleNoGoalsToSelect");
        }
    }

    public static class GoalRegistryWidget extends UIClickable<GoalRegistryWidget> implements WidgetAPI<GoalRegistryWidget>, HasHoverGlow {

        final HoverGlowComp glow = comp().get(NativeComponents.HOVER_GLOW);

        private final String goalId;

        public GoalRegistryWidget(String goalId) {
            super(450, 30, null);

            glow.color = base;
            glow.type = GlowType.UNDERLAY;

            this.goalId = goalId;
            buildUI();
        }

        @Override
        public void buildUI() {
            final CustomGoal goal = PlanningGoalRegistry.createGoal(goalId);
            if (goal.getIcon() != null) {
                final SpriteElement icon = new SpriteElement(28, 28, goal.getIcon(), null, null);
                add(icon).inLMid(0f);
            }

            final LabelAPI lbl = settings.createLabel(goalId, Fonts.DEFAULT_SMALL);
            add(lbl).setSize(410, 30).inLMid(40f);
            lbl.setAlignment(Alignment.LMID);
        }

        public InteractionComp<GoalRegistryWidget> getInteraction() {
            return interaction;
        }
    }

    public static class RemoveGoalBtn extends Button {
        private static final SpriteAPI ICON = settings.getSprite("warroom", "icon_close");

        public RemoveGoalBtn(final ContentPanel content, final CustomGoal goal) {
            super(20, 20, null, null, (btn) -> {
                content.selectedPlan.goals.remove(goal);
                content.buildUI();
            });

            bgAlpha = 0f;
            bgDisabledAlpha = 0f;

            final SpriteElement icon = new SpriteElement(20, 20, ICON, Color.RED, null);
            add(icon).inBL(0f, 0f);
            glow.type = GlowType.ADDITIVE;
            glow.additiveSprite = icon.getSprite();
        }
    }
}
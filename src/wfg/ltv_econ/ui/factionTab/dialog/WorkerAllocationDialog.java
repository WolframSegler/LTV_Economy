package wfg.ltv_econ.ui.factionTab.dialog;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.ui.ButtonAPI.UICheckboxSize;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.PlanConfig;
import wfg.ltv_econ.config.PlanConfig.WorkerAllocationPlan;
import wfg.ltv_econ.config.loader.PlanConfigLoader;
import wfg.ltv_econ.constants.UIColors;
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
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.CheckboxButton;
import wfg.native_ui.ui.functional.UIClickable;
import wfg.native_ui.ui.functional.Button.CutStyle;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.table.GridTable;
import wfg.native_ui.ui.table.WidgetAPI;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.ui.widget.MultiSelect;
import wfg.native_ui.ui.widget.RadioPanel;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.widget.RadioPanel.LayoutMode;
import wfg.native_ui.util.NativeUiUtils;

public class WorkerAllocationDialog extends DialogPanel {
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
    private static final int SEGMENT_PANEL_W = CONTENT_PANEL_W - opad*2;
    private static final int SEGMENT_PANEL_H = 30;
    private static final int GOAL_PANEL_W = SEGMENT_PANEL_W;
    private static final int GOAL_PANEL_H = 70;

    private ContentPanel contentPanel;
    private PlanSelectionGrid gridPanel;

    public WorkerAllocationDialog() {
        super(PANEL_W, PANEL_H, null, null, str("dismissTxt"));

        backgroundDimAmount = 0.2f;
        holo.borderAlpha = 0.8f;

        setConfirmShortcut();

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        final Button excludedBtn = new Button(m_panel, 200, BUTTON_H, str("uiBtnTitleClickToExcludeMarkets"), Fonts.DEFAULT_SMALL, (btn) -> {
            new ExcludedMarketsDialog().show(0.3f, 0.3f);
        });
        excludedBtn.cutStyle = CutStyle.ALL;
        add(excludedBtn).inTR(pad, pad);

        final LabelAPI title = settings.createLabel(str("uiTitleWorkerAllocPlanner"), Fonts.INSIGNIA_VERY_LARGE);
        title.autoSizeToWidth(PANEL_W);
        title.setAlignment(Alignment.MID);
        add(title).inTL(0f, 0f).setSize(PANEL_W, TITLE_H);

        contentPanel = new ContentPanel(m_panel, null); 
        add(contentPanel).inBR(0f, 0f);

        final Button createPlanBtn = new Button(m_panel, LEFT_SIDE_W, CREATE_PLAN_BTN_H, str("uiBtnTitleCreateWorkerAllocationPlan"), Fonts.ORBITRON_20AA, (btn) -> {
            contentPanel.selectedPlan = new WorkerAllocationPlan();
            contentPanel.selectedPlan.isCustom = true;
            contentPanel.selectedPlan.id = Long.toString(Misc.genRandomSeed());
            contentPanel.buildUI();
        });
        add(createPlanBtn).inTL(0f, TITLE_H);

        gridPanel = new PlanSelectionGrid(m_panel, contentPanel);
        add(gridPanel).inBL(0f, 0f);
    }

    public class PlanSelectionGrid extends GridTable<WorkerAllocationPlan, PlanSelectionWidget> implements HasOutline {

        public PlanSelectionGrid(UIPanelAPI parent, ContentPanel content) {
            super(parent, LEFT_SIDE_W, PANEL_H - CREATE_PLAN_BTN_H, WIDGET_W, WIDGET_H, pad);

            uniformOuterGap = true;
            isSelectionEnabled = true;

            buildUI();
        }

        protected List<WorkerAllocationPlan> getDataList() {
            return PlanConfig.getPlansCopy();
        }

        protected PlanSelectionWidget createWidget(WorkerAllocationPlan item, int index) {
            return new PlanSelectionWidget(PlanSelectionGrid.this, item);
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

        public PlanSelectionWidget(PlanSelectionGrid parent, WorkerAllocationPlan plan) {
            super(parent.getPanel(), WIDGET_W, WIDGET_H, null);
            this.plan = plan;

            glow.type = GlowType.UNDERLAY;
            glow.overlayBrightness = 0.6f;
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
            final Base icon = new Base(m_panel, iconS, iconS, plan.isCustom ? CUSTOM : PRESET, base, null);
            add(icon).inLMid(pad);

            final LabelAPI title = settings.createLabel(plan.id, Fonts.ORBITRON_20AA);
            title.setAlignment(Alignment.MID);
            add(title).inRMid(pad).setSize(WIDGET_W - iconS - pad, WIDGET_H);
        }

        @Override
        public void renderBelow(float alpha) {
            super.renderBelow(alpha);

            border.render(pos.getX(), pos.getY(), alpha);
        }

        public InteractionComp<PlanSelectionWidget> getInteraction() {
            return interaction;
        }

        public UIComponentAPI getElement() {
            return m_panel;
        }
    }

    public class ContentPanel extends CustomPanel implements UIBuildableAPI {
        private TextFieldAPI titleTextField = null;
        private TextFieldAPI descTextField = null;
        private boolean invalidId = false;

        public WorkerAllocationPlan selectedPlan;

        public ContentPanel(UIPanelAPI parent, WorkerAllocationPlan plan) {
            super(parent, CONTENT_PANEL_W, CONTENT_PANEL_H);

            selectedPlan = plan;
            buildUI();
        }

        @Override
        public void buildUI() {
            clearChildren();
            final TooltipMakerAPI container = ComponentFactory.createTooltip(CONTENT_PANEL_W, true);

            if (selectedPlan == null) {
                container.addPara(str("uiTpTxtNoWorkerAllocationPlanSelected"), gray, pad).getPosition().inMid();
            } else {
                final boolean editable = selectedPlan.isCustom;

                final int titleH = 30;
                final int descH = 100;

                container.addTitle(str("uiTpTitleWorkerAllocationPlanName"), base);

                if (editable) {
                    titleTextField = container.addTextField(300, titleH, Fonts.INSIGNIA_LARGE, pad);
                    titleTextField.setLimitByStringWidth(true);
                    titleTextField.setText(selectedPlan.id);

                    if (invalidId) {
                        container.addPara(str("uiTpTxtDuplicateWorkerAllocationPlanName"), gray, pad);
                    }
                } else {
                    titleTextField = null;
                    container.addPara(selectedPlan.id, pad);
                }

                container.addPara(str("uiTpTitleWorkerAllocationPlanDesc"), base, pad);

                if (editable) {
                    descTextField = container.addTextField(CONTENT_PANEL_W - opad*4, descH, Fonts.DEFAULT_SMALL, pad);
                    descTextField.setMaxChars(512);
                    descTextField.setLimitByStringWidth(false);
                    descTextField.setVerticalCursor(true);
                    descTextField.setText(selectedPlan.description);
                } else {
                    descTextField = null;
                    container.addPara(selectedPlan.description, pad);
                }

                if (editable) {
                    container.addSectionHeading(str("uiTpTitleWorkerAllocationObjectiveSettings"), Alignment.MID, opad);
                    
                    final RadioPanel objectiveRadio = new RadioPanel(container, 150, 45, LayoutMode.VERTICAL);
                    for (GoalType type : GoalType.values()) {
                        objectiveRadio.addOption(type.name(), selectedPlan.objConfig.goal == type);
                    }
                    objectiveRadio.optionSelected = (index) -> {
                        selectedPlan.objConfig.goal = GoalType.values()[index];
                    };
                    objectiveRadio.checkboxType = UICheckboxSize.SMALL;
                    objectiveRadio.checkboxSize = 20;
                    objectiveRadio.buildUI();
                    
                    container.addCustom(objectiveRadio.getPanel(), opad);

                    final Slider iterCountSlider = new Slider(container, null, 100, 100000, 300, 32);
                    iterCountSlider.roundBarValue = true;
                    iterCountSlider.roundingIncrement = 1;
                    iterCountSlider.showValueOnly = true;
                    iterCountSlider.setProgress(selectedPlan.objConfig.maxIter);
                    iterCountSlider.customText = () -> {
                        selectedPlan.objConfig.maxIter = (int) iterCountSlider.getProgress();
                        return String.format("%.0f", iterCountSlider.getProgress());
                    };
                    container.addCustom(iterCountSlider.getPanel(), pad).getPosition().rightOfBottom(objectiveRadio.getPanel(), opad);
                    container.addPara(str("uiTpTitleSimplexSolverIterCount"), base, pad).getPosition().aboveLeft(iterCountSlider.getPanel(), pad);

                    container.setHeightSoFar(container.getHeightSoFar() - 50);
                    NativeUiUtils.resetFlowLeft(container, hpad);

                    container.addSectionHeading(str("uiTpTitleWorkerAllocationPlanSegments"), Alignment.MID, opad);

                    final Button addSegmentBtn = new Button(container, 100, 25, str("uiBtnTitleAddWorkerAllocationPlanSegment"), Fonts.DEFAULT_SMALL, (btn) -> {
                        selectedPlan.segments.segments.put("segment", new PiecewiseSegment(1d, "segment"));
                        buildUI();
                    });
                    container.addCustom(addSegmentBtn.getPanel(), pad);

                    for (PiecewiseSegment seg : selectedPlan.segments.segments.values()) {
                        final SegmentPanel segPanel = new SegmentPanel(container, seg, selectedPlan.segments);
                        container.addCustom(segPanel.getPanel(), hpad);
                    }

                    container.addSectionHeading(str("uiTpTitleWorkerAllocationPlanGoals"), Alignment.MID, opad);

                    final Button addGoalBtn = new Button(container, 100, 25, str("uiBtnTitleAddWorkerAllocationPlanGoal"), Fonts.DEFAULT_SMALL, (btn) -> {
                        new AddGoalDialog(this).show(0.3f, 0.3f);
                    });
                    container.addCustom(addGoalBtn.getPanel(), pad);

                    for (CustomGoal goal : selectedPlan.goals) {
                        final GoalPanel goalPanel = new GoalPanel(container, this, goal);
                        container.addCustom(goalPanel.getPanel(), opad);
                    }
                }

                if (editable) {
                    final Button savePlanBtn = new Button(container, 100, 30, str("uiBtnTitleSaveWorkerAllocationPlan"), null, null);
                    container.addCustom(savePlanBtn.getPanel(), 0f).getPosition().inBL(hpad, hpad);
                    savePlanBtn.onClicked = (btn) -> {
                        try {
                            PlanConfig.map.put(selectedPlan.id, selectedPlan);
                            PlanConfigLoader.serializeAndWriteToCommon();
                            gridPanel.buildUI();
                        } catch (Exception e) {
                            new DialogPanel(400, 100, null, str("uiTitleFailedToSaveWorkerAllocationPlan") + e.toString(), str("dismissTxt"))
                                .show(0.3f, 0.3f);
                        }
                    };

                    final Button deletePlanBtn = new Button(container, 100, 30, str("uiBtnTitleDeleteWorkerAllocationPlan"), null, null);
                    container.addCustom(deletePlanBtn.getPanel(), 0f).getPosition().rightOfMid(savePlanBtn.getPanel(), hpad);
                    deletePlanBtn.onClicked = (btn) -> {
                        try {
                            PlanConfig.map.remove(selectedPlan.id);
                            PlanConfigLoader.serializeAndWriteToCommon();
                            selectedPlan = null;
                            gridPanel.buildUI();
                            buildUI();
                        } catch (Exception e) {
                            new DialogPanel(400, 100, null, str("uiTitleFailedToDeleteWorkerAllocationPlan") + e.toString(), str("dismissTxt"))
                                .show(0.3f, 0.3f);
                        }
                    };
                }
                final Button runPlanBtn = new Button(container, 100, 30, str("uiBtnTitleRunWorkerAllocationPlan"), null, null);
                container.addCustom(runPlanBtn.getPanel(), 0f).getPosition().inBR(hpad, hpad);
                runPlanBtn.onClicked = (btn) -> {
                    try {
                        EconomyEngine.instance().assignPlayerWorkers(selectedPlan);
                        new DialogPanel(400, 100, null, str("uiTitleWorkerAllocationPlanRunSuccess"), str("uiAcknowledged"))
                            .show(0.3f, 0.3f);

                    } catch (Exception e) {
                        new DialogPanel(400, 100, null, str("uiTitleFailedToRunWorkerAllocationPlan") + e.toString(), str("dismissTxt"))
                            .show(0.3f, 0.3f);
                    }
                };
            }

            ComponentFactory.addTooltip(container, CONTENT_PANEL_H, true, m_panel).inBL(0f, 0f);
        }

        @Override
        public void advance(float delta) {
            super.advance(delta);

            if (selectedPlan == null || !selectedPlan.isCustom) return;
            if (titleTextField == null || descTextField == null) return;

            selectedPlan.description = descTextField.getText();

            invalidId = false;
            final String titleText = titleTextField.getText();
            for (WorkerAllocationPlan plan : PlanConfig.getPlansCopy()) {
                if (!plan.equals(selectedPlan) && plan.id.equals(titleText)) {
                    invalidId = true;
                }
            }
            if (!selectedPlan.id.equals(titleText)) {
                if (invalidId) buildUI();
                else selectedPlan.id = titleText;
            }
        }
    }

    public static class SegmentPanel extends CustomPanel {
        private final PiecewiseSegment segment;
        private final PiecewiseSegments segments;
        private final TextFieldAPI idField;
        private final TextFieldAPI valueField;

        public SegmentPanel(UIPanelAPI parent, PiecewiseSegment seg, PiecewiseSegments segs) {
            super(parent, SEGMENT_PANEL_W, SEGMENT_PANEL_H);

            segment = seg;
            segments = segs;

            final TooltipMakerAPI tp = ComponentFactory.createTooltip(SEGMENT_PANEL_W, false);

            idField = tp.addTextField(SEGMENT_PANEL_W / 2 - opad*2, SEGMENT_PANEL_H - opad, Fonts.DEFAULT_SMALL, pad);
            idField.setMaxChars(64);
            idField.setText(seg.id);
            idField.getPosition().inBL(hpad, hpad);

            valueField = tp.addTextField(SEGMENT_PANEL_W / 2 - opad*2, SEGMENT_PANEL_H - opad, Fonts.DEFAULT_SMALL, pad);
            valueField.setMaxChars(64);
            valueField.setText(Double.toString(seg.cost));
            valueField.getPosition().inBR(hpad, hpad);

            ComponentFactory.addTooltip(tp, SEGMENT_PANEL_H - opad, false, m_panel).inBL(0f, 0f);
        }

        @Override
        public void advance(float delta) {
            super.advance(delta);

            if (!idField.getText().isBlank() && !segment.id.equals(idField.getText()))  {
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

    public static class GoalPanel extends CustomPanel {
        public GoalPanel(UIPanelAPI parent, ContentPanel content, CustomGoal goal) {
            super(parent, GOAL_PANEL_W, GOAL_PANEL_H);

            final LabelAPI title = settings.createLabel(goal.getSerializationId(), Fonts.INSIGNIA_LARGE);
            add(title).inTL(hpad, hpad);

            final RemoveGoalBtn removeBtn = new RemoveGoalBtn(m_panel, content, goal);
            add(removeBtn).inTR(hpad, hpad);

            if (goal.getIcon() != null) {
                final Base icon = new Base(m_panel, 28, 28, goal.getIcon(), null, null);
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
                    final CheckboxButton btn = new CheckboxButton(m_panel, 22, param.name, Fonts.DEFAULT_SMALL, null, UICheckboxSize.SMALL, false);
                    btn.onClicked = (button) -> {
                        button.setChecked(!button.isChecked());
                        boolParam.setter.accept(button.isChecked());
                    };
                    btn.setChecked(boolParam.getValue());
                    add(btn).inTL(hpad, yStart);
                    break;

                case DOUBLE:
                    final DoubleParameter doubleParam = (DoubleParameter) param;
                    final Slider slider = new Slider(m_panel, null, (float)doubleParam.getMin(), (float)doubleParam.getMax(), GOAL_PANEL_W - opad*2, 32);
                    slider.customText = () -> {
                        doubleParam.setValue(slider.getProgress()); // using the custom text setter as a listener.
                        return String.format("%.2f", slider.getProgress());
                    };
                    slider.setProgress((float) doubleParam.getValue());
                    add(slider).inTL(hpad, yStart);
                    break;

                case MULTI_SELECT:
                    final MultiSelectParameter multiParam = (MultiSelectParameter) param;
                    final MultiSelect multiPanel = new MultiSelect(m_panel, GOAL_PANEL_W - opad*2, GOAL_PANEL_H - 25, multiParam.getAllOptions(), LayoutMode.VERTICAL);
                    for (String val : multiParam.getValue()) {
                        multiPanel.selectFirst(val);
                    }
                    multiPanel.onSelected = (multi) -> {
                        multiParam.setValue(new HashSet<>(multiPanel.getSelectedStrings()));
                    };
                    multiPanel.buildUI();
                    add(multiPanel).inTL(hpad, yStart);
                    break;

                case RADIO:
                    final RadioParameter radioParam = (RadioParameter) param;
                    final RadioPanel radioPanel = new RadioPanel(m_panel, GOAL_PANEL_W - opad*2, GOAL_PANEL_H - 25, LayoutMode.VERTICAL);
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
            super(dialog.getPanel(), 460, 330, 450, 30, hpad);
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
            return new GoalRegistryWidget(m_panel, id);
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

        public GoalRegistryWidget(UIPanelAPI parent, String goalId) {
            super(parent, 450, 30, null);

            glow.color = base;
            glow.type = GlowType.UNDERLAY;

            this.goalId = goalId;
            buildUI();
        }

        @Override
        public void buildUI() {
            final CustomGoal goal = PlanningGoalRegistry.createGoal(goalId);
            if (goal.getIcon() != null) {
                final Base icon = new Base(m_panel, 28, 28, goal.getIcon(), null, null);
                add(icon).inLMid(0f);
            }

            final LabelAPI lbl = settings.createLabel(goalId, Fonts.DEFAULT_SMALL);
            add(lbl).setSize(410, 30).inLMid(40f);
            lbl.setAlignment(Alignment.LMID);
        }

        public InteractionComp<GoalRegistryWidget> getInteraction() {
            return interaction;
        }

        public UIComponentAPI getElement() {
            return m_panel;
        }
    }

    public static class RemoveGoalBtn extends Button {
        private static final SpriteAPI ICON = settings.getSprite("warroom", "icon_close");

        public RemoveGoalBtn(final UIPanelAPI parent, final ContentPanel content, final CustomGoal goal) {
            super(parent, 20, 20, null, null, (btn) -> {
                content.selectedPlan.goals.remove(goal);
                content.buildUI();
            });

            bgAlpha = 0f;
            bgDisabledAlpha = 0f;

            final Base icon = new Base(m_panel, 20, 20, ICON, Color.RED, null);
            add(icon).inBL(0f, 0f);
            glow.type = GlowType.ADDITIVE;
            glow.additiveSprite = icon.getSprite();
        }
    }
}
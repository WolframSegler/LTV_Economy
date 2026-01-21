package wfg.ltv_econ.ui.scripts;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.command.CommandTabData;
import com.fs.starfarer.campaign.command.OutpostItemRow;
import com.fs.starfarer.campaign.command.OutpostListPanel;
import com.fs.starfarer.campaign.econ.Market;
import com.fs.starfarer.campaign.ui.UITable;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.plugins.MarketWrapper;
import wfg.ltv_econ.ui.panels.EconomyOverviewPanel;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.util.CallbackRunnable;

public class OutpostsTabUIBuilder implements EveryFrameScript, CallbackRunnable<Button> {
    public static final int BUTTON_TAB_ID = 5;

    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_WIDTH = 130;

    public static final Object outpostRowMarketWrapperField;
    public static final Class<?> outpostRowMarketWrapperClass;
    public static final Object wrapperMarketField;
    static {
        outpostRowMarketWrapperField = findMarketHolder();
        outpostRowMarketWrapperClass = RolfLectionUtil.getFieldType(outpostRowMarketWrapperField);
        wrapperMarketField = findMarketField(outpostRowMarketWrapperField);
    }
    
    private boolean UiInjected = false;

    private Button econButton = null;
    private EconomyOverviewPanel overviewPanel = null;

    private UIPanelAPI outpostsTab = null;
    private List<Object> outpostsChildren = null;
    private List<UIPanelAPI> outpostsPanels = new ArrayList<>(6);
    
    @SuppressWarnings("unchecked")
    public void advance(float amount) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return;
        final CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (!campaignUI.isShowingDialog()) return;

        final int index = CampaignEngine.getInstance().getUIData().getCommandData().getSelectedTabIndex();
        if (UiInjected) {
            if (index == BUTTON_TAB_ID) return;
            if (index != BUTTON_TAB_ID && !econButton.isChecked()) return;
        }

        outpostsTab = Attachments.getCurrentTab();
        if (outpostsTab == null || campaignUI.getCurrentCoreTab() != CoreUITabId.OUTPOSTS) return;
        
        outpostsChildren = (List<Object>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, outpostsTab);

        if (!UiInjected) {
            addEconomyButton();
            updateColoniesPanel();
            UiInjected = true;
        }

        updateButtonAndPanels();
    }

    private final void updateButtonAndPanels() {
        final CommandTabData data = CampaignEngine.getInstance().getUIData().getCommandData();

        if (data.getSelectedTabIndex() != BUTTON_TAB_ID) {
            econButton.setChecked(false);
            if (overviewPanel != null) overviewPanel.getPanel().setOpacity(0f);

            outpostsPanels.forEach(p -> p.setOpacity(1f));
            outpostsPanels.clear();
            return;
        }
    }

    public final void run(Button btn) {
        econButton.setChecked(true);
        final CommandTabData data = CampaignEngine.getInstance().getUIData().getCommandData();
        if (data.getSelectedTabIndex() == BUTTON_TAB_ID) return;

        data.setSelectedTabIndex(BUTTON_TAB_ID);
        overviewPanel.getPanel().setOpacity(1f);

        for (Object child : outpostsChildren) {
            if (child instanceof ButtonAPI button) {
                button.unhighlight();
            } else if (child instanceof CustomPanelAPI) {

            } else if (child instanceof UIPanelAPI panel) {
                panel.setOpacity(0f);
                outpostsPanels.add(panel);
            }
        }
    }

    private final void addEconomyButton() {
        UIComponentAPI button5 = null;
        for (Object child : outpostsChildren) {
            if (child instanceof ButtonAPI button) {
                if (button.getText().contains("Custom production")) {
                    button5 = button;
                    break;
                }
            }
        }
        if (button5 == null) return;

        econButton = new Button(
            outpostsTab, BUTTON_WIDTH + 25, BUTTON_HEIGHT, "Economy", Fonts.ORBITRON_12, this
        );
        econButton.setShortcut(Keyboard.KEY_6);
        econButton.cutStyle = CutStyle.TL_TR;
        econButton.overrideCutSize = 6;
        econButton.setHighlightBrightness(0.3f);

        outpostsTab.addComponent(econButton.getPanel());
        econButton.getPos().rightOfTop(button5, 1);

        overviewPanel = new EconomyOverviewPanel(outpostsTab);
        outpostsTab.addComponent(overviewPanel.getPanel()).inTL(0, 20);
    }

    @SuppressWarnings("unchecked")
    private final void updateColoniesPanel() {
        final OutpostListPanel panel = (OutpostListPanel) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getColoniesPanel", outpostsTab);

        final UITable table = (UITable) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof UITable).findFirst().get();
        final var rows = (List<OutpostItemRow>) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getRows", table);

        for (OutpostItemRow row : rows) {
            final Object wrapper = RolfLectionUtil.getPrivateVariable(outpostRowMarketWrapperField, row);
            final Market original = (Market) RolfLectionUtil.getPrivateVariable(wrapperMarketField, wrapper);
            RolfLectionUtil.setPrivateVariable(wrapperMarketField, wrapper, new MarketWrapper(original));

            row.recreate();
        }
    }

    private static Object findMarketHolder() {
        for (Object field : RolfLectionUtil.getAllFields(OutpostItemRow.class)) {
            final Class<?> type = RolfLectionUtil.getFieldType(field);

            if (type.getEnclosingClass() == OutpostItemRow.class) {
                try {
                    final long length = RolfLectionUtil.getAllFields(type).stream()
                        .filter(e -> MarketAPI.class.isAssignableFrom(
                            RolfLectionUtil.getFieldType(e)
                        )).count();

                    if (length == 1) return field;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private static Object findMarketField(Object field) {
        return RolfLectionUtil.getAllFields(outpostRowMarketWrapperClass).stream()
            .filter(e -> MarketAPI.class.isAssignableFrom(
                RolfLectionUtil.getFieldType(e)
            )).findFirst().get();
    }

    public boolean isDone() { return !Global.getSector().isPaused(); }
    public boolean runWhilePaused() { return true; }
}
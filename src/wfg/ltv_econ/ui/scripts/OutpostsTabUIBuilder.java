package wfg.ltv_econ.ui.scripts;

import static wfg.native_ui.util.UIConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Alignment;
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
import wfg.ltv_econ.ui.panels.ColonyPopulationTable;
import wfg.ltv_econ.ui.panels.EconomyOverviewPanel;
import wfg.ltv_econ.ui.panels.FactionResourcesTable;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.Button.CutStyle;
import wfg.native_ui.util.CallbackRunnable;

public class OutpostsTabUIBuilder implements EveryFrameScript, CallbackRunnable<Button> {
    public static final int BUTTON_TAB_ID = 5;

    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_WIDTH = 130;

    public static final Object outpostRowMarketWrapperField = findMarketHolder();
    public static final Class<?> outpostRowMarketWrapperClass = RolfLectionUtil.getFieldType(outpostRowMarketWrapperField);
    public static final Object wrapperMarketField = findMarketField(outpostRowMarketWrapperField);
    
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
            addShowPopStatsButton();
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
        econButton.tooltip.enabled = false;

        outpostsTab.addComponent(econButton.getPanel());
        econButton.getPos().rightOfTop(button5, 1);

        overviewPanel = new EconomyOverviewPanel(outpostsTab);
        outpostsTab.addComponent(overviewPanel.getPanel()).inTL(0, 20);
    }

    private final void addShowPopStatsButton() {
        final OutpostListPanel panel = (OutpostListPanel) RolfLectionUtil.getMethodAndInvokeDirectly(
            "getColoniesPanel", outpostsTab);

        final UITable marketTable = (UITable) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof UITable).findFirst().get();
        final ButtonAPI anchor = (ButtonAPI) RolfLectionUtil.getAllVariables(panel).stream()
            .filter(e -> e instanceof ButtonAPI).map(e -> (ButtonAPI) e)
            .filter(e -> e.getText() != null && e.getText().contains("Manage administrators"))
            .findFirst().get();

        final int tableH = (int)marketTable.getHeight() + 2;
        final ColonyPopulationTable popTable = new ColonyPopulationTable(panel, tableH);
        final FactionResourcesTable facTable = new FactionResourcesTable(panel, tableH);
        panel.addComponent(popTable.getPanel()).inTL(1, opad + 1);
        panel.addComponent(facTable.getPanel()).inTL(1, opad + 1);
        popTable.getPanel().setOpacity(0f);
        facTable.getPanel().setOpacity(0f);

        final String coloniesTxt = "      Owned colonies";
        final String populationTxt = "      Population metrics";
        final String factionTxt = "      Faction resources";

        // size values copied from internal code
        final Button showColoniesButton = new Button(panel, 280, 24, coloniesTxt,
            Fonts.ORBITRON_12, null
        );
        final Button showPopButton = new Button(panel, 280, 24, populationTxt,
            Fonts.ORBITRON_12, null
        );
        final Button showFacButton = new Button(panel, 280, 24, factionTxt,
            Fonts.ORBITRON_12, null
        );
        final Runnable resetState = () -> {
            marketTable.setOpacity(0f);
            popTable.getPanel().setOpacity(0f);
            facTable.getPanel().setOpacity(0f);

            showColoniesButton.setChecked(false);
            showPopButton.setChecked(false);
            showFacButton.setChecked(false);
        };
        final CallbackRunnable<Button> coloniesRun = (btn) ->  {
            resetState.run();
            marketTable.setOpacity(1f);
            showColoniesButton.setChecked(true);
        };
        final CallbackRunnable<Button> populationRun = (btn) ->  {
            resetState.run();
            popTable.getPanel().setOpacity(1f);
            showPopButton.setChecked(true);
        };
        final CallbackRunnable<Button> factionRun = (btn) ->  {
            resetState.run();
            facTable.getPanel().setOpacity(1f);
            showFacButton.setChecked(true);
        };

        showColoniesButton.onClicked = coloniesRun;
        showPopButton.onClicked = populationRun;
        showFacButton.onClicked = factionRun;
        showColoniesButton.cutStyle = CutStyle.TL_BR;
        showPopButton.cutStyle = CutStyle.TL_BR;
        showFacButton.cutStyle = CutStyle.TL_BR;
        showColoniesButton.overrideCutSize = 8;
        showPopButton.overrideCutSize = 8;
        showFacButton.overrideCutSize = 8;
        showColoniesButton.setAlignment(Alignment.LMID);
        showPopButton.setAlignment(Alignment.LMID);
        showFacButton.setAlignment(Alignment.LMID);
        showColoniesButton.setShortcut(Keyboard.KEY_Q);
        showPopButton.setShortcut(Keyboard.KEY_A);
        showFacButton.setShortcut(Keyboard.KEY_S);

        panel.addComponent(showColoniesButton.getPanel()).belowMid(anchor, opad);
        panel.addComponent(showPopButton.getPanel()).belowMid(showColoniesButton.getPanel(), opad);
        panel.addComponent(showFacButton.getPanel()).belowMid(showPopButton.getPanel(), opad);

        showColoniesButton.setChecked(true);
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
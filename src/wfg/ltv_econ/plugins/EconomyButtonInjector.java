package wfg.ltv_econ.plugins;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.command.CommandTabData;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.ui.panels.EconomyOverviewPanel;
import wfg.wrap_ui.ui.Attachments;
import wfg.wrap_ui.ui.panels.Button;
import wfg.wrap_ui.ui.panels.CustomPanel;
import wfg.wrap_ui.ui.panels.Button.CutStyle;
import wfg.wrap_ui.ui.plugins.ButtonPlugin;
import wfg.wrap_ui.util.CallbackRunnable;

public class EconomyButtonInjector implements EveryFrameScript, CallbackRunnable<Button> {

    public static final int BUTTON_TAB_ID = 5; 
    public static final int BUTTON_HEIGHT = 18;
    public static final int BUTTON_WIDTH = 130;

    private static Button econButton = null;
    private static UIPanelAPI root = null;
    private static List<Object> rootChildren = null;
    private static List<UIPanelAPI> rootPanels = new ArrayList<>(6);
    private static EconomyOverviewPanel overviewPanel = null;

    private final SectorAPI sector = Global.getSector();
    
    private int frames = 0;
    public void advance(float amount) {
        if (!sector.isPaused()) {
            frames = 0;
            return;
        }

        if (!sector.getCampaignUI().isShowingDialog()) {
            return;
        }
        final CampaignUIAPI campaignUI = sector.getCampaignUI();

        frames++;
        if (frames < 2 || Global.getCurrentState() != GameState.CAMPAIGN) {
            return;
        }

        root = Attachments.getCurrentTab();
        if (root == null || campaignUI.getCurrentCoreTab() != CoreUITabId.OUTPOSTS) return;

        addButton();
        updateButtonAndPanels();
    }

    @SuppressWarnings("unchecked")
    private final void addButton() {
        rootChildren = (List<Object>) RolfLectionUtil.invokeMethodDirectly(
            CustomPanel.getChildrenNonCopyMethod, root);
        for (Object child : rootChildren) {
            if (child instanceof CustomPanelAPI cp && cp.getPlugin() instanceof ButtonPlugin) {
                return;
            }
        }

        UIComponentAPI button5 = null;
        for (Object child : rootChildren) {
            if (child instanceof ButtonAPI button) {
                if (button.getText().contains("Custom production")) {
                    button5 = button;
                    break;
                }
            }
        }
        if (button5 == null) return;

        econButton = new Button(
            root, BUTTON_WIDTH + 25, BUTTON_HEIGHT, "Economy", Fonts.ORBITRON_12, this
        );
        econButton.setShortcut(Keyboard.KEY_6);
        econButton.setCutStyle(CutStyle.TL_TR);
        econButton.setCutSize(6);
        econButton.highlightBrightness = 0.3f;

        root.addComponent(econButton.getPanel());
        econButton.getPos().rightOfTop(button5, 1);
    }

    public final void run(Button btn) {
        econButton.checked = true;
        final CommandTabData data = CampaignEngine.getInstance().getUIData().getCommandData();
        if (data.getSelectedTabIndex() == BUTTON_TAB_ID) return;

        data.setSelectedTabIndex(BUTTON_TAB_ID);
        for (Object child : rootChildren) {
            if (child instanceof ButtonAPI button) {
                button.unhighlight();
            } else if (child instanceof CustomPanelAPI custom) {
                if (custom.getPlugin() instanceof ButtonPlugin) {}

            } else if (child instanceof UIPanelAPI panel) {
                panel.setOpacity(0f);
                rootPanels.add(panel);
            }
        }

        EconomyEngine.getInstance().fakeAdvance();
        overviewPanel = new EconomyOverviewPanel(root);
        root.addComponent(overviewPanel.getPanel()).inTL(0, 20);
    }

    private final void updateButtonAndPanels() {
        final CommandTabData data = CampaignEngine.getInstance().getUIData().getCommandData();

        if (data.getSelectedTabIndex() != BUTTON_TAB_ID) {
            econButton.checked = false;
            if (overviewPanel != null) root.removeComponent(overviewPanel.getPanel());

            rootPanels.forEach(p -> p.setOpacity(1f));
            rootPanels.clear();
            return;
        }
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }
}
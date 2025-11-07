package wfg.wrap_ui.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.combat.CombatState;
import com.fs.state.AppDriver;

import wfg.reflection.ReflectionUtils;

/**
 * Provides attachment points for UI elements in a variety of contexts.
 */
public class Attachments {
    /**
     * Must be interacting with an entity.
     */
    public static final InteractionDialogAPI getInteractionDialog() {
        return Global.getSector().getCampaignUI().getCurrentInteractionDialog();
    }

    /**
     * Must be interacting with an entity.
     */
    public static final UIPanelAPI getInteractionCoreUI() {
        return getCampaignState().getEncounterDialog().getCoreUI();
    }

    /**
     * Returns the panel for the current tab (FLEET, CHARACTER, COMMAND etc.).
     * Must be interacting with an entity.
     */
    public static final UIPanelAPI getInteractionCurrentTab() {
        return getCampaignState().getEncounterDialog().getCoreUI().getCurrentTab();
    }

    /**
     * Returns the panel for the current tab (FLEET, CHARACTER, COMMAND etc.).
     * Must be in campaign mode.
     */
    public static final UIPanelAPI getCurrentTab() {
        return CampaignEngine.getInstance().getCampaignUI().getCore().getCurrentTab();
    }

    /**
     * Also known as {@code getDialogParent()}.
     * Must be in campaign mode.
     */
    public static final UIPanelAPI getCampaignScreenPanel() {
        return CampaignEngine.getInstance().getCampaignUI().getDialogParent();
    }

    /**
     * Must be in combat mode.
     */
    public static final UIPanelAPI getCombatScreenPanel() {
        return getCombatState().getWidgetPanel();
    }

    /**
     * Must be in campaign mode.
     */
    public static final UIPanelAPI getCoreUI() {
        return CampaignEngine.getInstance().getCampaignUI().getCore();
    }

    /**
     * Must be in combat mode.
     */
    public static final UIPanelAPI getTutorialOverlay() {
        return getCombatState().getTutorialOverlay();
    }

    /**
     * Must be in combat mode.
     */
    public static final UIPanelAPI getWarroomPanel() {
        return (UIPanelAPI) ReflectionUtils.invoke(getCombatState(), "getWarroom");
    }

    
    public static final CampaignState getCampaignState() {
        return ((CampaignState)AppDriver.getInstance().getCurrentState());
    }

    public static final CombatState getCombatState() {
        return ((CombatState)AppDriver.getInstance().getCurrentState());
    }
}
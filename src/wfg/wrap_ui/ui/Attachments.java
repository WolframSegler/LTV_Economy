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
 * Utility class providing access points to UI elements across different game contexts:
 * campaign, interaction, and combat. Allows retrieving core panels, tab panels,
 * dialogs, and overlays depending on the current mode. Is not null-safe.
 */
public class Attachments {
    /**
     * Must be interacting with an entity.
     */
    public static final InteractionDialogAPI getInteractionDialog() {
        return Global.getSector().getCampaignUI().getCurrentInteractionDialog();
    }

    /**
     * Returns the core UI panel of the current interaction.
     * Must be interacting with an entity.
     */
    public static final UIPanelAPI getInteractionCoreUI() {
        return getCampaignState().getEncounterDialog().getCoreUI();
    }

    /**
     * Must be in campaign mode.
     */
    public static final UIPanelAPI getCoreUI() {
        return CampaignEngine.getInstance().getCampaignUI().getCore();
    }

    /**
     * Returns the panel for the current tab (FLEET, CHARACTER, COMMAND etc.) within an interaction.
     * Must be interacting with an entity.
     */
    public static final UIPanelAPI getInteractionCurrentTab() {
        if (getCampaignState().getEncounterDialog() == null) return null;
        if (getCampaignState().getEncounterDialog().getCoreUI() == null) return null;
        return getCampaignState().getEncounterDialog().getCoreUI().getCurrentTab();
    }

    /**
     * Returns the panel for the current tab (FLEET, CHARACTER, COMMAND etc.) in the campaign screen.
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

    
    /**
     * @return The instance of CampaignState if the currentState is Campaign state. Fails otherwise.
     */
    public static final CampaignState getCampaignState() {
        return ((CampaignState)AppDriver.getInstance().getCurrentState());
    }

    /**
     * @return The instance of CombatState if the current state is CombatState. Fails otherwise.
     */
    public static final CombatState getCombatState() {
        return ((CombatState)AppDriver.getInstance().getCurrentState());
    }
}
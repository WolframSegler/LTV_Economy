package wfg.wrap_ui.ui.dialogs;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

public interface WrapDialogDelegate extends CustomDialogDelegate {
    /**
     * Allows the CustomDialogDelegate to access its InteractionDialog 
     */
    default void setInteractionDialog(InteractionDialogAPI a) {}
}

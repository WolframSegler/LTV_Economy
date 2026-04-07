package wfg.ltv_econ.plugins;

import static wfg.ltv_econ.constants.Mods.*;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.dialog.DialogPanel;

public class TitleScreenWarningPlugin extends BaseEveryFrameCombatPlugin {
    private static boolean shown = false;

    public void init(CombatEngineAPI engine) {
        if (shown) return;
        if (Global.getSettings().getCurrentState() != GameState.TITLE) return;
        shown = true;

        final ModManagerAPI manager = Global.getSettings().getModManager();

        boolean showWarnings = false;
        final StringBuilder txt = new StringBuilder("Warning!");
        if (manager.isModEnabled(GRAND_COL)) {
            showWarnings = true;
            txt.append("\n\nLTV-Economy already has a scroll bar for industries."+
                " Using Grand.Colonies will break the game."
            );
        }

        if (manager.isModEnabled(ASTRAL_ASCENT)) {
            showWarnings = true;
            txt.append("\n\nAstral Ascension is not supported by LTV-Economy."+
                " Use at your own risk."
            );
        }
        if (!showWarnings) return;

        final DialogPanel warningPanel = new DialogPanel(
            Attachments.getTitleScreenPanel(), null, txt.toString(), "Dismiss"
        );
        warningPanel.setConfirmShortcut();
        warningPanel.backgroundDimAmount = 0f;
        warningPanel.holo.borderAlpha = 0.6f;
        warningPanel.show(0.3f, 0.3f);
    }
}
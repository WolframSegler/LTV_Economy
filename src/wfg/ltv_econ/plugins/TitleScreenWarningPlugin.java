package wfg.ltv_econ.plugins;

import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.Mods.*;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

import wfg.native_ui.ui.dialog.DialogPanel;

public class TitleScreenWarningPlugin extends BaseEveryFrameCombatPlugin {
    private static boolean shown = false;

    public void init(CombatEngineAPI engine) {
        if (shown) return;
        if (settings.getCurrentState() != GameState.TITLE) return;
        shown = true;

        final ModManagerAPI manager = settings.getModManager();

        boolean showWarnings = false;
        final StringBuilder txt = new StringBuilder(str("warningTitle"));
        if (manager.isModEnabled(GRAND_COL)) {
            showWarnings = true;
            txt.append(str("warningTxtGrandColonies"));
        }

        if (manager.isModEnabled(ASTRAL_ASCENT)) {
            showWarnings = true;
            txt.append(str("warningTxtAstralAscension"));
        }

        if (manager.isModEnabled(AOTD_CBB) || manager.isModEnabled(AOTD_QOL) || manager.isModEnabled(AOTD_SOP) || manager.isModEnabled(AOTD_VOK) || manager.isModEnabled(AOTD_VOS)) {
            showWarnings = true;
            txt.append(str("warningTxtAOTD"));
        }

        if (!showWarnings) return;

        final DialogPanel warningPanel = new DialogPanel(null, txt.toString(), str("dismissTxt"));
        warningPanel.setConfirmShortcut();
        warningPanel.backgroundDimAmount = 0f;
        warningPanel.holo.borderAlpha = 0.6f;
        warningPanel.show(0.3f, 0.3f);
    }
}
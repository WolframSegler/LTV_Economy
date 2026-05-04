package wfg.ltv_econ.ui.marketInfo.buttons;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.marketInfo.dialogs.ManagePopulationDialog;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class ManagePopButton extends Button {
    private static final SpriteAPI ICON = settings.getSprite("icons", "management_button");

    public ManagePopButton(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, null, null, null);
        onClicked = (btn) -> {
            final ManagePopulationDialog dialogPanel = new ManagePopulationDialog(market);
            dialogPanel.show(0.3f, 0.3f);
        };
        setQuickMode(true);
        setShortcut(Keyboard.KEY_4);
        setAppendShortcutToText(false);
        setShowTooltipWhileInactive(true);
        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

        tooltip.builder = (tp, expanded) -> {
            tp.addPara("Population and worker management [%s]", pad,
                highlight, Keyboard.getKeyName(interaction.shortcut)
            );

            tp.addPara("The Statistics Bureau tracks census data, health trends, and workforce allocation across all industries. Policy decrees can be drafted and enacted from this office, shaping the colony's social and economic direction.", opad);
        };

        final Base icon = new Base(m_panel, width, height, ICON, null, null);
        add(icon).inBL(0f, 0f);
        glow.type = GlowType.ADDITIVE;
        glow.additiveSprite = icon.getSprite();

        if (!EconomyEngine.instance().isPlayerMarket(market.getId())) setEnabled(false);
    }
}
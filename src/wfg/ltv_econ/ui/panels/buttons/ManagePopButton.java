package wfg.ltv_econ.ui.panels.buttons;

import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.dialogs.ManagePopulationDialog;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.SpritePanel.Base;

public class ManagePopButton extends Button {
    private static final String ICON = Global.getSettings().getSpriteName("icons", "management_button");

    public ManagePopButton(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, null, null, null);
        onClicked = (btn) -> {
            final ManagePopulationDialog dialogPanel = new ManagePopulationDialog(market);
            dialogPanel.show(0.3f, 0.3f);
        };
        setQuickMode(true);
        setShortcut(Keyboard.KEY_3);
        setAppendShortcutToText(false);
        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

        context.ignore = false;
        tooltip.builder = (tp, expanded) -> {
            tp.addPara("Population and worker management [%s]", pad,
                highlight, Keyboard.getKeyName(interaction.shortcut)
            );
        };

        final Base icon = new Base(m_panel, width, height, ICON, null, null);
        add(icon).inBL(0f, 0f);
        glow.type = GlowType.ADDITIVE;
        glow.additiveSprite = icon.getSprite();

        if (!EconomyEngine.getInstance().isPlayerMarket(market.getId())) setEnabled(false);
        setShowTooltipWhileInactive(true);
    }
}
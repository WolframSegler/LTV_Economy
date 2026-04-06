package wfg.ltv_econ.ui.marketInfo.buttons;

import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.marketInfo.dialogs.MarketEventsDialog;
import wfg.ltv_econ.ui.reusable.DockButton;
import wfg.native_ui.ui.components.HoverGlowComp.GlowType;
import wfg.native_ui.ui.panels.SpritePanel.Base;

public class MarketEventsButton extends DockButton<MarketEventsDialog> {
    private static final String ICON = Global.getSettings().getSpriteName("icons", "events_button");

    public MarketEventsButton(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, null, null, () -> new MarketEventsDialog(market));

        setShortcut(Keyboard.KEY_1);
        setAppendShortcutToText(false);
        setShowTooltipWhileInactive(true);
        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

        context.ignore = false;
        tooltip.builder = (tp, expanded) -> {
            tp.addPara("Market events [%s]", pad, highlight, Keyboard.getKeyName(interaction.shortcut));
        };

        final Base icon = new Base(m_panel, width, height, ICON, null, null);
        add(icon).inBL(0f, 0f);
        glow.type = GlowType.ADDITIVE;
        glow.additiveSprite = icon.getSprite();

        if (!EconomyEngine.instance().isPlayerMarket(market.getId())) setEnabled(false);
    }
}
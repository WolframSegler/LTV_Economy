package wfg.ltv_econ.ui.marketInfo.buttons;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.ui.marketInfo.dialogs.MarketEventsDialog;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.visual.SpritePanel.Base;

public class MarketEventsButton extends DockButton<MarketEventsDialog> {
    private static final SpriteAPI ICON = settings.getSprite("icons", "events_button");

    public MarketEventsButton(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, null, null, () -> new MarketEventsDialog(market));

        setShortcut(Keyboard.KEY_2);
        setAppendShortcutToText(false);
        setShowTooltipWhileInactive(true);
        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

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
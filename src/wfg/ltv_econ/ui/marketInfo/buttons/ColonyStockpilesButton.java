package wfg.ltv_econ.ui.marketInfo.buttons;

import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.fleet.TradeMissionsDialog;
import wfg.ltv_econ.ui.marketInfo.dialogs.ColonyInvDialog;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.CallbackRunnable;
import wfg.native_ui.util.NativeUiUtils;

public class ColonyStockpilesButton extends DockButton<TradeMissionsDialog> {
    private static final SpriteAPI ICON = settings.getSprite("icons", "stockpiles_button");

    public ColonyStockpilesButton(UIPanelAPI parent, int width, int height, MarketAPI market) {
        super(parent, width, height, null, null, () -> new TradeMissionsDialog(market, true));

        final CallbackRunnable<Button> dockRun = onClicked;
        onClicked = (btn) -> {
            if (NativeUiUtils.isCtrlDown()) {
                dockRun.run(btn);
            } else {
                final ColonyInvDialog dialogPanel = new ColonyInvDialog(market);
                dialogPanel.show(0.3f, 0.3f);
            }
        };
        setShortcut(Keyboard.KEY_3);
        setAppendShortcutToText(false);
        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

        tooltip.builder = (tp, expanded) -> {
            tp.addPara("Colony stockpiles and credits [%s]", pad,
                highlight, Keyboard.getKeyName(interaction.shortcut)
            );

            tp.addPara("Trade Missions [%s + %s]", pad,
                highlight, "Ctrl", Keyboard.getKeyName(interaction.shortcut)
            );
        };

        final Base icon = new Base(m_panel, width, height, ICON, null, null);
        add(icon).inBL(0f, 0f);
        glow.type = GlowType.ADDITIVE;
        glow.additiveSprite = icon.getSprite();
    }
}
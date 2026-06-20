package wfg.ltv_econ.ui.fleetTab.button;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.str;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.ui.fleetTab.dialog.TransferToFactionInventoryDialog;
import wfg.native_ui.ui.component.HoverGlowComp.GlowType;
import wfg.native_ui.ui.functional.Button;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public class TransferToFactionInventoryBtn extends Button {
    private static final int SIZE = 32;
    private static final SpriteAPI ICON = settings.getSprite("fleetScreen", "icon_transfer_hangar");

    private final Fader parentWidgetFader;
    private final Base icon;

    public TransferToFactionInventoryBtn(UIPanelAPI parent, Fader parentWidgetFader, FleetMemberAPI member, UIPanelAPI fleetList) {
        super(parent, SIZE, SIZE, null, null, (btn) -> {
            new TransferToFactionInventoryDialog(member, fleetList).show(0.3f, 0.3f);
        });

        this.parentWidgetFader = parentWidgetFader;
        setShowTooltipWhileInactive(true);

        bgAlpha = 0f;
        bgDisabledAlpha = 0f;

        tooltip.positioner = (tp, exp) -> NativeUiUtils.anchorPanel(tp, m_panel, AnchorType.BottomLeft, pad);
        tooltip.builder = (tp, expanded) -> {
            tp.addTitle(str("uiTpTitleTransferToFactionInventory"), base);

            tp.addPara(str("uiTpTransferToFactionInventoryTxt1"), opad, Misc.getStoryBrightColor(), str("uiTxtBonusExperience"));

            if (member.isFlagship()) {
                tp.addPara(str("uiTpTransferToFactionInventoryTxt3"), negative, opad);
            } else {
                tp.addPara(str("uiTpTransferToFactionInventoryTxt2"), opad);
            }
        };

        icon = new Base(m_panel, SIZE, SIZE, ICON, dark, null);
        add(icon).inBL(0f, 0f);

        glow.type = GlowType.ADDITIVE;
        glow.additiveBrightness = 1.05f;
        glow.additiveSprite = icon.getSprite();
        glow.color = dark;

        if (member.isFlagship()) setEnabled(false);
    }

    @Override
    public void renderBelow(float delta) {
        super.renderBelow(delta);

        
        icon.texColor = NativeUiUtils.setAlpha(dark, 0.55f + parentWidgetFader.getBrightness() * 1.1f);
    }
}
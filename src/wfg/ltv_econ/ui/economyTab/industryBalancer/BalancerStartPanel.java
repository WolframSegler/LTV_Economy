package wfg.ltv_econ.ui.economyTab.industryBalancer;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.str;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import wfg.ltv_econ.economy.planning.IndustryBalancer;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.Button.CutStyle;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NativeUiUtils.AnchorType;

public final class BalancerStartPanel extends UIContainer implements UIBuildableAPI {
    private final IndustryBalancerHolo mHolo;

    private final boolean mManualTrigger;

    public BalancerStartPanel(IndustryBalancerHolo holo, boolean manualTrigger) {
        super(IndustryBalancerHolo.CONTENT_PANEL_W, IndustryBalancerHolo.CONTENT_PANEL_H);
        mHolo = holo;

        mManualTrigger = manualTrigger;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        boolean failed = false;
        try {
            Global.getSettings().loadFont("graphics/fonts/insignia17LTaa.fnt");
        } catch (Exception e) {
            failed = true;
        }
        final String textFont = failed ? Fonts.DEFAULT_SMALL : "graphics/fonts/insignia17LTaa.fnt";
        
        final String titleTxt = str(mManualTrigger ? "uiDialogTitleSectorIndustryImbalanceTitle" : "uiDialogTitleSectorIndustryImbalanceWarning");
        final String subtitleTxt = str(mManualTrigger ? "uiDialogTxtSectorIndustryImbalanceSubtitle" : "uiDialogTxtSectorIndustryImbalanceWarning");
        final String cancelBtnTxt = str(mManualTrigger ? "uiCancel" : "uiBtnTitleIndustryBalancerIgnore");
        final String fixBtnTxt = str("uiBtnTitleIndustryBalancerStart");

        final int panelW = IndustryBalancerHolo.CONTENT_PANEL_W;

        final LabelAPI titleLbl = settings.createLabel(titleTxt, Fonts.INSIGNIA_LARGE);
        final LabelAPI subtitleLbl = settings.createLabel(subtitleTxt, textFont);
        titleLbl.autoSizeToWidth(panelW);
        subtitleLbl.autoSizeToWidth(panelW);
        titleLbl.setAlignment(Alignment.MID);
        subtitleLbl.setAlignment(Alignment.TL);
        
        add(titleLbl).inTL(0f, pad);
        add(subtitleLbl).belowLeft((UIComponentAPI) titleLbl, hpad);

        final Button fixBtn = new Button(BUTTON_W, BUTTON_H, fixBtnTxt, Fonts.ORBITRON_20AA, (btn) -> {
            final IndustryBalancer balancer = new IndustryBalancer();
            balancer.runAsync(false);

            mHolo.setNext(new BalancerProgressPanel(mHolo, balancer.state()));
        });
        fixBtn.setCutStyle(CutStyle.TL_BR);
        fixBtn.tooltip.builder = (tp, expanded) -> {
            tp.addPara(str("uiBtnTpTxtIndustryBalancerStart"), 0f);
        };
        fixBtn.tooltip.positioner = (tp, expanded) -> {
            NativeUiUtils.anchorPanel(tp, fixBtn, AnchorType.LeftMid, opad*2);
        };
        add(fixBtn).inBR(hpad, hpad);

        final Button cancelBtn = new Button(BUTTON_W, BUTTON_H, cancelBtnTxt, Fonts.ORBITRON_20AA, (btn) -> {
            mHolo.dismiss();
        });
        cancelBtn.setCutStyle(CutStyle.TL_BR);
        if (!mManualTrigger) {
            cancelBtn.tooltip.builder = (tp, expanded) -> {
                tp.addPara(str("uiBtnTpTxtIndustryBalancerIgnore"), 0f);
            };
            cancelBtn.tooltip.positioner = (tp, expanded) -> {
                NativeUiUtils.anchorPanel(tp, cancelBtn, AnchorType.LeftMid, opad*2);
            };
        }
        add(cancelBtn).leftOfMid(fixBtn, hpad);
    }
}
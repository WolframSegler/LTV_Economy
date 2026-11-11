package wfg.ltv_econ.ui.economyTab.industryBalancer;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;

import wfg.ltv_econ.economy.planning.IndustryBalancer;
import wfg.ltv_econ.economy.planning.IndustryBalancer.BalancerState;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.Button.CutStyle;

public final class BalancerResultPanel extends UIContainer implements UIBuildableAPI {

    private final IndustryBalancerHolo mHolo;
    private final BalancerState mState;

    public BalancerResultPanel(IndustryBalancerHolo holo, BalancerState state) {
        super(IndustryBalancerHolo.CONTENT_PANEL_W, IndustryBalancerHolo.CONTENT_PANEL_H);
        mHolo = holo;
        mState = state;

        buildUI();
    }
    
    @Override
    public void buildUI() {
        clearChildren();

        final boolean industriesChanged = mState.changes().size() > 0;
        final double remainingDeficit = mState.totalSlack();
        final boolean hasDeficit = remainingDeficit > IndustryBalancer.SLACK_TOLERANCE;

        final String titleTxt = switch(mState.status()) {
            case SUCCESS -> str("uiDialogTitleIndustryBalancerSuccess");
            case CYCLE -> str("uiDialogTitleIndustryBalancerCycle");
            case STUCK -> str("uiDialogTitleIndustryBalancerStuck");
            default -> throw new IllegalStateException("Panel shown while balancer running");
        };

        final String subtitleTxt = switch(mState.status()) {
            case SUCCESS -> "";
            case CYCLE -> str("uiDialogTxtIndustryBalancerCycle");
            case STUCK -> str("uiDialogTxtIndustryBalancerStuck");
            default -> throw new IllegalStateException("Panel shown while balancer running");
        };

        final String indChangeCountStr = Integer.toString(mState.changes().size());
        final String industryChangeTxt = industriesChanged ? strf("uiDialogTxtIndustryBalancerChangeSummary", indChangeCountStr) :
            str("uiDialogTxtIndustryBalancerNoChanges");

        final String deficitTxt = Long.toString(Math.round(remainingDeficit));
        final String remainingDeficitTxt = hasDeficit ? 
            strf("uiDialogTxtIndustryBalancerRemainingDeficit", deficitTxt) : "";

        final int panelW = IndustryBalancerHolo.CONTENT_PANEL_W;
        final int titleH = 40;
        final int subtitleH = subtitleTxt.isEmpty() ? 0 : 60;
        final int indChangesH = 30;

        final LabelAPI title = settings.createLabel(titleTxt, Fonts.INSIGNIA_LARGE);
        title.setAlignment(Alignment.MID);
        add(title).setSize(panelW, titleH).inTL(0f, 0f);

        final LabelAPI subtitle = settings.createLabel(subtitleTxt, Fonts.DEFAULT_SMALL);
        subtitle.setAlignment(Alignment.LMID);
        add(subtitle).setSize(panelW, subtitleH).inTL(0f, titleH + pad);

        final LabelAPI indChanges = settings.createLabel(industryChangeTxt, Fonts.DEFAULT_SMALL);
        indChanges.setAlignment(Alignment.MID);
        indChanges.setHighlightColor(highlight);
        if (industriesChanged) indChanges.setHighlight(indChangeCountStr);
        add(indChanges).setSize(panelW, indChangesH).inTL(0f, titleH + subtitleH + pad*2);

        if (hasDeficit) {
            final LabelAPI deficitLbl = settings.createLabel(remainingDeficitTxt, Fonts.DEFAULT_SMALL);
            deficitLbl.setAlignment(Alignment.MID);
            deficitLbl.setHighlightColor(negative);
            deficitLbl.setHighlight(deficitTxt);
            add(deficitLbl).setSize(panelW, 30f).inTL(0f, titleH + subtitleH + indChangesH + pad*3);
        }

        final Button dismissBtn = new Button(BUTTON_W, BUTTON_H, str("uiDismiss"), Fonts.ORBITRON_20AA, (btn) -> {
            mHolo.dismiss();
        });
        dismissBtn.setCutStyle(CutStyle.TL_BR);
        add(dismissBtn).inBR(hpad, hpad);

        if (industriesChanged) {
            final Button changesBtn = new Button(BUTTON_W, BUTTON_H, str("uiReviewChanges"), Fonts.ORBITRON_20AA, (btn) -> {
                mHolo.setNext(new BalancerChangesPanel(mHolo, mState));
            });
            changesBtn.setCutStyle(CutStyle.TL_BR);
            add(changesBtn).leftOfMid(dismissBtn, hpad);
        }
    }
}
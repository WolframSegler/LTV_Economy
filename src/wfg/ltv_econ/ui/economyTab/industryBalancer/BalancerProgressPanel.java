package wfg.ltv_econ.ui.economyTab.industryBalancer;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.FaderUtil.State;

import wfg.ltv_econ.economy.planning.IndustryBalancer;
import wfg.ltv_econ.economy.planning.IndustryBalancer.BalancerState;
import wfg.ltv_econ.economy.planning.IndustryBalancer.BalancerStatus;
import wfg.ltv_econ.economy.planning.IndustryBalancer.MoveType;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.widget.Button;
import wfg.native_ui.ui.widget.Slider;
import wfg.native_ui.ui.widget.Button.CutStyle;
import wfg.native_ui.util.NativeUiUtils;
import wfg.native_ui.util.NumFormat;

public final class BalancerProgressPanel extends UIContainer implements UIBuildableAPI {
    private static final Color REMAINING_NONE_COLOR = new Color(210, 115, 90);
    private static final Color REMAINING_FULL_COLOR = new Color(90, 150, 110);
    private static final float TIME_BEFORE_CANCEL_OPTION = 60f; 

    private final IndustryBalancerHolo mHolo;
    private final BalancerState mState;

    private LabelAPI iterLbl;
    private LabelAPI deficitAddressedLbl;
    private LabelAPI currMoveLbl;
    private Slider deficitSlider;
    private Button cancelBtn;

    private boolean finishedInit = false;
    private float totalTimeInSec = 0f;

    public BalancerProgressPanel(IndustryBalancerHolo holo, BalancerState state) {
        super(IndustryBalancerHolo.CONTENT_PANEL_W, IndustryBalancerHolo.CONTENT_PANEL_H);
        mHolo = holo;
        mState = state;

        buildUI();
    }

    @Override
    public void buildUI() {
        clearChildren();

        if (mState.iteration() < 1) {
            final LabelAPI initLbl = settings.createLabel(str("uiDialogTitleIndustryBalancerInit"), Fonts.ORBITRON_16);
            add(initLbl).inMid();
            return;
        }

        boolean failed = false;
        try {
            Global.getSettings().loadFont("graphics/fonts/insignia17LTaa.fnt");
        } catch (Exception e) {
            failed = true;
        }
        final String statusFont = failed ? Fonts.DEFAULT_SMALL : "graphics/fonts/insignia17LTaa.fnt";
        final String deficitSliderTxt = str("uiDialogTxtIndustryBalancerRemainingDeficit");

        final float maxDeficit = (float) mState.currentSlack();

        final int panelW = IndustryBalancerHolo.CONTENT_PANEL_W;
        final int titleH = 30;
        final int statusLblH = 30;
        final int sliderH = 32;

        final LabelAPI title = settings.createLabel(str("uiDialogTitleIndustryBalancerRunning"), Fonts.INSIGNIA_LARGE);
        add(title).inTMid(pad);

        iterLbl = settings.createLabel("", statusFont);
        iterLbl.setHighlightColor(base);
        add(iterLbl).inTL(pad, titleH + pad);

        deficitAddressedLbl = settings.createLabel("", statusFont);
        deficitAddressedLbl.setHighlightColor(highlight);
        add(deficitAddressedLbl).inTL(pad, titleH + statusLblH + pad);

        currMoveLbl = settings.createLabel("", statusFont);
        add(currMoveLbl).inTL(pad, titleH + statusLblH*2 + pad*2);

        deficitSlider = new Slider("", 0f, maxDeficit, panelW - opad*2, sliderH);
        add(deficitSlider).inTMid(titleH + statusLblH*3 + pad*3);
        deficitSlider.roundBarValue = false;
        deficitSlider.setUserAdjustable(false);
        deficitSlider.customText = () -> {
            final float currDeficit = (float) mState.currentSlack();
            final float ratio = currDeficit > IndustryBalancer.SLACK_TOLERANCE ? currDeficit / maxDeficit : 0f;
            final Color color = NativeUiUtils.lerpColor(REMAINING_FULL_COLOR, REMAINING_NONE_COLOR, ratio);
            deficitSlider.setBarColor(color);
            return String.format(deficitSliderTxt, NumFormat.engNotate(deficitSlider.getProgressInterpolated()));
        };

        cancelBtn = new Button(BUTTON_W, BUTTON_H, str("uiCancel"), Fonts.ORBITRON_20AA, (btn) -> {
            mState.sigStop(); cancelBtn.setEnabled(false);
        });
        cancelBtn.setCutStyle(CutStyle.TL_BR);
        cancelBtn.getFader().forceOut();
        add(cancelBtn).inBL(hpad, hpad);
    }

    @Override
    public void advanceImpl(float delta) {
        super.advanceImpl(delta);

        final Throwable failure = mState.failure();
        if (failure != null) {
            if (failure instanceof RuntimeException re) throw re;
            if (failure instanceof Error e) throw e;
            throw new RuntimeException(failure);
        }

        if (mState.status() != BalancerStatus.RUNNING) {
            mHolo.setNext(new BalancerResultPanel(mHolo, mState));
            return;
        }

        if (!finishedInit) {
            if (mState.iteration() > 0) { finishedInit = true; buildUI(); }
            else return;
        }

        final String iterCountTxt = Integer.toString(mState.iteration());
        final String iterTxt = strf("uiDialogTxtIndustryBalancerIterationCount", iterCountTxt);
        iterLbl.setText(iterTxt);
        iterLbl.autoSizeToWidth(iterLbl.computeTextWidth(iterTxt));
        iterLbl.setHighlight(iterCountTxt);

        final String deficitCom = mState.currentComId() != null ? settings.getCommoditySpec(mState.currentComId()).getName() : str("uiNone");
        final String deficitTxt = strf("uiDialogTxtCommodityDeficitAddressed", deficitCom);
        deficitAddressedLbl.setText(deficitTxt);
        deficitAddressedLbl.autoSizeToWidth(deficitAddressedLbl.computeTextWidth(deficitTxt));
        deficitAddressedLbl.setHighlight(deficitCom);

        final String currMoveTxt = str(mState.currentMoveType() == MoveType.ADD_INDUSTRY ?
            "uiDialogTitleIndustryBalancerMoveTypeAdd" :
            "uiDialogTitleIndustryBalancerMoveTypeRemove"
        );
        currMoveLbl.setText(currMoveTxt);
        currMoveLbl.autoSizeToWidth(currMoveLbl.computeTextWidth(currMoveTxt));

        deficitSlider.setProgress((float) mState.currentSlack());

        totalTimeInSec += delta;

        if (totalTimeInSec > TIME_BEFORE_CANCEL_OPTION) {
            if (cancelBtn.getFader().getState() != State.IN) {
                cancelBtn.getFader().fadeIn();
            }
        }
    }
}
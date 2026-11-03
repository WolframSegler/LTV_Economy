package wfg.ltv_econ.ui.economyTab.industryBalancer;

import static wfg.native_ui.util.UIConstants.*;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.input.InputEventAPI;

import wfg.native_ui.internal.ui.dialog.FoldingPanel;
import wfg.native_ui.internal.ui.interaction.EventInterceptor;
import wfg.native_ui.internal.ui.interaction.EventInterceptor.EventInterceptorListener;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.util.RenderUtils;

public final class IndustryBalancerHolo extends FoldingPanel implements EventInterceptorListener {
    public static final int CONTENT_PANEL_W = 700;
    public static final int CONTENT_PANEL_H = 500;

    private static final float DIM_ALPHA = 0.66f;

    private final EventInterceptor mInterceptor;
    private boolean mDismissing = false;
    private boolean mClosed = false;

    public IndustryBalancerHolo() {
        super(CONTENT_PANEL_W, CONTENT_PANEL_H);

        mInterceptor = new EventInterceptor(this);
        mInterceptor.setSize(screenW, screenH);

        pos().inMid();
        forceFoldIn();
    }

    public void showBalancerDialog(boolean manualTrigger) {
        mInterceptor.attachTo(Attachments.getScreenPanel());
        mInterceptor.bringToFront();
        mInterceptor.pos().inBL(0f, 0f);

        bringToFront();

        foldOut(0.2f);
        flickerNoise(0f, 1f);

        Global.getSector().setPaused(true);

        setNext(new BalancerStartPanel(this, manualTrigger));
    }

    public void dismiss() {
        if (mDismissing) return;
        mDismissing = true;
        foldIn(0.25f);
        flickerNoise(0f, 1f);
    }

    @Override 
    public void absorbed(InputEventAPI e) {
        flickerNoise(0f, 0.5f);
    }

    @Override
    public void renderImpl(float alpha) {
        final float brightness = getFoldProgress();
        if (brightness > 0f) {
            RenderUtils.drawQuad(0f, 0f, screenW, screenH, Color.BLACK,
                alpha * DIM_ALPHA * brightness, false);
        }

        super.renderImpl(alpha);
    }

    @Override
    public void advanceImpl(float delta) {
        super.advanceImpl(delta);

        if (mDismissing && !mClosed && getFoldProgress() < 0.5f) {
            mClosed = true;
            mInterceptor.detach();
        }
    }

    public boolean dismissInterceptor() {
        return false;
    }

    public boolean suspendInterceptor() {
        return false;
    }
}
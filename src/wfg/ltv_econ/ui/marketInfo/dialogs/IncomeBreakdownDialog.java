package wfg.ltv_econ.ui.marketInfo.dialogs;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.native_ui.internal.ui.Side;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.ComponentFactory;
import wfg.native_ui.ui.container.DockPanel;

public class IncomeBreakdownDialog extends DockPanel {
    private static final SettingsAPI settings = Global.getSettings();

    public IncomeBreakdownDialog(final MarketAPI market) {
        super(Attachments.getCoreUI(), 500,
            (int) settings.getScreenHeight() - 200,
            Side.LEFT
        );

        offsetY = 100f;
        bgAlpha = 0.9f;
        removeWhenClosed = true;

        buildUI();
    }

    @Override
    public void buildUI() {
        final int width = (int) (pos.getWidth() - opad*2);

        final LabelAPI title = settings.createLabel("Income Breakdown", Fonts.INSIGNIA_LARGE);
        add(title).inTL(opad, opad*2);

        final TooltipMakerAPI scrollPanel = ComponentFactory.createTooltip(width, true);
        float yCoord = pad;

        incomeBreakdownUI(scrollPanel);

        scrollPanel.setHeightSoFar(yCoord);
        final int offset = opad * 2 + 30;
        ComponentFactory.addTooltip(scrollPanel, pos.getHeight() - offset - opad, true, m_panel).inTL(opad, offset);
    }

    public final void incomeBreakdownUI(final TooltipMakerAPI tp) {
        // TODO implement income breakdown
    }
}
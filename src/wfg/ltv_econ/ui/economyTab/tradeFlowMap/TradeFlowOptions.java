package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import static wfg.native_ui.util.UIConstants.opad;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.economyTab.CommoditySelectionPanel;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panels.Button;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.Button.CutStyle;
import wfg.native_ui.util.CallbackRunnable;

public class TradeFlowOptions extends CustomPanel<TradeFlowOptions> {
    private static final int FILTERS_BTN_H = 32;

    public TradeFlowOptions(UIPanelAPI parent, int width, int height, UIBuildableAPI content) {
        super(parent, width, height);

        final FiltersDialog filtersDock = new FiltersDialog(400,
           (int) Global.getSettings().getScreenHeight() - 200
        );

        final CommoditySelectionPanel options = new CommoditySelectionPanel(
            m_panel, (int) pos.getWidth(), (int) pos.getHeight() - FILTERS_BTN_H - opad, content
        );
        add(options.getPanel()).inBL(0, 0);

        final CallbackRunnable<Button> run = (btn) -> {
            if (filtersDock.isOpen()) filtersDock.close();
            else filtersDock.open();
        };

        final Button filterBtn = new Button(m_panel, width, FILTERS_BTN_H, "Filters", null, run);
        filterBtn.cutStyle = CutStyle.TL_TR;
        filterBtn.bgAlpha = 1f;
        filterBtn.setQuickMode(true);
        add(filterBtn).inTL(0f, 0f);
    }
}
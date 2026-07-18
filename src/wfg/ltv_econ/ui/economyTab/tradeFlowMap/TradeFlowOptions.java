package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import org.lwjgl.input.Keyboard;

import wfg.ltv_econ.constant.strings.LocalizedStrings;
import wfg.ltv_econ.ui.economyTab.CommoditySelectionPanel;
import wfg.ltv_econ.ui.fleet.FiltersDialog;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.functional.DockButton;
import wfg.native_ui.ui.functional.Button.CutStyle;

public final class TradeFlowOptions extends UIContainer {
    private static final int FILTERS_BTN_H = 32;

    public TradeFlowOptions(int width, int height, UIBuildableAPI content) {
        super(width, height);

        final CommoditySelectionPanel options = new CommoditySelectionPanel(
            (int) getWidth(), (int) getHeight() - FILTERS_BTN_H - 2, content
        );
        add(options).inBL(0f, 0f);

        final DockButton<FiltersDialog> filterBtn = new DockButton<>(width, FILTERS_BTN_H, LocalizedStrings.str("filtersTitle"),
            null, () -> new FiltersDialog(content)
        );
        filterBtn.setCutStyle(CutStyle.TL_TR);
        filterBtn.bgAlpha = 1f;
        filterBtn.setShortcutAndAppendToText(Keyboard.KEY_T);
        add(filterBtn).inTL(0f, 0f);
    }
}
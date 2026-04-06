package wfg.ltv_econ.ui.economyTab.tradeFlowMap;

import static wfg.native_ui.util.UIConstants.opad;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.economyTab.CommoditySelectionPanel;
import wfg.ltv_econ.ui.reusable.DockButton;
import wfg.native_ui.ui.core.UIBuildableAPI;
import wfg.native_ui.ui.panels.CustomPanel;
import wfg.native_ui.ui.panels.Button.CutStyle;

public class TradeFlowOptions extends CustomPanel<TradeFlowOptions> {
    private static final int FILTERS_BTN_H = 32;

    public TradeFlowOptions(UIPanelAPI parent, int width, int height, UIBuildableAPI content) {
        super(parent, width, height);

        final CommoditySelectionPanel options = new CommoditySelectionPanel(
            m_panel, (int) pos.getWidth(), (int) pos.getHeight() - FILTERS_BTN_H - opad, content
        );
        add(options.getPanel()).inBL(0f, 0f);

        final DockButton<FiltersDialog> filterBtn = new DockButton<>(m_panel, width, FILTERS_BTN_H, "Filters",
            null, () -> new FiltersDialog(content)
        );
        filterBtn.cutStyle = CutStyle.TL_TR;
        filterBtn.bgAlpha = 1f;
        filterBtn.setShortcut(Keyboard.KEY_Q);
        add(filterBtn).inTL(0f, 0f);
    }
}
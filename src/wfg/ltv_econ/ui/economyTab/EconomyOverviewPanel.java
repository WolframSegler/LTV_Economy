package wfg.ltv_econ.ui.economyTab;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import wfg.ltv_econ.ui.economyTab.tradeFlowMap.ComTradeFlowMap;
import wfg.ltv_econ.ui.economyTab.tradeFlowMap.TradeFlowOptions;
import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

public final class EconomyOverviewPanel extends AbstractManagementPanel {

    public EconomyOverviewPanel() {
        super();

        buildUI();
    }

    protected final String getTitle() {
        return str("uiTitleEconOverview");
    }

    protected final String getSubtitle() {
        return str("uiEconOverviewSubtitle");
    }

    protected final List<NavButtonDef> getNavButtonDefs() {
        final List<NavButtonDef> defs = new ArrayList<>();

        defs.add(new NavButtonDef(str("uiBtnTitleComFlows"), Keyboard.KEY_Q,
            () -> {
                final GlobalCommodityFlow content = new GlobalCommodityFlow(
                    CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                CommoditySelectionPanel options = new CommoditySelectionPanel(
                    OPTIONS_PANEL_W, OPTIONS_PANEL_H, content
                );
                contentPanel.addComponent(content).inBL(0f, 0f);
                optionsPanel.addComponent(options).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef(str("uiTableBtnTitleTradeRoutes"), Keyboard.KEY_W,
            () -> {
                final ComTradeFlowMap content = new ComTradeFlowMap(
                    CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                final TradeFlowOptions options = new TradeFlowOptions(
                    OPTIONS_PANEL_W, OPTIONS_PANEL_H, content
                );
                contentPanel.addComponent(content).inBL(0f, 0f);
                optionsPanel.addComponent(options).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef(str("uiBtnTitlePopulation"), Keyboard.KEY_A,
            () -> {
                final SectorPopulationPanel content = new SectorPopulationPanel(
                    CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                contentPanel.addComponent(content).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef(str("uiBtnTitleDebug"), Keyboard.KEY_S,
            () -> {
                final DebugPanel content = new DebugPanel(
                    CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                contentPanel.addComponent(content).inBL(0f, 0f);
            }
        ));

        return defs;
    }
}
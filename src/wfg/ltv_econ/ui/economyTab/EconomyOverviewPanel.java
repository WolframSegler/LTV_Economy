package wfg.ltv_econ.ui.economyTab;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.economyTab.tradeFlowMap.ComTradeFlowMap;
import wfg.ltv_econ.ui.economyTab.tradeFlowMap.TradeFlowOptions;
import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

public class EconomyOverviewPanel extends AbstractManagementPanel {

    public EconomyOverviewPanel(UIPanelAPI parent) {
        super(parent);
        buildUI();
    }

    protected final String getTitle() {
        return "Economy Overview";
    }

    protected final String getSubtitle() {
        return "Sector oversight and administration";
    }

    protected final List<NavButtonDef> getNavButtonDefs() {
        final List<NavButtonDef> defs = new ArrayList<>();

        defs.add(new NavButtonDef("Global Commodity Flow",
            () -> {
                final GlobalCommodityFlow content = new GlobalCommodityFlow(
                    contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                CommoditySelectionPanel options = new CommoditySelectionPanel(
                    optionsPanel, OPTIONS_PANEL_W, OPTIONS_PANEL_H, content
                );
                contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
                optionsPanel.addComponent(options.getPanel()).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef("Trade Routes",
            () -> {
                final ComTradeFlowMap content = new ComTradeFlowMap(
                    contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                final TradeFlowOptions options = new TradeFlowOptions(
                    optionsPanel, OPTIONS_PANEL_W, OPTIONS_PANEL_H, content
                );
                contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
                optionsPanel.addComponent(options.getPanel()).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef("Settings",
            () -> {
                final EconomySettingsPanel content = new EconomySettingsPanel(
                    contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
            }
        ));

        return defs;
    }
}
package wfg.ltv_econ.ui.economyTab;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.ui.economyTab.tradeFlowMap.ComTradeFlowMap;
import wfg.ltv_econ.ui.economyTab.tradeFlowMap.TradeFlowOptions;
import wfg.ltv_econ.ui.reusable.AbstractManagementPanel;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class EconomyOverviewPanel extends AbstractManagementPanel {

    public EconomyOverviewPanel(UIPanelAPI parent) {
        super(parent);
        buildUI();
    }

    protected final String getTitle() {
        return str("uiEconOverviewTitle");
    }

    protected final String getSubtitle() {
        return str("uiEconOverviewSubtitle");
    }

    protected final List<NavButtonDef> getNavButtonDefs() {
        final List<NavButtonDef> defs = new ArrayList<>();

        defs.add(new NavButtonDef(str("uiComFlowsBtnTitle"), Keyboard.KEY_Q,
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

        defs.add(new NavButtonDef(str("uiTableTradeRoutesBtnTitle"), Keyboard.KEY_W,
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

        defs.add(new NavButtonDef(str("uiPopulationBtnTitle"), Keyboard.KEY_A,
            () -> {
                final SectorPopulationPanel content = new SectorPopulationPanel(
                    contentPanel, CONTENT_PANEL_W, CONTENT_PANEL_H
                );
                contentPanel.addComponent(content.getPanel()).inBL(0f, 0f);
            }
        ));

        defs.add(new NavButtonDef(str("uiDebugBtnTitle"), Keyboard.KEY_S,
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
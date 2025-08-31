package wfg_ltv_econ.ui.dialogs;

import java.awt.Color;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.util.Misc;

import wfg_ltv_econ.economy.CommodityStats;
import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.ui.LtvUIState;
import wfg_ltv_econ.ui.LtvUIState.UIState;
import wfg_ltv_econ.ui.panels.LtvCustomPanel;
import wfg_ltv_econ.ui.panels.SortableTable;
import wfg_ltv_econ.ui.panels.LtvCustomPanel.HasActionListener;
import wfg_ltv_econ.ui.panels.LtvSpritePanel.Base;
import wfg_ltv_econ.ui.panels.SortableTable.cellAlg;
import wfg_ltv_econ.ui.plugins.LtvSpritePanelPlugin;
import wfg_ltv_econ.util.NumFormat;

public class ColonyInvDialog implements LtvCustomDialogDelegate, HasActionListener {

    public static final int PANEL_W = 840;
    public static final int PANEL_H = 600;

    private final LtvCustomPanel<?, ?, CustomPanelAPI> m_parentWrapper;
    private InteractionDialogAPI interactionDialog;

    public ColonyInvDialog(LtvCustomPanel<?, ?, CustomPanelAPI> parent) {
        m_parentWrapper = parent;
    }

    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        LtvUIState.setState(UIState.DETAIL_DIALOG);

        CustomDetailDialogPanel<?> m_panel = new CustomDetailDialogPanel<>(
            m_parentWrapper.getRoot(),
            panel,
            m_parentWrapper.getMarket(),
            PANEL_W, PANEL_H,
            null
        );

        panel.addComponent(m_panel.getPanel()).inBL(0, 0);

        SortableTable table = new SortableTable(
            m_panel.getRoot(),
            m_panel.getPanel(),
            PANEL_W - 20, PANEL_H - 20,
            m_parentWrapper.getMarket(),
            20, 30
        );

        final String BaseProdTpTxt = "Theoretical local daily production, assuming no deficits or shortages.";
        final String RealProdTpTxt = "Actual daily production after accounting for stored deficits.";
        final String BalanceTpTxt = "Net daily change in stockpile, ignoring imports or exports.";

        table.addHeaders(
            "", 40, null, true, false, 1,
            "Commodity", 160, "Commodity.", true, true, 1,
            "Stored", 100, "Amount in Colony stockpile.", false, false, -1,
            "Demand", 100, "Total demand by colony.", false, false, -1,
            "Base Production", 160, BaseProdTpTxt, false, false, -1,
            "Real Production", 160, RealProdTpTxt, false, false, -1,
            "Balance", 100, BalanceTpTxt, false, false, -1
        );

        final EconomyEngine engine = EconomyEngine.getInstance();

        for (CommoditySpecAPI com : EconomyEngine.getEconCommodities()) {

            CommodityStats comStats = engine.getComStats(com.getId(), m_panel.getMarket());

            Base comIcon = new Base(
                m_panel.getRoot(), m_panel.getPanel(), m_panel.getMarket(),26, 26,
                new LtvSpritePanelPlugin<>(), com.getIconName(), null, null, false
            );
            
            long stored = comStats.getStoredAmount();
            long demand = comStats.getBaseDemand(false);
            long baseProd = comStats.getLocalProduction(false);
            long modifiedProd = comStats.getLocalProduction(true);
            long balance = comStats.getLocalProduction(true) - comStats.getBaseDemand(true);

            Color balanceColor = balance < 0 ? 
                Misc.getNegativeHighlightColor() : Misc.getPositiveHighlightColor();

            table.addCell(comIcon, cellAlg.MID, null, null);
            table.addCell(com.getName(), cellAlg.LEFT, com.getName(), Misc.getBasePlayerColor());
            table.addCell(NumFormat.engNotation(stored), cellAlg.MID, stored, null);
            table.addCell(NumFormat.engNotation(demand), cellAlg.MID, demand, Misc.getNegativeHighlightColor());
            table.addCell(NumFormat.engNotation(baseProd), cellAlg.MID, baseProd, Misc.getHighlightColor());
            table.addCell(NumFormat.engNotation(modifiedProd), cellAlg.MID, modifiedProd, Misc.getHighlightColor());
            table.addCell(NumFormat.engNotation(balance), cellAlg.MID, balance, balanceColor);

            table.pushRow(
                CodexDataV2.getCommodityEntryId(com.getId()), m_panel.getMarket(), null, null, null
            );
        }

        m_panel.add(table.getPanel()).inTL(10, 10);

        table.sortRows(2);

        table.createPanel();
    }

    @Override
    public void setInteractionDialog(InteractionDialogAPI a) {
        interactionDialog = a;
    }

    @Override
    public void customDialogConfirm() {
        customDialogCancel();
    }

    @Override
    public void customDialogCancel() {
        LtvUIState.setState(UIState.NONE);

        if (interactionDialog != null) {
            interactionDialog.dismiss();
        }
    }

    public String getCancelText() {
        return "Dismiss";
    }

    public String getConfirmText() {
        return "Dismiss";
    }

    public boolean hasCancelButton() {
        return false;
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }
}

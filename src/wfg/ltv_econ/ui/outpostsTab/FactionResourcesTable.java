package wfg.ltv_econ.ui.outpostsTab;

import java.awt.Color;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.internal.ui.core.UIContainer;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.CellAlg;
import wfg.native_ui.ui.table.SortableTable.ColumnData;
import wfg.native_ui.ui.table.SortableTable.RowData;
import wfg.native_ui.ui.visual.AbstractSpriteElement.SpriteElement;
import wfg.native_ui.util.NumFormat;

public final class FactionResourcesTable extends UIContainer implements HasBackground {
    protected final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public FactionResourcesTable(int height) {
        super(ColonyPopulationTable.PANEL_W, height);

        bg.alpha = 1f;

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final EconomyInfo info = engine.mInfo;
        final int rowH = 30;
        
        clearChildren();
        final SortableTable table = new SortableTable(getWidth(), getHeight(), 18, rowH) {
            @Override
            protected String getEmptyMessage() {
                return str("uiNoStaticAssets");
            }
        };

        table.addHeaders(
            new ColumnData("", 40, null, false, true, false, 1),
            new ColumnData(str("uiTableCommodityTitle"), 160, str("uiTpTxtFactionCom"), true, true, true, 1),
            new ColumnData(str("uiTableStored"), 85, str("uiTableTpTxtStored"), true, false, false, -1),
            new ColumnData(str("uiTableDemandTitle"), 95, str("uiTableTpTxtDemand"), true, false, false, -1),
            new ColumnData(str("uiTableProductionTitle"), 120, str("uiTpTxtFactionProd"), true, false, false, -1),
            new ColumnData(str("uiTableMarketShare"), 120, str("uiTpTxtFactionComShare"), true, false, false, -1),
            new ColumnData(str("uiTableCreditFlow"), 120, str("uiTableTpTxtCreditFlow"), true, false, false, -1),
            new ColumnData(str("uiTableInputOutputBalance"), 100, str("uiTableTpTxtInputOutputBalance"), true, false, false, -1),
            new ColumnData(str("uiAutarky"), 95, str("uiTpTxtAutarky"), true, false, false, -1)
        );

        for (CommoditySpecAPI com : EconomyConstants.econCommoditySpecs) {
            final String comID = com.getId();
            final SpriteElement icon = new SpriteElement(26, 26, com.getIconName(), null, null);

            final double stored = info.getFactionComStockpiles(comID, Factions.PLAYER);
            final float demand = info.getFactionTargetQuantum(comID, Factions.PLAYER);
            final float prod = info.getFactionComProd(comID, Factions.PLAYER);
            if (prod == 0f && demand == 0f) continue;

            final int exportShare = (int) (info.getFactionExportShare(comID, Factions.PLAYER) * 100);
            final long credits = info.getFactionNetComSpending(comID, Factions.PLAYER);
            final double balance = info.getFactionComBalance(comID, Factions.PLAYER);
            final int autarky = (int) ((Math.min(1f, demand == 0f ? 1f: prod/demand)) * 100);

            final Color demandColor = demand > prod ? negative : base;
            final Color creditsColor = credits < 0 ? negative : highlight;
            final Color balanceColor = balance < 0d ? negative : highlight;

            table.addRow(new RowData()
                .cell(icon, CellAlg.MID, null, null)
                .cell(com.getName(), CellAlg.LEFT, null, base)
                .cell(NumFormat.engNotate(stored), CellAlg.LEFTOPAD, stored, base)
                .cell(NumFormat.engNotate(demand), CellAlg.LEFTOPAD, demand, demandColor)
                .cell(NumFormat.engNotate(prod), CellAlg.LEFTOPAD, prod, highlight)
                .cell(exportShare + "%", CellAlg.LEFTOPAD, exportShare, base)
                .cell(NumFormat.formatCredit(credits), CellAlg.LEFTOPAD, credits, creditsColor)
                .cell(NumFormat.engNotate(balance), CellAlg.LEFTOPAD, balance, balanceColor)
                .cell(autarky + "%", CellAlg.LEFTOPAD, autarky, base)
            );
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(7, false);
    }
}
package wfg.ltv_econ.ui.outpostsTab;

import java.awt.Color;

import static wfg.ltv_econ.constant.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.ui.component.BackgroundComp;
import wfg.native_ui.ui.component.NativeComponents;
import wfg.native_ui.ui.core.UIElementFlags.HasBackground;
import wfg.native_ui.ui.panel.CustomPanel;
import wfg.native_ui.ui.table.SortableTable;
import wfg.native_ui.ui.table.SortableTable.cellAlg;
import wfg.native_ui.ui.visual.SpritePanel.Base;
import wfg.native_ui.util.NumFormat;

public class FactionResourcesTable extends CustomPanel implements HasBackground {
    protected final BackgroundComp bg = comp().get(NativeComponents.BACKGROUND);

    public FactionResourcesTable(UIPanelAPI parent, int height) {
        super(parent, ColonyPopulationTable.PANEL_W, height);

        bg.alpha = 1f;

        buildUI();
    }

    public void buildUI() {
        final EconomyEngine engine = EconomyEngine.instance();
        final EconomyInfo info = engine.info;
        final int rowH = 30;
        
        clearChildren();
        final SortableTable table = new SortableTable(m_panel, (int) pos.getWidth(),
            (int) pos.getHeight(), 18, rowH
        );

        table.addHeaders(
            "", 40, null, true, false, 1,
            str("uiTableCommodityTitle"), 160, str("uiTpTxtFactionCom"), true, true, 1,
            str("uiTableStored"), 85, str("uiTableTpTxtStored"), false, false, -1,
            str("uiTableDemandTitle"), 95, str("uiTableTpTxtDemand"), false, false, -1,
            str("uiTableProductionTitle"), 120, str("uiTpTxtFactionProd"), false, false, -1,
            str("uiTableMarketShare"), 120, str("uiTpTxtFactionComShare"), false, false, -1,
            str("uiTableCreditFlow"), 120, str("uiTableTpTxtCreditFlow"), false, false, -1,
            str("uiTableInputOutputBalance"), 100, str("uiTableTpTxtInputOutputBalance"), false, false, -1,
            str("uiAutarky"), 95, str("uiTpTxtAutarky"), false, false, -1
        );

        if (engine.getMarketPopulationData().size() > 0) {
            for (CommoditySpecAPI com : EconomyConstants.econCommoditySpecs) {
                final String comID = com.getId();
                final Base icon = new Base(
                    m_panel, 26, 26, com.getIconName(), null, null
                );
    
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
    
                table.addCell(icon, cellAlg.MID, null, null);
                table.addCell(com.getName(), cellAlg.LEFT, null, base);
                table.addCell(NumFormat.engNotate(stored), cellAlg.LEFTOPAD, stored, base);
                table.addCell(NumFormat.engNotate(demand), cellAlg.LEFTOPAD, demand, demandColor);
                table.addCell(NumFormat.engNotate(prod), cellAlg.LEFTOPAD, prod, highlight);
                table.addCell(exportShare + "%", cellAlg.LEFTOPAD, exportShare, base);
                table.addCell(NumFormat.formatCredit(credits), cellAlg.LEFTOPAD, credits, creditsColor);
                table.addCell(NumFormat.engNotate(balance), cellAlg.LEFTOPAD, balance, balanceColor);
                table.addCell(autarky + "%", cellAlg.LEFTOPAD, autarky, base);
    
                table.pushRow(
                    null, null, null, CodexDataV2.getCommodityEntryId(com.getId()), null, null
                );
            }
        } else {
            final LabelAPI lbl = settings.createLabel(str("uiNoStaticAssets"), Fonts.DEFAULT_SMALL);
            lbl.setColor(base);
            table.add(lbl).inMid();
        }

        table.outline.enabled = true;
        add(table).inTL(0f, 0f);
        table.sortRows(7, false);
    }
}
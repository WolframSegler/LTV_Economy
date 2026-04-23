package wfg.ltv_econ.ui.reusable;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;

public class CommodityInfoBar extends GenericInfoBar {

    public CommodityInfoBar(UIPanelAPI parent, int width, int height, boolean hasOutline,
        CommodityCell cell
    ) {
        super(parent, width, height, hasOutline);

        final float production = cell.getProduction(true);
        final float target = cell.getTargetQuantum(true);
        
        final int interval = EconConfig.TRADE_INTERVAL;
        final EconomyInfo info = EconomyEngine.instance().info;
        final double globalExports = info.getGlobalExportAmount(cell.comID, cell.marketID);
        final double globalImports = info.getGlobalImportAmount(cell.comID, cell.marketID);
        final double inFactionExports = info.getInFactionExportAmount(cell.comID, cell.marketID);
        final double inFactionImports = info.getInFactionImportAmount(cell.comID, cell.marketID);

        final double dailyGlobalExports = globalExports / interval;
        final double dailyGlobalImports = globalImports / interval;
        final double dailyInFactionExports = inFactionExports / interval;
        final double dailyInFactionImports = inFactionImports / interval;
        final double dailyTotalExports = dailyGlobalExports + dailyInFactionExports;
        final double dailyTotalImports = dailyGlobalImports + dailyInFactionImports;

        final float demandMetLocally = Math.min(production, target);
        final float targetQuantumPreTrade = target - demandMetLocally;
        final float demandMetInFaction = (float) Math.min(dailyInFactionImports, targetQuantumPreTrade);
        final float demandMetGlobal = (float) Math.min(dailyGlobalImports, targetQuantumPreTrade - demandMetInFaction);
        final float demandMetViaTrade = demandMetInFaction + demandMetGlobal;
        final float overImports = (float) dailyTotalImports - demandMetViaTrade;
        final float prodSurplus = Math.max(0f, production - target);
        final float didNotExport = (float) Math.max(0f, prodSurplus - dailyTotalExports);
        final float demandMet = demandMetLocally + demandMetViaTrade;
        final float demandUnmet = target - demandMet;

        final float footprint = demandMet + demandUnmet + overImports + (float) dailyTotalExports + didNotExport;

        barMap.put(UIColors.COM_LOCAL_PROD, demandMetLocally / footprint);
        barMap.put(UIColors.COM_EXPORT,(float) (dailyTotalExports / footprint));
        barMap.put(UIColors.COM_NOT_EXPORTED, didNotExport / footprint);
        barMap.put(UIColors.COM_FACTION_IMPORT, (float) (demandMetInFaction / footprint));
        barMap.put(UIColors.COM_IMPORT, (float) (demandMetGlobal / footprint));
        barMap.put(UIColors.COM_OVER_IMPORT, overImports / footprint);
        barMap.put(UIColors.COM_DEFICIT, demandUnmet / footprint);
    }
}
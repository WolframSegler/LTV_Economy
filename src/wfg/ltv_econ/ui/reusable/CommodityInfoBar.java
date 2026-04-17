package wfg.ltv_econ.ui.reusable;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;

public class CommodityInfoBar extends GenericInfoBar {

    public CommodityInfoBar(UIPanelAPI parent, int width, int height, boolean hasOutline,
        CommodityCell cell
    ) {
        super(parent, width, height, hasOutline);

        final float footprint = cell.getFlowEconomicFootprint();

        if (footprint <= 0f) {
            throw new IllegalStateException(
                "CommodityInfoBar cannot display info: economic footprint is zero for " 
                + cell.comID
            );
        }

        final float demandMetLocalRatio = cell.getTargetQuantumMetLocally() / footprint;
        final float inFactionImportRatio = cell.getTargetQuantumMetViaFactionTrade() / footprint;
        final float globalImportRatio = (cell.getTargetQuantumMetViaGlobalTrade() +
            cell.getTargetQuantumMetViaInformalTrade()) / footprint;
        final float overImportRatio = cell.getOverImports() / footprint;
        final float exportedRatio = cell.getTotalExports() / footprint;
        final float notExportedRatio = cell.getRemainingExportableAfterTargetQuantum() / footprint;
        final float deficitRatio = cell.getTargetQuantumUnmet() / footprint;

        barMap.put(UIColors.COM_LOCAL_PROD, demandMetLocalRatio);
        barMap.put(UIColors.COM_EXPORT, exportedRatio);
        barMap.put(UIColors.COM_NOT_EXPORTED, notExportedRatio);
        barMap.put(UIColors.COM_FACTION_IMPORT, inFactionImportRatio);
        barMap.put(UIColors.COM_IMPORT, globalImportRatio);
        barMap.put(UIColors.COM_OVER_IMPORT, overImportRatio);
        barMap.put(UIColors.COM_DEFICIT, deficitRatio);
    }
}
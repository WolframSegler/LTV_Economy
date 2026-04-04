package wfg.ltv_econ.ui.reusable;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;

public class CommodityInfoBar extends GenericInfoBar<CommodityInfoBar> {

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

        final float demandMetLocalRatio = cell.getFlowDeficitMetLocally() / footprint;
        final float inFactionImportRatio = cell.getFlowDeficitMetViaFactionTrade() / footprint;
        final float globalImportRatio = (cell.getFlowDeficitMetViaGlobalTrade() +
            cell.getFlowDeficitMetViaInformalTrade()) / footprint;
        final float overImportRatio = cell.getFlowOverImports() / footprint;
        final float importExclusiveRatio = cell.getImportExclusiveDemand() / footprint;
        final float exportedRatio = cell.getTotalExports() / footprint;
        final float notExportedRatio = cell.getFlowCanNotExport() / footprint;
        final float deficitRatio = cell.getFlowDeficit() / footprint;

        barMap.put(UIColors.COM_LOCAL_PROD, demandMetLocalRatio);
        barMap.put(UIColors.COM_EXPORT, exportedRatio);
        barMap.put(UIColors.COM_NOT_EXPORTED, notExportedRatio);
        barMap.put(UIColors.COM_FACTION_IMPORT, inFactionImportRatio);
        barMap.put(UIColors.COM_IMPORT, globalImportRatio);
        barMap.put(UIColors.COM_OVER_IMPORT, overImportRatio);
        barMap.put(UIColors.COM_IMPORT_EXCLUSIVE, importExclusiveRatio);
        barMap.put(UIColors.COM_DEFICIT, deficitRatio);
    }
}
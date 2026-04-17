package wfg.ltv_econ.ui.reusable;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;

public class StockpileInfoBar extends GenericInfoBar {

    public StockpileInfoBar(UIPanelAPI parent, int width, int height, boolean hasOutline, CommodityCell cell) {
        super(parent, width, height, hasOutline);

        final double footprint = cell.getStoredEconomicFootprint();

        if (footprint <= 0.0) {
            throw new IllegalStateException("StockpileInfoBar cannot display: economic footprint is zero for " + cell.comID);
        }

        final double deficit = cell.getStoredDeficit();
        final double excess = cell.getStoredExcess();
        final double preferred = footprint - deficit - excess;

        final double deficitRatio = deficit / footprint;
        final double targetRatio = preferred / footprint;
        final double excessRatio = excess / footprint;

        barMap.put(UIColors.STOCKPILES_DEFICIT, (float) deficitRatio);
        barMap.put(UIColors.STOCKPILES_TARGET, (float) targetRatio);
        barMap.put(UIColors.STOCKPILES_EXCESS, (float) excessRatio);
    }
}
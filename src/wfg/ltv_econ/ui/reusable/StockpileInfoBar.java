package wfg.ltv_econ.ui.reusable;

import com.fs.starfarer.api.ui.UIPanelAPI;

import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;

public class StockpileInfoBar extends GenericInfoBar {

    public StockpileInfoBar(UIPanelAPI parent, int width, int height, boolean hasOutline, CommodityCell cell) {
        super(parent, width, height, hasOutline);

        final double footprint = cell.getStoredEconomicFootprint();

        if (footprint <= 0d) {
            throw new IllegalStateException("StockpileInfoBar cannot display: economic footprint is zero for " + cell.comID);
        }

        final double shortfall = cell.getStoredShortfall();
        final double surplus = cell.getStoredSurplus();
        final double preferred = footprint - shortfall - surplus;

        final double shortfallRatio = shortfall / footprint;
        final double targetRatio = preferred / footprint;
        final double surplusRatio = surplus / footprint;

        barMap.put(UIColors.BAR_DEFICIT, (float) shortfallRatio);
        barMap.put(UIColors.BAR_STORED, (float) targetRatio);
        barMap.put(UIColors.BAR_SURPLUS, (float) surplusRatio);
    }
}
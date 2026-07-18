package wfg.ltv_econ.ui.reusable;

import wfg.ltv_econ.constant.UIColors;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.native_ui.internal.ui.core.UIContainer;

public class CommodityBarPanel extends UIContainer {
    private static final int gap = 2;

    public CommodityBarPanel(int width, int height, boolean hasOutline, final CommodityCell cell) {
        super(width, height);

        final int flowBarHeight = (int) (height * 0.12f);
        final int mainBarHeight = height - flowBarHeight - gap;

        final DailyFlowBar flowBar = new DailyFlowBar(width, flowBarHeight, hasOutline, cell);
        final CommodityInfoBar infoBar = new CommodityInfoBar(width, mainBarHeight, hasOutline, cell);

        add(flowBar).inTL(0f, 0f);
        add(infoBar).inBL(0f, 0f);
    }

    public static class CommodityInfoBar extends GenericInfoBar {
        public CommodityInfoBar(int width, int height, boolean hasOutline, final CommodityCell cell) {
            super(width, height, hasOutline);
            final double stored = cell.getStored();
            final float target = cell.getTargetStored();
            final float pendingExports = cell.inFactionExports + cell.globalExports;

            final double maxBar = Math.max(target, stored + cell.inFactionImports + cell.globalImports) + pendingExports;

            final double storedWithinTarget = Math.min(stored, target) / maxBar;
            final double inFacImportFill = cell.inFactionImports / maxBar;
            final double globalImportFill = cell.globalImports / maxBar;
            final double surplusFill = Math.max(0d, stored - target) / maxBar;
            final double deficitFill = Math.max(0d, target - stored - cell.inFactionImports - cell.globalImports) / maxBar;
            final double exportFill = pendingExports / maxBar;

            barMap.put(UIColors.BAR_DEFICIT, (float) deficitFill);
            barMap.put(UIColors.BAR_GLOBAL_IMPORT, (float) globalImportFill);
            barMap.put(UIColors.BAR_INFAC_IMPORT, (float) inFacImportFill);
            barMap.put(UIColors.BAR_STORED, (float) storedWithinTarget);
            barMap.put(UIColors.BAR_SURPLUS, (float) surplusFill);
            barMap.put(UIColors.BAR_EXPORT, (float) exportFill);
        }
    }

    public static class DailyFlowBar extends GenericInfoBar {
        public DailyFlowBar(int width, int height, boolean hasOutline, final CommodityCell cell) {
            super(width, height, hasOutline);

            final float prod = cell.getProduction(true);
            final float cons = cell.getConsumption(true);
            final float max = Math.max(prod, cons);
            final float total = (max < 0.01f) ? 1f : max;

            if (prod >= cons) {
                barMap.put(UIColors.FLOW_SATISFIED, cons / total);
                barMap.put(UIColors.FLOW_EXCESS, (prod - cons) / total);
            } else {
                barMap.put(UIColors.FLOW_SHORTFALL, (cons - prod) / total);
                barMap.put(UIColors.FLOW_SATISFIED, prod / total);
            }
        }
    }
}
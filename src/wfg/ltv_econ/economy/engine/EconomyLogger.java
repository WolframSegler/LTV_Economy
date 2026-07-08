package wfg.ltv_econ.economy.engine;

import java.util.Map;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.native_ui.util.NumFormat;

public class EconomyLogger {
    private static final Logger log = Global.getLogger(EconomyEngine.class);
    transient EconomyEngine engine;

    EconomyLogger(EconomyEngine engine) { this.engine = engine; }

    public final void logEconomySnapshot() {
        final StringBuilder sb = new StringBuilder();
        sb.append("---- ECONOMY SNAPSHOT START ----\n");

        for (Map.Entry<String, CommodityDomain> dom : engine.comDomains.entrySet()) {
            long totalProduction = 0;
            long totalConsumption = 0;
            long totalTargetQuantum = 0;
            long totalPendingImports = 0;
            long totalPendingExports = 0;
            long totalInformalImports = 0;
            long totalInformalExports = 0;
            long totalStockpile = 0;
            long totalTargetStockpile = 0;
            long totalShortfall = 0;
            long totalSurplus = 0;
            long totalExportable = 0;

            for (CommodityCell cell : dom.getValue().getAllCells()) {
                totalProduction += cell.getProduction(true);
                totalConsumption += cell.getConsumption(true);
                totalTargetQuantum += cell.getTargetQuantum(true);
                totalPendingImports += cell.getPendingImports();
                totalPendingExports += cell.getPendingExports();
                totalInformalImports += cell.informalImports;
                totalInformalExports += cell.informalExports;
                totalStockpile += cell.getStored();
                totalTargetStockpile += cell.getTargetStockpiles();
                totalShortfall += cell.getStoredShortfall();
                totalSurplus += cell.getStoredSurplus();
                totalExportable += cell.computeExportAmount();
            }

            sb.append("\nCommodity: ").append(dom.getKey()).append("\n")
            .append("production (daily): ").append(NumFormat.engNotate(totalProduction)).append("\n")
            .append("consumption (daily): ").append(NumFormat.engNotate(totalConsumption)).append("\n")
            .append("targetQuantum (daily): ").append(NumFormat.engNotate(totalTargetQuantum)).append("\n")
            .append("pending imports: ").append(NumFormat.engNotate(totalPendingImports)).append("\n")
            .append("pending exports: ").append(NumFormat.engNotate(totalPendingExports)).append("\n")
            .append("informal imports (daily): ").append(NumFormat.engNotate(totalInformalImports)).append("\n")
            .append("informal exports (daily): ").append(NumFormat.engNotate(totalInformalExports)).append("\n")
            .append("stockpile: ").append(NumFormat.engNotate(totalStockpile)).append("\n")
            .append("targetStockpile: ").append(NumFormat.engNotate(totalTargetStockpile)).append("\n")
            .append("stockpileShortfall: ").append(NumFormat.engNotate(totalShortfall)).append("\n")
            .append("stockpileSurplus: ").append(NumFormat.engNotate(totalSurplus)).append("\n")
            .append("exportable amount: ").append(NumFormat.engNotate(totalExportable)).append("\n")
            .append("---------------------------------------\n");
        }

        sb.append("---- ECONOMY SNAPSHOT END ----");
        log.info(sb.toString());
    }

    public final void logEconomySnapshotAsCSV() {
        final StringBuilder csv = new StringBuilder(2048);
        csv.append("Commodity,Production,Consumption,TargetQuantum,PendingImports,PendingExports,InformalImports,InformalExports,Stockpile,TargetStockpile,Shortfall,Surplus,Exportable\n");

        for (Map.Entry<String, CommodityDomain> entry : engine.comDomains.entrySet()) {
            final String comID = entry.getKey();
            final CommodityDomain dom = entry.getValue();

            long prod = 0, cons = 0, targ = 0;
            long pendImp = 0, pendExp = 0, infImp = 0, infExp = 0;
            long stock = 0, targetStock = 0, shortfall = 0, surplus = 0, exportable = 0;

            for (CommodityCell cell : dom.getAllCells()) {
                prod += cell.getProduction(true);
                cons += cell.getConsumption(true);
                targ += cell.getTargetQuantum(true);
                pendImp += cell.getPendingImports();
                pendExp += cell.getPendingExports();
                infImp += cell.informalImports;
                infExp += cell.informalExports;
                stock += cell.getStored();
                targetStock += cell.getTargetStockpiles();
                shortfall += cell.getStoredShortfall();
                surplus += cell.getStoredSurplus();
                exportable += cell.computeExportAmount();
            }

            csv.append(String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                comID, prod, cons, targ, pendImp, pendExp, infImp, infExp,
                stock, targetStock, shortfall, surplus, exportable));
        }
        log.info(csv.toString());
    }

    public final void logCreditsSnapshot() {
        // unchanged – it doesn't touch commodities
        final StringBuilder sb = new StringBuilder();
        sb.append("\n=== Market Credits Snapshot ===\n");

        engine.marketCredits.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .forEach(entry -> sb.append(entry.getKey())
            .append(": ")
            .append(entry.getValue())
            .append(" credits\n"));

        sb.append("=== End Snapshot ===");
        log.info(sb.toString());
    }
}
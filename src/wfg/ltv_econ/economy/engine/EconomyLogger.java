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
            long totalImports = 0;
            long totalExports = 0;
            long totalStockpile = 0;
            long totalTargetStockpile = 0;
            long totalStoredDeficit = 0;
            long totalStoredExcess = 0;

            for (CommodityCell cell : dom.getValue().getAllCells()) {
                totalProduction += cell.getProduction(true);
                totalConsumption += cell.getConsumption(true);
                totalTargetQuantum += cell.getTargetQuantum(true);
                totalImports += cell.getTotalImports();
                totalExports += cell.getTotalExports();
                totalStockpile += cell.getStored();
                totalTargetStockpile += cell.getTargetStockpiles();
                totalStoredDeficit += cell.getStoredDeficit();
                totalStoredExcess += cell.computeExportAmount();
            }

            sb.append("\nCommodity: ").append(dom.getKey()).append("\n")
            .append("production (daily): ").append(NumFormat.engNotate(totalProduction)).append("\n")
            .append("consumption (daily): ").append(NumFormat.engNotate(totalConsumption)).append("\n")
            .append("targetQuantum (daily): ").append(NumFormat.engNotate(totalTargetQuantum)).append("\n")
            .append("imports (daily): ").append(NumFormat.engNotate(totalImports)).append("\n")
            .append("exports (daily): ").append(NumFormat.engNotate(totalExports)).append("\n")
            .append("stockpile: ").append(NumFormat.engNotate(totalStockpile)).append("\n")
            .append("targetStockpile: ").append(NumFormat.engNotate(totalTargetStockpile)).append("\n")
            .append("storedDeficit: ").append(NumFormat.engNotate(totalStoredDeficit)).append("\n")
            .append("storedExcess (exportable): ").append(NumFormat.engNotate(totalStoredExcess)).append("\n")
            .append("---------------------------------------\n");
        }

        sb.append("---- ECONOMY SNAPSHOT END ----");
        log.info(sb.toString());
    }

    public final void logEconomySnapshotAsCSV() {
        final StringBuilder csv = new StringBuilder(2048);
        csv.append("Commodity,Production(real),Consumption(real),TargetQuantum,Imports,Exports,Stockpile,TargetStockpile,StoredDeficit,StoredExcess\n");

        for (Map.Entry<String, CommodityDomain> entry : engine.comDomains.entrySet()) {
            final String comID = entry.getKey();
            final CommodityDomain dom = entry.getValue();

            long totalProduction = 0;
            long totalConsumption = 0;
            long totalTargetQuantum = 0;
            long totalImports = 0;
            long totalExports = 0;
            long totalStockpile = 0;
            long totalTargetStockpile = 0;
            long totalStoredDeficit = 0;
            long totalStoredExcess = 0;

            for (CommodityCell cell : dom.getAllCells()) {
                totalProduction += cell.getProduction(true);
                totalConsumption += cell.getConsumption(true);
                totalTargetQuantum += cell.getTargetQuantum(true);
                totalImports += cell.getTotalImports();
                totalExports += cell.getTotalExports();
                totalStockpile += cell.getStored();
                totalTargetStockpile += cell.getTargetStockpiles();
                totalStoredDeficit += cell.getStoredDeficit();
                totalStoredExcess += cell.computeExportAmount();
            }

            csv.append(String.format("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                comID, totalProduction, totalConsumption, totalTargetQuantum,
                totalImports, totalExports, totalStockpile,
                totalTargetStockpile, totalStoredDeficit, totalStoredExcess));
        }
        log.info(csv.toString());
    }

    public final void logCreditsSnapshot() {
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
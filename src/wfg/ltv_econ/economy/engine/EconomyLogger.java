package wfg.ltv_econ.economy.engine;

import java.util.Collection;
import java.util.Map;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.native_ui.util.NumFormat;

public class EconomyLogger {
    transient EconomyEngine engine;

    EconomyLogger(EconomyEngine engine) { this.engine = engine; }
    
    public final void logEconomySnapshot() {
        Global.getLogger(getClass()).info("---- ECONOMY SNAPSHOT START ----");

        for (Map.Entry<String, CommodityDomain> dom : engine.m_comDomains.entrySet()) {
            long potencialProd = 0;
            long realProd = 0;
            long potencialDemand = 0;
            long realDemand = 0;
            long available = 0;
            double availabilityRatio = 0f;
            long deficit = 0;
            long globalStockpile = 0;
            long totalExports = 0;
            long inFactionExports = 0;
            long globalExports = 0;

            for (CommodityCell cell : dom.getValue().getAllCells()) {
                potencialProd += cell.getProduction(false);
                realProd += cell.getProduction(true);
                potencialDemand += cell.getBaseDemand(true);
                realDemand += cell.getDemand();
                available += cell.getFlowAvailable();
                availabilityRatio += cell.getFlowAvailabilityRatio();
                deficit += cell.getFlowDeficit();
                globalStockpile += cell.getStored();
                totalExports += cell.getTotalExports();
                inFactionExports += cell.inFactionExports;
                globalExports += cell.globalExports;
            }

            availabilityRatio /= (float) dom.getValue().getAllCells().size();

            Global.getLogger(getClass()).info("\n"+
                "Commodity: " + dom.getKey() + "\n"+
                "potencialProd: " + NumFormat.engNotation(potencialProd) + "\n"+
                "realProd: " + NumFormat.engNotation(realProd) + "\n"+
                "potencialDemand: " + NumFormat.engNotation(potencialDemand) + "\n"+
                "realDemand: " + NumFormat.engNotation(realDemand) + "\n"+
                "available: " + NumFormat.engNotation(available) + "\n"+
                "availabilityRatio: " + availabilityRatio + "\n"+
                "deficit: " + NumFormat.engNotation(deficit) + "\n"+
                "globalStockpile: " + NumFormat.engNotation(globalStockpile) + "\n"+
                "totalExports: " + NumFormat.engNotation(totalExports) + "\n"+
                "inFactionExports: " + NumFormat.engNotation(inFactionExports) + "\n"+
                "globalExports: " + NumFormat.engNotation(globalExports) + "\n"+

                "---------------------------------------"
            );
        }
        
        Global.getLogger(getClass()).info("---- ECONOMY SNAPSHOT END ----");
    }

    public final void logEconomySnapshotAsCSV() {
        StringBuilder csv = new StringBuilder(2048);

        csv.append("Commodity,PotencialProd,RealProd,PotencialDemand,RealDemand,Available,AvailabilityRatio,Deficit,GlobalStockpile,TotalExports,InFactionExports,GlobalExports\n");

        for (Map.Entry<String, CommodityDomain> entry : engine.m_comDomains.entrySet()) {
            final String comID = entry.getKey();
            final CommodityDomain dom = entry.getValue();

            long potencialProd = 0;
            long realProd = 0;
            long potencialDemand = 0;
            long realDemand = 0;
            long available = 0;
            double availabilityRatio = 0f;
            long deficit = 0;
            long globalStockpile = 0;
            long totalExports = 0;
            long inFactionExports = 0;
            long globalExports = 0;

            final Collection<CommodityCell> allCells = dom.getAllCells();
            for (CommodityCell cell : allCells) {
                potencialProd += cell.getProduction(false);
                realProd += cell.getProduction(true);
                potencialDemand += cell.getBaseDemand(true);
                realDemand += cell.getDemand();
                available += cell.getFlowAvailable();
                availabilityRatio += cell.getFlowAvailabilityRatio();
                deficit += cell.getFlowDeficit();
                globalStockpile += cell.getStored();
                totalExports += cell.getTotalExports();
                inFactionExports += cell.inFactionExports;
                globalExports += cell.globalExports;
            }

            availabilityRatio /= (double) allCells.size();

            csv.append(String.format("%s,%d,%d,%d,%d,%d,%.4f,%d,%d,%d,%d,%d\n",
                comID,
                potencialProd,
                realProd,
                potencialDemand,
                realDemand,
                available,
                availabilityRatio,
                deficit,
                globalStockpile,
                totalExports,
                inFactionExports,
                globalExports
            ));
        }
        Global.getLogger(getClass()).info(csv.toString());
    }

    public final void logCreditsSnapshot() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n=== Market Credits Snapshot ===\n");

        engine.m_marketCredits.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .forEach(entry -> sb.append(entry.getKey())
            .append(": ")
            .append(entry.getValue())
            .append(" credits\n"));

        sb.append("=== End Snapshot ===");
        Global.getLogger(EconomyEngine.class).info(sb.toString());
    }
}
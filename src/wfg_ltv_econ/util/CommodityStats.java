package wfg_ltv_econ.util;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityStats {

    public final CommodityOnMarketAPI com;
    public final MarketAPI market;

    public long totalActivity = 0;
    public long available = 0;

    public long localProduction = 0;
    public long localDemand = 0;
    public long localDeficit = 0;

    public long totalImports = 0;
    public long inFactionImports = 0;
    public long externalImports = 0;

    public long totalExports = 0;
    public long inFactionExport = 0;
    public long globalExport = 0;
    public long canNotExport = 0;

    public long demandMet = 0;
    public long demandMetWithLocal = 0;
    public long demandMetNotWithLocal = 0;


    public CommodityStats(CommodityOnMarketAPI com, MarketAPI market) {
        this.com = com;
        this.market = market;

        int shippingGlobal = Global.getSettings().getShippingCapacity(market, false);
        int shippingInFaction = Global.getSettings().getShippingCapacity(market, true);

        localProduction = getLocalProduction(market, com.getId());
        available = Math.max(com.getAvailable(), localProduction);

        localDemand = com.getMaxDemand();
        localDeficit = Math.max(0, localDemand - available);

        demandMet = Math.min(localDemand, available);
        demandMetWithLocal = Math.min(localProduction, localDemand);
        demandMetNotWithLocal = Math.max(0, demandMet - demandMetWithLocal);

        totalActivity = Math.max(available, localDemand);

        // IMPORTS
        totalImports = Math.max(0, (localDemand - localProduction) - localDeficit);

        // Scan availability mods for same-faction foreign sources
        inFactionImports = 0;
        for (StatMod mod : com.getAvailableStat().getFlatMods().values()) {
            Object source = mod.getSource();
            if (source instanceof String) {
                String sourceId = (String) source;
                MarketAPI sourceMarket = Global.getSector().getEconomy().getMarket(sourceId);
                if (sourceMarket != null &&
                        !sourceMarket.getId().equals(market.getId()) &&
                        sourceMarket.getFactionId().equals(market.getFactionId())) {
                    inFactionImports += mod.value;
                }
            }
        }

        inFactionImports = Math.min(inFactionImports, totalImports);
        externalImports = totalImports - inFactionImports;

        // EXPORTS
        long totalExportable = Math.max(0, localProduction - localDemand);

        List<MarketAPI> factionMarkets = getFactionMarkets(market.getFactionId());
        int totalFactionDemand = getUnmetLocalDemand(factionMarkets, com.getId());
        int inFactionExportable = Math.min(totalFactionDemand, shippingInFaction);
        inFactionExport = Math.min(totalExportable, inFactionExportable);

        totalExports = Math.min(totalExportable, shippingGlobal + inFactionExport);

        globalExport = Math.min(totalExports - inFactionExport, shippingGlobal);

        canNotExport = totalExportable - (inFactionExport + globalExport);

        if (inFactionExport < 0) inFactionExport = 0;

        if (externalImports < 0) externalImports = 0;

        if (demandMetWithLocal < 0) demandMetWithLocal = 0;
    }

    public static List<MarketAPI> getFactionMarkets(String factionId) {
        List<MarketAPI> result = new ArrayList<>();
        
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFactionId().equals(factionId) && market.isPlanetConditionMarketOnly() == false) {
                result.add(market);
            }
        }
        
        return result;
    }

    public static int getUnmetLocalDemand(List<MarketAPI> markets, String comID) {
        int totalUnmet = 0;

        for (MarketAPI market : markets) {
            CommodityOnMarketAPI com = market.getCommodityData(comID);

            int localDemand = com.getMaxDemand();
            int localSupply = Math.min(com.getMaxSupply(), com.getAvailable());

            int unmet = localDemand - localSupply;

            if (unmet > 0) {
                totalUnmet += unmet;
            }
        }

        return totalUnmet;
    }

    public static int getLocalProduction(MarketAPI market, String comID) {
        int totalProd = 0;
        for(Industry industry : market.getIndustries()) {
            if(industry.getSupply(comID).getQuantity().getModifiedInt() < 1) {
                continue;
            }
            totalProd += industry.getSupply(comID).getQuantity().getModifiedInt();
        }
        return totalProd;
    }

    public void printAllInfo() {
        Global.getLogger(getClass()).error("Commodity: " + com.getCommodity().getName());
        Global.getLogger(getClass()).error("totalActivity: " + totalActivity);
        Global.getLogger(getClass()).error("localProduction: " + localProduction);
        Global.getLogger(getClass()).error("localDemand: " + localDemand);
        Global.getLogger(getClass()).error("localDeficit: " + localDeficit);
        Global.getLogger(getClass()).error("totalImports: " + totalImports);
        Global.getLogger(getClass()).error("inFactionImports: " + inFactionImports);
        Global.getLogger(getClass()).error("externalImports: " + externalImports);
        Global.getLogger(getClass()).error("totalExports: " + totalExports);
        Global.getLogger(getClass()).error("inFactionExport: " + inFactionExport);
        Global.getLogger(getClass()).error("globalExport: " + globalExport);
        Global.getLogger(getClass()).error("canNotExport: " + canNotExport);
        Global.getLogger(getClass()).error("demandMet: " + demandMet);
        Global.getLogger(getClass()).error("demandMetWithLocal: " + demandMetWithLocal);
        Global.getLogger(getClass()).error("demandMetNotWithLocal: " + demandMetNotWithLocal);
        Global.getLogger(getClass()).error("available: " + available);

        float total1 = demandMetWithLocal +inFactionExport + globalExport + canNotExport;
        total1 += inFactionImports + externalImports + localDeficit;

        float total1Ratio = total1 / totalActivity;

        float total2 = demandMetWithLocal + totalExports + canNotExport + totalImports + localDeficit;

        float total2Ratio = total2 / totalActivity;

        Global.getLogger(getClass()).error("Ratio1: " + total1Ratio);
        Global.getLogger(getClass()).error("Ratio2: " + total2Ratio);
    }
}

package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;

public class CommodityStats {

    public final CommodityOnMarketAPI m_com;
    public final MarketAPI market;

    // Storage
    public long stored = 0;
    public long localProduction = 0;

    // Demand
    public long demandPreTrade = 0;
    public long demandMetWithLocal = 0;
    public long demandMetWithGlobal = 0;

    // Import
    public long inFactionImports = 0;
    public long externalImports = 0;

    // Export
    public long inFactionExport = 0;
    public long globalExport = 0;
    public long canNotExport = 0;

    public final long getTotalActivity() {
        return localProduction + getDeficitPreTrade();
    }

    public final long getDemandMet() { return demandMetWithLocal + demandMetWithGlobal; }
    public final long getDeficitPreTrade() { return demandPreTrade - demandMetWithLocal; }
    public final long getDeficit() { return demandPreTrade - getDemandMet(); }

    public long getTotalImports() { return inFactionImports + externalImports; }
    public long getTotalExports() { return inFactionExport + globalExport; }
    public final long getCanNotExport() {
        final long totalExportable = Math.max(0, localProduction - getDeficitPreTrade());
        return Math.max(0, totalExportable - getTotalExports());
    }

    public final void addInFactionImport(int a) {
        inFactionImports += a;
    }

    public final void addExternalImport(int a) {
        externalImports += a;
    }

    public final void addInFactionExport(int a) {
        inFactionExport += a;
    }

    public final void addGlobalExport(int a) {
        globalExport += a;
    }

    public CommodityStats(String comID, MarketAPI market) {
        this.market = market;
        this.m_com = market.getCommodityData(comID);

        recalculateTotalDemandSupplyForCommodity(market, comID);
        localProduction = m_com.getMaxSupply(); // TotalSupply in reality
        demandPreTrade = m_com.getMaxDemand(); // TotalDemand in reality

        demandMetWithLocal = Math.min(localProduction, demandPreTrade);

        Update();
    }

    /**
     * Call after all trade between the colonies are complete.
     */
    public final void Update() {
        final int shippingGlobal = Global.getSettings().getShippingCapacity(market, false);
        final int shippingInFaction = Global.getSettings().getShippingCapacity(market, true);


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

    public static List<Industry> getVisibleIndustries(MarketAPI market) {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public static void recalculateTotalDemandSupplyForCommodity(MarketAPI market, String comID) {
        CommodityOnMarketAPI com = market.getCommodityData(comID);

        com.setMaxDemand(0);
        com.setMaxSupply(0);

        for (Industry industry : getVisibleIndustries(market)) {

            // Ensure that the demand uses the ID of the commodity
            int demand = industry.getDemand(comID).getQuantity().getModifiedInt();
            com.setMaxDemand(com.getMaxDemand() + demand);

            int supply = industry.getSupply(comID).getQuantity().getModifiedInt();
            com.setMaxSupply(com.getMaxSupply() + supply);
        }
    }

    public static void recalculateMaxDemandAndSupplyForAll(MarketAPI market) {
        // Resets Max Demand & Supply
        for (CommodityOnMarketAPI com : market.getAllCommodities()) {
            com.setMaxDemand(0);
            com.setMaxSupply(0);
        }

        for (Industry industry : getVisibleIndustries(market)) {
            for (MutableCommodityQuantity demand : industry.getAllDemand()) {
                CommodityOnMarketAPI com = market.getCommodityData(demand.getCommodityId());
                com.setMaxDemand(com.getMaxDemand() + demand.getQuantity().getModifiedInt());
            }

            for (MutableCommodityQuantity supply : industry.getAllSupply()) {
                CommodityOnMarketAPI com = market.getCommodityData(supply.getCommodityId());
                com.setMaxSupply(com.getMaxSupply() + supply.getQuantity().getModifiedInt());
            }
        }
    }

    public void printAllInfo() {
        Global.getLogger(getClass()).error("Commodity: " + m_com.getCommodity().getName());
        Global.getLogger(getClass()).error("totalActivity: " + totalActivity);
        Global.getLogger(getClass()).error("localProduction: " + localProduction);
        Global.getLogger(getClass()).error("demandPreTrade: " + demandPreTrade);
        Global.getLogger(getClass()).error("deficitPreTrade: " + getDeficitPreTrade());
        Global.getLogger(getClass()).error("totalImports: " + getTotalImports());
        Global.getLogger(getClass()).error("inFactionImports: " + inFactionImports);
        Global.getLogger(getClass()).error("externalImports: " + externalImports);
        Global.getLogger(getClass()).error("totalExports: " + getTotalExports());
        Global.getLogger(getClass()).error("inFactionExport: " + inFactionExport);
        Global.getLogger(getClass()).error("globalExport: " + globalExport);
        Global.getLogger(getClass()).error("canNotExport: " + canNotExport);
        Global.getLogger(getClass()).error("demandMet: " + getDemandMet());
        Global.getLogger(getClass()).error("demandMetWithLocal: " + demandMetWithLocal);
        Global.getLogger(getClass()).error("demandMetNotWithLocal: " + demandMetWithGlobal);

        float total1 = demandMetWithLocal + inFactionExport + globalExport + canNotExport;
        total1 += inFactionImports + externalImports + getDeficitPreTrade();

        float total1Ratio = total1 / totalActivity;

        float total2 = demandMetWithLocal + getTotalExports() + canNotExport + getTotalImports() + getDeficitPreTrade();

        float total2Ratio = total2 / totalActivity;

        Global.getLogger(getClass()).error("Ratio1: " + total1Ratio);
        Global.getLogger(getClass()).error("Ratio2: " + total2Ratio);
    }
}

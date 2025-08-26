package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Pair;

public class CommodityStats {

    public final CommodityOnMarketAPI m_com;
    public final MarketAPI market;

    // Storage
    private long stored = 0;
    public float availabilityRatio = 1f;
    public long localProduction = 0;

    // Demand
    public long demandPreTrade = 0;
    public long demandMetWithLocal = 0;
    public long demandMetViaTrade = 0;

    // Import
    public long inFactionImports = 0;
    public long externalImports = 0;

    // Export
    public long inFactionExport = 0;
    public long globalExport = 0;

    public final long getTotalActivity() {
        return localProduction + getDeficitPreTrade();
    }

    public final long getDemandMet() { return demandMetWithLocal + demandMetViaTrade; }
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

        Update();
    }

    public final void addExternalImport(int a) {
        externalImports += a;

        Update();
    }

    public final void addInFactionExport(int a) {
        inFactionExport += a;

        Update();
    }

    public final void addGlobalExport(int a) {
        globalExport += a;

        Update();
    }

    public final void addStoredAmount(long a) {
        stored = Math.max(0, stored + a);
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

    private final void Update() {
        // final int shippingGlobal = Global.getSettings().getShippingCapacity(market, false);
        // final int shippingInFaction = Global.getSettings().getShippingCapacity(market, true);

        demandMetViaTrade = getTotalImports();
    }

    /**
     * Gets called each day to update the values and the stored amount.
     */
    public final void advance() {
        availabilityRatio = (float) getDemandMet() / demandPreTrade;
        
        addStoredAmount(getCanNotExport() - getDeficit());

        final long amount = getCanNotExport() - getDeficit();
        CargoAPI cargo = market.getSubmarket(ltv_getAvaliableInCargo().one).getCargo();

        if (amount > 0) {
            cargo.addItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), amount);
        } else {
            cargo.removeItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), amount);
        }
    }

    public Pair<String, Integer> ltv_getAvaliableInCargo() {
		String submarket = Submarkets.SUBMARKET_OPEN;
		if (market.getSubmarket(Submarkets.LOCAL_RESOURCES) != null) {
			submarket = Submarkets.LOCAL_RESOURCES;
		} else if (market.getSubmarket(Submarkets.SUBMARKET_OPEN) == null) {
			return new Pair<String, Integer>(submarket, 0);
		}

		return new Pair<String, Integer>(submarket,
			(int) market.getSubmarket(submarket).getCargo().getCommodityQuantity(m_com.getId()));
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
        Global.getLogger(getClass()).error("totalActivity: " + getTotalActivity());
        Global.getLogger(getClass()).error("localProduction: " + localProduction);
        Global.getLogger(getClass()).error("demandPreTrade: " + demandPreTrade);
        Global.getLogger(getClass()).error("deficitPreTrade: " + getDeficitPreTrade());
        Global.getLogger(getClass()).error("totalImports: " + getTotalImports());
        Global.getLogger(getClass()).error("inFactionImports: " + inFactionImports);
        Global.getLogger(getClass()).error("externalImports: " + externalImports);
        Global.getLogger(getClass()).error("totalExports: " + getTotalExports());
        Global.getLogger(getClass()).error("inFactionExport: " + inFactionExport);
        Global.getLogger(getClass()).error("globalExport: " + globalExport);
        Global.getLogger(getClass()).error("canNotExport: " + getCanNotExport());
        Global.getLogger(getClass()).error("demandMet: " + getDemandMet());
        Global.getLogger(getClass()).error("demandMetWithLocal: " + demandMetWithLocal);
        Global.getLogger(getClass()).error("demandMetNotWithLocal: " + demandMetViaTrade);

        float total1 = demandMetWithLocal + inFactionExport + globalExport + getCanNotExport();
        total1 += inFactionImports + externalImports + getDeficitPreTrade();

        float total1Ratio = total1 / getTotalActivity();

        float total2 = demandMetWithLocal + getTotalExports() + getCanNotExport() + getTotalImports() + getDeficitPreTrade();

        float total2Ratio = total2 / getTotalActivity();

        Global.getLogger(getClass()).error("Ratio1: " + total1Ratio);
        Global.getLogger(getClass()).error("Ratio2: " + total2Ratio);
    }
}

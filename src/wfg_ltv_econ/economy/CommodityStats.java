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
    
    // Local
    public long localProduction = 0;
    public long demandBase = 0;

    // Import
    public long inFactionImports = 0;
    public long globalImports = 0;

    // Export
    public long inFactionExports = 0;
    public long globalExports = 0;

    public final long getDemandMet() {
        return getDemandMetLocally() + getDemandMetViaTrade();
    }
    public final long getDemandMetLocally() {
        return Math.min(localProduction, demandBase); 
    }
    public final long getDeficitPreTrade() {
        return demandBase - getDemandMetLocally();
    }
    public final long getDemandMetViaTrade() {
        return Math.min(getTotalImports(), demandBase - getDemandMetLocally());
    }
    public final long getDeficit() {
        return demandBase - getDemandMet();
    }
    public long getTotalImports() {
        return inFactionImports + globalImports;
    }
    public long getTotalExports() {
        return inFactionExports + globalExports;
    }
    public final long getAvailable() {
        return localProduction + getTotalImports();
    }
    public final long getCanNotExport() {
        final long totalExportable = Math.max(0, localProduction - getDeficitPreTrade());
        return Math.max(0, totalExportable - getTotalExports());
    }
    public final long getEconomicFootprint() {
        return getDemandMet() + getDeficit() + getTotalExports() + getCanNotExport();
    }
    public final double getAvailabilityRatio() {
        return demandBase == 0 ? 1f : (double) getDemandMet() / demandBase;
    }


    public final void addInFactionImport(int a) {
        inFactionImports += a;

        Update();
    }

    public final void addExternalImport(int a) {
        globalImports += a;

        Update();
    }

    public final void addInFactionExport(int a) {
        inFactionExports += a;

        Update();
    }

    public final void addGlobalExport(int a) {
        globalExports += a;

        Update();
    }

    public final long getStoredAmount() {
        return stored;
    }

    public final void addStoredAmount(long a) {
        stored = Math.max(0, stored + a);
    }

    public CommodityStats(String comID, MarketAPI market) {
        this.market = market;
        this.m_com = market.getCommodityData(comID);

        Update();
    }

    private final void Update() {
        recalculateTotalDemandSupplyForCommodity(market, m_com.getId());
        localProduction = m_com.getMaxSupply(); // TotalSupply in reality
        demandBase = m_com.getMaxDemand(); // TotalDemand in reality
    }

    /**
     * Gets called each day to update the values and the stored amount.
     */
    public final void advance() {

        Update();
        
        addStoredAmount(getCanNotExport() - getDeficit());

        final long amount = getCanNotExport() - getDeficit();
        CargoAPI cargo = market.getSubmarket(ltv_getAvaliableInCargo().one).getCargo();

        if (amount > 0) {
            cargo.addItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), amount);
        } else {
            cargo.removeItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), Math.abs(amount));
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

    /**
     * Returns the fraction of demand that could be satisfied by the current stored amount.
     */
    public float getStoredCoverageRatio() {
        if (demandBase <= 0) return 1f;

        return Math.min((float) stored / demandBase, 1f);
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
        Global.getLogger(getClass()).error("economicFootprint: " + getEconomicFootprint());
        Global.getLogger(getClass()).error("localProduction: " + localProduction);
        Global.getLogger(getClass()).error("baseDemand: " + demandBase);
        Global.getLogger(getClass()).error("deficitPreTrade: " + getDeficitPreTrade());
        Global.getLogger(getClass()).error("totalImports: " + getTotalImports());
        Global.getLogger(getClass()).error("inFactionImports: " + inFactionImports);
        Global.getLogger(getClass()).error("externalImports: " + globalImports);
        Global.getLogger(getClass()).error("totalExports: " + getTotalExports());
        Global.getLogger(getClass()).error("inFactionExport: " + inFactionExports);
        Global.getLogger(getClass()).error("globalExport: " + globalExports);
        Global.getLogger(getClass()).error("canNotExport: " + getCanNotExport());
        Global.getLogger(getClass()).error("demandMet: " + getDemandMet());
        Global.getLogger(getClass()).error("demandMetWithLocal: " + getDemandMetLocally());
        Global.getLogger(getClass()).error("demandMetNotWithLocal: " + getDemandMetViaTrade());

        float trade = getDemandMetLocally() + getTotalExports() + getCanNotExport() + getDemandMetViaTrade() + getDeficit();

        float ratio = trade / getEconomicFootprint();

        Global.getLogger(getClass()).error("ratio: " + ratio);
    }
}

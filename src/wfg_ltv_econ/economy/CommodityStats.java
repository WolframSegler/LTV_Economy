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
    public final long getTotalExportable() {
        return Math.max(0, localProduction - demandBase);
    }
    public final long getCanNotExport() {
        return Math.max(0, getTotalExportable() - getTotalExports());
    }
    public final long getEconomicFootprint() {
        return getDemandMet() + getDeficit() + getTotalExports() + getCanNotExport();
    }
    public final double getAvailabilityRatio() {
        return demandBase == 0 ? 1f : (double) getDemandMet() / demandBase;
    }


    public final void addInFactionImport(long a) {
        inFactionImports += a;
    }

    public final void addGlobalImport(long a) {
        globalImports += a;
    }

    public final void addInFactionExport(long a) {
        inFactionExports += a;
    }

    public final void addGlobalExport(long a) {
        globalExports += a;
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

        update();
    }

    public final void update() {
        localProduction = 0;
        demandBase = 0;

        for (Industry industry : getVisibleIndustries(market)) {

            // Ensure that the demand uses the ID of the commodity
            int demand = industry.getDemand(m_com.getId()).getQuantity().getModifiedInt();
            demandBase += demand;

            int supply = industry.getSupply(m_com.getId()).getQuantity().getModifiedInt();
            localProduction += supply;
        }

        localProduction = localProduction < 1 ? 0 : localProduction;
        demandBase = demandBase < 1 ? 0 : demandBase;
    }

    public final void resetTradeValues() {
        inFactionImports = 0;
        globalImports = 0;
        inFactionExports = 0;
        globalExports = 0;
    }

    /**
     * Gets called each day to update the values and the stored amount.
     */
    public final void advance(boolean fakeAdvance) {

        update();

        if (getTotalExportable() > 0) {
            trade();
        }
        
        if (!fakeAdvance) {
            addStoredAmount(getCanNotExport() - getDeficit());
    
            final long amount = getCanNotExport() - getDeficit();
            CargoAPI cargo = market.getSubmarket(ltv_getAvaliableInCargo().one).getCargo();
    
            if (amount > 0) {
                cargo.addItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), amount);
            } else {
                cargo.removeItems(CargoAPI.CargoItemType.RESOURCES, m_com.getId(), Math.abs(amount));
            }
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

    private final void trade() {
        List<MarketAPI> importers = EconomyEngine.getInstance().getMarketsImportingCom(
            m_com.getCommodity(), market, false
        );

        importers = EconomyEngine.getInstance().computeTradeScore(importers, market, m_com.getCommodity());

        long exportableRemaining = getTotalExportable();

        for(MarketAPI importer : importers) {
            if (exportableRemaining <= 0) break;

            CommodityStats importerStats = EconomyEngine.getInstance().getComStats(m_com.getId(), importer);
            boolean sameFaction = importerStats.market.getFaction().equals(market.getFaction());

            long importerDeficit = importerStats.getDeficit();
            if (importerDeficit <= 0) continue;

            long amountToSend = Math.min(exportableRemaining, importerDeficit);

            exportableRemaining -= amountToSend;
            if(sameFaction) {
                inFactionExports += amountToSend;
                importerStats.addInFactionImport(amountToSend);
            } else {
                globalExports += amountToSend;
                importerStats.addGlobalImport(amountToSend);
            }
        }
    }

    /**
     * Returns the fraction of demand that could be satisfied by the current stored amount.
     */
    public float getStoredCoverageRatio() {
        if (demandBase <= 0) return 1f;

        return Math.min((float) stored / demandBase, 1f);
    }

    public List<Industry> getVisibleIndustries() {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public static List<Industry> getVisibleIndustries(MarketAPI market) {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
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

        // Guard against below zero
        for (CommodityOnMarketAPI com : market.getAllCommodities()) {
            com.setMaxDemand(com.getMaxSupply() < 1 ? 0 : com.getMaxDemand());
            com.setMaxSupply(com.getMaxDemand() < 1 ? 0 : com.getMaxSupply());
        }
    }

    public void logAllInfo() {
        Global.getLogger(getClass()).error("Commodity: " + m_com.getCommodity().getName());
        Global.getLogger(getClass()).error("economicFootprint: " + getEconomicFootprint());
        Global.getLogger(getClass()).error("localProduction: " + localProduction);
        Global.getLogger(getClass()).error("baseDemand: " + demandBase);
        Global.getLogger(getClass()).error("deficitPreTrade: " + getDeficitPreTrade());
        Global.getLogger(getClass()).error("totalImports: " + getTotalImports());
        Global.getLogger(getClass()).error("inFactionImports: " + inFactionImports);
        Global.getLogger(getClass()).error("globalImports: " + globalImports);
        Global.getLogger(getClass()).error("totalExports: " + getTotalExports());
        Global.getLogger(getClass()).error("inFactionExport: " + inFactionExports);
        Global.getLogger(getClass()).error("globalExport: " + globalExports);
        Global.getLogger(getClass()).error("canNotExport: " + getCanNotExport());
        Global.getLogger(getClass()).error("demandMet: " + getDemandMet());
        Global.getLogger(getClass()).error("demandMetWithLocal: " + getDemandMetLocally());
        Global.getLogger(getClass()).error("demandMetViaTrade: " + getDemandMetViaTrade());

        float trade = getDemandMetLocally() + getTotalExports() + getCanNotExport() + getDemandMetViaTrade() + getDeficit();

        float ratio = trade / getEconomicFootprint();

        Global.getLogger(getClass()).error("ratio: " + ratio);
    }
}

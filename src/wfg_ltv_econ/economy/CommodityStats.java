package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;

public class CommodityStats {

    public static final String WORKER_MOD_ID = "worker_assigned";

    public final String comID;
    public final String marketID;

    public transient MarketAPI market;

    // Storage
    private long stored = 0;

    private int localProd = 0;
    private int demandBase = 0;

    private Map<String, MutableStat> localProdMutables = new HashMap<>();
    private Map<String, MutableStat> demandBaseMutables = new HashMap<>();

    public float localProdMult = 1f;
    public float demandBaseMult = 1f;

    // Import
    public long inFactionImports = 0;
    public long globalImports = 0;

    // Export
    public long inFactionExports = 0;
    public long globalExports = 0;

    public final Map<String, MutableStat> getLocalProductionStat() {
        return localProdMutables;
    }
    public final Map<String, MutableStat> getBaseDemandStat() {
        return demandBaseMutables;
    }
    public final MutableStat getLocalProductionStat(String industryID) {
        final MutableStat mutable = localProdMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final MutableStat getBaseDemandStat(String industryID) {
        final MutableStat mutable = demandBaseMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final int getLocalProduction(boolean modified) {
        return modified ? (int) (localProd*localProdMult) : localProd;
    }
    public final int getBaseDemand(boolean modified) {
        return modified ? (int) (demandBase*demandBaseMult) : demandBase;
    }
    public final long getDemandMet() {
        return getDemandMetLocally() + getDemandMetViaTrade();
    }
    public final long getDemandMetLocally() {
        return Math.min((long) getLocalProduction(true), getBaseDemand(false)); 
    }
    public final long getDeficitPreTrade() {
        return getBaseDemand(false) - getDemandMetLocally();
    }
    public final long getDemandMetViaTrade() {
        return Math.min(getTotalImports(), getBaseDemand(false) - getDemandMetLocally());
    }
    public final long getDeficit() {
        return getBaseDemand(false) - getDemandMet();
    }
    public long getTotalImports() {
        return inFactionImports + globalImports;
    }
    public long getTotalExports() {
        return inFactionExports + globalExports;
    }
    public final long getAvailable() {
        return getLocalProduction(true) + getTotalImports();
    }
    public final long getBaseExportable() {
        return Math.max(0, getLocalProduction(true) - getBaseDemand(false));
    }
    public final long getRemainingExportable() {
        return getBaseExportable() - getTotalExports();
    }
    public final long getCanNotExport() {
        return Math.max(0, getBaseExportable() - getTotalExports());
    }
    public final long getEconomicFootprint() {
        return getDemandMet() + getDeficit() + getTotalExports() + getCanNotExport();
    }
    public final double getAvailabilityRatio() {
        return getBaseDemand(false) == 0 ? 1f : (double) getDemandMet() / getBaseDemand(false);
    }
    public final long getRealBalance() {
        return getAvailable() - getBaseDemand(true) - getTotalExports();
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

    public CommodityStats(String comID, String marketID) {
        this.marketID = marketID;
        this.comID = comID;

        readResolve();
    }

    public Object readResolve() {
        market = Global.getSector().getEconomy().getMarket(marketID);

        return this;
    }

    public final void update() {
        localProdMutables = new HashMap<>();
        demandBaseMutables = new HashMap<>();

        Global.getLogger(getClass()).error(market.getName() + " " + market.getSize());
        Global.getLogger(getClass()).error(comID);

        for (Industry industry : getVisibleIndustries(market)) {
            if(industry.getSupply(comID).getQuantity().getModifiedValue() > 0) {
                MutableStat supplyStat = LtvCompatibilityLayer.convertIndSupplyStat(industry, comID);
                localProdMutables.put(industry.getId(), supplyStat);

                Global.getLogger(getClass()).error(industry.getId() + " supply:");
               
                Global.getLogger(getClass()).error("base: " + supplyStat.base);
                Global.getLogger(getClass()).error("Flat");
                for(StatMod a : supplyStat.getFlatMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("Mult");
                for(StatMod a : supplyStat.getMultMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("Percent");
                for(StatMod a : supplyStat.getPercentMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("final: " + supplyStat.getModifiedInt());

            }
            if(industry.getDemand(comID).getQuantity().getModifiedValue() > 0) {
                MutableStat demandStat = LtvCompatibilityLayer.convertIndDemandStat(industry, comID);
                demandBaseMutables.put(industry.getId(), demandStat);

                Global.getLogger(getClass()).error(industry.getId() + " demand:");
                Global.getLogger(getClass()).error("base: " + demandStat.base);
                Global.getLogger(getClass()).error("Flat");
                for(StatMod a : demandStat.getFlatMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("Mult");
                for(StatMod a : demandStat.getMultMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("Percent");
                for(StatMod a : demandStat.getPercentMods().values()) {
                    Global.getLogger(getClass()).error(a.source +" "+ a.value +" "+a.desc);
                }
                Global.getLogger(getClass()).error("final: " + demandStat.getModifiedInt());
            }
        }

        Global.getLogger(getClass()).error("----------------------------------------");

        for (MutableStat stat : localProdMutables.values()) {
            localProd += stat.getModifiedInt();
        }

        for (MutableStat stat : demandBaseMutables.values()) {
            demandBase += stat.getModifiedInt();
        }

        localProd = localProd < 1 ? 0 : localProd;
        demandBase = demandBase < 1 ? 0 : demandBase;
    }

    public final void reset() {
        inFactionImports = 0;
        globalImports = 0;
        inFactionExports = 0;
        globalExports = 0;

        localProdMult = 1f;
        demandBaseMult = 1f;

        localProd = 0;
        demandBase = 0;
    }

    /**
     * Gets called each day to update the values and the stored amount.
     */
    public final void advance(boolean fakeAdvance) {
        
        if (!fakeAdvance) {
            addStoredAmount(getRealBalance());
        }
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
        if (getBaseDemand(false) <= 0) return 1f;

        return Math.min((float) stored / getBaseDemand(false), 1f);
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

    public void logAllInfo() {
        final String comName = Global.getSettings().getCommoditySpec(comID).getName();
        Global.getLogger(getClass()).error("Commodity: " + comName);
        Global.getLogger(getClass()).error("economicFootprint: " + getEconomicFootprint());
        Global.getLogger(getClass()).error("localProduction: " + getLocalProduction(true));
        Global.getLogger(getClass()).error("baseDemand: " + getBaseDemand(false));
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

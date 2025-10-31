package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.industry.IndustryIOs;

public class CommodityStats {

    public static final String WORKER_MOD_ID = "worker_assigned";

    public final String comID;
    public final String marketID;

    public transient MarketAPI market;
    public transient CommoditySpecAPI spec;

    // Storage
    private long stored = 0;

    private int localProd = 0;
    private int demandBase = 0;

    private Map<String, MutableStat> localProdMutables = new HashMap<>();
    private Map<String, MutableStat> demandBaseMutables = new HashMap<>();

    public float localProdMult = 1f;

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
        return modified ? (int) (demandBase*getStoredCoverageRatio()) : demandBase;
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
        return getDemandMetViaFactionTrade() + getDemandMetViaGlobalTrade();
    }
    public final long getDemandMetViaFactionTrade() {
        return Math.min(inFactionImports, getDeficitPreTrade());
    }
    public final long getDemandMetViaGlobalTrade() {
        return Math.min(globalImports, getDeficitPreTrade() - getDemandMetViaFactionTrade());
    }
    public final long getOverImports() {
        return Math.max(0, getTotalImports() - getDemandMetViaTrade());
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
        return getDemandMet() + getDeficit() + getOverImports() + getTotalExports() + getCanNotExport();
    }
    public final double getAvailabilityRatio() {
        return getBaseDemand(false) == 0 ? 1f : (double) getDemandMet() / getBaseDemand(false);
    }
    public final long getRealBalance() {
        return getAvailable() - getBaseDemand(true) - getTotalExports();
    }
    public final float getStoredCoverageRatio() {
        if (demandBase <= 0) return 1f;

        return Math.min(stored / (float) demandBase, 1f);
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
        spec = Global.getSettings().getCommoditySpec(comID);

        return this;
    }

    public final void update() {
        localProd = 0;
        demandBase = 0;
        
        localProdMutables = new HashMap<>();
        demandBaseMutables = new HashMap<>();

        for (Industry industry : getVisibleIndustries(market)) {
            if (IndustryIOs.hasSupply(industry, comID)) {
                MutableStat supplyStat = CompatLayer.convertIndSupplyStat(industry, comID);
                localProdMutables.put(industry.getId(), supplyStat);

            }
            if (IndustryIOs.hasDemand(industry, comID)) {
                MutableStat demandStat = CompatLayer.convertIndDemandStat(industry, comID);
                demandBaseMutables.put(industry.getId(), demandStat);
                
            }
        }

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

    public float getUnitPrice(PriceType type, int amount) {
        final float basePrice = spec.getBasePrice();

        final long stock = Math.max(getStoredAmount() - amount, 1);
        final float demand = EconomyConfig.DAYS_TO_COVER * Math.max(demandBase, 1);
        final float ratio = stock / demand;

        final float ELASTICITY = 0.65f;
        float scarcityMult = (float) Math.pow(1f / ratio, ELASTICITY);
        scarcityMult = Math.max(0.25f, Math.min(4f, scarcityMult));

        float directionBias;
        switch (type) {
        case BUYING:
            directionBias = 0.9f;
            break;
        case SELLING:
            directionBias = 1.1f;
            break;
        case NEUTRAL: default:
            directionBias = 1f;
            break;
        }

        float price = basePrice * scarcityMult * directionBias;

        return Math.max(price, 1f);
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
        float trade = getDemandMetLocally() + getTotalExports() + getCanNotExport() +
            getDemandMetViaTrade() + getDeficit();

        float ratio = trade / getEconomicFootprint();

        Global.getLogger(getClass()).info("\n" +
            "---- COMMODITY STATS LOG ----" + "\n" +
            "Commodity: " + comID + "\n" +
            "economicFootprint: " + getEconomicFootprint() + "\n" +
            "localProduction: " + getLocalProduction(true) + "\n" +
            "baseDemand: " + getBaseDemand(false) + "\n" +
            "deficitPreTrade: " + getDeficitPreTrade() + "\n" +
            "totalImports: " + getTotalImports() + "\n" +
            "inFactionImports: " + inFactionImports + "\n" +
            "globalImports: " + globalImports + "\n" +
            "totalExports: " + getTotalExports() + "\n" +
            "inFactionExport: " + inFactionExports + "\n" +
            "globalExport: " + globalExports + "\n" +
            "canNotExport: " + getCanNotExport() + "\n" +
            "demandMet: " + getDemandMet() + "\n" +
            "demandMetWithLocal: " + getDemandMetLocally() + "\n" +
            "demandMetViaTrade: " + getDemandMetViaTrade() + "\n" +
            "ratio: " + ratio
        );
    }

    public static enum PriceType {
        BUYING,   // what the market will pay when buying from others
        SELLING,  // what the market charges when selling to others
        NEUTRAL  // internal baseline
    }
}
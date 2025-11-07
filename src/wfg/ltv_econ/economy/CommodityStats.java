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
    private double stored = 0;

    private transient float localProd = 0;
    private transient float demandBase = 0;

    private transient Map<String, MutableStat> localProdMutables = new HashMap<>();
    private transient Map<String, MutableStat> demandBaseMutables = new HashMap<>();

    public transient float localProdMult = 1f;

    // Import
    public transient float inFactionImports = 0;
    public transient float globalImports = 0;
    private transient float importExclusiveDemand = 0;

    // Export
    public transient float inFactionExports = 0;
    public transient float globalExports = 0;

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
    public final float getLocalProduction(boolean modified) {
        return modified ? localProd*localProdMult : localProd;
    }
    public final float getBaseDemand(boolean modified) {
        return modified ? demandBase*getStoredCoverageRatio() : demandBase;
    }
    public final float getDeficitMet() {
        return getDeficitMetLocally() + getDeficitMetViaTrade();
    }
    public final float getDeficitMetLocally() {
        return Math.min(getLocalProduction(true), demandBase); 
    }
    public final float getDeficitPreTrade() {
        return demandBase - getDeficitMetLocally();
    }
    public final float getDemandPreTrade() {
        return getDeficitPreTrade() + importExclusiveDemand;
    }
    public final float getImportExclusiveDemand() {
        return importExclusiveDemand;
    }
    public final float getDeficitMetViaTrade() {
        return getDeficitMetViaFactionTrade() + getDeficitMetViaGlobalTrade();
    }
    public final float getDeficitMetViaFactionTrade() {
        return Math.min(inFactionImports, getDeficitPreTrade());
    }
    public final float getDeficitMetViaGlobalTrade() {
        return Math.min(globalImports, getDeficitPreTrade() - getDeficitMetViaFactionTrade());
    }
    public final float getOverImports() {
        return Math.max(0, getTotalImports() - importExclusiveDemand - getDeficitMetViaTrade());
    }
    public final float getDeficit() {
        return demandBase - getDeficitMet();
    }
    public final float getTotalImports() {
        return inFactionImports + globalImports;
    }
    public final float getTotalExports() {
        return inFactionExports + globalExports;
    }
    public final float getAvailable() {
        return getLocalProduction(true) + getTotalImports();
    }
    public final float getBaseExportable() {
        return Math.max(0, getLocalProduction(true) - demandBase);
    }
    public final float getRemainingExportable() {
        return getBaseExportable() - getTotalExports();
    }
    public final float getCanNotExport() {
        return Math.max(0, getBaseExportable() - getTotalExports());
    }
    public final float getEconomicFootprint() {
        return getDeficitMet() + getDeficit() + getImportExclusiveDemand() + getOverImports() +
            getTotalExports() + getCanNotExport();
    }
    public final float getAvailabilityRatio() {
        return demandBase == 0 ? 1f : getDeficitMet() / demandBase;
    }
    public final float getRealBalance() {
        return getAvailable() - getBaseDemand(true) - getTotalExports();
    }
    public final float getStoredCoverageRatio() {
        if (demandBase <= 0) return 1f;

        return (float) Math.min(stored / demandBase, 1f);
    }

    public final void addInFactionImport(float a) {
        inFactionImports += a;
    }

    public final void addGlobalImport(float a) {
        globalImports += a;
    }

    public final void addInFactionExport(float a) {
        inFactionExports += a;
    }

    public final void addGlobalExport(float a) {
        globalExports += a;
    }

    public final double getStored() {
        return stored;
    }

    public final long getRoundedStored() {
        return (long) stored;
    }

    public final void addStoredAmount(double a) {
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
        importExclusiveDemand = 0;
        
        localProdMutables = new HashMap<>();
        demandBaseMutables = new HashMap<>();

        for (Industry industry : getVisibleIndustries(market)) {
            if (IndustryIOs.hasSupply(industry, comID)) {
                if (!IndustryIOs.getIndConfig(industry).ignoreLocalStockpiles) {
                    final MutableStat supplyStat = CompatLayer.convertIndSupplyStat(industry, comID);
                    localProdMutables.put(industry.getId(), supplyStat);
                }

            }
            if (IndustryIOs.hasDemand(industry, comID)) {
                final MutableStat demandStat = CompatLayer.convertIndDemandStat(industry, comID);
                
                if (IndustryIOs.getIndConfig(industry).ignoreLocalStockpiles) {
                    importExclusiveDemand += demandStat.getModifiedValue();
                } else {
                    demandBaseMutables.put(industry.getId(), demandStat);
                }
            }
        }

        for (MutableStat stat : localProdMutables.values()) {
            localProd += stat.getModifiedValue();
        }

        for (MutableStat stat : demandBaseMutables.values()) {
            demandBase += stat.getModifiedValue();
        }

        localProd = localProd < 1 ? 0 : localProd;
        demandBase = demandBase < 1 ? 0 : demandBase;
    }

    public final void reset() {
        inFactionImports = 0;
        globalImports = 0;
        importExclusiveDemand = 0;

        inFactionExports = 0;
        globalExports = 0;

        localProdMult = 1f;

        localProd = 0;
        demandBase = 0;
    }

    /**
     * Gets called each day to update the values and the stored amount.
     */
    public final void advance() {
        addStoredAmount(getRealBalance());
    }

    public float getUnitPrice(PriceType type, int amount) {
        final float basePrice = spec.getBasePrice();

        final double stock = Math.max(getStored() - amount, 1);
        final float demand = EconomyConfig.DAYS_TO_COVER * Math.max(demandBase, 1);
        final float ratio = (float) stock / demand;

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
        float trade = getDeficitMetLocally() + getTotalExports() + getCanNotExport() +
            getDeficitMetViaTrade() + getDeficit();

        float ratio = trade / getEconomicFootprint();

        Global.getLogger(getClass()).info("\n" +
            "---- COMMODITY STATS LOG ----" + "\n" +
            "Commodity: " + comID + "\n" +
            "economicFootprint: " + getEconomicFootprint() + "\n" +
            "localProduction: " + getLocalProduction(true) + "\n" +
            "baseDemand: " + demandBase + "\n" +
            "deficitPreTrade: " + getDeficitPreTrade() + "\n" +
            "totalImports: " + getTotalImports() + "\n" +
            "inFactionImports: " + inFactionImports + "\n" +
            "globalImports: " + globalImports + "\n" +
            "totalExports: " + getTotalExports() + "\n" +
            "inFactionExport: " + inFactionExports + "\n" +
            "globalExport: " + globalExports + "\n" +
            "canNotExport: " + getCanNotExport() + "\n" +
            "demandMet: " + getDeficitMet() + "\n" +
            "demandMetWithLocal: " + getDeficitMetLocally() + "\n" +
            "demandMetViaTrade: " + getDeficitMetViaTrade() + "\n" +
            "ratio: " + ratio
        );
    }

    public static enum PriceType {
        BUYING,   // what the market will pay when buying from others
        SELLING,  // what the market charges when selling to others
        NEUTRAL  // internal baseline
    }
}
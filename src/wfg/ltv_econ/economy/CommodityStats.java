package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.PriceVariability;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.campaign.econ.Market;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.industry.IndustryIOs;

/**
 * <h3>Naming Convention: <code>Flow</code> vs. <code>Stored</code></h3>
 *
 * <p>The naming convention to distinguish between
 * values that represent <strong>per-tick quantities</strong> and values that
 * represent <strong>stored quantities</strong>.</p>
 *
 * <h3>1. <code>Flow*</code> methods</h3>
 * <ul>
 *   <li>Represent tick-based production, consumption, imports, exports,
 *       and all other transient values.</li>
 *   <li>Apply only to the current simulation tick.</li>
 * </ul>
 *
 * <h3>2. <code>Stored*</code> methods</h3>
 * <ul>
 *   <li>Represent persistent stored quantities carried across ticks.</li>
 *   <li>Define the market’s reserves, deficits, and long-term stability.</li>
 * </ul>
 *
 * <p>Any method that does <em>not</em> use one of these prefixes should be treated as a
 * derived or conceptual value, independent of the flow/stock distinction.</p>
 *
 * <p>This convention ensures that the meaning of each getter is always clear and
 * prevents subtle logic errors when computing balances or updating stored amounts.</p>
 */
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

    public final Map<String, MutableStat> getFlowProductionStat() {
        return localProdMutables;
    }
    public final Map<String, MutableStat> getFlowBaseDemandStat() {
        return demandBaseMutables;
    }
    public final MutableStat getProductionStat(String industryID) {
        final MutableStat mutable = localProdMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final MutableStat getBaseDemandStat(String industryID) {
        final MutableStat mutable = demandBaseMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final float getProduction(boolean modified) {
        return modified ? localProd*localProdMult : localProd;
    }
    public final float getBaseDemand(boolean modified) {
        return modified ? demandBase*getStoredAvailabilityRatio() : demandBase;
    }
    public final float getFlowDeficitMet() {
        return getFlowDeficitMetLocally() + getFlowDeficitMetViaTrade();
    }
    public final float getFlowDeficitMetLocally() {
        return Math.min(getProduction(true), demandBase); 
    }
    public final float getFlowDeficitPreTrade() {
        return demandBase - getFlowDeficitMetLocally();
    }
    public final float getFlowDemandPreTrade() {
        return getFlowDeficitPreTrade() + importExclusiveDemand;
    }
    public final float getImportExclusiveDemand() {
        return importExclusiveDemand;
    }
    public final float getFlowDeficitMetViaTrade() {
        return getFlowDeficitMetViaFactionTrade() + getFlowDeficitMetViaGlobalTrade();
    }
    public final float getFlowDeficitMetViaFactionTrade() {
        return Math.min(inFactionImports, getFlowDeficitPreTrade());
    }
    public final float getFlowDeficitMetViaGlobalTrade() {
        return Math.min(globalImports, getFlowDeficitPreTrade() - getFlowDeficitMetViaFactionTrade());
    }
    public final float getFlowOverImports() {
        return Math.max(0, getTotalImports() - importExclusiveDemand - getFlowDeficitMetViaTrade());
    }
    public final float getFlowDeficit() {
        return demandBase - getFlowDeficitMet();
    }
    public final float getTotalImports() {
        return inFactionImports + globalImports;
    }
    public final float getTotalExports() {
        return inFactionExports + globalExports;
    }
    public final float getFlowAvailable() {
        return getProduction(true) + getTotalImports();
    }
    public final double getStoredAvailable() {
        return getStored() + getFlowAvailable();
    }
    public final float getPreferredStockpile() {
        return EconomyConfig.DAYS_TO_COVER * demandBase;
    }
    public final float getFlowProductionSurplus() {
        return Math.max(0, getProduction(true) - demandBase);
    }
    public final float getFlowRemainingExportable() {
        return getFlowProductionSurplus() - getTotalExports();
    }
    public final double getStoredRemainingExportable() {
        return Math.max(0, stored + getFlowRemainingExportable() - getPreferredStockpile());
    }
    public final float getFlowCanNotExport() {
        return Math.max(0, getFlowRemainingExportable());
    }
    public final float getFlowEconomicFootprint() {
        return getFlowDeficitMet() + getFlowDeficit() + getImportExclusiveDemand() + getFlowOverImports() +
            getTotalExports() + getFlowCanNotExport();
    }
    public final double getStoredEconomicFootprint() {
        return getStoredAvailabilityRatio() * getPreferredStockpile() +
            getStoredRemainingExportable() +
            getImportExclusiveDemand() +
            getTotalExports();
    }
    public final float getFlowRealBalance() {
        return getFlowAvailable() - getBaseDemand(true) - getTotalExports();
    }
    public final float getFlowAvailabilityRatio() {
        return demandBase == 0 ? 1f : getFlowDeficitMet() / demandBase;
    }
    public final float getStoredAvailabilityRatio() {
        return demandBase <= 0 ? 1f : (float) Math.min(stored / demandBase, 1f);
    }
    public final double getStored() {
        return stored;
    }
    public final long getRoundedStored() {
        return (long) stored;
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

    public final void advance() {
        addStoredAmount(getFlowRealBalance());
    }

    public static final float MAX_SUBMARKET_STOCK_MULT = 50f;
    public float getUnitPrice(PriceType type, int amount) {
        final int n = Math.abs(amount);
        final boolean isBuying = type == PriceType.MARKET_BUYING;
        final int signedAmount = isBuying ? n : -n;

        final float basePrice = spec.getBasePrice();
        final float demand = getPreferredStockpile();
        final double storedAfter = stored + signedAmount;

        final double SHIFT = demand * MAX_SUBMARKET_STOCK_MULT;
        final float buyExp  = 0.25f * MAX_SUBMARKET_STOCK_MULT;
        final float sellExp = 0.5f * MAX_SUBMARKET_STOCK_MULT;
        final float exponent = isBuying ? buyExp : sellExp;

        final float avgMultiplier = (float)((Math.pow(demand, exponent) / (1.0 - exponent)) *
            (Math.pow(Math.max(SHIFT + storedAfter,1), 1.0 - exponent) -
            Math.pow(SHIFT + stored, 1.0 - exponent))
        ) / signedAmount;

        final float equilibrium = (float) Math.pow(demand / (SHIFT + demand), exponent);
        final float scarcityNorm = avgMultiplier / equilibrium;

        final float priceMult = (float) Math.max(0.25, Math.min(4.0, scarcityNorm));
        return Math.max(basePrice * priceMult, 1f);
    }

    public float computeVanillaPrice(int amount, boolean isSellingToMarket, boolean isPlayer) {
        if (amount < 1) return 0f;

        final Market mkt = (Market) market;
        if (spec.getPriceVariability() == PriceVariability.V0) {
            return spec.getBasePrice() * amount;
        }

        final PriceType type = isSellingToMarket ? PriceType.MARKET_BUYING : PriceType.MARKET_SELLING;
        final float unitPrice = getUnitPrice(type, amount);

        final StatBonus priceMod;
        if (isPlayer) {
            priceMod = isSellingToMarket ? mkt.getCommodityData(comID).getPlayerDemandPriceMod() :
                mkt.getCommodityData(comID).getPlayerSupplyPriceMod();
        } else {
            priceMod = isSellingToMarket ? mkt.getDemandPriceMod() : mkt.getSupplyPriceMod();
        }

        final float totalPrice = priceMod.computeEffective(unitPrice) * amount;

        return (float)Math.floor(Math.max(totalPrice, amount));
    }

    public static final List<MarketAPI> getFactionMarkets(String factionId) {
        List<MarketAPI> result = new ArrayList<>();

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.getFactionId().equals(factionId) && market.isPlanetConditionMarketOnly() == false) {
                result.add(market);
            }
        }

        return result;
    }

    public final List<Industry> getVisibleIndustries() {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public static final List<Industry> getVisibleIndustries(MarketAPI market) {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public final void logAllInfo() {
        float trade = getFlowDeficitMetLocally() + getTotalExports() + getFlowCanNotExport() +
            getFlowDeficitMetViaTrade() + getFlowDeficit();

        float ratio = trade / getFlowEconomicFootprint();

        Global.getLogger(getClass()).info("\n" +
            "---- COMMODITY STATS LOG ----" + "\n" +
            "Commodity: " + comID + "\n" +
            "economicFootprint: " + getFlowEconomicFootprint() + "\n" +
            "localProduction: " + getProduction(true) + "\n" +
            "baseDemand: " + demandBase + "\n" +
            "deficitPreTrade: " + getFlowDeficitPreTrade() + "\n" +
            "totalImports: " + getTotalImports() + "\n" +
            "inFactionImports: " + inFactionImports + "\n" +
            "globalImports: " + globalImports + "\n" +
            "totalExports: " + getTotalExports() + "\n" +
            "inFactionExport: " + inFactionExports + "\n" +
            "globalExport: " + globalExports + "\n" +
            "canNotExport: " + getFlowCanNotExport() + "\n" +
            "demandMet: " + getFlowDeficitMet() + "\n" +
            "demandMetWithLocal: " + getFlowDeficitMetLocally() + "\n" +
            "demandMetViaTrade: " + getFlowDeficitMetViaTrade() + "\n" +
            "ratio: " + ratio
        );
    }

    public static enum PriceType {
        MARKET_BUYING,   // what the market will pay when buying from others
        MARKET_SELLING,  // what the market charges when selling to others
        NEUTRAL  // internal baseline
    }
}
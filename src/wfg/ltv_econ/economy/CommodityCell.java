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
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.econ.ShippingDisruption;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
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
public class CommodityCell {

    public static final String WORKER_MOD_ID = "worker_assigned";

    public final String comID;
    public final String marketID;

    public transient MarketAPI market;
    public transient CommoditySpecAPI spec;

    // Storage
    private double stored = 0;

    private MutableStat localProd = new MutableStat(0f);
    private MutableStat baseDemand = new MutableStat(0f);

    private transient Map<String, MutableStat> localProdMutables = new HashMap<>();
    private transient Map<String, MutableStat> demandBaseMutables = new HashMap<>();

    // Import
    public transient float inFactionImports = 0;
    public transient float globalImports = 0;
    public transient float importEffectiveness = 1f;
    private transient float importExclusiveDemand = 0;

    // Export
    public transient float inFactionExports = 0;
    public transient float globalExports = 0;

    public final Map<String, MutableStat> getFlowProdIndStats() {
        return localProdMutables;
    }
    public final Map<String, MutableStat> getFlowDemandIndStats() {
        return demandBaseMutables;
    }
    public final MutableStat getProdIndStat(String industryID) {
        final MutableStat mutable = localProdMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final MutableStat getDemandIndStat(String industryID) {
        final MutableStat mutable = demandBaseMutables.get(industryID);
        return mutable == null ? new MutableStat(0) : mutable;
    }
    public final float getProduction(boolean modified) {
        return modified ? localProd.getModifiedValue() : localProd.base;
    }
    public final float getBaseDemand(boolean modified) {
        return modified ? baseDemand.getModifiedValue() : baseDemand.base;
    }
    public final float getDemand() {
        return getBaseDemand(true)*getStoredAvailabilityRatio();
    }
    public final MutableStat getProductionStat() {
        return localProd;
    }
    public final MutableStat getDemandStat() {
        return baseDemand;
    }
    public final float getFlowDeficitMet() {
        return getFlowDeficitMetLocally() + getFlowDeficitMetViaTrade();
    }
    public final float getFlowDeficitMetLocally() {
        return Math.min(getProduction(true), getBaseDemand(true));
    }
    public final float getFlowDeficitPreTrade() {
        return getBaseDemand(true) - getFlowDeficitMetLocally();
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
        return Math.max(0, getTotalImports(false) - importExclusiveDemand - getFlowDeficitMetViaTrade());
    }
    public final float getFlowDeficit() {
        return getBaseDemand(true) - getFlowDeficitMet();
    }
    public final float getTotalImports(boolean modified) {
        if (modified) return (inFactionImports + globalImports) * importEffectiveness;
        return inFactionImports + globalImports;
    }
    public final float getTotalExports() {
        return inFactionExports + globalExports;
    }
    public final float getFlowAvailable() {
        return getProduction(true) + getTotalImports(true);
    }
    public final double getStoredAvailable() {
        return stored + getFlowAvailable();
    }
    public final float getPreferredStockpile() {
        return EconomyConfig.DAYS_TO_COVER * getBaseDemand(true);
    }
    public final float getFlowProductionSurplus() {
        return Math.max(0, getProduction(true) - getBaseDemand(true));
    }
    public final float getFlowRemainingExportable() {
        return getFlowProductionSurplus() - getTotalExports();
    }
    public final double getStoredRemainingExportable() {
        return Math.max(0.0, stored + getFlowRemainingExportable()
            - getProduction(true) * EconomyConfig.PRODUCTION_HOLD_FACTOR
            - getPreferredStockpile() * EconomyConfig.EXPORT_THRESHOLD_FACTOR
        );
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
        return getFlowAvailable() - getDemand() - getTotalExports();
    }
    public final float getFlowAvailabilityRatio() {
        return getBaseDemand(true) == 0 ? 1f : getFlowDeficitMet() / getBaseDemand(true);
    }
    public final float getStoredAvailabilityRatio() {
        return getBaseDemand(true) <= 0 ? 1f : (float) Math.min(stored / getBaseDemand(true), 1f);
    }
    public final float getDesiredAvailabilityRatio() {
        return getBaseDemand(true) <= 0 ? 1f : (float) Math.min(stored / getPreferredStockpile(), 1f);
    }
    public final double getStoredDeficit() {
        return Math.max(0, getPreferredStockpile() - stored);
    }
    public final double getStoredExcess() {
        return getStoredRemainingExportable();
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

    public CommodityCell(String comID, String marketID) {
        this.marketID = marketID;
        this.comID = comID;

        readResolve();
    }

    public Object readResolve() {
        market = Global.getSector().getEconomy().getMarket(marketID);
        spec = Global.getSettings().getCommoditySpec(comID);

        localProdMutables = new HashMap<>();
        demandBaseMutables = new HashMap<>();

        return this;
    }

    public final void update() {
        // RESET UPDATE SPECIFIC FLAGS
        importExclusiveDemand = 0;
        importEffectiveness = 1f;
        localProdMutables.clear();
        demandBaseMutables.clear();

        for (Industry industry : getVisibleIndustries()) {
            if (IndustryIOs.hasOutput(industry, comID)) {
                if (!IndustryIOs.getIndConfig(industry).ignoreLocalStockpiles) {
                    final MutableStat supplyStat = CompatLayer.convertIndSupplyStat(industry, comID);
                    localProdMutables.put(industry.getId(), supplyStat);
                }

            }
            if (IndustryIOs.hasInput(industry, comID)) {
                final MutableStat demandStat = CompatLayer.convertIndDemandStat(industry, comID);
                
                if (IndustryIOs.getIndConfig(industry).ignoreLocalStockpiles) {
                    importExclusiveDemand += demandStat.getModifiedValue();
                } else {
                    demandBaseMutables.put(industry.getId(), demandStat);
                }
            }
        }

        float localProdBase = 0;
        float baseDemandBase = 0;

        for (MutableStat stat : localProdMutables.values()) {
            localProdBase += stat.getModifiedValue();
        }

        for (MutableStat stat : demandBaseMutables.values()) {
            baseDemandBase += stat.getModifiedValue();
        }

        localProd.setBaseValue(localProdBase);
        baseDemand.setBaseValue(baseDemandBase);

        for (StatMod mod : market.getCommodityData(comID).getAvailableStat().getFlatMods().values()) {
            if (!mod.source.contains(ShippingDisruption.COMMODITY_LOSS_PREFIX)) continue;
            // TODO Integrate cargo raids better
            importEffectiveness = 1f + 0.4f*mod.value; // Can be -1 or -2.
            break;
        }
    }

    public final void reset() {
        inFactionImports = 0;
        globalImports = 0;

        inFactionExports = 0;
        globalExports = 0;

        importExclusiveDemand = 0;
        importEffectiveness = 1f;

        localProdMutables.clear();
        demandBaseMutables.clear();

        localProd.setBaseValue(0f);
        baseDemand.setBaseValue(0f);
    }

    public final void advance() {
        addStoredAmount(getFlowRealBalance());
    }

    public final float getUnitPrice(PriceType type, int amount) {
        return getUnitPrice(type, amount, stored, spec.getBasePrice(), getPreferredStockpile());
    }
    private static final float INHERENT_DEMAND = 4f;
    private static final float SHIFT_FRACTION = 0.001f;
    private static final float epsilon = 1e-3f;
    private static final double scarcityExpBuy = 0.85;
    private static final double scarcityExpNeutral = 1.0;
    private static final double scarcityExpSell = 1.15;
    public static final float getUnitPrice(PriceType type, int amount, double stored, float basePrice,
        float preferred
    ) {
        final boolean buying = (type == PriceType.MARKET_BUYING);

        final int n  = Math.abs(amount);
        final int sn = buying ? n : -n;
        final float d = Math.max(preferred, INHERENT_DEMAND);
        final double s = Math.max(stored, INHERENT_DEMAND);
        final float shift = SHIFT_FRACTION * d;
        final double exp = switch (type) {
            case MARKET_BUYING -> scarcityExpBuy;
            case MARKET_SELLING -> scarcityExpSell;
            case NEUTRAL -> scarcityExpNeutral;
        };

        final double s0 = s + shift;
        final double s1 = Math.max(s + sn + shift, epsilon);
        final double a = Math.min(s0, s1);
        final double b = Math.max(s0, s1);
        final double delta = b - a;
        final float sd = d + shift;

        final float avgMult;
        if (n <= 0) {
            avgMult = (float) Math.pow(sd / s0, exp);
        } else {
            final double I;
            if (exp == 1.0) {
                I = sd * Math.log(b / a);
            } else {
                final double prefactor = Math.pow(sd, exp) / (1.0 - exp);
                I = prefactor * (Math.pow(b, 1.0 - exp) - Math.pow(a, 1.0 - exp));
            }
            avgMult = (float) (I / delta);
        }

        final float priceMult = Math.max(0.1f, Math.min(4f, avgMult));
        return Math.max(1f, basePrice * priceMult);
    }

    public float computeVanillaPrice(int amount, boolean isSellingToMarket, boolean isPlayer) {
        if (amount < 1) return 0f;

        final Market mkt = (Market) market;

        final PriceType type = isSellingToMarket ? PriceType.MARKET_BUYING : PriceType.MARKET_SELLING;
        final float unitPrice = getUnitPrice(
            type, amount, stored + market.getCommodityData(comID).getTradeModPlus().getModifiedInt(),
            spec.getBasePrice(), getPreferredStockpile()
        );

        final StatBonus priceMod;
        if (isPlayer) {
            priceMod = isSellingToMarket ? mkt.getCommodityData(comID).getPlayerDemandPriceMod() :
                mkt.getCommodityData(comID).getPlayerSupplyPriceMod();
        } else {
            priceMod = isSellingToMarket ? mkt.getDemandPriceMod() : mkt.getSupplyPriceMod();
        }

        final float directionMult = isSellingToMarket ? 0.95f : 1.05f;

        final float totalPrice = priceMod.computeEffective(unitPrice) * amount * directionMult;

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

    public final void logAllInfo() {

        final float accountedFlow
            = getFlowDeficitMetLocally()
            + getFlowDeficitMetViaTrade()
            + getFlowDeficit()
            + getTotalExports()
            + getFlowCanNotExport()
            + getImportExclusiveDemand();

        final float footprint = getFlowEconomicFootprint();
        final float ratio = footprint <= 0 ? 1f : accountedFlow / footprint;

        final StringBuilder sb = new StringBuilder(512);

        sb.append("\n---- COMMODITY STATS LOG ----\n");
        sb.append("Commodity&market: ").append(comID + "__" + marketID).append("\n\n");

        // ===== CORE SCALE =====
        sb.append("[Scale]\n");
        sb.append(" economicFootprint: ").append(footprint).append("\n");
        sb.append(" baseDemand (modified): ").append(baseDemand.getModifiedValue()).append("\n");
        sb.append(" baseDemand (base): ").append(baseDemand.base).append("\n");
        sb.append(" preferredStockpile: ").append(getPreferredStockpile()).append("\n\n");

        // ===== PRODUCTION =====
        sb.append("[Production]\n");
        sb.append(" localProduction (modified): ").append(getProduction(true)).append("\n");
        sb.append(" localProduction (base): ").append(getProduction(false)).append("\n");
        sb.append(" productionSurplus: ").append(getFlowProductionSurplus()).append("\n\n");

        // ===== DEMAND & DEFICIT =====
        sb.append("[Demand]\n");
        sb.append(" demandMetTotal: ").append(getFlowDeficitMet()).append("\n");
        sb.append(" demandMetLocally: ").append(getFlowDeficitMetLocally()).append("\n");
        sb.append(" demandMetViaTrade: ").append(getFlowDeficitMetViaTrade()).append("\n");
        sb.append(" deficitPreTrade: ").append(getFlowDeficitPreTrade()).append("\n");
        sb.append(" finalDeficit: ").append(getFlowDeficit()).append("\n");
        sb.append(" availabilityRatio: ").append(getFlowAvailabilityRatio()).append("\n\n");

        // ===== IMPORTS =====
        sb.append("[Imports]\n");
        sb.append(" totalImports (raw): ").append(getTotalImports(false)).append("\n");
        sb.append(" totalImports (effective): ").append(getTotalImports(true)).append("\n");
        sb.append(" inFactionImports: ").append(inFactionImports).append("\n");
        sb.append(" globalImports: ").append(globalImports).append("\n");
        sb.append(" importEffectiveness: ").append(importEffectiveness).append("\n");
        sb.append(" importExclusiveDemand: ").append(importExclusiveDemand).append("\n");
        sb.append(" overImports: ").append(getFlowOverImports()).append("\n\n");

        // ===== EXPORTS =====
        sb.append("[Exports]\n");
        sb.append(" totalExports: ").append(getTotalExports()).append("\n");
        sb.append(" inFactionExports: ").append(inFactionExports).append("\n");
        sb.append(" globalExports: ").append(globalExports).append("\n");
        sb.append(" remainingExportable: ").append(getFlowRemainingExportable()).append("\n");
        sb.append(" canNotExport: ").append(getFlowCanNotExport()).append("\n\n");

        // ===== FLOW BALANCE =====
        sb.append("[Flow Balance]\n");
        sb.append(" flowAvailable: ").append(getFlowAvailable()).append("\n");
        sb.append(" realBalance: ").append(getFlowRealBalance()).append("\n\n");

        // ===== STORAGE =====
        sb.append("[Storage]\n");
        sb.append(" stored: ").append(stored).append("\n");
        sb.append(" storedRounded: ").append(getRoundedStored()).append("\n");
        sb.append(" storedAvailabilityRatio: ").append(getStoredAvailabilityRatio()).append("\n");
        sb.append(" storedAvailable (flow + storage): ").append(getStoredAvailable()).append("\n");
        sb.append(" storedRemainingExportable: ").append(getStoredRemainingExportable()).append("\n");
        sb.append(" storedEconomicFootprint: ").append(getStoredEconomicFootprint()).append("\n\n");

        // ===== CONSISTENCY =====
        sb.append("[Consistency]\n");
        sb.append(" accountedFlow: ").append(accountedFlow).append("\n");
        sb.append(" footprint: ").append(footprint).append("\n");
        sb.append(" ratio: ").append(ratio).append("\n");

        Global.getLogger(getClass()).info(sb.toString());
    }

    public static enum PriceType {
        MARKET_BUYING,   // what the market will pay when buying from others
        MARKET_SELLING,  // what the market charges when selling to others
        NEUTRAL  // internal baseline
    }
}
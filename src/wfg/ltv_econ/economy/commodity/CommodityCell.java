package wfg.ltv_econ.economy.commodity;

import static wfg.native_ui.util.Globals.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.campaign.econ.Market;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.util.ArrayMutableStat;
import wfg.native_ui.util.ArrayMap;

/**
 * <p><strong>Quantum</strong> denotes a daily flow quantity (units per day),
 * e.g., <code>targetQuantum</code> (desired daily inflow) or
 * <code>inflowQuantum</code> (actual daily inflow).</p>
 */
public class CommodityCell implements Serializable {
    private static final String DEMAND_ONLY_KEY = "dok";
    private static final String DEMAND_ONLY_DESC = "Demand without consumption";
    private static final int IND_ARRAY_AVG_SIZE = 6;

    public final String comID;
    public final String marketID;

    public transient MarketAPI market;
    public transient CommoditySpecAPI spec;

    private double stored = 0.0;

    private final ArrayMutableStat production = new ArrayMutableStat(0f);
    private final ArrayMutableStat consumption = new ArrayMutableStat(0f);
    private final ArrayMutableStat targetQuantum = new ArrayMutableStat(0f);
    private final StatBonus informalImportMods = new StatBonus();
    private transient ArrayMap<String, MutableStat> productionMutables = new ArrayMap<>(IND_ARRAY_AVG_SIZE);
    private transient ArrayMap<String, MutableStat> consumptionMutables = new ArrayMap<>(IND_ARRAY_AVG_SIZE);
    
    public transient float inFactionImports = 0f;
    public transient float inFactionExports = 0f;
    public transient float globalImports = 0f;
    public transient float globalExports = 0f;
    public transient float informalImports = 0f;
    public transient float informalExports = 0f;

    public final ArrayMap<String, MutableStat> getIndProductionStats() {
        return productionMutables;
    }
    public final ArrayMap<String, MutableStat> getIndConsumptionStats() {
        return consumptionMutables;
    }
    public final MutableStat getIndProdStat(String industryID) {
        final MutableStat mutable = productionMutables.get(industryID);
        return mutable == null ? new MutableStat(0f) : mutable;
    }
    public final MutableStat getIndDemandStat(String industryID) {
        final MutableStat mutable = consumptionMutables.get(industryID);
        return mutable == null ? new MutableStat(0f) : mutable;
    }
    public final float getProduction(boolean modified) {
        return modified ? production.getModifiedValue() : production.getBaseValue();
    }
    public final float getConsumption(boolean modified) {
        return modified ? consumption.getModifiedValue() : consumption.getBaseValue();
    }
    public final float getTargetQuantum(boolean modified) {
        return modified ? targetQuantum.getModifiedValue() : targetQuantum.getBaseValue();
    }
    public final ArrayMutableStat getProductionStat() {
        return production;
    }
    public final ArrayMutableStat getConsumptionStat() {
        return consumption;
    }
    public final ArrayMutableStat getTargetQuantumStat() {
        return targetQuantum;
    }
    public final StatBonus getInformalImportMods() {
        return informalImportMods;
    }
    public final float getTotalImports() {
        return inFactionImports + globalImports + informalImports;
    }
    public final float getTotalExports() {
        return inFactionExports + globalExports + informalExports;
    }
    public final float getTargetQuantumMetLocally() {
        return Math.min(getProduction(true), getTargetQuantum(true));
    }
    public final float getTargetQuantumPreTrade() {
        return getTargetQuantum(true) - getTargetQuantumMetLocally();
    }
    public final float getTargetQuantumMetViaFactionTrade() {
        return Math.min(inFactionImports, getTargetQuantumPreTrade());
    }
    public final float getTargetQuantumMetViaGlobalTrade() {
        return Math.min(globalImports, getTargetQuantumPreTrade() - getTargetQuantumMetViaFactionTrade());
    }
    public final float getTargetQuantumMetViaInformalTrade() {
        return Math.min(informalImports, getTargetQuantumPreTrade()
            - getTargetQuantumMetViaFactionTrade() - getTargetQuantumMetViaGlobalTrade()
        );
    }
    public final float getTargetQuantumMetViaTrade() {
        return getTargetQuantumMetViaFactionTrade() + getTargetQuantumMetViaGlobalTrade() +
            getTargetQuantumMetViaInformalTrade();
    }
    public final float getTargetQuantumMet() {
        return getTargetQuantumMetLocally() + getTargetQuantumMetViaTrade();
    }
    public final float getTargetQuantumUnmet() {
        return getTargetQuantum(true) - getTargetQuantumMet();
    }
    public final float getInflowQuantum() {
        return getProduction(true) + getTotalImports();
    }
    public final float getTargetStockpiles() {
        return EconConfig.DAYS_TO_COVER * getTargetQuantum(true);
    }
    public final float getSurplusAfterTargetQuantum() {
        return Math.max(0f, getProduction(true) - getTargetQuantum(true));
    }
    public final float getRemainingExportableAfterTargetQuantum() {
        return Math.max(0f, getSurplusAfterTargetQuantum() - getTotalExports());
    }
    public final float getOverImports() {
        return Math.max(0f, getTotalImports() - getTargetQuantumMetViaTrade());
    }
    public final double computeExportAmount() {
        return Math.max(0.0, stored + getRemainingExportableAfterTargetQuantum()
            - getProduction(true) * EconConfig.PRODUCTION_HOLD_FACTOR
            - getTargetStockpiles() * EconConfig.EXPORT_THRESHOLD_FACTOR
        );
    }
    public final float computeImportAmount() {
        final float cap = EconConfig.DAYS_TO_COVER_PER_IMPORT * getTargetQuantum(true);
        final float target = Math.max((float) Math.min(getTargetStockpiles() - stored, cap), 0f);

        return Math.max(target - getTotalImports(), 0f);
    }
    public final float getFlowEconomicFootprint() {
        return getTargetQuantumMet() + getTargetQuantumUnmet() + getOverImports()
            + getTotalExports() + getRemainingExportableAfterTargetQuantum();
    }
    public final double getStoredEconomicFootprint() {
        return Math.max(stored, getTargetStockpiles());
    }
    public final float getQuantumRealBalance() {
        return getInflowQuantum() - getConsumption(true) - getTotalExports();
    }
    public final float getStoredAvailabilityRatio() {
        final float demand = getConsumption(true);
        return demand <= 0f ? 1f : (float) Math.min(stored / demand, 1f);
    }
    public final float getDesiredAvailabilityRatio() {
        final double target = getTargetStockpiles();
        return target <= 0f ? 1f : (float) Math.min(stored / target, 1f);
    }
    public final double getStoredDeficit() {
        return Math.max(0.0, getTargetStockpiles() - stored);
    }
    public final double getStoredExcess() {
        return computeExportAmount();
    }
    public final double getStored() {
        return stored;
    }
    public final long getRoundedStored() {
        return (long) stored;
    }

    public final void addStoredAmount(double a) {
        stored = Math.max(0.0, stored + a);
    }

    public CommodityCell(String comID, String marketID) {
        this.marketID = marketID;
        this.comID = comID;

        readResolve();
    }

    public Object readResolve() {
        market = Global.getSector().getEconomy().getMarket(marketID);
        spec = settings.getCommoditySpec(comID);

        productionMutables = new ArrayMap<>(IND_ARRAY_AVG_SIZE);
        consumptionMutables = new ArrayMap<>(IND_ARRAY_AVG_SIZE);

        return this;
    }

    public final void update() {
        // RESET UPDATE SPECIFIC FLAGS
        productionMutables.clear();
        consumptionMutables.clear();
        production.unmodifyBase();
        consumption.unmodifyBase();
        targetQuantum.unmodifyBase();

        for (Industry ind : getVisibleIndustries()) {
            final String indID = ind.getId();
            // Register IOs
            if (ind.getSupply(comID).getQuantity().getModifiedValue() > 0.01f &&
                !IndustryIOs.hasOutput(ind, comID)
            ) {
                IndustryIOs.createAndRegisterDynamicOutput(ind, comID, true);
            }

            if (ind.getDemand(comID).getQuantity().getModifiedValue() > 0.01f &&
                !IndustryIOs.hasInput(ind, comID)
            ) {
                IndustryIOs.createAndRegisterDynamicInput(ind, comID, true);
            }

            // Retrieve IOs
            if (IndustryIOs.hasOutput(ind, comID)) {
                if (!IndustryConfigManager.getIndConfig(ind).demandOnly) {
                    final MutableStat supplyStat = CompatLayer.convertIndSupplyStat(ind, comID);
                    productionMutables.put(indID, supplyStat);
                    production.modifyBase(indID, supplyStat.getModifiedValue());
                }

            }
            if (IndustryIOs.hasInput(ind, comID)) {
                final MutableStat demandStat = CompatLayer.convertIndDemandStat(ind, comID);
                
                if (IndustryConfigManager.getIndConfig(ind).demandOnly) {
                    targetQuantum.modifyBase(DEMAND_ONLY_KEY + "_" + indID, demandStat.getModifiedValue(),
                        DEMAND_ONLY_DESC + " - " + ind.getCurrentName()
                    );
                } else {
                    consumptionMutables.put(indID, demandStat);
                    consumption.modifyBase(indID, demandStat.getModifiedValue(), ind.getCurrentName());
                }
            }
        }

        targetQuantum.applyMods(consumption);
    }

    public final void reset() {
        inFactionImports = 0f;
        globalImports = 0f;
        informalImports = 0f;

        inFactionExports = 0f;
        globalExports = 0f;
        informalExports = 0f;

        productionMutables.clear();
        consumptionMutables.clear();

        production.unmodifyBase();
        consumption.unmodifyBase();
        targetQuantum.unmodifyBase();
    }

    public final void advance() {
        addStoredAmount(getQuantumRealBalance());
    }

    public final float getUnitPrice(PriceType type, long amount) {
        return getUnitPrice(type, amount, stored, spec.getBasePrice(), getTargetStockpiles());
    }
    
    private static final float INHERENT_DEMAND = 4f;
    private static final float SHIFT_FRACTION = 0.002f;
    private static final float epsilon = 1e-3f;
    private static final double scarcityExpBuy = 0.85;
    private static final double scarcityExpNeutral = 1.0;
    private static final double scarcityExpSell = 1.15;
    public static final float getUnitPrice(PriceType type, long amount, double stored, float basePrice,
        float preferred
    ) {
        final boolean buying = (type == PriceType.MARKET_BUYING);

        final long n = Math.abs(amount);
        final long sn = buying ? n : -n;
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

    public final float computeVanillaPrice(long amount, boolean isSellingToMarket, boolean isPlayer) {
        if (amount < 1 || market == null) return 0f;

        final Market mkt = (Market) market;

        if (spec.isExotic()) {
            return mkt.getDemandPriceAssumingStockpileUtility(
                mkt.getCommodityData(comID), 0.0, amount, isPlayer
            );
        }

        final PriceType type = isSellingToMarket ? PriceType.MARKET_BUYING : PriceType.MARKET_SELLING;
        final float unitPrice = getUnitPrice(
            type, amount, stored + market.getCommodityData(comID).getTradeModPlus().getModifiedInt(),
            spec.getBasePrice(), getTargetStockpiles()
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

    public final List<Industry> getVisibleIndustries() {
        if (market == null) return Collections.emptyList();
        final List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public final void logAllInfo() {
        final StringBuilder sb = new StringBuilder(512);
        sb.append("\n---- COMMODITY STATS LOG ----\n");
        sb.append("Commodity&market: ").append(comID).append("__").append(marketID).append("\n\n");

        sb.append("[Daily Flows]\n");
        sb.append(" production (base): ").append(getProduction(false)).append("\n");
        sb.append(" consumption (base): ").append(getConsumption(false)).append("\n");
        sb.append(" production (modified): ").append(getProduction(true)).append("\n");
        sb.append(" consumption (modified): ").append(getConsumption(true)).append("\n");
        sb.append(" targetQuantum (desired inflow base): ").append(getTargetQuantum(false)).append("\n");
        sb.append(" targetQuantum (desired inflow modified): ").append(getTargetQuantum(true)).append("\n");
        sb.append(" totalImports: ").append(getTotalImports()).append("\n");
        sb.append(" totalExports: ").append(getTotalExports()).append("\n");
        sb.append(" inflowQuantum (prod + imports): ").append(getInflowQuantum()).append("\n");
        sb.append(" realBalance (inflow - consumption - exports): ").append(getQuantumRealBalance()).append("\n\n");

        // Stockpile
        sb.append("[Stockpile]\n");
        sb.append(" stored: ").append(stored).append("\n");
        sb.append(" targetStockpiles (desired stock): ").append(getTargetStockpiles()).append("\n");
        sb.append(" storedDeficit (gap to target): ").append(getStoredDeficit()).append("\n");
        sb.append(" storedExcess (exportable surplus): ").append(computeExportAmount()).append("\n");
        sb.append(" storedAvailabilityRatio (days of consumption): ").append(getStoredAvailabilityRatio()).append("\n");
        sb.append(" desiredAvailabilityRatio (fill level): ").append(getDesiredAvailabilityRatio()).append("\n\n");

        // Import/Export breakdown (daily)
        sb.append("[Trade Breakdown]\n");
        sb.append(" inFactionImports: ").append(inFactionImports).append("\n");
        sb.append(" globalImports: ").append(globalImports).append("\n");
        sb.append(" informalImports: ").append(informalImports).append("\n");
        sb.append(" inFactionExports: ").append(inFactionExports).append("\n");
        sb.append(" globalExports: ").append(globalExports).append("\n");
        sb.append(" informalExports: ").append(informalExports).append("\n\n");

        // Derived metrics
        sb.append("[Derived]\n");
        sb.append(" surplusAfterTargetQuantum (prod - target): ").append(getSurplusAfterTargetQuantum()).append("\n");
        sb.append(" remainingExportableAfterTargetQuantum: ").append(getRemainingExportableAfterTargetQuantum()).append("\n");

        Global.getLogger(getClass()).info(sb.toString());
    }

    public static enum PriceType {
        MARKET_BUYING,   // what the market will pay when buying from others
        MARKET_SELLING,  // what the market charges when selling to others
        NEUTRAL  // internal baseline
    }
}
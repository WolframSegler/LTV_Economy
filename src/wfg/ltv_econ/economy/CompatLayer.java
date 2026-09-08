package wfg.ltv_econ.economy;

import java.util.Iterator;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;

/**
 * <p>
 * The <strong>Compatibility Layer</strong> between vanilla/modded industries and
 * the LTV-economy system. In the LTV system, all production and consumption modifiers are
 * treated multiplicatively, whereas vanilla Starsector and many mods use additive
 * or percentage-based bonuses.
 * </p>
 *
 * <p>
 * This class translates vanilla-style flat and percent modifiers into the multiplicative format
 * expected by the LTV economy engine. By doing so, it ensures that:
 * </p>
 * <ul>
 *   <li>Vanilla industries do not need to be subclassed or modified.</li>
 *   <li>Community or modded industries automatically integrate with the LTV scaling system.</li>
 *   <li>All calculations in the economy engine remain consistent and scale correctly.</li>
 * </ul>
 * </p>
 */
public final class CompatLayer {

    public static final String BASE_MOD_SUFFIX = "_0";
    public static final String MARKET_COND_MOD_SUFFIX = "_1";

    public static final String DEMAND_RED_MOD = "ind_dr";
    public static final String SUPPLY_BONUS_MOD = "ind_sb";

    public static final MutableStat convertIndDemandStat(Industry ind, String inputID) {
        return convertIndDemandStat(ind, inputID, WorkerRegistry.get(ind), false);
    }

    public static final MutableStat convertIndSupplyStat(Industry ind, String outputID) {
        return convertIndSupplyStat(ind, outputID, WorkerRegistry.get(ind), false);
    }

    public static final MutableStat convertIndDemandStat(Industry ind, String inputID, WorkerIndustryData data, boolean validOnly) {
        final MutableStat src = ind.getDemand(inputID).getQuantity();
        final MutableStat dest = new MutableStat(0f);

        copyMods(data, ind, src, dest, inputID, true, validOnly);
        return dest;
    }

    public static final MutableStat convertIndSupplyStat(Industry ind, String outputID, WorkerIndustryData data, boolean validOnly) {
        if (IndustryConfigManager.getIndConfig(ind).demandOnly) return new MutableStat(0f);
        
        final MutableStat src = ind.getSupply(outputID).getQuantity();
        final MutableStat dest = new MutableStat(0f);

        copyMods(data, ind, src, dest, outputID, false, validOnly);
        return dest;
    }

    private static final void copyMods(WorkerIndustryData data, Industry ind, MutableStat base, MutableStat dest,
        String comID, boolean isDemand, boolean validOnly
    ) {
        final float baseVal = getBaseValue(ind, data, comID, isDemand, validOnly);
        dest.setBaseValue(baseVal);
        if (baseVal == 0f) return;

        final MutableStat bonus = isDemand ? getDemandReductionMutable(ind, data, comID, baseVal, validOnly) : ind.getSupplyBonus();
        dest.applyMods(getModifiers(ind, comID, base, bonus));
    }

    /**
     * Retrieve the base value (worker-dependent) of an industry for a given commodity.
     */
    public static final float getBaseValue(Industry ind, WorkerIndustryData data, String comID, boolean isDemand, boolean validOnly) {
        final float value = isDemand ? IndustryIOs.getRealSumInput(data, ind, comID, validOnly) :
            IndustryIOs.getRealOutput(ind, data, comID, validOnly);
        final boolean hasRelevantCondition = isDemand || hasRelevantCondition(comID, ind.getMarket());
        return hasRelevantCondition ? value : 0f;
    }
    
    public static final StatBonus getModifiers(
        Industry ind, String comID, MutableStat base, MutableStat bonus
    ) {
        final StatBonus statBonus = new StatBonus();

        final String installedItemID = ind.getSpecialItem() != null ? ind.getSpecialItem().getId() : null;

        if (installedItemID != null) {
            for (StatMod mod : base.getFlatMods().values()) {
                if (mod.source.contains(installedItemID)) {
                    final float converted = industryModConverter((int) mod.value);
                    statBonus.modifyMult(mod.source + EconomyLoop.KEY + ind.getId(), converted, mod.desc);
                }
            }
        }

        applyResourceDepositMods(ind, statBonus, comID);

        if (bonus == null) return statBonus;
        int bonusID = 0;

        for (StatMod mod : bonus.getPercentMods().values()) {
            statBonus.modifyPercent(bonusID++ + EconomyLoop.KEY + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getMultMods().values()) {
            statBonus.modifyMult(bonusID++ + EconomyLoop.KEY + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getFlatMods().values()) {
            float converted = industryModConverter((int) mod.value);

            statBonus.modifyMult(bonusID++ + EconomyLoop.KEY + ind.getId(), converted, mod.desc);
        }

        return statBonus;
    }

    public static final float getModifiersMult(
        Industry ind, String comID, boolean isDemand
    ) {
        final MutableStat bonus = isDemand ?
            getDemandReductionMutable(ind, comID) : ind.getSupplyBonus();
        final MutableStat base = isDemand ?
            ind.getDemand(comID).getQuantity() : ind.getSupply(comID).getQuantity();

        return getModifiers(ind, comID, base, bonus).computeEffective(1f);
    }

    public static final boolean hasRelevantCondition(String comID, MarketAPI market) {
        boolean hasRelevantCondition = true;
        if (ResourceDepositsCondition.COMMODITY.containsValue(comID)) {
            hasRelevantCondition = false;
            for (MarketConditionAPI cond : market.getConditions()) {
                String condComID = ResourceDepositsCondition.COMMODITY.get(cond.getId());
                if (comID.equals(condComID)) {
                    return true;
                }
            }
        }
        return hasRelevantCondition;
    }

    /**
     * The official mapping from cargo units to economy units.
     */
    public static final int cargoUnitToEconUnit(double amount) {
        if (amount <= 1d) return 0;

        return Math.max(0, (int) Math.floor(Math.log10(amount)));
    }

    /*
     * Converts vanilla flat mods to LTV mult mods.
     */
    private static final float industryModConverter(int flatValue) {
        switch (flatValue) {
        case 0:
            return 1f;
        case 1:
            return 1.3f;
        case 2:
            return 1.8f;
        case 3: 
            return 2.4f;
        case 4:
            return 3f;
        case 5:
            return 4f;
        case 6:
            return 5f;
        case -1:
            return 0.75f;
        case -2:
            return 0.5f;
        case -3:
            return 0.3f;
        case -4:
            return 0.2f;
        case -5:
            return 0.1f;
        case -6:
            return 0.05f;
        default:
            if (flatValue < 0) {
                return (float) Math.pow(0.75, flatValue);
            } else if (flatValue > 0) {
                return (float) Math.pow(1.3, flatValue);
            } else {
                return 1;
            }
        }
    }

    private static final float marketConditionModConverter(int flatValue) {
        switch (flatValue) {
        case 0:
            return 1f;
        case 1:
            return 1.5f;
        case 2:
            return 2f;
        case 3: 
            return 3f;
        case 4:
            return 5f;
        case 5:
            return 8f;
        case 6:
            return 12f;
        case -1:
            return 0.5f;
        case -2:
            return 0.25f;
        case -3:
            return 0.1f;
        case -4:
            return 0.05f;
        case -5:
            return 0.01f;
        case -6:
            return 0.001f;
        default:
            return flatValue;
        }
    }

    private static final void applyResourceDepositMods(Industry ind, StatBonus dest, String comID) {
        for (MarketConditionAPI cond : ind.getMarket().getConditions()) {
            final String commodityId = ResourceDepositsCondition.COMMODITY.get(cond.getId());
            if (commodityId == null || !commodityId.equals(comID)) continue;

            final String industryId = ResourceDepositsCondition.INDUSTRY.get(commodityId);
            if (industryId == null || !industryId.equals(ind.getId())) continue;

            final Integer mod = ResourceDepositsCondition.MODIFIER.get(cond.getId());
            if (mod == null) continue;

            final float converted = marketConditionModConverter(mod);
            dest.modifyMult(cond.getId() + EconomyLoop.KEY + ind.getId(), converted, cond.getName());
        }
    }

    private static final MutableStat getDemandReductionMutable(Industry ind, String inputID) {
        return getDemandReductionMutable(ind, WorkerRegistry.get(ind), inputID,
            IndustryIOs.getRealSumInput(ind, inputID), false);
    }

    private static final MutableStat getDemandReductionMutable(Industry ind, WorkerIndustryData data, String inputID, float sumInput, boolean validOnly) {
        final MutableStat modifier = ind.getDemandReduction().createCopy();

        /*
        * Bonuses inside demandReduction are positive, even though they reduce demand.
        * Their sign must be flipped for compatibility
        */ 
        for (StatMod mod : modifier.getFlatMods().values()) {
            if (mod.value > 0) mod.value = -mod.value;
        }

        final float nonAbstractRatio = computeNonAbstractInputRatio(ind, data, inputID, sumInput, validOnly);

        if (ind.getSupplyBonus() != null && nonAbstractRatio > 0f) {
            MutableStat scaledSupplyBonus = scaleSupplyBonusToRatio(ind.getSupplyBonus(), nonAbstractRatio);
            modifier.applyMods(scaledSupplyBonus);
        }

        return modifier;
    }

    /**
     * Computes the fraction of total input demand that comes from non-abstract outputs.
     * Supply bonuses only apply to that portion.
     */
    private static final float computeNonAbstractInputRatio(Industry ind, WorkerIndustryData data, String inputID, float totalInput, boolean validOnly) {
        if (totalInput <= 0f) return 0f;

        float nonAbstractInput = 0f;

        for (String outputID : IndustryIOs.getRealOutputs(ind, data, false, validOnly).keySet()) {
            final Map<String, Float> inputs = IndustryIOs.getRealInputs(ind, data, outputID, false, validOnly);
            nonAbstractInput += inputs.getOrDefault(inputID, 0f);
        }

        return nonAbstractInput / totalInput;
    }

    /**
     * Scales the supply bonus so that its effect is proportional to the ratio
     * of non-abstract input demand. Flat bonuses are converted to multiplicative ones.
     */
    private static final MutableStat scaleSupplyBonusToRatio(MutableStat supplyBonus, float ratio) {
        final MutableStat scaled = supplyBonus.createCopy();

        for (final Iterator<StatMod> it = scaled.getFlatMods().values().iterator(); it.hasNext();) {
            final StatMod mod = it.next();
            scaled.modifyMult(mod.source, industryModConverter((int) mod.value), mod.desc);
            it.remove();
        }

        if (ratio < 1f) {
            for (StatMod mod : scaled.getMultMods().values()) {
                mod.value = 1f + (mod.value - 1f) * ratio;
            }
            for (StatMod mod : scaled.getPercentMods().values()) {
                mod.value *= ratio;
            }
        }

        return scaled;
    }
}
package wfg.ltv_econ.economy;

import java.util.Iterator;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;

import wfg.ltv_econ.configs.IndustryConfigManager.OutputCom;
import wfg.ltv_econ.industry.IndustryIOs;

/**
 * <p>
 * <strong>CompatibilityLayer</strong> exists to provide a compatibility layer between vanilla/modded
 * industries and the <em>LTV</em> economy system. In the LTV system, all production and consumption
 * modifiers are treated multiplicatively, whereas vanilla Starsector and many mods use additive
 * (flat) or percentage-based bonuses.
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
        final MutableStat src = ind.getDemand(inputID).getQuantity();
        final MutableStat dest = new MutableStat(0f);
        final MutableStat modifier = getDemandReductionMutable(ind, inputID);

        copyMods(ind, src, modifier, dest, inputID, true);
        return dest;
    }

    public static final MutableStat convertIndSupplyStat(Industry ind, String outputID) {
        final MutableStat src = ind.getSupply(outputID).getQuantity();
        final MutableStat supplyBonus = ind.getSupplyBonus();
        final MutableStat dest = new MutableStat(0f);

        copyMods(ind, src, supplyBonus, dest, outputID, false);
        return dest;
    }

    private static final void copyMods(Industry ind, MutableStat base, MutableStat bonus, MutableStat dest,
        String comID, boolean isDemand) {

        float value = getBaseValue(ind, comID, isDemand);
        dest.setBaseValue(value);
        if (value == 0) return;

        dest.applyMods(getModifiers(ind, comID, base, bonus));
    }

    /**
     * Retrieve the base value (worker-dependent) of an industry for a given commodity.
     */
    public static final float getBaseValue(Industry ind, String comID, boolean isDemand) {
        float value = isDemand ? IndustryIOs.getRealSumInput(ind, comID) : IndustryIOs.getRealOutput(ind, comID);
        boolean hasRelevantCondition = isDemand || hasRelevantCondition(comID, ind.getMarket());
        return hasRelevantCondition ? value : 0f;
    }

    
    public static final MutableStat getModifiers(
        Industry ind, String comID, MutableStat base, MutableStat bonus
    ) {
        MutableStat modifierStat = new MutableStat(1f);

        String installedItemID = ind.getSpecialItem() != null ? ind.getSpecialItem().getId() : null;

        if (installedItemID != null) {
            for (StatMod mod : base.getFlatMods().values()) {
                if (mod.source.contains(installedItemID)) {
                    float converted = industryModConverter((int) mod.value);
                    modifierStat.modifyMult(mod.source + EconomyEngine.KEY + ind.getId(), converted, mod.desc);
                }
            }
        }

        applyResourceDepositMods(ind, modifierStat, comID);

        if (bonus == null) return modifierStat;
        int bonusID = 0;

        for (StatMod mod : bonus.getPercentMods().values()) {
            modifierStat.modifyPercent(bonusID++ + EconomyEngine.KEY + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getMultMods().values()) {
            modifierStat.modifyMult(bonusID++ + EconomyEngine.KEY + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getFlatMods().values()) {
            float converted = industryModConverter((int) mod.value);

            modifierStat.modifyMult(bonusID++ + EconomyEngine.KEY + ind.getId(), converted, mod.desc);
        }

        return modifierStat;
    }

    public static final float getModifiersMult(
        Industry ind, String comID, boolean isDemand
    ) {
        final MutableStat bonus = isDemand ?
            getDemandReductionMutable(ind, comID) : ind.getSupplyBonus();
        final MutableStat base = isDemand ?
            ind.getDemand(comID).getQuantity() : ind.getSupply(comID).getQuantity();

        return getModifiers(ind, comID, base, bonus).getModifiedValue();
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

    public static final void applyResourceDepositMods(Industry ind, MutableStat dest, String comID) {
        for (MarketConditionAPI cond : ind.getMarket().getConditions()) {
            String commodityId = ResourceDepositsCondition.COMMODITY.get(cond.getId());
            if (commodityId == null || !commodityId.equals(comID)) continue;

            String industryId = ResourceDepositsCondition.INDUSTRY.get(commodityId);
            if (industryId == null || !industryId.equals(ind.getId())) continue;

            Integer mod = ResourceDepositsCondition.MODIFIER.get(cond.getId());
            if (mod == null) continue;

            float converted = marketConditionModConverter(mod);
            dest.modifyMult(cond.getId() + EconomyEngine.KEY + ind.getId(), converted, cond.getName());
        }
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

    private static final MutableStat getDemandReductionMutable(Industry ind, String inputID) {
        final MutableStat modifier = ind.getDemandReduction().createCopy();

        /*
        * Bonuses inside demandReduction are positive, even though they reduce demand.
        * Their sign must be flipped for compatibility
        */ 
        for (StatMod mod : modifier.getFlatMods().values()) {
            if (mod.value > 0) mod.value = -mod.value;
        }

        float totalInput = IndustryIOs.getRealSumInput(ind, inputID);
        float nonAbstractInput = 0f;
        float ratio = 0f;

        final Map<String, OutputCom> outputs = IndustryIOs.getIndConfig(ind).outputs;

        for (String outputID : IndustryIOs.getRealOutputs(ind, true).keySet()) {
            OutputCom output = outputs.get(outputID);

            Map<String, Float> inputs = IndustryIOs.getRealInputs(ind, outputID, false);

            if (inputs.containsKey(inputID) && !output.isAbstract) {
                nonAbstractInput += inputs.get(inputID);
            }
        }
        ratio = totalInput > 0 ? nonAbstractInput / totalInput : 0f;


        if (ind.getSupplyBonus() != null && ratio > 0) {
            final MutableStat scaledBonus = ind.getSupplyBonus().createCopy();

            for (Iterator<StatMod> it = scaledBonus.getFlatMods().values().iterator(); it.hasNext();) {
                StatMod mod = it.next();
                scaledBonus.modifyMult(
                    mod.source, industryModConverter((int) mod.value), mod.desc
                );
                it.remove();
            }

            for (StatMod mod : scaledBonus.getMultMods().values()) {
                if (ratio < 1) {
                    mod.value = 1f + (mod.value - 1f) * ratio;
                }
            }

            for (StatMod mod : scaledBonus.getPercentMods().values()) {
                if (ratio < 1) {
                    mod.value *= ratio;
                }
            }

            modifier.applyMods(scaledBonus);
        }

        return modifier;
    }
}

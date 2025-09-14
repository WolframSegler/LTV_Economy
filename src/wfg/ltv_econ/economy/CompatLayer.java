package wfg.ltv_econ.economy;

import java.util.Iterator;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;

import wfg.ltv_econ.economy.IndustryConfigLoader.OutputCom;
import wfg.ltv_econ.economy.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
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
 *
 * <p>
 * <strong>Usage:</strong> When the economy engine encounters a vanilla or third-party industry
 * modifier (flat or percent), it passes it through this translator to obtain an equivalent
 * multiplicative value in LTV space. This value can then be safely applied to production,
 * consumption, or any other LTV-scaled metric.
 * </p>
 */
public final class CompatLayer {

    public static final String BASE_MOD_SUFFIX = "_0";
    public static final String MARKET_COND_MOD_SUFFIX = "_1";

    public static final MutableStat convertIndDemandStat(Industry ind, String inputID) {
        final MutableStat src = ind.getDemand(inputID).getQuantity();
        final MutableStat dest = new MutableStat(0f);

        final MutableStat modifier = ind.getDemandReduction().createCopy();

        float totalInput = IndustryIOs.getSumInput(ind, inputID);
        float nonAbstractInput = 0f;
        float ratio = 0f;

        if (IndustryIOs.hasConfig(ind)) {
            final Map<String, OutputCom> outputs = IndustryIOs.getIndConfig(ind).outputs;

            for (String outputID : IndustryIOs.getOutputs(ind, true).keySet()) {
                OutputCom output = outputs.get(outputID);
    
                Map<String, Float> inputs = IndustryIOs.getInputs(ind, outputID);
    
                if (inputs.containsKey(inputID) && !output.isAbstract) {
                    nonAbstractInput += inputs.get(inputID);
                }
            }
            ratio = totalInput > 0 ? nonAbstractInput / totalInput : 0f;

        } else {
            ratio = EconomyEngine.isWorkerAssignable(ind) ? 1 : 0;
        }

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

        copyMods(ind, src, modifier, dest, "ind_dr", inputID, true);
        return dest;
    }

    public static final MutableStat convertIndSupplyStat(Industry ind, String outputID) {
        final MutableStat src = ind.getSupply(outputID).getQuantity();
        final MutableStat supplyBonus = ind.getSupplyBonus();
        final MutableStat dest = new MutableStat(0f);

        copyMods(ind, src, supplyBonus, dest, "ind_sb", outputID, false);
        return dest;
    }

    private static final void copyMods(Industry ind, MutableStat base, MutableStat bonus, MutableStat dest,
        String modID, String comID, boolean isDemand) {

        boolean useConfig = IndustryIOs.hasConfig(ind);

        float value = 0;
        if (useConfig) {
            if (isDemand) {
                value = IndustryIOs.getSumInput(ind, comID);
            } else {
                value = IndustryIOs.getOutput(ind, comID);
            }

            if (value == 0) return;

        } else {
            StatMod baseMod = null;
            float cumulativeBase = 0f;
    
            for (StatMod mod : base.getFlatMods().values()) {
                if (mod.source.endsWith(BASE_MOD_SUFFIX) && mod.value > 0) {
                    baseMod = baseMod == null ? mod : baseMod;
    
                } else {
                    if (!mod.source.equals(modID) && 
                        !mod.source.endsWith(MARKET_COND_MOD_SUFFIX) &&
                        mod.value >= 0
                    ) {
                        cumulativeBase += mod.value;
                    }
                }
            }

            value = (baseMod != null ? baseMod.value : cumulativeBase);
        }

        String installedItemID = ind.getSpecialItem() != null ? ind.getSpecialItem().getId() : null;

        if (installedItemID != null) {
            for (StatMod mod : base.getFlatMods().values()) {
                if (mod.source.contains(installedItemID)) {
                    float converted = industryModConverter((int) mod.value);
                    dest.modifyMult(mod.source + "::" + ind.getId(), converted, mod.desc);
                }
            }
        }

        boolean hasRelevantCondition = true;
        if (useConfig && ResourceDepositsCondition.COMMODITY.containsValue(comID) && !isDemand) {
            hasRelevantCondition = false;
            for (MarketConditionAPI cond : ind.getMarket().getConditions()) {
                String condComID = ResourceDepositsCondition.COMMODITY.get(cond.getId());
                if (comID.equals(condComID)) {
                    hasRelevantCondition = true;
                    break;
                }
            }
        }

        if (hasRelevantCondition) {
            dest.setBaseValue(industryBaseConverter(value, ind, comID, useConfig));
        } else {
            dest.setBaseValue(0);
            return;
        }

        applyResourceDepositMods(ind, dest, comID);

        if (bonus == null) return;

        for (StatMod mod : bonus.getPercentMods().values()) {
            dest.modifyPercent(mod.source + "::" + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getMultMods().values()) {
            dest.modifyMult(mod.source + "::" + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : bonus.getFlatMods().values()) {
            float converted = industryModConverter((int) mod.value);
            dest.modifyMult(mod.source + "::" + ind.getId(), converted, mod.desc);
        }
    }

    /*
     * Converts vanilla base value to LTV base value with correct scaling.
     */
    private static final float industryBaseConverter(
        float baseValue, Industry ind, String comID, boolean useConfig
    ) {

        if (useConfig) return baseValue;

        final WorkerRegistry reg = WorkerRegistry.getInstance(); 
        final WorkerIndustryData data = reg.getData(ind.getMarket().getId(), ind.getId());
        final int size = ind.getMarket().getSize();

        if (EconomyEngine.isWorkerAssignable(ind)) {
            final float workersPerUnit = EconomyEngine.getWorkersPerUnit(comID, OCCTag.AVERAGE);

            final int workerDrivenCount = ind.getAllSupply().size();
            if (data != null) baseValue *= (data.getWorkersAssigned() / (workerDrivenCount*workersPerUnit));

            return baseValue;
        }

        float size3Base = baseValue - (size - 3);

        if (data != null) {
            return size3Base;
        }
        if (size3Base <= 0f) return 1f; // For industries that do not scale well with market size

        // Throttle market size scaling to 8.5 instead of 10 to prevent modded buildings
        // with unusually high base production from breaking the economy.
        // Vanilla Starsector uses 10^x to represent production units (e.g., x → ~10^(x-1) ),
        // but using 10 for all modded buildings causes exponential blow-ups in LTV values.
        return (float) Math.pow(8.5, size - 3) * size3Base;
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
            dest.modifyMult(cond.getId() + "::" + ind.getId(), converted, cond.getName());
        }
    }
}

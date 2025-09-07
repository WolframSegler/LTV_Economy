package wfg.ltv_econ.economy;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;

import wfg.ltv_econ.economy.IndustryConfigLoader.IndustryConfig;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;

/**
 * <p>
 * <strong>LtvCompatibilityLayer</strong> exists to provide a compatibility layer between vanilla/modded
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
public final class LtvCompatibilityLayer {

    public static final MutableStat convertIndDemandStat(Industry ind, String comID) {
        final String baseID = EconomyEngine.getBaseIndustryID(ind);
        final IndustryConfig config =  EconomyEngine.getInstance().ind_config.get(baseID);

        final MutableStat src = ind.getDemand(comID).getQuantity();
        final MutableStat dest = new MutableStat(0f);

        MutableStat modifier;
        if (
            (config != null && config.outputs.get(comID) != null && !config.outputs.get(comID).isAbstract) ||
            (config == null && ind.isIndustry() && !ind.isStructure())
        ) {
            modifier = ind.getDemandReduction().createCopy();
            modifier.applyMods(ind.getSupplyBonus());
        } else {
            modifier = ind.getDemandReduction();
        }

        copyMods(ind, src, dest, "ind_dr", modifier, comID);
        return dest;
    }

    public static final MutableStat convertIndSupplyStat(Industry ind, String comID) {
        final MutableStat src = ind.getSupply(comID).getQuantity();
        final MutableStat dest = new MutableStat(0f);

        copyMods(ind, src, dest, "ind_sb", ind.getSupplyBonus(), comID);
        return dest;
    }

    private static final void copyMods(Industry ind, MutableStat base, MutableStat dest, String modID,
        MutableStat mods, String comID) {

        final String baseID = "ind_" + ind.getId() + "_0";
        final StatMod baseMod = base.getFlatMods().get(baseID);

        float baseValue = 0;
        if (baseMod != null && baseMod.value > 0) {
            baseValue = baseMod.value;
        } else {
            float cumulativeBase = 0f;
            for (StatMod f : base.getFlatMods().values()) {
                if (f.source.equals(modID) || f.value < 0) continue;
                cumulativeBase += f.value;
            }
            baseValue = cumulativeBase;
        }

        dest.setBaseValue(industryBaseConverter(baseValue, ind, comID));

        if (mods == null) return;

        for (StatMod mod : mods.getPercentMods().values()) {
            dest.modifyPercent(mod.source + "::" + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : mods.getMultMods().values()) {
            dest.modifyMult(mod.source + "::" + ind.getId(), mod.value, mod.desc);
        }

        for (StatMod mod : mods.getFlatMods().values()) {
            float converted = industryModConverter((int) mod.value);
            dest.modifyMult(mod.source + "::" + ind.getId(), converted, mod.desc);
        }
    }

    /*
     * Converts vanilla base value to LTV base value with correct scaling.
     */
    private static final float industryBaseConverter(float baseValue, Industry ind, String comID) {
        final String baseID = EconomyEngine.getBaseIndustryID(ind);
        final IndustryConfig config =  EconomyEngine.getInstance().ind_config.get(baseID);

        final WorkerRegistry reg = WorkerRegistry.getInstance(); 
        final WorkerIndustryData data = reg.getData(ind.getMarket().getId(), ind.getId());

        if (
            config != null && config.outputs.get(comID) != null && !config.outputs.get(comID).isAbstract
        ) {
            final int workerDrivenCount = (int) config.outputs.values().stream()
                .filter(o -> o.usesWorkers && !o.isAbstract)
                .count();
            if (data != null) baseValue *= (data.getWorkersAssigned() / (float) workerDrivenCount);
        } else if (EconomyEngine.isWorkerAssignable(ind)) {
            final int workerDrivenCount = ind.getAllSupply().size();
            if (data != null) baseValue *= (data.getWorkersAssigned() / (float) workerDrivenCount);
        }

        if (config != null) return baseValue;

        final int size = ind.getMarket().getSize();

        float size3Base = baseValue - (size - 3);

        if (size3Base < 0f) size3Base = 0f;

        if (data != null) {
            return size3Base;
        }
        return (float) Math.pow(10, size - 3) * size3Base;
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
            return 1.69f;
        case 3: 
            return 2.197f;
        case 4:
            return 2.856f;
        case 5:
            return 3.713f;
        case 6:
            return 4.827f;
        case -1:
            return 0.75f;
        case -2:
            return 0.563f;
        case -3:
            return 0.422f;
        case -4:
            return 0.316f;
        case -5:
            return 0.237f;
        case -6:
            return 0.178f;
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
}

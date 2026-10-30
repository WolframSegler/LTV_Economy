package wfg.ltv_econ.economy.commodity;

import java.util.Map;
import java.util.function.ToDoubleFunction;

import wfg.ltv_econ.industry.IndustryIOs;
import wfg.native_ui.util.ArrayMap;

public final class DeficitCalculator {
    private DeficitCalculator() {};
    
    private static final float MIN_MULTIPLIER = 0.01f;

    /**
     * Computes the weighted input deficit for an output: the fraction of nominal
     * production lost to input scarcity.
     *
     * @param inputs input commodity ID → weight; may be null or empty
     * @param availability input commodity ID → availability in [0, 1]
     * @return weighted deficit in [0, 1]
     */
    public static final float computeWeightedDeficit(
        final ArrayMap<String, Float> inputs,
        final ToDoubleFunction<String> availability
    ) {
        if (inputs == null || inputs.isEmpty()) return 0f;

        double sum = 0d;
        for (float v : inputs.values()) {
            if (v > 0f) sum += v;
        }
        if (sum <= 0d) return 0f;

        double deficit = 0d;
        for (Map.Entry<String, Float> e : inputs.singleEntrySet()) {
            final String inputID = e.getKey();
            if (IndustryIOs.ABSTRACT_COM.equals(inputID)) continue;

            final double avail = availability.applyAsDouble(inputID);
            final double weightNorm = e.getValue() / sum;
            deficit += weightNorm * (1d - avail);
        }

        return (float) deficit;
    }

    public static final float deficitToMultiplier(final float deficit) {
        return Math.max(1f - deficit, MIN_MULTIPLIER);
    }
}
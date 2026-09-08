package wfg.ltv_econ.industry;

import static wfg.native_ui.util.Globals.settings;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry.WorkerPool;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.native_ui.util.Arithmetic;

public final class ConstructionDemandPredictor {
    private ConstructionDemandPredictor() {};
    private static final float eps = 0.00001f;
    private static final WeakHashMap<Industry, PredictionCache> industryCache = new WeakHashMap<>(6);

    private static final class PredictionCache {
        public final WorkerIndustryData data;
        public final int loopCycle; 

        public PredictionCache(WorkerIndustryData data, int loopCycle) {
            this.data = data;
            this.loopCycle = loopCycle;
        }
    }
    
    /**
     * @param ind the industry being built.
     * @param inputID Commodity ID of the input whose demand is being predicted.
     * @return Predicted daily demand in units per day (>= 0f). Returns 0f if not applicable.
     */
    public static final float computePredictedDailyDemand(Industry ind, String inputID) {
        if (!IndustryIOs.hasInput(ind.getSpec().getId(), inputID)) return 0f;

        final WorkerIndustryData data = getIndustryWorkerPrediction(ind);
        return CompatLayer.convertIndDemandStat(ind, inputID, data, true).getModifiedValue();
    }

    public static synchronized WorkerIndustryData getIndustryWorkerPrediction(Industry ind) {
        final int currentCycle = EconomyEngine.instance().getMainLoopCycle();
        PredictionCache cache = industryCache.get(ind);
        if (cache == null || cache.loopCycle != currentCycle) {
            WorkerIndustryData freshData = computeIndustryWorkerPrediction(ind);
            cache = new PredictionCache(freshData, currentCycle);
            industryCache.put(ind, cache);
        }
        return cache.data;
    }

    private static final WorkerIndustryData computeIndustryWorkerPrediction(Industry ind) {
        final MarketAPI market = ind.getMarket();
        final String indID = ind.getSpec().getId();
        final IndustryConfig indCfg = IndustryConfigManager.getIndConfig(indID);
        final WorkerPool pool = WorkerPoolRegistry.instance().getPool(market.getId());

        final float capacity = getIndustryCapacity(indCfg);
        final float freeRatio = pool.getFreeWorkerRatio();
        final float deficit = Math.max(0f, capacity - freeRatio);
        final float available = getReallocatedWorkerRatioFromLowerMarginOutputs(indCfg, market, ind);
        final float reallocated = Math.min(deficit, available);
        final float totalBudget = Math.min(capacity, freeRatio + reallocated);

        return getPredictedAssignments(market, indCfg, ind, totalBudget);
    }

    private static final float getIndustryCapacity(IndustryConfig config) {
        float indWorkerPoolCapacity = 0f;
        for (OutputConfig output : config.outputs.values()) {
            if (output.activeDuringBuilding || !output.usesWorkers || output.dynamic) continue;
            indWorkerPoolCapacity += output.workerAssignableLimit;
        }
        return Math.min(1f, indWorkerPoolCapacity);
    }

    private static final float getReallocatedWorkerRatioFromLowerMarginOutputs(IndustryConfig config, MarketAPI market, Industry targetInd) {
        final WorkerRegistry reg = WorkerRegistry.instance();
        final float ratio = EconConfig.PREDICTED_WORKER_REALLOCATION_FRACTION;
        
        final int outputCount = config.outputs.size();
        final float[] margins = new float[outputCount];
        final float[] capacitiesRemaining = new float[outputCount];
        int currIndex = 0;

        for (OutputConfig output : config.outputs.values()) {
            final float prod = CompatLayer.getModifiersMult(targetInd, output.comID, false);
            margins[currIndex] = IndustryIOs.getMarginalProfitPerWorker(config.indID, output.comID, prod);
            capacitiesRemaining[currIndex] = output.workerAssignableLimit;

            currIndex++;
        }

        float toBeReallocated = 0f;

        for (Industry ind : market.getIndustries()) {
            if (ind == targetInd) continue;
            final WorkerIndustryData data = reg.getRegisterData(ind);

            for (Map.Entry<String, Float> entry : data.getRegistry().singleEntrySet()) {
                final String outputId = entry.getKey();
                final float prod = CompatLayer.getModifiersMult(ind, outputId, false);
                final float margin = IndustryIOs.getMarginalProfitPerWorker(data.indID, entry.getKey(), prod);
                float remainingForInput = entry.getValue() * ratio;

                for (int i = 0; i < outputCount; i++) {
                    if (remainingForInput < eps) break;
                    if (margins[i] > margin && capacitiesRemaining[i] > eps) {
                        final float toReallocate = Math.min(remainingForInput, capacitiesRemaining[i]);
                        capacitiesRemaining[i] -= toReallocate;
                        remainingForInput -= toReallocate;
                        toBeReallocated += toReallocate;
                    }
                }
            }
        }

        return toBeReallocated;
    }

    private static final WorkerIndustryData getPredictedAssignments(MarketAPI market, IndustryConfig config, Industry ind, float budget) {
        final WorkerIndustryData data = new WorkerIndustryData(market.getId(), config.indID);
        final EconomyInfo info = EconomyEngine.instance().info;
        final Collection<OutputConfig> outputs = config.outputs.values();
        final String factionID = market.getFactionId();
        
        float minMargin = Float.POSITIVE_INFINITY;
        float totalWeights = 0f;
        final int maxSize = config.outputs.size();
        final float[] margins = new float[maxSize];
        final float[] weights = new float[maxSize];
        final float[] limits = new float[maxSize];

        int i = 0;
        for (OutputConfig output : outputs) {
            if (output.activeDuringBuilding || !output.usesWorkers || output.dynamic) continue;

            final float productivity = CompatLayer.getModifiersMult(ind, output.comID, false);
            margins[i] = IndustryIOs.getMarginalProfitPerWorker(data.indID, output.comID, productivity);
            limits[i] = output.workerAssignableLimit;
            
            minMargin = Math.min(minMargin, margins[i]);
            i++;
        }

        if (minMargin < 0f) {
            final float shift = Math.abs(minMargin) + eps;
            minMargin = eps;
            for (int j = 0; j < i; j++) {
                margins[j] += shift;
            }
        }

        if (minMargin < 1f ) {
            final float invert = 1f / minMargin;
            for (int j = 0; j < i; j++) {
                margins[j] *= invert;
            }
        }

        i = 0;
        for (OutputConfig output : outputs) {
            if (output.activeDuringBuilding || !output.usesWorkers || output.dynamic) continue;

            final float demand = info.getFactionTargetQuantum(output.comID, factionID);
            final float prod = info.getFactionComProd(output.comID, factionID);
            final float autarkyRatio = demand + prod == 0f ? 0f : Math.min(1f, demand == 0f ? 1f : prod/demand);

            final float price = settings.getCommoditySpec(output.comID).getBasePrice();

            final float squared = margins[i] * margins[i];
            final float autarky = (float) Arithmetic.clamp(Math.sqrt(100f - autarkyRatio * 100f), 0.1f, 0.9f);
            final float weight = squared * price * autarky * limits[i];
            weights[i] = weight;
            totalWeights += weight;

            i++;
        }

        if (totalWeights <= 0f) return data;

        final float[] results = allocateWithCaps(weights, limits, budget);

        i = 0;
        for (OutputConfig output : outputs) {
            if (output.activeDuringBuilding || !output.usesWorkers || output.dynamic) continue;

            data.setRatioForOutput(output.comID, results[i]);

            i++;
        }

        return data;
    }

    /**
     * Allocates budget among outputs with given weights and hard caps.
     * Each result[i] <= limits[i] and sum(result) == budget.
     * @implNote assumes budget <= sum(limits).
     * @return results[n].
     */
    private static final float[] allocateWithCaps(float[] weights, float[] limits, float budget) {
        final int n = weights.length;
        final float[] result = new float[n];
        final float[] w = weights.clone();
        final boolean[] capped = new boolean[n];
        float remaining = budget;

        while (true) {
            float sumUncapped = 0f;
            int uncappedCount = 0;
            for (int i = 0; i < n; i++) {
                if (!capped[i]) {
                    sumUncapped += w[i];
                    uncappedCount++;
                }
            }
            if (uncappedCount == 0 || sumUncapped <= eps) break;

            float scale = remaining / sumUncapped;
            boolean anyCapped = false;
            for (int i = 0; i < n; i++) {
                if (!capped[i]) {
                    float candidate = w[i] * scale;
                    if (candidate >= limits[i] - eps) {
                        result[i] = limits[i];
                        remaining -= limits[i];
                        w[i] = 0f;
                        capped[i] = true;
                        anyCapped = true;
                    }
                }
            }
            if (!anyCapped) {
                for (int i = 0; i < n; i++) {
                    if (!capped[i]) {
                        result[i] = w[i] * scale;
                        remaining -= result[i];
                    }
                }
                break;
            }
        }
        return result;
    }
}
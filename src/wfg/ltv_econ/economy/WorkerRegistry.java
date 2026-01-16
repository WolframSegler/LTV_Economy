package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.industry.IndustryIOs;

/**
 * The registry always uses IndustryIOs.getBaseIndustryID() internally to manage industry IDs
 */
public class WorkerRegistry {
    public static final String WorkerRegSerialID = "ltv_econ_worker_registry";
    public static final String KEY = "::";

    private final Map<String, WorkerIndustryData> registry = new HashMap<>();

    private static WorkerRegistry instance;

    private WorkerRegistry() {}

    public static final WorkerRegistry loadInstance(boolean forceRefresh) {
        WorkerRegistry workerRegistry = (WorkerRegistry) Global.getSector()
            .getPersistentData().get(WorkerRegSerialID);

        if (workerRegistry != null && !forceRefresh) {
            instance = workerRegistry;
        } else {
            instance = new WorkerRegistry();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(WorkerRegistry.class).info("Worker Registery constructed");
            }
        }

        return instance;
    }

    public static final void saveInstance() {
        Global.getSector().getPersistentData().put(WorkerRegSerialID, instance);
        instance = null;
    }

    public static final void setInstance(WorkerRegistry a) {
        instance = a;
    }

    public static final WorkerRegistry getInstance() {
        if (instance == null) loadInstance(false);
        return instance;
    }

    public final void resetWorkersAssigned(boolean resetPlayerIndustries) {
        for (WorkerIndustryData data : registry.values()) {
            if (!resetPlayerIndustries && data.market.isPlayerOwned()) continue;
            data.resetWorkersAssigned();
        }
    }

    public static final List<Industry> getVisibleIndustries(MarketAPI market) {
        List<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public final void register(Industry ind) {
        if (!IndustryIOs.getIndConfig(ind).workerAssignable) return;

        final String key = makeKey(ind.getMarket().getId(), IndustryIOs.getBaseIndustryID(ind));
        registry.putIfAbsent(key, new WorkerIndustryData(ind.getMarket().getId(), ind));
    }

    public final void register(MarketAPI market) {
        for (Industry ind : getVisibleIndustries(market)) register(ind);
    }

    public final void register(String marketID) {
        register(Global.getSector().getEconomy().getMarket(marketID));
    }

    public final void remove(String marketID, IndustrySpecAPI ind) {
        registry.remove(makeKey(marketID, IndustryIOs.getBaseIndustryID(ind)));
    }

    public final void remove(String marketID) {
        registry.keySet().removeIf(key -> key.startsWith(marketID + KEY));
    }

    public final boolean isMarketDataPresent(String marketID) {
        for (String dataID : registry.keySet()) {
            if (dataID.startsWith(marketID + KEY)) {
                return true;
            }
        }
        return false;
    }

    public final int getIndustryCountUsingWorkers(String marketID) {
        int count = 0;
        for (String dataID : registry.keySet()) {
            if (dataID.startsWith(marketID + KEY)) {
                count++;
            }
        }

        return count;
    }

    public final List<WorkerIndustryData> getIndustriesUsingWorkers(String marketID) {
        final ArrayList<WorkerIndustryData> list = new ArrayList<>(8); 
        for (Map.Entry<String, WorkerIndustryData> data : registry.entrySet()) {
            if (data.getKey().startsWith(marketID + KEY)) {
                list.add(data.getValue());
            }
        }

        return list;
    }

    public static final long getWorkerCap(MarketAPI market) {
        final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);

        return pool.getWorkerPool();
    }

    public final WorkerIndustryData getData(String marketID, String industryID) {
        return registry.get(makeKey(marketID, industryID));
    }

    public final WorkerIndustryData getData(String marketID, IndustrySpecAPI ind) {
        return getData(marketID, IndustryIOs.getBaseIndustryID(ind));
    }

    public final WorkerIndustryData getData(Industry ind) {
        return getData(ind.getMarket().getId(), IndustryIOs.getBaseIndustryID(ind));
    }

    public final void setData(WorkerIndustryData data) {
        registry.put(makeKey(data.marketID, data.indID), data);
    }

    public final boolean hasMarket(String marketID) {
        for (String regID : registry.keySet()) {
            if (regID.contains(marketID + KEY)) return true;
        }
        return false;
    }

    public final ArrayList<WorkerIndustryData> getRegistry() {
        return new ArrayList<>(registry.values());
    }

    public final ArrayList<String> getKeys() {
        return new ArrayList<>(registry.keySet());
    }

    private static final String makeKey(String marketID, String industryID) {
        return marketID + KEY + industryID;
    }

    public static class WorkerIndustryData {
        public final String marketID;
        public final String indID;
        
        public transient MarketAPI market;
        public transient Industry ind;
        
        private final Map<String, Float> outputRatios;
        private float outputRatioSum = 0;

        public WorkerIndustryData(String marketID, Industry industry) {
            this.marketID = marketID;
            this.indID = IndustryIOs.getBaseIndustryID(industry);
            this.outputRatios = new HashMap<>();

            for (OutputConfig output : IndustryIOs.getIndConfig(industry).outputs.values()) {
                if (!output.usesWorkers) continue;
                outputRatios.put(output.comID, 0f);
            }

            readResolve();
        }

        public Object readResolve() {
            market = Global.getSector().getEconomy().getMarket(marketID);
            ind = market.getIndustry(indID);

            return this;
        }

        /**
         * Copy Constructor
         */
        public WorkerIndustryData(WorkerIndustryData other) {
            this.marketID = other.marketID;
            this.indID = other.indID;

            this.outputRatios = new HashMap<>(other.outputRatios);

            this.outputRatioSum = other.outputRatioSum;

            this.market = other.market;
            this.ind = other.ind;
        }

        @Override
        public String toString() {
            return "["+marketID+KEY+indID+outputRatioSum+"]";
        }

        public final float getWorkerAssignedRatio(boolean resetCache) {
            if (resetCache) recalculateOutputRatioSum();
            return outputRatioSum;
        }

        public final float getRelativeWorkerAssignedRatio() {
            return getWorkerAssignedRatio(false) / outputRatioSum;
        }

        public final long getWorkersAssigned() {
            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);

            return (long) (outputRatioSum * pool.getWorkerPool());
        }

        public final long getAssignedForOutput(String comID) {
            if (!outputRatios.containsKey(comID)) return 0;


            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);

            return (long) (pool.getWorkerPool() * outputRatios.get(comID));
        }

        public final float getAssignedRatioForOutput(String comID) {
            return outputRatios.containsKey(comID) ? outputRatios.get(comID) : 0;
        }

        public final float getRelativeAssignedRatioForOutput(String comID) {
            if (!outputRatios.containsKey(comID)) return 0;
            return getAssignedRatioForOutput(comID) / outputRatioSum;
        }

        /**
         * @return A boolean indicating the success of the operation.
         */
        public final boolean setRatioForOutput(String comID, float ratio) {
            if (!outputRatios.containsKey(comID)) outputRatios.put(comID, 0f);

            final float oldRatio = outputRatios.get(comID);
            float diff = ratio - oldRatio;

            // compute allowed adjustment based on total sum
            final float maxAllowedDiff = 1f - outputRatioSum;
            final float minAllowedDiff = -oldRatio;

            // clamp the diff to the allowed range
            if (diff > maxAllowedDiff) diff = maxAllowedDiff;
            if (diff < minAllowedDiff) diff = minAllowedDiff;

            // apply adjusted ratio
            final float newRatio = oldRatio + diff;
            outputRatios.put(comID, newRatio);
            outputRatioSum += diff;

            return true;
        }


        public final void recalculateOutputRatioSum() {
            outputRatioSum = 0f;
            for (float value : outputRatios.values()) outputRatioSum += value;
        }

        public final void resetWorkersAssigned() {
            outputRatios.replaceAll((k, v) -> 0f);

            outputRatioSum = 0;
        }

        public final Set<String> getRegisteredOutputs() {
            return outputRatios.keySet();
        }
    }
}
package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.industry.IndustryIOs;

public class WorkerRegistry {
    public static final float DEFAULT_WORKER_CAP = 0.6f;

    private final Map<String, WorkerIndustryData> registry = new HashMap<>();

    private static WorkerRegistry instance;

    private WorkerRegistry() {
        for (MarketAPI market : EconomyEngine.getMarketsCopy()) {
        for (Industry ind : CommodityStats.getVisibleIndustries(market)) {
            if (EconomyEngine.isWorkerAssignable(ind)) {
                register(market, ind);
            }
        }
        }
    }

    public static final void createInstance() {
        instance = new WorkerRegistry();
    }

    public static final void setInstance(WorkerRegistry a) {
        instance = a;
    }

    public static final WorkerRegistry getInstance() {
        return instance;
    }

    public final void register(MarketAPI market, Industry ind) {
        final String key = makeKey(market.getId(), ind.getId());
        if (EconomyEngine.isWorkerAssignable(ind)) {
            registry.putIfAbsent(key, new WorkerIndustryData(market, ind));
        }
    }

    public final void register(String marketID) {
        final MarketAPI market = Global.getSector().getEconomy().getMarket(marketID);
        for (Industry ind : market.getIndustries()) {
            register(market, ind);
        }
    }

    public final void remove(String marketID, String indID) {
        registry.remove(makeKey(marketID, indID));
    }

    public final void remove(String marketID) {
        registry.keySet().removeIf(key -> key.startsWith(marketID + "::"));
    }

    public final boolean isMarketDataPresent(String marketID) {
        for (String dataID : registry.keySet()) {
            if (dataID.startsWith(marketID + "::")) {
                return true;
            }
        }
        return false;
    }

    public final int getIndustriesUsingWorkers(String marketID) {
        int count = 0;
        for (String dataID : registry.keySet()) {
            if (dataID.startsWith(marketID + "::")) {
                count++;
            }
        }

        return count;
    }

    public static final long getWorkerCap(MarketAPI market) {
        WorkerPoolCondition pool = WorkerIndustryData.getPoolCondition(market);
        if (pool == null) return 0;

        return pool.getWorkerPool();
    }

    public final WorkerIndustryData getData(String marketID, String industryID) {
        return registry.get(makeKey(marketID, industryID));
    }

    public final void setData(WorkerIndustryData data) {
        registry.put(makeKey(data.marketID, data.indID), data);
    }

    private static final String makeKey(String marketId, String industryId) {
        return marketId + "::" + industryId;
    }

    public final List<WorkerIndustryData> getRegister() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    public static class WorkerIndustryData {
        public final String marketID;
        public final String indID;
        
        public transient MarketAPI market;
        public transient Industry ind;
        
        private final Map<String, Float> outputRatios;
        private float outputRatioSum = 0;

        public WorkerIndustryData(MarketAPI market, Industry industry) {
            this.marketID = market.getId();
            this.indID = industry.getId();
            this.outputRatios = new HashMap<>();

            for (String outputID : IndustryIOs.getIndConfig(industry).outputs.keySet()) {
                outputRatios.put(outputID, 0f);
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

        public final float getWorkerAssignedRatio(boolean resetCache) {
            if (resetCache) recalculateOutputRatioSum();
            return outputRatioSum;
        }

        public final float getRelativeWorkerAssignedRatio() {
            return getWorkerAssignedRatio(false) / outputRatioSum;
        }

        public final long getWorkersAssigned() {
            final WorkerPoolCondition pool = getPoolCondition(market);
            if (pool == null) return 0;

            return (long) (outputRatioSum * pool.getWorkerPool());
        }

        public final long getAssignedForOutput(String comID) {
            if (!outputRatios.containsKey(comID)) return 0;

            final WorkerPoolCondition pool = getPoolCondition(market);
            if (pool == null) return 0;

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
            if (ratio < 0) ratio = 0;
            if (ratio > 1) ratio = 1;

            if (!outputRatios.containsKey(comID)) outputRatios.put(comID, 0f);

            final float diff = ratio - outputRatios.get(comID);
            final float result = outputRatioSum + diff;

            if (result > 1f) return false;
            if (getPoolCondition(market).getFreeWorkerRatio() - diff < 0f) return false;

            outputRatios.put(comID, ratio);
            outputRatioSum = result;

            return true;
        }

        public final void recalculateOutputRatioSum() {
            float sum = 0f;
            for (float value : outputRatios.values()) sum += value;

            outputRatioSum = sum;
        }

        public final void resetWorkersAssigned() {
            outputRatios.replaceAll((k, v) -> 0f);

            outputRatioSum = 0;
        }

        public final Set<String> getRegisteredOutputs() {
            return outputRatios.keySet();
        }

        public static final WorkerPoolCondition getPoolCondition(MarketAPI market) {

            final MarketConditionAPI workerPoolCondition = market.getCondition(WorkerPoolCondition.ConditionID);
            if (workerPoolCondition == null) return null;

            return (WorkerPoolCondition) workerPoolCondition.getPlugin();
        }
    }
}

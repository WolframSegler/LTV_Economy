package wfg.ltv_econ.economy.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

/**
 * The registry always uses IndustryIOs.getBaseIndustryID() internally to manage industry IDs
 */
public class WorkerRegistry implements Serializable {
    public static final String KEY = "::";

    private final ArrayMap<String, WorkerIndustryData> registry = new ArrayMap<>();

    public static final WorkerRegistry instance() {
        return LtvEconSaveData.instance().workerRegistry;
    }

    public final void resetWorkersAssigned(boolean resetPlayerIndustries) {
        for (WorkerIndustryData data : registry.values()) {
            if (!resetPlayerIndustries && data.market.isPlayerOwned()) continue;
            data.resetWorkersAssigned();
        }
    }

    public static final List<Industry> getVisibleIndustries(MarketAPI market) {
        final List<Industry> industries = new ArrayList<>(market.getIndustries());
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
        
        private final ArrayMap<String, Float> outputRatios;
        private float outputRatioSum = 0;

        public WorkerIndustryData(String marketID, Industry industry) {
            this.marketID = marketID;
            this.indID = IndustryIOs.getBaseIndustryID(industry);
            this.outputRatios = new ArrayMap<>();

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

            this.outputRatios = new ArrayMap<>(other.outputRatios);

            this.outputRatioSum = other.outputRatioSum;

            this.market = other.market;
            this.ind = other.ind;
        }

        @Override
        public String toString() {
            return "["+marketID+KEY+indID+outputRatioSum+"]";
        }

        public String toStringWithOutputs() {
            return "["+marketID+KEY+indID+"] -> "+outputRatios;
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
            final float maxAllowedDiff = 1f - outputRatioSum;
            final float minAllowedDiff = -oldRatio;
            final float diff = Arithmetic.clamp(ratio - oldRatio, minAllowedDiff, maxAllowedDiff);

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
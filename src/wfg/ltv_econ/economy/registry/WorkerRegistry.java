package wfg.ltv_econ.economy.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

/**
 * The registry always uses {@link IndustryConfigManager#getBaseIndustryID()} internally to manage industry IDs
 */
public final class WorkerRegistry implements Serializable {
    private static final int ESTIMATE_WORKER_INDUSTRY_PER_MARKET = 4;

    private final ArrayMap<String, ArrayMap<String, IndustryWorkerData>> registry = new ArrayMap<>(EconomyInfo.getMarketsCount());

    public static final WorkerRegistry instance() {
        return LtvEconSaveData.instance().workerRegistry;
    }

    private final Object readResolve() {
        registry.values().removeIf(inner -> {
            inner.values().removeIf(p -> p.market == null);
            return inner.isEmpty();
        });

        return this;
    }

    public final void resetWorkersAssigned(boolean resetPlayerIndustries) {
        for (ArrayMap<String, IndustryWorkerData> inner : registry.values()) {
            for (IndustryWorkerData data : inner.values()) {
                if (!resetPlayerIndustries && data.market.isPlayerOwned()) continue;
                data.resetWorkersAssigned();
            }
        }
    }

    public final void resetWorkersAssignedByMarket(List<MarketAPI> markets) {
        for (ArrayMap<String, IndustryWorkerData> inner : registry.values()) {
            for (IndustryWorkerData data : inner.values()) {
                if (markets.contains(data.market)) {
                    data.resetWorkersAssigned();
                }
            }
        }
    }

    public final void resetWorkersAssignedById(List<String> marketIDs) {
        for (ArrayMap<String, IndustryWorkerData> inner : registry.values()) {
            for (IndustryWorkerData data : inner.values()) {
                if (marketIDs.contains(data.marketID)) {
                    data.resetWorkersAssigned();
                }
            }
        }
    }

    public static final ArrayList<Industry> getVisibleIndustries(MarketAPI market) {
        final ArrayList<Industry> industries = new ArrayList<>(market.getIndustries());
        industries.removeIf(Industry::isHidden);
        return industries;
    }

    public final void register(Industry ind) {
        if (!IndustryConfigManager.getIndConfig(ind).workerAssignable) return;

        getForceRegisterData(ind);
    }

    public final void register(MarketAPI market) {
        for (Industry ind : getVisibleIndustries(market)) register(ind);
    }

    public final void register(String marketID) {
        register(Global.getSector().getEconomy().getMarket(marketID));
    }

    public final void remove(String marketID, IndustrySpecAPI ind) {
        final var marketMap = getMarketMap(marketID);
        if (marketMap == null) return;

        marketMap.remove(IndustryConfigManager.getBaseIndustryID(ind));
    }

    public final void remove(String marketID) {
        registry.remove(marketID);
    }

    public final int getIndustryCountUsingWorkers(String marketID) {
        final var marketMap = getMarketMap(marketID);
        return marketMap == null ? 0 : marketMap.size();
    }

    public final List<IndustryWorkerData> getIndustriesUsingWorkers(String marketID) {
        final var marketMap = getMarketMap(marketID);
        if (marketMap == null) return Collections.emptyList();

        return new ArrayList<>(marketMap.values());
    }

    public static final long getWorkerCap(String marketID) {
        return WorkerPoolRegistry.get(marketID).getWorkerPool();
    }

    public final IndustryWorkerData getData(String marketID, String industryID) {
        final var marketMap = getMarketMap(marketID);
        return marketMap == null ? null : marketMap.get(IndustryConfigManager.getBaseIndustryID(industryID));
    }

    public final IndustryWorkerData getData(String marketID, IndustrySpecAPI ind) {
        return getData(marketID, ind.getId());
    }

    public final IndustryWorkerData getData(Industry ind) {
        return getData(ind.getMarket().getId(), ind.getSpec());
    }

    public static final IndustryWorkerData get(String marketID, String industryID) {
        return instance().getData(marketID, industryID);
    }

    public static final IndustryWorkerData get(String marketID, IndustrySpecAPI ind) {
        return instance().getData(marketID, ind);
    }

    public static final IndustryWorkerData get(Industry ind) {
        return instance().getData(ind);
    }

    public final synchronized IndustryWorkerData getRegisterData(String marketID, String industryID) {
        if (!IndustryConfigManager.getIndConfig(industryID).workerAssignable) return null;

        return getForceRegisterData(marketID, industryID);
    }

    public final synchronized IndustryWorkerData getForceRegisterData(String marketID, String industryID) {
        return getOrCreateMarketMap(marketID).computeIfAbsent(
            IndustryConfigManager.getBaseIndustryID(industryID), i -> new IndustryWorkerData(marketID, i)
        );
    }

    public final IndustryWorkerData getRegisterData(String marketID, IndustrySpecAPI ind) {
        return getRegisterData(marketID, ind.getId());
    }

    public final IndustryWorkerData getRegisterData(Industry ind) {
        return getRegisterData(ind.getMarket().getId(), ind.getSpec());
    }

    public final IndustryWorkerData getForceRegisterData(String marketID, IndustrySpecAPI ind) {
        return getForceRegisterData(marketID, ind.getId());
    }

    public final IndustryWorkerData getForceRegisterData(Industry ind) {
        return getForceRegisterData(ind.getMarket().getId(), ind.getSpec());
    }

    public final void setData(IndustryWorkerData data) {
        getOrCreateMarketMap(data.marketID).put(data.indID, data);
    }

    public final ArrayList<IndustryWorkerData> getRegistry() {
        final ArrayList<IndustryWorkerData> list = new ArrayList<>();

        for (ArrayMap<String, IndustryWorkerData> inner : registry.values()) {
            list.addAll(inner.values());
        }

        return list;
    }

    private final ArrayMap<String, IndustryWorkerData> getMarketMap(final String marketID) {
        return registry.get(marketID);
    }

    private final ArrayMap<String, IndustryWorkerData> getOrCreateMarketMap(final String marketID) {
        return registry.computeIfAbsent(marketID, m -> new ArrayMap<>(ESTIMATE_WORKER_INDUSTRY_PER_MARKET));
    }

    public static class IndustryWorkerData {
        public final String marketID;
        public final String indID;
        
        public transient MarketAPI market;
        
        private final ArrayMap<String, Float> outputRatios = new ArrayMap<>(2);
        private float outputRatioSum = 0f;

        public IndustryWorkerData(String marketID, String industryID) {
            this.marketID = marketID;
            this.indID = IndustryConfigManager.getBaseIndustryID(industryID);

            readResolve();
        }

        public Object readResolve() {
            market = Global.getSector().getEconomy().getMarket(marketID);

            return this;
        }

        /** Copy Constructor */
        public IndustryWorkerData(IndustryWorkerData other) {
            this.marketID = other.marketID;
            this.indID = other.indID;

            this.outputRatios.putAll(other.outputRatios);

            this.outputRatioSum = other.outputRatioSum;

            this.market = other.market;
        }

        @Override
        public String toString() {
            return "[" + marketID + " - " + indID + outputRatioSum + "]";
        }

        public final String toStringWithOutputs() {
            return "[" + marketID + " - " + indID + "] -> " + outputRatios;
        }

        public final float getWorkerAssignedRatio(boolean resetCache) {
            if (resetCache) recalculateOutputRatioSum();
            return outputRatioSum;
        }

        public final float getRelativeWorkerAssignedRatio() {
            return getWorkerAssignedRatio(false) / outputRatioSum;
        }

        public final long getWorkersAssigned() {
            return (long) (outputRatioSum * WorkerRegistry.getWorkerCap(marketID));
        }

        public final long getAssignedForOutput(String comID) {
            return (long) (WorkerRegistry.getWorkerCap(marketID) * outputRatios.getOrDefault(comID, 0f));
        }

        public final float getAssignedRatioForOutput(String comID) {
            return outputRatios.containsKey(comID) ? outputRatios.get(comID) : 0;
        }

        public final float getRelativeAssignedRatioForOutput(String comID) {
            if (!outputRatios.containsKey(comID)) return 0;
            return getAssignedRatioForOutput(comID) / outputRatioSum;
        }

        public final void setRatioForOutput(String comID, float ratio) {
            final float oldRatio = outputRatios.computeIfAbsent(comID, id -> 0f);
            final float diff = Arithmetic.clamp(ratio - oldRatio,
                -oldRatio, // minAllowedDiff
                1f - outputRatioSum // maxAllowedDiff
            );

            outputRatios.put(comID, oldRatio + diff);
            outputRatioSum += diff;
        }

        public final void recalculateOutputRatioSum() {
            outputRatioSum = 0f;
            for (float value : outputRatios.values()) outputRatioSum += value;
        }

        /**
         * Checks presence on the market. Resets {@link #outputRatios} if not present.
         * 
         * @return presence status
         */
        public final boolean ensurePresence() {
            if (market != null) {
                for (Industry ind : market.getIndustries()) {
                    if (IndustryConfigManager.getBaseIndustryID(ind).equals(indID)) {
                        return true;
                    }
                }
            }

            resetWorkersAssigned();
            return false;
        }

        public final void resetWorkersAssigned() {
            outputRatios.replaceAll((k, v) -> 0f);

            outputRatioSum = 0f;
        }

        public final Set<String> getRegisteredOutputs() {
            return outputRatios.keySet();
        }

        public final ArrayMap<String, Float> getRegistry() {
            return outputRatios;
        }
    }
}
package wfg.ltv_econ.economy.registry;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.Arithmetic;

public class WorkerPoolRegistry {
    private final ConcurrentHashMap<String, WorkerPool> registry = new ConcurrentHashMap<>(EconomyInfo.getMarketsCount());

    public static final WorkerPoolRegistry instance() {
        return LtvEconSaveData.instance().poolRegistry;
    }

    public static final WorkerPool get(MarketAPI market) {
        return get(market.getId());
    }

    public static final WorkerPool get(String marketID) {
        return instance().getPool(marketID);
    }

    public final WorkerPool getPool(MarketAPI market) {
        return getPool(market.getId());
    }

    public final WorkerPool getPool(String marketID) {
        return registry.computeIfAbsent(marketID, p -> new WorkerPool(marketID));
    }

    public final void register(MarketAPI market) {
        register(market.getId());
    }

    public final void register(String marketID) {
        registry.putIfAbsent(marketID, new WorkerPool(marketID));
    }

    public final void recalculate() {
        registry.values().forEach(WorkerPool::recalculate);
    }

    public final void recalculateWorkerPool() {
        registry.values().forEach(WorkerPool::recalculateWorkerPool);
    }

    public final void recalculateFreeWorkers() {
        registry.values().forEach(WorkerPool::recalculateFreeWorkers);
    }

    public final ArrayList<WorkerPool> getRegistry() {
        return new ArrayList<>(registry.values());
    }

    public static class WorkerPool {
        private final String marketID;
        private transient MarketAPI market;

        private long workerPool = 0l;
        private float freeWorkerRatio = 1f;

        public WorkerPool(String marketID) {
            this.marketID = marketID;

            readResolve();

            recalculate();
        }

        private final Object readResolve() {
            market = Global.getSector().getEconomy().getMarket(marketID);

            return this;
        }

        public final synchronized void recalculate() {
            recalculateWorkerPool();
            recalculateFreeWorkers();
        }

        public final synchronized void recalculateWorkerPool() {
            workerPool = getWorkerPoolUncached();
        }

        public final synchronized void recalculateFreeWorkers() {
            setFreeWorkerRatio(getFreeWorkerRatioUncached());
        }

        public final synchronized long getWorkerPoolUncached() {
            final int size = market.getSize();
            final double base = getWorkerRatio(size) * Math.pow(10, size);
            if (LaborConfig.GROWTH_EFFECT_WORKER_POOL) {
                final float t = Misc.getMarketSizeProgress(market);
                final double dest = getWorkerRatio(size+1) * Math.pow(10, size+1);
                return (long) Arithmetic.lerp(base, dest, t);
            } else {
                return (long) base;
            }
        }

        public final synchronized float getFreeWorkerRatioUncached() {
            final WorkerRegistry reg = WorkerRegistry.instance();
            if (reg == null) return 0f;

            float totalAssigned = 0f;
            for (WorkerIndustryData data : reg.getIndustriesUsingWorkers(market.getId())) {
                data.ensurePresence();
                totalAssigned += data.getWorkerAssignedRatio(false);
            }

            return Math.max(0f, 1f - totalAssigned);
        }

        public final long getWorkerPool() {
            return workerPool;
        }

        public final float getFreeWorkerRatio() {
            return freeWorkerRatio;
        }

        public final void setWorkerPool(long workers) {
            if (workers < 0l) return;
            workerPool = workers;
        }

        public final boolean setFreeWorkerRatio(float workers) {
            if (workers < 0f || workers > 1f) return false;

            freeWorkerRatio = workers;
            return true;
        }

        public final boolean isWorkerRatioAssignable(float amount) {
            return freeWorkerRatio >= amount;
        }

        public final boolean assignFreeWorkers(float assignedWorkers) {
            if (freeWorkerRatio < assignedWorkers)
                return false;

            freeWorkerRatio -= assignedWorkers;
            return true;
        }

        public final void releaseWorkers(float releasedWorkers) {
            freeWorkerRatio += releasedWorkers;
            if (freeWorkerRatio > 1f) {
                freeWorkerRatio = 1f;
            }
        }

        public static final float getWorkerRatio(int size) {
            switch (size) {
                case 1, 2: return 1f;
                case 3: return 0.9f;
                case 4: return 0.8f;
                case 5: return 0.68f;
                case 6: return 0.52f;
                case 7: return 0.4f;
                case 8: return 0.26f;
                case 9: return 0.15f;
                case 10: return 0.08f;
                default: return 1f/(2f * size);
            }
        }
    }
}
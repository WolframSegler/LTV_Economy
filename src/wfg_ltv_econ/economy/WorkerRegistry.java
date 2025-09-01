package wfg_ltv_econ.economy;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;

import wfg_ltv_econ.conditions.WorkerPoolCondition;

public class WorkerRegistry {
    private final Map<String, WorkerIndustryData> registry = new HashMap<>();

    private static WorkerRegistry instance;

    public static final void setInstance(WorkerRegistry a) {
        instance = a;
    }

    public static final WorkerRegistry getInstance() {
        return instance;
    }

    public final void register(MarketAPI market, Industry ind) {
        String key = makeKey(market.getId(), ind.getId());
        registry.putIfAbsent(key, new WorkerIndustryData(market.getId(), ind.getId()));
    }

    public final WorkerIndustryData get(String marketID, String industryID) {
        return registry.get(makeKey(marketID, industryID));
    }

    public final WorkerIndustryData getData(MarketAPI market, Industry ind) {
        return get(market.getId(), ind.getId());
    }

    private static final String makeKey(String marketId, String industryId) {
        return marketId + "::" + industryId;
    }

    public class WorkerIndustryData {
        public final String marketID;
        public final String indID;

        public transient MarketAPI market;
        public transient Industry ind;

        private float workersAssigned;

        public WorkerIndustryData(String marketID, String industryID) {
            this.marketID = marketID;
            this.indID = industryID;
            this.workersAssigned = 0;

            readResolve();
        }

        public Object readResolve() {
            market = Global.getSector().getEconomy().getMarket(marketID);
            ind = market.getIndustry(indID);

            return this;
        }

        public final void setAssignedWorkers(float workers) {
            workersAssigned = workers;
        }

        public final float getWorkerAssignedRatio() {
            return workersAssigned;
        }

        public final long getWorkersAssigned() {
            MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
            if (workerPoolCondition == null) {
                return 0;
            }
            WorkerPoolCondition pool = (WorkerPoolCondition) workerPoolCondition.getPlugin();
            return (long) (workersAssigned * pool.getWorkerPool());
        }

        public final void setWorkersAssigned(float newAmount) {
            if (0 > newAmount || newAmount > 1 || market == null) {
                return;
            }
            MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
            if (workerPoolCondition == null) {
                return;
            }
            final WorkerPoolCondition pool = (WorkerPoolCondition) workerPoolCondition.getPlugin();

            final float delta = newAmount - workersAssigned;

            if (delta > 0f) {
                if (pool.isWorkerRatioAssignable(delta)) {
                    pool.assignFreeWorkers(delta);
                    workersAssigned = newAmount;
                }
            } else if (delta < 0f) {
                pool.releaseWorkers(-delta);
                workersAssigned = newAmount;
            }
        }

        public final long getWorkerCap() {
            MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
            if (workerPoolCondition == null) {
                return 0;
            }
            WorkerPoolCondition pool = (WorkerPoolCondition) workerPoolCondition.getPlugin();

            return pool.getWorkerPool();
        }
    }
}

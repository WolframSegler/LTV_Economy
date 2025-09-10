package wfg.ltv_econ.economy;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;

public class WorkerRegistry {
    public static final float DEFAULT_WORKER_CAP = 0.6f;

    private final Map<String, WorkerIndustryData> registry = new HashMap<>();

    private static WorkerRegistry instance;

    private WorkerRegistry() {
        for (MarketAPI market : EconomyEngine.getMarketsCopy()) {
        for (Industry ind : CommodityStats.getVisibleIndustries(market)) {
            if (EconomyEngine.isWorkerAssignable(ind)) {
                register(market.getId(), ind.getId());
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

    public final void register(String marketID, String indID) {
        final String key = makeKey(marketID, indID);
        final Industry ind = Global.getSector().getEconomy().getMarket(marketID).getIndustry(indID);
        if (EconomyEngine.isWorkerAssignable(ind)) {
            registry.putIfAbsent(key, new WorkerIndustryData(marketID, indID));
        }
    }

    public final void register(String marketID) {
        final MarketAPI market = Global.getSector().getEconomy().getMarket(marketID);
        for (Industry ind : market.getIndustries()) {
            register(marketID, ind.getId());
        }
    }

    public final void remove(String marketID, String indID) {
        registry.remove(makeKey(marketID, indID));
    }

    public final void remove(String marketID) {
        final MarketAPI market = Global.getSector().getEconomy().getMarket(marketID);
        for (Industry ind : market.getIndustries()) {
            registry.remove(makeKey(marketID, ind.getId()));
        }
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
        MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
        if (workerPoolCondition == null) {
            return 0;
        }
        WorkerPoolCondition pool = (WorkerPoolCondition) workerPoolCondition.getPlugin();

        return pool.getWorkerPool();
    }

    public final WorkerIndustryData getData(String marketID, String industryID) {
        return registry.get(makeKey(marketID, industryID));
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

        public final float getWorkerAssignedRatio() {
            return workersAssigned;
        }

        public final long getWorkersAssigned() {
            final MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
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
            final MarketConditionAPI workerPoolCondition = market.getCondition("worker_pool");
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
    }
}

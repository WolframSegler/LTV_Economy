package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.industry.LtvBaseIndustry;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine {
    private static EconomyEngine instance;

    private final Map<String, CommoditySpecAPI> m_commoditySpecs;
    private final Map<String, CommodityInfo> m_commoditInfo;

    public static void createInstance() {
        if (instance == null) {
            instance = new EconomyEngine();
        }
    }

    public static EconomyEngine getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private EconomyEngine() {
        this.m_commoditInfo = new HashMap<>();
        this.m_commoditySpecs = new HashMap<>();

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            m_commoditySpecs.put(spec.getId(), spec);
            m_commoditInfo.put(spec.getId(), new CommodityInfo(spec));
        }

        fakeAdvance();
    }

    public final void update() {
        for (Map.Entry<String, CommodityInfo> comInfo : m_commoditInfo.entrySet()) {
            for (CommodityStats stats : comInfo.getValue().getAllStats()) {
                stats.update();
            }
        }
    }

    protected int dayTracker = -1;

    public final void advance(float delta) {
        final int day = Global.getSector().getClock().getDay();

        if (dayTracker == -1) {
            dayTracker = day;
        }

		if (dayTracker == day) return;

        dayTracker = day;

        mainLoop(false);
    }

    public final void fakeAdvance() {
        mainLoop(true);
    }

    private final void mainLoop(boolean fakeAdvance) {
        assignWorkers();

        for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {

            com.getValue().reset();

            com.getValue().trade();

            com.getValue().advance(fakeAdvance);
        }
    }

    public final void registerMarket(MarketAPI market) {
        for (Map.Entry<String, CommodityInfo> comInfo : m_commoditInfo.entrySet()) {
            comInfo.getValue().addMarket(market);
        }
    }

    public final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public final List<CommoditySpecAPI> getCommoditiesCopy() {
        return new ArrayList<>(m_commoditySpecs.values());
    }

    public final CommodityInfo getCommodityInfo(String comID) {
        return m_commoditInfo.get(comID);
    }

    public final CommodityStats getComStats(String comID, MarketAPI market) {
        final CommodityStats stats = m_commoditInfo.get(comID).getStats(market);
        if (stats != null) {
            stats.update();
        }
        return stats;
    }

    private final void assignWorkers() {
        for (MarketAPI market : getMarketsCopy()) {
            if (market.isPlayerOwned() || market.isHidden()) continue;

            final List<Industry> workingIndustries = CommodityStats.getVisibleIndustries(market);
            if (workingIndustries.isEmpty() || !market.hasCondition(WorkerPoolCondition.ConditionID)) continue;

            WorkerPoolCondition cond =(WorkerPoolCondition)market.getCondition(WorkerPoolCondition.ConditionID);

            int workerAssignableIndustries = 0;

            for (Industry ind : workingIndustries) {
                if (ind instanceof LtvBaseIndustry && ind.isFunctional()) {
                    workerAssignableIndustries++;
                }
            }

            if (workerAssignableIndustries == 0) continue;

            long workersPerIndustry = (long) (cond.getWorkerPool() / (float) workerAssignableIndustries);
            for (Industry ind : workingIndustries) {
                if (ind instanceof LtvBaseIndustry industry && ind.isFunctional()) {
                    industry.setWorkersAssigned(workersPerIndustry);
                }
            }
        }
    }

    public final long getTotalGlobalExports(String comID) {
        long totalGlobalExports = 0;
        for (CommodityStats stats : m_commoditInfo.get(comID).getAllStats()) {
            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }

    public final long getTotalInFactionExports(String comID, FactionAPI faction) {
        long TotalFactionExports = 0;

        for (CommodityStats stats : m_commoditInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            TotalFactionExports += stats.inFactionExports;
        }

        return TotalFactionExports;
    }

    public final long getFactionTotalGlobalExports(String comID, FactionAPI faction) {
        long totalGlobalExports = 0;

        for (CommodityStats stats : m_commoditInfo.get(comID).getAllStats()) {
            if (stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }

            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }
}

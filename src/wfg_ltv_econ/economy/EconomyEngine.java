package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.economy.IndustryConfigLoader.OutputCom;
import wfg_ltv_econ.industry.LtvBaseIndustry;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine {
    private static EconomyEngine instance;

    private final Map<String, CommodityInfo> m_commoditInfo;
    public final Map<String, Map<String, IndustryConfigLoader.OutputCom>> configs;

    private int marketAmount = 0; 

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

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            m_commoditInfo.put(spec.getId(), new CommodityInfo(spec));
        }

        marketAmount = getMarketsCopy().size();

        configs = IndustryConfigLoader.loadAsMap();

        fakeAdvance();
    }

    public final void update() {
        for (CommodityInfo comInfo : m_commoditInfo.values()) {
            comInfo.update();
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

        if (getMarketsCopy().size() != marketAmount) {

            for (CommodityInfo comInfo : m_commoditInfo.values()) {
                comInfo.refreshMarkets();
            }
        }

        mainLoop(false);
    }

    public final void fakeAdvance() {
        mainLoop(true);
    }

    // Order matters here
    private final void mainLoop(boolean fakeAdvance) {        
        assignWorkers();

        for (CommodityInfo comInfo : m_commoditInfo.values()) {
            comInfo.reset();

            comInfo.update();
        }

        weightedOutputDeficitMods();

        weightedInputDeficitMods();

        for (CommodityInfo comInfo : m_commoditInfo.values()) {
            comInfo.trade();

            comInfo.advance(fakeAdvance);
        }
    }

    public final void registerMarket(MarketAPI market) {
        for (CommodityInfo comInfo : m_commoditInfo.values()) {
            comInfo.addMarket(market);
        }
    }

    public final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public static final List<CommoditySpecAPI> getEconCommodities() {
        return Global.getSettings().getAllCommoditySpecs().stream()
            .filter(spec -> !spec.isNonEcon())
            .collect(Collectors.toList()); 
    }

    public final CommodityInfo getCommodityInfo(String comID) {
        return m_commoditInfo.get(comID);
    }

    public boolean hasCommodity(String comID) {
        return m_commoditInfo.containsKey(comID);
    }

    public final CommodityStats getComStats(String comID, MarketAPI market) {
        Global.getLogger(getClass()).error(comID);
        final CommodityStats stats = m_commoditInfo.get(comID).getStats(market);
        if (stats != null) {
            stats.update();
        }
        return stats;
    }

    public final void weightedOutputDeficitMods() {

        for (CommodityInfo comInfo : m_commoditInfo.values()) {
        for (Map.Entry<MarketAPI, CommodityStats> marketEntry : comInfo.getStatsMap().entrySet()) {
            CommodityStats stats = marketEntry.getValue();
            double outputMultiplier = 1f;

            for (Industry ind : stats.getVisibleIndustries()) {
                Map<String, OutputCom> indObj = configs.get(ind.getId());
                if (indObj == null) continue;

                OutputCom outputCom = indObj.get(stats.m_com.getId());
                if (outputCom == null) continue;

                Map<String, Float> weights = outputCom.demand;
                for (Map.Entry<String, Float> inputWeight : weights.entrySet()) {
                    CommodityStats inputStats = getComStats(inputWeight.getKey(), marketEntry.getKey());
                    if (inputStats == null) continue;

                    float coverage = inputStats.getStoredCoverageRatio();
                    outputMultiplier -= inputWeight.getValue() * (1f - coverage);
                }
            }

            stats.localProductionMult = Math.max(outputMultiplier, 0.01f);
        }
        }
    }

    public final void weightedInputDeficitMods() {

        for (CommodityInfo comInfo : m_commoditInfo.values()) {
        for (Map.Entry<MarketAPI, CommodityStats> marketEntry : comInfo.getStatsMap().entrySet()) {
            CommodityStats stats = marketEntry.getValue();

            double maxInputMultiplier = 1f;

            // Loop through industries in this market that are relevant to this commodity
            for (Industry ind : stats.getVisibleIndustries()) {
                Map<String, OutputCom> indObj = configs.get(ind.getId());
                if (indObj == null) continue;

                // Check all outputs this industry produces
                for (Map.Entry<String, OutputCom> outputEntry : indObj.entrySet()) {
                    String outputCommodityId = outputEntry.getKey();  // key is the commodity produced
                    OutputCom outputCom = outputEntry.getValue();

                    Map<String, Float> inputWeights = outputCom.demand;

                    // Only proceed if this output consumes the current commodity
                    if (inputWeights.containsKey(stats.m_com.getId())) {
                        double contribution = inputWeights.get(stats.m_com.getId()); // weight of this input

                        // Get the production stats of the output commodity
                        CommodityStats outputStats = getComStats(outputCommodityId, marketEntry.getKey());
                        if (outputStats == null) continue;

                        double throttledRatio = outputStats.localProductionMult;

                        // Scale the allowed input proportionally to output production
                        maxInputMultiplier = Math.min(maxInputMultiplier, throttledRatio / contribution);
                    }
                }
            }

            stats.demandBaseMult = Math.max(maxInputMultiplier, 0f);
        }
        }
    }

    private final void assignWorkers() {

        for (MarketAPI market : getMarketsCopy()) {
            if (market.isPlayerOwned() || market.isHidden()) continue;

            final List<Industry> workingIndustries = CommodityStats.getVisibleIndustries(market);
            if (workingIndustries.isEmpty() || !market.hasCondition(WorkerPoolCondition.ConditionID)) continue;

            int workerAssignableIndustries = 0;

            for (Industry ind : workingIndustries) {
                if (ind instanceof LtvBaseIndustry && ind.isFunctional()) {
                    workerAssignableIndustries++;
                }
            }

            if (workerAssignableIndustries == 0) continue;

            for (Industry ind : workingIndustries) {
                if (ind instanceof LtvBaseIndustry industry && ind.isFunctional()) {
                    industry.setWorkersAssigned(1 / (float) workerAssignableIndustries);
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

    public static final <T> List<T> symmetricDifference(List<T> list1, List<T> list2) {
        Set<T> set1 = new HashSet<>(list1);
        Set<T> set2 = new HashSet<>(list2);

        Set<T> result = new HashSet<>(set1);
        result.addAll(set2);           // union
        Set<T> tmp = new HashSet<>(set1);
        tmp.retainAll(set2);           // intersection
        result.removeAll(tmp);         // remove intersection

        return new ArrayList<>(result);
    }

}

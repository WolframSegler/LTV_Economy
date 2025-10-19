package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.economy.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.wrap_ui.util.NumFormat;

import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine extends BaseCampaignEventListener
    implements PlayerColonizationListener, ColonyDecivListener {

    private static EconomyEngine instance;

    private final Set<String> m_registeredMarkets;
    private final Map<String, CommodityInfo> m_comInfo;

    public static void createInstance() {
        if (instance == null) {
            instance = new EconomyEngine();
        }
    }

    public static void setInstance(EconomyEngine a) {
        instance = a;
    }

    public static EconomyEngine getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private EconomyEngine() {
        super(true);
        m_registeredMarkets = new HashSet<>();
        m_comInfo = new HashMap<>();

        for (MarketAPI market : getMarketsCopy()) {
            if (!market.isInEconomy()) continue;

            m_registeredMarkets.add(market.getId());
        }

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            m_comInfo.put(spec.getId(), new CommodityInfo(spec, m_registeredMarkets));
        }

        readResolve();
    }

    public final Object readResolve() {
        Global.getSector().getListenerManager().addListener(this, true);
        Global.getSector().addListener(this);

        return this;
    }

    protected int dayTracker = -1;

    public final void advance(float delta) {
        final int day = Global.getSector().getClock().getDay();

        if (dayTracker == -1) {
            dayTracker = day;
        }

        if (dayTracker == day)
            return;

        dayTracker = day;

        mainLoop(false);
    }

    public final void fakeAdvance() {
        mainLoop(true);
    }

    /**
     * Advances the economy by one production & trade cycle.
     * <p>
     * This method is the central loop for processing all markets, industries, and commodities
     * within the EconomyEngine. It handles production, labor assignment, input deficit adjustments,
     * trade, and stockpile updates. Each step occurs in a specific order to preserve the integrity
     * of the simulation and ensure worker assignment is based on actual unmet demand from
     * worker-independent industries.
     * </p>
     * <p><b>Cycle steps:</b></p>
     * <ol>
     *   <li>
     *     <b>refreshMarkets()</b> - Synchronizes the EconomyEngine's market list with the game's
     *     official Economy. Adds new markets and removes non-existent ones.
     *   </li>
     *   <li>
     *     <b>comInfo.reset()</b> - Resets per-commodity values such as imports, exports, and
     *     stockpile usage from the previous cycle. Ensures a clean slate for updates.
     *   </li>
     *   <li>
     *     <b>Worker-independent industry update</b> - Updates commodities produced by industries that
     *     do not depend on workers. Worker-dependent industries are temporarily ignored, so their
     *     demand does not affect the calculation.
     *   </li>
     *   <li>
     *     <b>assignWorkers()</b> - Assigns workers to worker-dependent industries using the
     *     matrix-based solver. This step uses only the demand from worker-independent industries,
     *     ensuring optimal allocation without iterative guesswork. Runs only if
     *     <code>fakeAdvance == false</code>.
     *   </li>
     *   <li>
     *     <b>comInfo.update()</b> - Updates all commodities to account for the newly assigned workers,
     *     converting production into LTV-based stats, calculating base demand, and updating local production.
     *   </li>
     *   <li>
     *     <b>weightedOutputDeficitMods()</b> - Adjusts local production based on input deficits.
     *     Only considers stockpiles available at the start of this cycle. This throttling
     *     ensures industries cannot consume unavailable inputs.
     *   </li>
     *   <li>
     *     <b>comInfo.trade()</b> - Handles exports of surplus commodities and imports to
     *     fill storage for the next cycle. Trade occurs after production so industries
     *     always consume existing stockpiles. Imports feed storage for future production.
     *   </li>
     *   <li>
     *     <b>comInfo.advance(fakeAdvance)</b> - Updates stockpiles after production and trade.
     *     If <code>fakeAdvance</code> is true, this step does nothing. Stockpiles now include
     *     newly imported goods ready for the next cycle.
     *   </li>
     * </ol>
     *
     * <p><b>Important notes:</b></p>
     * <ul>
     *   <li>Worker assignment must occur after worker-independent production is updated but before
     *       worker-dependent industries are updated, to ensure demand reflects actual unmet needs.</li>
     *   <li>If <code>fakeAdvance</code> is true, worker assignments and stockpile updates are skipped,
     *       allowing simulation previews without altering the economy state.</li>
     * </ul>
     *
     * @param fakeAdvance If true, simulates a tick without modifying worker assignments or
     *                    stockpiles (useful for interfaces or previews).
     */
    private final void mainLoop(boolean fakeAdvance) {
        refreshMarkets();

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.reset();
        }

        if (!fakeAdvance) {
            resetWorkersAssigned();
    
            for (CommodityInfo comInfo : m_comInfo.values()) {
                comInfo.update();
            }
        }

        if (!fakeAdvance) {
            // assignWorkers();
            assignWorkersSmart();
        }

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.update();
        }
        
        weightedOutputDeficitMods();

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.trade();

            comInfo.advance(fakeAdvance);
        }
    }

    public final void registerMarket(String marketID) {
        if (m_registeredMarkets.add(marketID)) {
            for (CommodityInfo comInfo : m_comInfo.values()) {
                comInfo.addMarket(marketID);
            }

        }
        
        WorkerRegistry.getInstance().register(marketID);
    }

    public final void removeMarket(String marketID) {
        if (m_registeredMarkets.remove(marketID)) {
            for (CommodityInfo comInfo : m_comInfo.values()) {
                comInfo.removeMarket(marketID);
            }

            WorkerRegistry.getInstance().remove(marketID);
        }
    }

    public final void refreshMarkets() {
        Set<String> currentMarketIDs = new HashSet<>();

        for (MarketAPI market : getMarketsCopy()) {
            String marketID = market.getId();
            currentMarketIDs.add(marketID);
            registerMarket(marketID);
        }

        for (Iterator<String> it = m_registeredMarkets.iterator(); it.hasNext();) {
            String registeredID = it.next();
            if (!currentMarketIDs.contains(registeredID)) {
                it.remove(); 
                for (CommodityInfo comInfo : m_comInfo.values()) {
                    comInfo.removeMarket(registeredID);
                }
                WorkerRegistry.getInstance().remove(registeredID);
            }
        }
    }

    public Set<String> getRegisteredMarkets() {
        return Collections.unmodifiableSet(m_registeredMarkets);
    }

    public static final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public void reportPlayerOpenedMarket() {
        fakeAdvance();
        Global.getLogger(getClass()).error("MarketOpened");
    }

    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        final String marketID = planet.getMarket().getId();
        registerMarket(marketID);
        Global.getLogger(getClass()).error("MarketColonized");
    }

    public void reportPlayerAbandonedColony(MarketAPI market) {
        removeMarket(market.getId());
        Global.getLogger(getClass()).error("MarketAbandoned");
    }

    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        removeMarket(market.getId());
        Global.getLogger(getClass()).error("MarketDecivilized");
    }

    public void reportColonyAboutToBeDecivilized(MarketAPI a, boolean b) {}

    public static final List<CommoditySpecAPI> getEconCommodities() {
        return Global.getSettings().getAllCommoditySpecs().stream()
                .filter(spec -> !spec.isNonEcon())
                .collect(Collectors.toList());
    }

    public final CommodityInfo getCommodityInfo(String comID) {
        return m_comInfo.get(comID);
    }

    public final boolean hasCommodity(String comID) {
        return m_comInfo.containsKey(comID);
    }

    public final CommodityStats getComStats(String comID, String marketID) {
        final CommodityInfo comInfo = m_comInfo.get(comID);

        if (comInfo == null) {
            throw new RuntimeException("Referencing a non-econ or missing commodity: " + comID);
        }

        final CommodityStats stats = comInfo.getStats(marketID);
        return stats;
    }

    public static final boolean isWorkerAssignable(Industry ind) {
        final IndustryConfig config = IndustryIOs.getIndConfig(ind);
        if (config != null) {
            return config.workerAssignable;
        } else {
            return ind.isIndustry() && !ind.isStructure();
        }
    }

    public static final float getWorkersPerUnit(String comID, OCCTag tag) {
        final float Pout = Global.getSettings().getCommoditySpec(comID).getBasePrice();
        final float LPV_day = IndustryIOs.labor_config.LPV_day;
        final float RoVC = IndustryIOs.labor_config.getRoVC(tag);

        return (Pout * RoVC) / LPV_day;
    } 

    public final void weightedOutputDeficitMods() {
        for (CommodityInfo comInfo : m_comInfo.values()) {
        for (CommodityStats stats : comInfo.getStatsMap().values()) {

            float totalDeficit = 0f;
            float totalMarketOutput = stats.getLocalProduction(false);
            float invMarketOutput = 1f / totalMarketOutput;

            for (Map.Entry<String, MutableStat> industryEntry : stats.getLocalProductionStat().entrySet()) {
                String industryID = industryEntry.getKey();
                MutableStat industryStat = industryEntry.getValue();

                float industryOutput = industryStat.getModifiedValue();
                if (industryOutput <= 0 || totalMarketOutput <= 0) continue;

                float industryShare = industryOutput * invMarketOutput;

                Industry ind = stats.market.getIndustry(industryID);

                Map<String, Float> inputWeights;
                float sum = 0f;

                inputWeights = IndustryIOs.getInputs(ind, stats.comID, true);
                if (inputWeights.isEmpty()) continue;
                for (float value : inputWeights.values()) {
                    sum += value;
                }

                if (sum <= 0f) continue;

                float industryDeficit = 0f;
                for (Map.Entry<String, Float> inputEntry : inputWeights.entrySet()) {
                    String inputID = inputEntry.getKey();
                    if (IndustryIOs.ABSTRACT_COM.contains(inputID)) continue;
                    
                    CommodityStats inputStats = getComStats(inputID, stats.market.getId());

                    float weightNorm = inputEntry.getValue() / sum;

                    industryDeficit += weightNorm * (1 - inputStats.getStoredCoverageRatio());
                }

                totalDeficit += industryDeficit * industryShare;
            }

            stats.localProdMult = Math.max(1f - totalDeficit, 0.01f);
        }
        }
    }

    private final void resetWorkersAssigned() {
        final WorkerRegistry reg = WorkerRegistry.getInstance();

        for (WorkerIndustryData data : reg.getRegister()) {
            data.setWorkersAssigned(0);
        }
    }

    private final void assignWorkers() {

        final WorkerRegistry reg = WorkerRegistry.getInstance();

        for (MarketAPI market : getMarketsCopy()) {
            if (market.isPlayerOwned() || !market.isInEconomy()) continue;

            final List<Industry> workingIndustries = CommodityStats.getVisibleIndustries(market);
            if (workingIndustries.isEmpty() || !market.hasCondition(WorkerPoolCondition.ConditionID)) {
                continue;
            }

            List<WorkerIndustryData> workerAssignable = new ArrayList<>();

            float totalWorkerAbsorbtionCapacity = 0;
            for (Industry ind : workingIndustries) {
                WorkerIndustryData data = reg.getData(market.getId(), ind.getId());
                if (ind.isFunctional() && data != null) {
                    workerAssignable.add(data);

                    data.setWorkersAssigned(0);

                    IndustryConfig config = IndustryIOs.getIndConfig(ind);

                    totalWorkerAbsorbtionCapacity += config == null ?
                        WorkerRegistry.DEFAULT_WORKER_CAP : config.workerAssignableLimit;
                }
            }
            if (workerAssignable.isEmpty()) continue;

            float workerPerIndustry = 1f / totalWorkerAbsorbtionCapacity;
            if (workerPerIndustry > 1f) workerPerIndustry = 1f;

            for (WorkerIndustryData data : workerAssignable) {
                IndustryConfig config = IndustryIOs.getIndConfig(data.ind);
                float limit = config == null ? WorkerRegistry.DEFAULT_WORKER_CAP : config.workerAssignableLimit;

                data.setWorkersAssigned(limit*workerPerIndustry);
            }
        }
    }

    public final void assignWorkersSmart() {
        // final WorkerRegistry reg = WorkerRegistry.getInstance();

        final Map<String, Map<String, Float>> baseOutputs = IndustryIOs.getBaseOutputs();
        final Map<String, Map<String, Map<String, Float>>> baseInputs = IndustryIOs.getBaseInputs();
        final String key = "::";

        List<String> commodities = getEconCommodities().stream()
                .map(CommoditySpecAPI::getId)
                .collect(Collectors.toList());

        final SettingsAPI settings = Global.getSettings();

        Iterator<String> it = commodities.iterator();
        while (it.hasNext()) {
            String com = it.next();
            boolean remove = true;
            for (Map.Entry<String, Map<String, Float>> entry : IndustryIOs.getBaseOutputs().entrySet()) {
                IndustrySpecAPI spec = settings.getIndustrySpec(entry.getKey());
                if (IndustryIOs.getIndConfig(spec).workerAssignable && entry.getValue().containsKey(com)) {
                    remove = false;
                    break;
                }
            }
            if (remove) it.remove();
        }

        List<String> industries = null;
        { // Memory management
            Set<String> industrySet = new LinkedHashSet<>();
            for (IndustrySpecAPI spec : Global.getSettings().getAllIndustrySpecs()) {
                if (IndustryIOs.hasConfig(spec) || IndustryIOs.hasDynamicConfig(spec)) {
                    if (IndustryIOs.getIndConfig(spec).workerAssignable) {
                        industrySet.add(IndustryIOs.getBaseIndustryID(spec));
                    }
                }
            }
            industries = new ArrayList<>(industrySet);
        }

        // Flatten industries into industry-output pairs
        List<String> industryOutputPairs = new ArrayList<>();
        for (String indID : industries) {
            Map<String, Float> outputs = baseOutputs.get(indID);
            if (outputs != null) {
                for (String outputID : outputs.keySet()) {
                    industryOutputPairs.add(indID + key + outputID);
                }
            }
        }

        double[][] A = new double[commodities.size()][industries.size()];

        for (int j = 0; j < industryOutputPairs.size(); j++) {
            final String pair = industryOutputPairs.get(j);
            final String[] parts = pair.split(key, 2);
            final String indID = parts[0];
            final String outputID = parts[1];

            // Inputs for this output
            Map<String, Map<String, Float>> indInputs = baseInputs.get(indID);
            if (indInputs != null) {
                for (Map<String, Float> inputs : indInputs.values()) {
                    for (Map.Entry<String, Float> inputEntry : inputs.entrySet()) {
                        final String inputCommodity = inputEntry.getKey();
                        final float value = inputEntry.getValue();

                        final int row = commodities.indexOf(inputCommodity);
                        if (row >= 0) {
                            A[row][j] -= value; // negative for consumption
                        }
                    }
                }
            }

            Map<String, Float> indOutputs = baseOutputs.get(indID);
            if (indOutputs != null && indOutputs.containsKey(outputID)) {
                final int row = commodities.indexOf(outputID);
                if (row >= 0) {
                    A[row][j] += indOutputs.get(outputID); // positive for production
                }
            }
        }

        Map<String, Integer> commodityToRow = new HashMap<>();
        for (int i = 0; i < commodities.size(); i++) {
            commodityToRow.put(commodities.get(i), i);
        }

        double[] d = new double[commodities.size()];
        for (String commodityID : commodities) {
            Integer row = commodityToRow.get(commodityID);
            if (row != null) {
                d[row] = getGlobalDemand(commodityID);
            }
        }

        double[] workerVector = WorkerAssignmentSolver.solveLPWithSlack(A, d);

        WorkerAssignmentSolver.logInputMatrix(A, industryOutputPairs, commodities);

        WorkerAssignmentSolver.logDemandVector(d, commodities);

        WorkerAssignmentSolver.logCommodityResults(A, workerVector, commodities);

    }

    public final long getTotalGlobalExports(String comID) {
        long totalGlobalExports = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final long total = getTotalGlobalExports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalExports / (float) total) * 100);
    }

    public final long getTotalGlobalImports(String comID) {
        long totalGlobalImports = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            totalGlobalImports += stats.globalImports;
        }

        return totalGlobalImports;
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final long total = getTotalGlobalImports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalImports / (float) total) * 100);
    }

    public final long getTotalInFactionExports(String comID, FactionAPI faction) {
        long TotalFactionExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            TotalFactionExports += stats.inFactionExports;
        }

        return TotalFactionExports;
    }

    public final long getFactionTotalGlobalExports(String comID, FactionAPI faction) {
        long totalGlobalExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }

            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }

    /**
     * Returns the sum of all the available commodity counts of a market
     */
    public final long getMarketActivity(MarketAPI market) {
        long totalActivity = 0;
        for (CommodityInfo info : m_comInfo.values()) {
            if (!getRegisteredMarkets().contains(market.getId())) {
                registerMarket(market.getId());
            }
            CommodityStats stats = info.getStats(market.getId());

            totalActivity += stats.getAvailable();
        }

        return totalActivity;
    }

    /**
     * Includes over-imports.
     */
    public final float getGlobalTradeRatio(MarketAPI market) {
        if (!market.isInEconomy()) return 0f;

        final double activity = getMarketActivity(market);

        float ratio = 0f;

        for (CommodityInfo info : m_comInfo.values()) {
            CommodityStats stats = info.getStats(market.getId());

            ratio += Math.abs(stats.globalImports - stats.getAvailable()) / activity;
        }

        return ratio;
    }

    public final long getGlobalDemand(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getBaseDemand(false);

        return total;
    }

    /**
     * 1 is no deficit and 0 is max deficit
     */
    public static final Pair<String, Float> getMaxDeficit(MarketAPI market, String... commodityIds) {
		Pair<String, Float> result = new Pair<String, Float>();
		result.two = 1f;
		if (Global.CODEX_TOOLTIP_MODE || !EconomyEngine.isInitialized()) return result;

		for (String id : commodityIds) {
			final CommodityStats stats = EconomyEngine.getInstance().getComStats(id, market.getId());
			if (stats == null) {
				return result;
			}

			float available = stats.getStoredCoverageRatio();

			if (available < result.two) {
				result.one = id;
				result.two = available;
			}
		}
		return result;
	}

    public final void logEconomySnapshot() {
        Global.getLogger(getClass()).info("---- ECONOMY SNAPSHOT START ----");

        for (Map.Entry<String, CommodityInfo> info : m_comInfo.entrySet()) {
            long potencialProd = 0;
            long realProd = 0;
            long potencialDemand = 0;
            long realDemand = 0;
            long available = 0;
            double availabilityRatio = 0f;
            long deficit = 0;
            long globalStockpile = 0;
            long totalExports = 0;
            long inFactionExports = 0;
            long globalExports = 0;

            for (CommodityStats stats : info.getValue().getAllStats()) {
                potencialProd += stats.getLocalProduction(false);
                realProd += stats.getLocalProduction(true);
                potencialDemand += stats.getBaseDemand(false);
                realDemand += stats.getBaseDemand(true);
                available += stats.getAvailable();
                availabilityRatio += stats.getAvailabilityRatio();
                deficit += stats.getDeficit();
                globalStockpile += stats.getStoredAmount();
                totalExports += stats.getTotalExports();
                inFactionExports += stats.inFactionExports;
                globalExports += stats.globalExports;
            }

            availabilityRatio /= (float) info.getValue().getAllStats().size();

            Global.getLogger(getClass()).info("\n"+
                "Commodity: " + info.getKey() + "\n"+
                "potencialProd: " + NumFormat.engNotation(potencialProd) + "\n"+
                "realProd: " + NumFormat.engNotation(realProd) + "\n"+
                "potencialDemand: " + NumFormat.engNotation(potencialDemand) + "\n"+
                "realDemand: " + NumFormat.engNotation(realDemand) + "\n"+
                "available: " + NumFormat.engNotation(available) + "\n"+
                "availabilityRatio: " + availabilityRatio + "\n"+
                "deficit: " + NumFormat.engNotation(deficit) + "\n"+
                "globalStockpile: " + NumFormat.engNotation(globalStockpile) + "\n"+
                "totalExports: " + NumFormat.engNotation(totalExports) + "\n"+
                "inFactionExports: " + NumFormat.engNotation(inFactionExports) + "\n"+
                "globalExports: " + NumFormat.engNotation(globalExports) + "\n"+

                "---------------------------------------"
            );
        }
        
        Global.getLogger(getClass()).info("---- ECONOMY SNAPSHOT END ----");
    }

    public final void logEconomySnapshotAsCSV() {
        StringBuilder csv = new StringBuilder(2048);

        csv.append("Commodity,PotencialProd,RealProd,PotencialDemand,RealDemand,Available,AvailabilityRatio,Deficit,GlobalStockpile,TotalExports,InFactionExports,GlobalExports\n");

        for (Map.Entry<String, CommodityInfo> entry : m_comInfo.entrySet()) {
            String commodity = entry.getKey();
            CommodityInfo info = entry.getValue();

            long potencialProd = 0;
            long realProd = 0;
            long potencialDemand = 0;
            long realDemand = 0;
            long available = 0;
            double availabilityRatio = 0f;
            long deficit = 0;
            long globalStockpile = 0;
            long totalExports = 0;
            long inFactionExports = 0;
            long globalExports = 0;

            Collection<CommodityStats> allStats = info.getAllStats();
            for (CommodityStats stats : allStats) {
                potencialProd += stats.getLocalProduction(false);
                realProd += stats.getLocalProduction(true);
                potencialDemand += stats.getBaseDemand(false);
                realDemand += stats.getBaseDemand(true);
                available += stats.getAvailable();
                availabilityRatio += stats.getAvailabilityRatio();
                deficit += stats.getDeficit();
                globalStockpile += stats.getStoredAmount();
                totalExports += stats.getTotalExports();
                inFactionExports += stats.inFactionExports;
                globalExports += stats.globalExports;
            }

            availabilityRatio /= (double) allStats.size();

            csv.append(String.format("%s,%d,%d,%d,%d,%d,%.4f,%d,%d,%d,%d,%d\n",
                commodity,
                potencialProd,
                realProd,
                potencialDemand,
                realDemand,
                available,
                availabilityRatio,
                deficit,
                globalStockpile,
                totalExports,
                inFactionExports,
                globalExports
            ));
        }
        Global.getLogger(getClass()).info(csv.toString());
    }
}
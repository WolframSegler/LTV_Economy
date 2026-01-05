package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.DebtDebuffTier;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.constants.SubmarketsID;
import wfg.ltv_econ.economy.CommodityStats.PriceType;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryGrouper;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.plugins.LtvEconomyModPlugin;
import wfg.ltv_econ.industry.IndustryGrouper.GroupedMatrix;
import wfg.wrap_ui.util.NumFormat;
import static wfg.ltv_econ.constants.economyValues.*;
import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.graid.CommodityGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidType;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;
import com.fs.starfarer.api.campaign.listeners.GroundRaidObjectivesListener;

/**
 * The {@link EconomyEngine} is the core controller for the LTV_Economy simulation.
 * <p>
 * It manages all economic activity across markets, including:
 * <ul>
 *     <li>Commodity production, demand, and trade flows</li>
 *     <li>Market credit balances and transactions</li>
 *     <li>Worker assignment and labor optimization</li>
 *     <li>Synchronization with the Starsector campaign economy</li>
 * </ul>
 *
 * <h3>Overview</h3>
 * Each {@link MarketAPI} in the sector is represented internally by:
 * <ul>
 *     <li>A credit balance (per-market budget)</li>
 *     <li>A set of active commodities ({@link CommodityInfo})</li>
 *     <li>Dynamic production and demand statistics ({@link CommodityStats})</li>
 * </ul>
 * The engine runs continuously as a listener to the campaign economy. It replaces
 * vanilla credit flows with its own localized financial model, while remaining compatible
 * with other game systems and mods.
 *
 * <h3>Main Responsibilities</h3>
 * <ul>
 *     <li>Maintain lists of all registered and player-owned markets.</li>
 *     <li>Track and update per-market credit balances.</li>
 *     <li>Run the economic update loop (production → demand → trade → post-processing).</li>
 *     <li>Handle market lifecycle events such as colonization, decivilization, and abandonment.</li>
 * </ul>
 *
 * <h3>Internal Structure</h3>
 * <ul>
 *     <li>{@code m_registeredMarkets} – All markets currently part of the simulation.</li>
 *     <li>{@code m_playerMarketData} – Subset of markets owned by the player with unique data attached.</li>
 *     <li>{@code m_marketCredits} – Per-market credit reserves.</li>
 *     <li>{@code m_comInfo} – Mapping of commodity IDs to {@link CommodityInfo} containers.</li>
 *     <li>{@code mainLoopExecutor} – A single-thread executor that runs the simulation asynchronously. Can be toggled.</li>
 * </ul>
 *
 * <h3>Main Loop</h3>
 * The {@link #mainLoop(boolean)} method executes the economic simulation for one tick:
 * <ol>
 *     <li>Refreshes the list of active markets.</li>
 *     <li>Resets and recalculates commodity production/demand via {@link CommodityInfo}.</li>
 *     <li>Assigns workers using a solver-based optimization step.</li>
 *     <li>Executes trade and adjusts credit balances accordingly.</li>
 *     <li>Advances all commodities to persist state.</li>
 * </ol>
 *
 * @author Wolfram Segler
 */
public class EconomyEngine extends BaseCampaignEventListener implements
    EveryFrameScript, PlayerColonizationListener, ColonyDecivListener, GroundRaidObjectivesListener
{

    public static final String EconEngineSerialID = "ltv_econ_econ_engine";
    public static final String KEY = "::";
    private static EconomyEngine instance;
    
    private final Set<String> m_registeredMarkets;
    private final Map<String, PlayerMarketData> m_playerMarketData;
    private final Map<String, Long> m_marketCredits = new HashMap<>();
    private final Map<String, CommodityInfo> m_comInfo;

    private transient ExecutorService mainLoopExecutor;

    public PlayerFactionSettings playerFactionSettings = new PlayerFactionSettings();

    public static EconomyEngine createInstance() {
        if (instance == null) instance = new EconomyEngine();
        return instance;
    }

    public static EconomyEngine loadInstance() {
        final SectorAPI sector = Global.getSector();

        EconomyEngine engine = (EconomyEngine) sector.getPersistentData().get(EconEngineSerialID);

        if (engine != null) {
            instance = engine;
        } else {
            engine = EconomyEngine.createInstance();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(EconomyEngine.class).info("Economy Engine constructed");
            }
        }

        final List<CampaignEventListener> listeners = LtvEconomyModPlugin.getListeners();

        listeners.removeIf(l -> l.getClass() == EconomyEngine.class);
        listeners.add(0, engine);
        sector.addTransientScript(engine);
        sector.getListenerManager().addListener(engine, true);

        return instance;
    }

    public static void setInstance(EconomyEngine a) {
        instance = a;
    }

    public static EconomyEngine getInstance() {
        if (instance == null) return loadInstance();
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private EconomyEngine() {
        super(false);
        m_registeredMarkets = new HashSet<>();
        m_playerMarketData = new HashMap<>();
        m_comInfo = new HashMap<>();

        for (MarketAPI market : getMarketsCopy()) {
            if (!market.isInEconomy()) continue;

            final String marketID = market.getId();
            m_registeredMarkets.add(marketID);
            m_marketCredits.put(marketID, (long) EconomyConfig.STARTING_CREDITS_FOR_MARKET);
            if (market.isPlayerOwned()) m_playerMarketData.put(
                marketID, new PlayerMarketData(marketID)
            );
        }

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            m_comInfo.put(spec.getId(), new CommodityInfo(spec, m_registeredMarkets, this));
        }

        readResolve();
    }

    public final Object readResolve() {
        mainLoopExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LTV-MainLoop");
            t.setDaemon(true);
            return t;
        });

        cyclesSinceWorkerAssign = EconomyConfig.WORKER_ASSIGN_INTERVAL;

        return this;
    }

    protected int dayTracker = -1;
    protected int cyclesSinceWorkerAssign = 0;

    public boolean isDone() { return false;}
    public boolean runWhilePaused() { return false;}
    public final void advance(float delta) {
        final int day = Global.getSector().getClock().getDay();

        if (dayTracker == -1) dayTracker = day;
        
        if (dayTracker == day) return;

        dayTracker = day;

        if (EconomyConfig.MULTI_THREADING) {
            mainLoopExecutor.submit(() -> {
                try {
                    mainLoop(false);
                } catch (Exception e) {
                    Global.getLogger(EconomyEngine.class).error(e);
                }
            });
        } else {
            mainLoop(false);
        }
    }

    public final void fakeAdvance() {
        mainLoop(true);
    }

    /**
     * Advances the economy by one cycle.
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
     *     do not depend on workers. Worker-dependent industries are implicity ignored after
     *     {@link #resetWorkersAssigned} is executed, as they have no demand.
     *   </li>
     *   <li>
     *     <b>assignWorkers()</b> - Assigns workers to worker-dependent industries using a
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
     *     If <code>fakeAdvance</code> is true, this step does nothing. Stockpiles now include produced and
     *     newly imported goods ready for the next cycle.
     *   </li>
     *   <li>
     *     <b>playerMarketData.advance(fakeAdvance)</b> - Updates population statistics of player markets.
     *     If <code>fakeAdvance</code> is true, this step does nothing.
     *   </li>
     * </ol>
     *
     * <p><b>Important notes:</b></p>
     * <ul>
     *   <li>Worker assignment must occur after worker-independent production is updated but before
     *       worker-dependent industries are updated, to ensure demand reflects actual unmet needs.</li>
     *   <li>If <code>fakeAdvance</code> is true, worker assignments and stockpile updates are skipped,
     *       allowing simulation previews without advancing the economy.</li>
     * </ul>
     *
     * @param fakeAdvance If true, simulates a tick without modifying worker assignments or
     *                    stockpiles (useful for interfaces or previews).
     */
    private final void mainLoop(boolean fakeAdvance) {
        refreshMarkets();

        m_comInfo.values().forEach(CommodityInfo::reset);

        if (!fakeAdvance) {
            if (cyclesSinceWorkerAssign >= EconomyConfig.WORKER_ASSIGN_INTERVAL) {
                resetWorkersAssigned(false);
                m_comInfo.values().forEach(CommodityInfo::update);

                assignWorkers();
                cyclesSinceWorkerAssign = 0;
            } else {
                cyclesSinceWorkerAssign++;
            }
        }

        m_comInfo.values().forEach(CommodityInfo::update);
        
        weightedOutputDeficitMods();

        if (!fakeAdvance) m_playerMarketData.values().forEach(PlayerMarketData::advance);

        m_comInfo.values().parallelStream().forEach(info -> info.trade(fakeAdvance));

        if (!fakeAdvance) {
            m_comInfo.values().forEach(CommodityInfo::advance);

            applyWages();
            redistributeFactionCredits(playerFactionSettings.redistributeCredits);
            applyDebtEffects();
        }
    }

    public final void registerMarket(MarketAPI market) {
        final String marketID = market.getId();
        if (!m_registeredMarkets.add(marketID)) return;

        WorkerRegistry.getInstance().register(marketID);
        m_marketCredits.put(marketID, (long) EconomyConfig.STARTING_CREDITS_FOR_MARKET);
        if (market.isPlayerOwned()) m_playerMarketData.put(marketID, new PlayerMarketData(marketID));

        m_comInfo.values().forEach(c -> c.addMarket(marketID));
    }

    public final void removeMarket(MarketAPI market) {
        final String marketID = market.getId();
        if (!m_registeredMarkets.remove(marketID)) return;

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.removeMarket(marketID);
        }

        if (market.isPlayerOwned()) m_playerMarketData.remove(marketID);
        m_marketCredits.remove(marketID);
        WorkerRegistry.getInstance().remove(marketID);
    }

    public final void removeMarket(String marketID) {
        if (!m_registeredMarkets.remove(marketID)) return;

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.removeMarket(marketID);
        }

        m_playerMarketData.remove(marketID);
        m_marketCredits.remove(marketID);
        WorkerRegistry.getInstance().remove(marketID);
    }

    public final void refreshMarkets() {
        final Map<String, MarketAPI> currentMarkets = getMarketsCopy().stream()
            .collect(Collectors.toMap(MarketAPI::getId, m -> m));

        for (MarketAPI market : currentMarkets.values()) {
            registerMarket(market);
        }

        currentMarkets.keySet().removeAll(m_registeredMarkets);

        for (MarketAPI market : currentMarkets.values()) {
            removeMarket(market);
        }
    }

    public Set<String> getRegisteredMarkets() {
        return Collections.unmodifiableSet(m_registeredMarkets);
    }

    public static final List<MarketAPI> getMarketsCopy() {
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        markets.removeIf(m -> !m.isInEconomy());
        return markets;
    }

    public final boolean isPlayerMarket(String marketID) {
        return m_playerMarketData.keySet().contains(marketID);
    }

    public final PlayerMarketData getPlayerMarketData(String marketID) {
        return m_playerMarketData.get(marketID);
    }

    public void reportPlayerOpenedMarket() {
        fakeAdvance();
    }

    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        final MarketAPI market = planet.getMarket();
        registerMarket(market);
        planet.getMarket().addSubmarket(SubmarketsID.STOCKPILES);
        planet.getMarket().removeSubmarket(Submarkets.LOCAL_RESOURCES);
    }

    public void reportPlayerAbandonedColony(MarketAPI market) {
        removeMarket(market);
    }

    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        removeMarket(market);
    }

    public void reportColonyAboutToBeDecivilized(MarketAPI a, boolean b) {}

    public static final List<CommoditySpecAPI> getEconCommodities() {
        return Global.getSettings().getAllCommoditySpecs().stream()
            .filter(spec -> !spec.isNonEcon())
            .collect(Collectors.toList());
    }

    public static final List<String> getEconCommodityIDs() {
        return Global.getSettings().getAllCommoditySpecs().stream()
            .filter(spec -> !spec.isNonEcon())
            .map(CommoditySpecAPI::getId)
            .collect(Collectors.toList());
    }

    public final List<CommodityInfo> getCommodityInfos() {
        return new ArrayList<>(m_comInfo.values());
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
        final float LPV_day = LaborConfig.LPV_day;
        final float RoVC = LaborConfig.getRoVC(tag);

        return (Pout * RoVC) / LPV_day;
    } 

    public final void weightedOutputDeficitMods() {
        m_comInfo.values().stream()
            .flatMap(comInfo -> comInfo.getStatsMap().values().stream())
            .parallel()
            .forEach(this::computeStatsDeficits);
    }

    private final void computeStatsDeficits(CommodityStats stats) {
        float totalDeficit = 0f;
        final float totalMarketOutput = stats.getProduction(false);
        final float invMarketOutput = 1f / totalMarketOutput;

        for (Map.Entry<String, MutableStat> industryEntry : stats.getFlowProdIndStats().entrySet()) {
            final String industryID = industryEntry.getKey();
            final MutableStat industryStat = industryEntry.getValue();

            final float industryOutput = industryStat.getModifiedValue();
            if (industryOutput <= 0 || totalMarketOutput <= 0) continue;

            final float industryShare = industryOutput * invMarketOutput;

            final Industry ind = stats.market.getIndustry(industryID);

            Map<String, Float> inputWeights;
            float sum = 0f;

            inputWeights = IndustryIOs.getRealInputs(ind, stats.comID, true);
            if (inputWeights.isEmpty()) continue;
            for (float value : inputWeights.values()) {
                sum += value;
            }

            if (sum <= 0f) continue;

            float industryDeficit = 0f;
            for (Map.Entry<String, Float> inputEntry : inputWeights.entrySet()) {
                String inputID = inputEntry.getKey();
                if (IndustryIOs.ABSTRACT_COM.contains(inputID)) continue;
                
                final CommodityStats inputStats = getComStats(inputID, stats.market.getId());

                final float weightNorm = inputEntry.getValue() / sum;

                industryDeficit += weightNorm * (1 - inputStats.getStoredAvailabilityRatio());
            }

            totalDeficit += industryDeficit * industryShare;
        }

        stats.getProductionStat().modifyMult(
            "deficits", Math.max(1f - totalDeficit, 0.01f), "Input shortages"
        );
    }

    private final void resetWorkersAssigned(boolean resetPlayerIndustries) {
        final WorkerRegistry reg = WorkerRegistry.getInstance();

        for (WorkerIndustryData data : reg.getRegister()) {
            if (!resetPlayerIndustries && data.market.isPlayerOwned()) continue;
            data.resetWorkersAssigned();
        }
    }

    public final void assignWorkers() {
        final WorkerRegistry reg = WorkerRegistry.getInstance();

        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();
        final List<String> industryOutputPairs = IndustryMatrix.getIndustryOutputPairs();
        final GroupedMatrix A = IndustryGrouper.getStaticGrouping();

        Map<String, Integer> commodityToRow = new HashMap<>();
        for (int i = 0; i < commodities.size(); i++) {
            commodityToRow.put(commodities.get(i), i);
        }

        final double[] d = new double[commodities.size()];
        for (String commodityID : commodities) {
            Integer row = commodityToRow.get(commodityID);
            if (row != null) {
                d[row] = getGlobalDemand(commodityID);
            }
        }

        final List<List<Integer>> outputsPerMarket = new ArrayList<>();
        final List<MarketAPI> markets = getMarketsCopy();
        markets.removeIf(m -> m.isPlayerOwned());

        for (int i = 0; i < markets.size(); i++) {
            final List<Integer> outputIndexes = new ArrayList<>();
            final MarketAPI market = markets.get(i);

            for (Industry ind : CommodityStats.getVisibleIndustries(market)) {
                if (!ind.isFunctional()) continue;

                final IndustryConfig config = IndustryIOs.getIndConfig(ind);
                if (!config.workerAssignable) continue;

                final String indID = IndustryIOs.getBaseIndIDifNoConfig(ind.getSpec());

                for (String outputID : IndustryIOs.getIndConfig(ind).outputs.keySet()) {
                    if (!CompatLayer.hasRelevantCondition(outputID, market)) continue;
                    if (!IndustryIOs.isOutputValidForMarket(
                        config.outputs.get(outputID), market, outputID
                    )) continue;

                    for (int j = 0; j < industryOutputPairs.size(); j++) {
                        String pair = industryOutputPairs.get(j);

                        if (pair.equals(indID + KEY + outputID)) {
                            outputIndexes.add(j);
                        }
                    }
                }
            }
            outputsPerMarket.add(outputIndexes);
        }

        final double[] workerVector = WorkforcePlanner.calculateGlobalWorkerTargets(A.reducedMatrix, d);

        final Map<MarketAPI, float[]> assignedWorkersPerMarket = WorkforcePlanner.allocateWorkersToMarkets(
            workerVector, markets, industryOutputPairs, outputsPerMarket, A
        );

        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.entrySet()) {
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(entry.getKey());
            if (cond == null) continue;

            final String marketID = entry.getKey().getId();
            final float[] assignments = entry.getValue();
            final long totalWorkers = cond.getWorkerPool();

            for (int i = 0; i < industryOutputPairs.size(); i++) {
                if (assignments[i] == 0) continue;

                final String[] indAndOutputID = industryOutputPairs.get(i).split(Pattern.quote(KEY), 2);
                final String indID = indAndOutputID[0];
                final String outputID = indAndOutputID[1];

                final float ratio = (assignments[i] / totalWorkers);

                reg.getData(marketID, indID).setRatioForOutput(outputID, ratio);
            }
        }
    }

    public final void applyWages() {
        final EconomyAPI econ = Global.getSector().getEconomy(); 
        for (String marketID : m_registeredMarkets) {
            final MarketAPI market = econ.getMarket(marketID);
            if (market == null) {
                removeMarket(marketID);
                continue;
            }
            addCredits(marketID, (int) -getWagesForMarket(econ.getMarket(marketID)));
        }
    }

    public final void redistributeFactionCredits(boolean includePlayerFaction) {
        final float REDISTRIBUTION_STRENGTH = 0.2f;
        final int DAYS_AFTER_REDISTRIBUTION = 30;

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!includePlayerFaction && faction.isPlayerFaction()) continue;

            final ArrayList<MarketAPI> markets = new ArrayList<>();
            for (MarketAPI market : getMarketsCopy()) {
                if (market.getDaysInExistence() < DAYS_AFTER_REDISTRIBUTION) continue;
                if (market.getFaction().equals(faction)) markets.add(market);
            }
            if (markets.size() < 2) continue;

            double weightedCredits = 0;
            double totalWeight = 0;

            for (MarketAPI market : markets) {
                final double weight = Math.pow(10, market.getSize() - 3);
                weightedCredits += getCredits(market.getId()) * weight;
                totalWeight += weight;
            }
            if (totalWeight <= 0) continue;

            final long weightedAvg = (long) (weightedCredits / totalWeight);

            for (MarketAPI market : markets) {
                final long credits = getCredits(market.getId());
                final long diff = weightedAvg - credits;
                addCredits(market.getId(), (long) (diff * REDISTRIBUTION_STRENGTH));
            }
        }
    }

    public final void applyDebtEffects() {
        for (MarketAPI market : getMarketsCopy()) {
            final long credits = getCredits(market.getId());
            DebtDebuffTier appliedTier = null;

            for (DebtDebuffTier tier : EconomyConfig.DEBT_DEBUFF_TIERS) {
                if (credits < tier.threshold()) appliedTier = tier;
                else break;
            }

            final String src = "ltv_econ_debt_debuff";

            if (appliedTier == null) {
                market.getStability().unmodify(src);
                market.getUpkeepMult().unmodify(src);
                market.getIncoming().getWeight().unmodify(src);
            } else {
                market.getStability().modifyFlat(
                    src,
                    appliedTier.stabilityPenalty(),
                    "market debt"
                );
                market.getUpkeepMult().modifyPercent(
                    src,
                    appliedTier.upkeepMultiplierPercent(),
                    "inefficiencies caused by market debt"
                );
                market.getIncoming().getWeight().modifyFlat(
                    src,
                    appliedTier.immigrationModifier(),
                    "Unattractiveness caused by market debt"
                );
            }
        }
    }

    public final double getTotalGlobalExports(String comID) {
        double total = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) 
        total += stats.globalExports;

        return total;
    }

    public final double getTotalFactionExports(String comID) {
        double total = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats())
        total += stats.inFactionExports;

        return total;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final double total = getTotalGlobalExports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalExports / (float) total) * 100);
    }

    public final double getTotalGlobalImports(String comID) {
        double totalGlobalImports = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            totalGlobalImports += stats.globalImports;
        }

        return totalGlobalImports;
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final double total = getTotalGlobalImports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalImports / (float) total) * 100);
    }

    public final double getTotalInFactionExports(String comID, FactionAPI faction) {
        double TotalFactionExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            TotalFactionExports += stats.inFactionExports;
        }

        return TotalFactionExports;
    }

    public final float getFactionTotalExportMarketShare(String comID, String factionID) {
        final double total = getTotalGlobalExports(comID);
        if (total == 0) return 0;
        double totalGlobalExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(factionID)) {
                continue;
            }
            totalGlobalExports += stats.globalExports;
        }
        return (float) totalGlobalExports / (float) total;
    }

    public final float getFactionTotalImportMarketShare(String comID, String factionID) {
        final double total = getTotalGlobalImports(comID);
        if (total == 0) return 0;
        double totalGlobalImports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(factionID)) {
                continue;
            }
            totalGlobalImports += stats.globalImports;
        }
        return (float) (totalGlobalImports / total);
    }

    public final double getFactionTotalGlobalExports(String comID, FactionAPI faction) {
        double totalGlobalExports = 0;

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
    public final double getMarketActivity(MarketAPI market) {
        double totalActivity = 0;
        for (CommodityInfo info : m_comInfo.values()) {
            if (!getRegisteredMarkets().contains(market.getId())) {
                registerMarket(market);
            }
            CommodityStats stats = info.getStats(market.getId());

            totalActivity += stats.getFlowAvailable();
        }

        return totalActivity;
    }

    public static final long getGlobalWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);
            if (cond == null) continue;

            total += cond.getWorkerPool();
        }
        return total;
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

            ratio += Math.abs(stats.globalImports - stats.getFlowAvailable()) / activity;
        }

        return ratio;
    }

    public final long getGlobalDemand(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getBaseDemand(false);

        return total;
    }

    public final long getGlobalProduction(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getProduction(true);

        return total;
    }

    public final long getGlobalSurplus(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getFlowCanNotExport();

        return total;
    }

    public final long getGlobalDeficit(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getFlowDeficit();

        return total;
    }
    
    public final long getGlobalTradeVolume(String comID) {
        long total = 0;

        for (CommodityStats stats : getCommodityInfo(comID).getAllStats())
        total += stats.getTotalExports();

        return total;
    }

    public final float getGlobalAveragePrice(String comID, int units) {
        float total = 0;

        final Collection<CommodityStats> allStats = getCommodityInfo(comID).getAllStats();
        for (CommodityStats stats : allStats)
        total += stats.getUnitPrice(PriceType.NEUTRAL, units);

        return total / (float) allStats.size();
    }

    public final long getGlobalStockpiles(String comID) {
        double total = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats())
        total += stats.getStored();

        return (long) total;
    }

    public final void addCredits(String marketID, long amount) {
        long current = m_marketCredits.getOrDefault(marketID, 0l);

        if (amount > 0 && current > Long.MAX_VALUE - amount) {
            current = Long.MAX_VALUE;

        } else if (amount < 0 && current < Long.MIN_VALUE - amount) {
            current = Long.MIN_VALUE;

        } else {
            current += amount;
        }

        m_marketCredits.put(marketID, current);
    }

    /**
     * Returns 0 if no market is registered
     */
    public final long getCredits(String marketID) {
        return m_marketCredits.getOrDefault(marketID, 0l);
    }

    /**
     * Works properly only for player colonies.
     *
     * <p>{@code getIncomeMult()} does not affect trade income. It represents
     * administrative efficiency and applies only to abstract income, upkeep,
     * and wage-related systems.</p>
     */
    public final long getNetIncome(MarketAPI market, boolean lastMonth) {
        final long exportIncome = getExportIncome(market, lastMonth);
        final long importCost = getImportExpense(market, lastMonth);
        final int wageCost = (int) getWagesForMarket(market)*MONTH;
        final int indIncome = getIndustryIncome(market);
        final int indUpkeep = getIndustryUpkeep(market);
        final int hazardPay = market.isImmigrationIncentivesOn() ?
            (int) market.getImmigrationIncentivesCost() : 0;

        return exportIncome + indIncome - hazardPay - importCost - wageCost - indUpkeep;
    }

    /*
     * Works properly only for player colonies. 
     */
    public final long getExportIncome(MarketAPI market, boolean lastMonth) {
        long exportIncome = 0;
        if (m_playerMarketData.keySet().contains(market.getId())) {
            for (CommodityInfo info : m_comInfo.values()) {
                final IncomeLedger ledger = info.getLedger(market.getId());
                exportIncome += lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
            }
        }

        return exportIncome;
    }

    public final long getExportIncome(MarketAPI market, String comID, boolean lastMonth) {
        if (m_playerMarketData.keySet().contains(market.getId())) {

            final IncomeLedger ledger = m_comInfo.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
        }
        return 0;
    }

    /*
     * Works properly only for player colonies. 
     */
    public final long getImportExpense(MarketAPI market, boolean lastMonth) {
        long importCost = 0;
        if (m_playerMarketData.keySet().contains(market.getId())) {
            for (CommodityInfo info : m_comInfo.values()) {
                final IncomeLedger ledger = info.getLedger(market.getId());
                importCost += lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
            }
        }

        return importCost;
    }

    public final long getImportExpense(MarketAPI market, String comID, boolean lastMonth) {
        if (m_playerMarketData.keySet().contains(market.getId())) {
            final IncomeLedger ledger = m_comInfo.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
        }
        return 0;
    }

    /**
     * Per day value
     */
    public final float getWagesForMarket(MarketAPI market) {
        final String marketID = market.getId();
        float wage = 0f;

        for (WorkerIndustryData data : WorkerRegistry.getInstance().getIndustriesUsingWorkers(marketID)) {
            wage += data.getWorkersAssigned() * (LaborConfig.LPV_day /
                (isPlayerMarket(marketID) ? m_playerMarketData.get(marketID).getRoSV() : LaborConfig.RoSV));
        }

        return wage * market.getUpkeepMult().getModifiedValue();
    }

    public final int getIndustryIncome(MarketAPI market) {
        float income = 0f;
        for (Industry ind : market.getIndustries()) {
            income += getIndustryIncome(ind, market).getModifiedValue();
        }

        return (int) income;
    }

    public final int getIndustryUpkeep(MarketAPI market) {
        float upkeep = 0f;
        for (Industry ind : market.getIndustries()) {
            upkeep += getIndustryUpkeep(ind, market).getModifiedValue();
        }

        return (int) upkeep;
    }

    public final MutableStat getIndustryIncome(Industry ind, MarketAPI market) {
        final MutableStat income = new MutableStat(ind.getSpec().getIncome());
        income.modifyMult("market_size", market.getSize() - 2);
        income.modifyMult("market_income_mods", market.getIncomeMult().getModifiedValue());

        return income;
    }

    public final MutableStat getIndustryUpkeep(Industry ind, MarketAPI market) {
        final MutableStat upkeep = new MutableStat(ind.getSpec().getUpkeep());
        upkeep.modifyMult("market_size", market.getSize() - 2);
		upkeep.modifyMultAlways(
            "ind_hazard", market.getUpkeepMult().getModifiedValue(), "Market upkeep multiplier"
        );

        return upkeep;
    }

    private transient Set<String> turnedOffIncentiveMarkets = new HashSet<>();
    @Override
    public void reportEconomyTick(int iterIndex) {
        final EconomyAPI econ = Global.getSector().getEconomy();

        if (turnedOffIncentiveMarkets == null) turnedOffIncentiveMarkets = new HashSet<>();
        turnedOffIncentiveMarkets.clear();

        for (String marketID : m_playerMarketData.keySet()) {
            final MarketAPI market = econ.getMarket(marketID);
            if (market == null) {
                m_playerMarketData.remove(marketID);
                continue;
            }
            for (Industry ind : CommodityStats.getVisibleIndustries(market)) {

                ind.getIncome().unmodify();
                ind.getIncome().base = 0f;
                ind.getUpkeep().unmodify();
                ind.getUpkeep().base = 0f;
            }

            /**
             * TODO: Player Export Income Nullification
             *
             * Context:
             * - Export income is computed dynamically via CommodityMarketData and MarketShareData.
             *
             * Observations:
             * - CommodityOnMarketAPI.getExportIncome() = exportMarketShare * getMarketValue() * incomeMult
             * - MarketShareData is created on-demand in CommodityMarketData.getMarketShareData().
             * - Modifying exportMarketShare or marketValue can zero player exports but may break:
             *     - Colony crises events
             *     - AI trade decisions
             *     - Global trade calculations
             *
             * Risks:
             * - Zeroing exportMarketShare may prevent events from triggering.
             * - Changes are per tick; may be overridden by vanilla updates.
             * - Direct reflection modification may be reverted if applied at wrong timing.
             *
             * Notes / Potential Solutions:
             * - Modify the entries of this.marketValuePerFaction inside CommodityMarketData instead.
             * - Modify market.getIncomeMult() instead.
             */
            for (CommodityOnMarketAPI com : market.getAllCommodities()) {
                com.getCommodityMarketData().getMarketShareData(market).setExportMarketShare(0);
            }

            if (market.isImmigrationIncentivesOn()) {
                turnedOffIncentiveMarkets.add(marketID);
                market.setImmigrationIncentivesOn(false);
            }
        }
    }

    /**
     * This method runs after {@link #reportEconomyTick}. Practically the same method but delayed.
     */
    @Override
    public void reportEconomyMonthEnd() {
        final EconomyAPI econ = Global.getSector().getEconomy();

        for (String marketID : turnedOffIncentiveMarkets) {
            econ.getMarket(marketID).setImmigrationIncentivesOn(true);
        }

        for (CommodityInfo info : m_comInfo.values()) {
            info.endMonth();
        }

        final MonthlyReport report = SharedData.getData().getCurrentReport();
        FDNode marketsNode = report.getNode(MonthlyReport.OUTPOSTS);
		marketsNode.name = "Colonies";
		marketsNode.custom = MonthlyReport.OUTPOSTS;
		marketsNode.tooltipCreator = report.getMonthlyReportTooltip();

        for (PlayerMarketData data : m_playerMarketData.values()) {
            final MarketAPI market = data.market;
            final long netIncome = getNetIncome(market, true);
            final float r = data.getEffectiveProfitRatio();
            final float playerIncome = Math.max(netIncome, 0) * r;
            addCredits(data.marketID, (long) -playerIncome);

            
            final FDNode mNode = report.getNode(marketsNode, market.getId());
            mNode.name = market.getName() + " (" + market.getSize() + ")";
            mNode.custom = market;

            // Player cut node
            final FDNode playerIncomeNode = report.getNode(mNode, "player_share");
            playerIncomeNode.name = "Effective player share (" + Math.round(r * 100) + "%)";
            playerIncomeNode.icon = Global.getSettings().getSpriteName("icons", "player_ratio");
            playerIncomeNode.income = 0.0001f;
            playerIncomeNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    tp.addPara(
                        "The ratio of monthly profits that get automatically transferred to you: %s.",
                        pad, highlight, NumFormat.formatCredit((long) playerIncome)
                    );
                    tp.addPara(
                        "The effective value can be below the chosen value if the colony is in debt.", 
                        pad
                    );
                    tp.addPara("All income values are modified by this value", pad);
                }
            };

            if (playerIncome < 1) {
                playerIncomeNode.name += " - none";
            }

            // Industries & structures
            final FDNode indNode = report.getNode(mNode, "industries"); 
            indNode.name = "Industries & structures";
            indNode.custom = MonthlyReport.INDUSTRIES;
            indNode.mapEntity = market.getPrimaryEntity();
            indNode.tooltipCreator = report.getMonthlyReportTooltip();
            for (Industry ind : market.getIndustries()) {
                final FDNode iNode = report.getNode(indNode, ind.getId());
                iNode.name = ind.getCurrentName();
                iNode.income += getIndustryIncome(ind, market).getModifiedInt() * r;
                iNode.upkeep += getIndustryUpkeep(ind, market).getModifiedInt() * r;
                iNode.custom = ind;
                iNode.mapEntity = market.getPrimaryEntity();
            }

            // Exports
            final FDNode exportNode = report.getNode(mNode, "exports"); 
            exportNode.name = "Exports";
            exportNode.custom = MonthlyReport.EXPORTS;
            exportNode.mapEntity = market.getPrimaryEntity();
            exportNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    final String inFactionBonus = String.format("%d%%",
                        (int)(1f - EconomyConfig.FACTION_EXCHANGE_MULT)*100);

                    tp.addPara(
                        "Income from exports by this outpost or colony. " +
                        "Smuggling exports do not produce income. " +
                        "In-faction imports are %s cheaper than normal",
                        pad, highlight, inFactionBonus
                    );
                }
            };
            for (CommoditySpecAPI com : getEconCommodities()) {
                final FDNode eNode = report.getNode(exportNode, com.getId());
                final String comID = com.getId();
                eNode.name = com.getName();
                eNode.income += getExportIncome(market, comID, true) * r;
                eNode.upkeep += getImportExpense(market, comID, true) * r;
                eNode.mapEntity = market.getPrimaryEntity();
            }

            // Wages
            final FDNode wageNode = report.getNode(mNode, "wages"); 
            wageNode.name = "Wages";
            wageNode.mapEntity = market.getPrimaryEntity();
            wageNode.upkeep += getWagesForMarket(market)*MONTH * r;
            wageNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    tp.addPara(
                        "Monthly wages for workers at this colony.", pad
                    );
                }
            };
        }
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

			float available = stats.getStoredAvailabilityRatio();

			if (available < result.two) {
				result.one = id;
				result.two = available;
			}
		}
		return result;
	}

    public void modifyRaidObjectives(MarketAPI market, SectorEntityToken entity,
        List<GroundRaidObjectivePlugin> objectives, RaidType type, int marineTokens, int priority
    ) {}
	
	public void reportRaidObjectivesAchieved(RaidResultData data, InteractionDialogAPI dialog,
        Map<String, MemoryAPI> memoryMap
    ) {
        if (!data.market.isInEconomy()) return;
        for (GroundRaidObjectivePlugin objective : data.objectives) {
            if (objective instanceof CommodityGroundRaidObjectivePluginImpl obj) {
                if (!m_comInfo.containsKey(objective.getId())) continue;
                final CommodityStats stats = getComStats(obj.getId(), data.market.getId());
                stats.addStoredAmount(-obj.getQuantityLooted());
            }
        }
    }

    public void reportFleetSpawned(CampaignFleetAPI fleet) {

    }

    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {

    }

    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpDestination to) {

    }

    public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {
        
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
                potencialProd += stats.getProduction(false);
                realProd += stats.getProduction(true);
                potencialDemand += stats.getBaseDemand(false);
                realDemand += stats.getBaseDemand(true);
                available += stats.getFlowAvailable();
                availabilityRatio += stats.getFlowAvailabilityRatio();
                deficit += stats.getFlowDeficit();
                globalStockpile += stats.getStored();
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
                potencialProd += stats.getProduction(false);
                realProd += stats.getProduction(true);
                potencialDemand += stats.getBaseDemand(false);
                realDemand += stats.getBaseDemand(true);
                available += stats.getFlowAvailable();
                availabilityRatio += stats.getFlowAvailabilityRatio();
                deficit += stats.getFlowDeficit();
                globalStockpile += stats.getStored();
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

    public final void logCreditsSnapshot() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n=== Market Credits Snapshot ===\n");

        m_marketCredits.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .forEach(entry -> sb.append(entry.getKey())
            .append(": ")
            .append(entry.getValue())
            .append(" credits\n"));

        sb.append("=== End Snapshot ===");
        Global.getLogger(EconomyEngine.class).info(sb.toString());
    }
}
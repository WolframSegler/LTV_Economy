package wfg.ltv_econ.economy.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.DebtDebuffTier;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.IndustryMatrix;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkforcePlanner;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryGrouper;
import wfg.ltv_econ.industry.IndustryGrouper.GroupedMatrix;
import wfg.ltv_econ.industry.IndustryIOs;

public class EconomyLoop {
    public static final String KEY = "::";

    transient EconomyEngine engine;
    private int cyclesSinceWorkerAssign = 0;

    public EconomyLoop(EconomyEngine engine) { this.engine = engine; }

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
     *     <b>CommodityDomain.reset()</b> - Resets per-commodity values such as imports, exports, and
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
     *     <b>CommodityDomain.update()</b> - Updates all commodities to account for the newly assigned workers,
     *     converting production into LTV-based stats, calculating base demand, and updating local production.
     *   </li>
     *   <li>
     *     <b>weightedOutputDeficitMods()</b> - Adjusts local production based on input deficits.
     *     Only considers stockpiles available at the start of this cycle. This throttling
     *     ensures industries cannot consume unavailable inputs.
     *   </li>
     *   <li>
     *     <b>CommodityDomain.trade()</b> - Handles exports of surplus commodities and imports to
     *     fill storage for the next cycle. Trade occurs after production so industries
     *     always consume existing stockpiles. Imports feed storage for future production.
     *   </li>
     *   <li>
     *     <b>CommodityDomain.advance(fakeAdvance)</b> - Updates stockpiles after production and trade.
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
    final void mainLoop(boolean fakeAdvance, boolean forceWorkerAssignment) {
        refreshMarkets();

        engine.m_comDomains.values().forEach(CommodityDomain::reset);

        if (!fakeAdvance || forceWorkerAssignment) {
            if (cyclesSinceWorkerAssign >= EconomyConfig.WORKER_ASSIGN_INTERVAL || forceWorkerAssignment) {

                WorkerRegistry.getInstance().resetWorkersAssigned(false);
                engine.m_comDomains.values().forEach(CommodityDomain::update);
                assignWorkers();

                cyclesSinceWorkerAssign = 0;
            } else {
                cyclesSinceWorkerAssign++;
            }
        }

        engine.m_comDomains.values().forEach(CommodityDomain::update);
        
        weightedOutputDeficitMods();

        if (!fakeAdvance) engine.m_playerMarketData.values().forEach(PlayerMarketData::advance);

        engine.m_comDomains.values().parallelStream().forEach(dom -> dom.trade(fakeAdvance));

        if (!fakeAdvance) {
            engine.m_comDomains.values().forEach(CommodityDomain::advance);

            applyWages();
            redistributeFactionCredits(engine.playerFactionSettings.redistributeCredits);
            applyDebtEffects();
        }
    }

    public final void refreshMarkets() {
        final Set<String> economyMarketIDs = EconomyInfo.getMarketsCopy().stream()
            .map(MarketAPI::getId)
            .collect(Collectors.toSet());
        final Set<String> registeredMarkets = new HashSet<>(engine.m_registeredMarkets);

        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (!registeredMarkets.contains(market.getId())) engine.registerMarket(market);
        }

        for (String marketID : registeredMarkets) {
            if (!economyMarketIDs.contains(marketID)) engine.removeMarket(marketID);
        }
    }

    public final void assignWorkers() {
        final WorkerRegistry reg = WorkerRegistry.getInstance();

        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();
        final List<String> industryOutputPairs = IndustryMatrix.getIndustryOutputPairs();
        final var indOutputPairToColumn = IndustryMatrix.getIndOutputPairToColumnMap();
        final GroupedMatrix A = IndustryGrouper.getStaticGrouping();

        Map<String, Integer> commodityToRow = new HashMap<>();
        for (int i = 0; i < commodities.size(); i++) {
            commodityToRow.put(commodities.get(i), i);
        }

        final double[] d = new double[commodities.size()];
        for (String commodityID : commodities) {
            Integer row = commodityToRow.get(commodityID);
            if (row != null) {
                d[row] = engine.info.getGlobalDemand(commodityID);
            }
        }

        final List<List<Integer>> outputsPerMarket = new ArrayList<>();
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        markets.removeIf(m -> m.isPlayerOwned());

        for (int i = 0; i < markets.size(); i++) {
            final List<Integer> outputIndexes = new ArrayList<>();
            final MarketAPI market = markets.get(i);

            for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
                if (!ind.isFunctional()) continue;

                final IndustryConfig config = IndustryIOs.getIndConfig(ind);
                if (!config.workerAssignable) continue;

                final String indID = IndustryIOs.getBaseIndIDifNoConfig(ind.getSpec());

                for (String outputID : IndustryIOs.getIndConfig(ind).outputs.keySet()) {
                    if (!CompatLayer.hasRelevantCondition(outputID, market)) continue;
                    if (!IndustryIOs.isOutputValidForMarket(config.outputs.get(outputID), ind, outputID)) continue;

                    final int idx = indOutputPairToColumn.getOrDefault(indID + KEY + outputID, -1);
                    if (idx != -1) outputIndexes.add(idx);
                }
            }
            outputsPerMarket.add(outputIndexes);
        }

        final double[] workerVector = WorkforcePlanner.calculateGlobalWorkerTargets(A.reducedMatrix, d);

        final Map<MarketAPI, float[]> assignedWorkersPerMarket = WorkforcePlanner.allocateWorkersToMarkets(
            workerVector, markets, industryOutputPairs, outputsPerMarket, A
        );

        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.entrySet()) {
            final MarketAPI market = entry.getKey();
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);

            final float[] assignments = entry.getValue();
            final long totalWorkers = cond.getWorkerPool();

            for (int i = 0; i < industryOutputPairs.size(); i++) {
                if (assignments[i] == 0) continue;

                final String[] indAndOutputID = industryOutputPairs.get(i).split(Pattern.quote(KEY), 2);

                final float ratio = (assignments[i] / totalWorkers);
                final String baseInd = IndustryIOs.getBaseIndustryID(indAndOutputID[0]);
                WorkerIndustryData data = reg.getData(market.getId(), baseInd);
                if (data == null) {
                    reg.register(market);
                    data = reg.getData(market.getId(), baseInd);
                }
                data.setRatioForOutput(indAndOutputID[1], ratio);
            }
        }
    }

    public final void weightedOutputDeficitMods() {
        engine.m_comDomains.values().stream()
            .flatMap(dom -> dom.getCellsMap().values().stream())
            .parallel()
            .forEach(this::computeComDeficits);
    }

    private final void computeComDeficits(CommodityCell cell) {
        float totalDeficit = 0f;
        final float totalMarketOutput = cell.getProduction(false);
        final float invMarketOutput = 1f / totalMarketOutput;

        for (Map.Entry<String, MutableStat> industryEntry : cell.getFlowProdIndStats().entrySet()) {
            final String industryID = industryEntry.getKey();
            final MutableStat industryStat = industryEntry.getValue();

            final float industryOutput = industryStat.getModifiedValue();
            if (industryOutput <= 0 || totalMarketOutput <= 0) continue;

            final float industryShare = industryOutput * invMarketOutput;

            final Industry ind = cell.market.getIndustry(industryID);

            Map<String, Float> inputWeights;
            float sum = 0f;

            inputWeights = IndustryIOs.getRealInputs(ind, cell.comID, true);
            if (inputWeights.isEmpty()) continue;
            for (float value : inputWeights.values()) {
                sum += value;
            }

            if (sum <= 0f) continue;

            float industryDeficit = 0f;
            for (Map.Entry<String, Float> inputEntry : inputWeights.entrySet()) {
                String inputID = inputEntry.getKey();
                if (IndustryIOs.ABSTRACT_COM.contains(inputID)) continue;
                
                final CommodityCell inputCell = engine.getComCell(inputID, cell.market.getId());

                final float weightNorm = inputEntry.getValue() / sum;

                industryDeficit += weightNorm * (1 - inputCell.getStoredAvailabilityRatio());
            }

            totalDeficit += industryDeficit * industryShare;
        }

        if (totalDeficit > 0f) {
            cell.getProductionStat().modifyMult(
                "deficits", Math.max(1f - totalDeficit, 0.01f), "Input shortages"
            );
        } else cell.getProductionStat().unmodifyMult("deficits");
    }

    public final void applyWages() {
        final EconomyAPI econ = Global.getSector().getEconomy(); 
        final List<String> toRemove = new ArrayList<>(4);

        for (String marketID : engine.m_registeredMarkets) {
            if (econ.getMarket(marketID) == null) {
                toRemove.add(marketID);
            } else {
                engine.addCredits(marketID, (int) -engine.info.getWagesForMarket(econ.getMarket(marketID)));
            }
        }

        for (String marketID : toRemove) {
            engine.removeMarket(marketID);
        }
    }

    public void redistributeFactionCredits(boolean includePlayerFaction) {
        final double REDISTRIBUTION_STRENGTH = 0.2;
        final int DAYS_AFTER_REDISTRIBUTION = 30;

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!includePlayerFaction && faction.isPlayerFaction()) continue;

            final List<MarketAPI> markets = new ArrayList<>();
            for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
                if (market.getDaysInExistence() < DAYS_AFTER_REDISTRIBUTION) continue;
                if (market.getFaction().equals(faction)) markets.add(market);
            }
            if (markets.size() < 2) continue;

            final int n = markets.size();
            long totalCredits = 0l;
            long totalWeight = 0l;
            long[] weights = new long[n];
            long[] credits = new long[n];

            for (int i = 0; i < n; i++) {
                MarketAPI m = markets.get(i);
                final long w = (long) Math.pow(10, m.getSize() - 3);
                weights[i] = w;
                totalWeight += w;

                final long c = engine.getCredits(m.getId());
                credits[i] = c;
                totalCredits += c;
            }
            if (totalWeight <= 0l) continue;

            for (int i = 0; i < n; i++) {
                final double desired = totalCredits * (weights[i] / totalWeight);
                final double diff = desired - credits[i];
                final long change = Math.round(diff * REDISTRIBUTION_STRENGTH);

                engine.addCredits(markets.get(i).getId(), change);
            }
        }
    }

    public final void applyDebtEffects() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            final long credits = engine.getCredits(market.getId());
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
}
package wfg.ltv_econ.economy.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.WorkforceAllocator.FeasibilityResult;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.industry.CompatLayer;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.ui.economyTab.industryBalancer.IndustryBalancerHolo;
import wfg.native_ui.util.ArrayMap;

public final class IndustryBalancer {
    public enum BalancerStatus { RUNNING, SUCCESS, CYCLE, STUCK }
    public enum ChangeType { ADDED, REMOVED, UPGRADED }
    public enum MoveType { ADD_INDUSTRY, FREE_SLOT, }

    public record IndustryChange(
        MarketAPI market,
        String sourceIndustryId,
        String targetIndustryId,
        ChangeType type
    ) {}

    public record IndustryCandidate(
        MarketAPI market,
        Industry industry,
        BitSet outputs
    ) {}

    public record CommoditySlack(
        String comId,
        double slack
    ) {}

    public static final class BalancerState {
        private volatile BalancerStatus status = BalancerStatus.RUNNING;
        private volatile int iteration = 0;
        private volatile String currentComId = null;
        private volatile MoveType currentMoveType = MoveType.ADD_INDUSTRY;
        private volatile double totalSlack = 0d;
        private volatile Throwable mFailure = null;
        private volatile boolean sigStop = false;
        private volatile List<CommoditySlack> topSlack = List.of();
        private final List<IndustryChange> changes = new ArrayList<>();

        public final BalancerStatus status() { return status; }
        public final int iteration() { return iteration; }
        public final String currentComId() { return currentComId; }
        public final MoveType currentMoveType() { return currentMoveType; }
        public final double totalSlack() { return totalSlack; }
        public final List<CommoditySlack> topSlack() { return topSlack; }
        public final Throwable failure() { return mFailure; }
        public final void sigstop() { sigStop = true; }

        public final List<IndustryChange> changes() {
            if (status == BalancerStatus.RUNNING) throw new IllegalStateException("Cannot view changes while the balancer is running");
            return changes;
        }
    }

    public  static final double SLACK_TOLERANCE = 1d;

    private static final int MAX_ITERATIONS = 500;
    private static final double MIN_WORKERS = 50d;
    private static final double USELESS_USAGE_FRACTION  = 0.1d;
    private static final double SLOT_SATURATION_THRESHOLD = 1.2d;
    private static final long FNV_OFFSET = 0xcbf29ce484222325l;
    private static final long FNV_PRIME  = 0x100000001b3l;

    private final BalancerState mState = new BalancerState();
    private volatile boolean mStarted = false;

    public static final boolean showIfSlackDetected() {
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        markets.removeIf(MarketAPI::isPlayerOwned);

        final FeasibilityResult result = WorkforceAllocator.optimizePiecewiseWorkerAlloc(
            markets, ProductionMatrix.getMatrix(),
            ProductionMatrix.getIndustryOutputPairs(),
            PlanningData.getOutputsPerMarket(markets),
            false
        );

        if (worstCommoditySlackIndex(result.slack()) < 0) return false;

        new IndustryBalancerHolo().showBalancerDialog(false);
        return true;
    }

    public final void runAsync(boolean includePlayerColonies) {
        if (mStarted) throw new IllegalStateException("Cannot start a new run while a run is in progress.");

        final Thread t = new Thread(() -> {
            try {
                WorkerRegistry.instance().resetWorkersAssigned(false);
                EconomyEngine.instance().getComDomains().forEach(CommodityDomain::update);

                mState.status = balance(includePlayerColonies);

                EconomyEngine.instance().fakeAdvanceWithAssignWorkers();

            } catch (Throwable ex) {
                mState.mFailure = ex;
                mState.status = BalancerStatus.STUCK;
                throw ex;
            }
        }, "IndustryBalancer");

        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public final BalancerState state() { return mState; }

    private final BalancerStatus balance(boolean includePlayerColonies) {
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        if (!includePlayerColonies) markets.removeIf(MarketAPI::isPlayerOwned);

        final double[][] A = ProductionMatrix.getMatrix();
        final List<String> outputPairs = ProductionMatrix.getIndustryOutputPairs();
        final Set<Long> visited = new HashSet<>();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            mState.iteration = iteration;

            if (mState.sigStop) break;

            if (!visited.add(stateHash(markets))) return BalancerStatus.CYCLE;

            final FeasibilityResult result = WorkforceAllocator.optimizePiecewiseWorkerAlloc(
                markets, A, outputPairs, PlanningData.getOutputsPerMarket(markets), false
            );
            mState.totalSlack = sumArray(result.slack());
            mState.topSlack = topSlackCommodities(result.slack(), 3);
            
            final int worst = worstCommoditySlackIndex(result.slack());
            if (worst < 0) return BalancerStatus.SUCCESS;

            final int[] ordered = commoditiesWithSlackDescending(result.slack());

            boolean committed = false;
            for (final int candidate : ordered) {
                mState.currentComId = ProductionMatrix.getWorkerRelatedCommodityIDs().get(candidate);

                mState.currentMoveType = MoveType.ADD_INDUSTRY;
                if (tryAddWithPhase(candidate, result, markets, A, 1)) { committed = true; break; }

                mState.currentMoveType = MoveType.FREE_SLOT;
                if (tryAddWithPhase(candidate, result, markets, A, 2)) { committed = true; break; }
            }

            if (!committed) return BalancerStatus.STUCK;
        }
        return BalancerStatus.STUCK;
    }

    private final void recordChange(MarketAPI market, String sourceIndId, String targetIndId, ChangeType type) {
        mState.changes.add(new IndustryChange(market, sourceIndId, targetIndId, type));
    }

    private boolean tryAddWithPhase(
        int worstCommIdx, FeasibilityResult result, List<MarketAPI> markets,
        double[][] A, int phase
    ) {
        final String targetComId = ProductionMatrix.getWorkerRelatedCommodityIDs().get(worstCommIdx);
        final ArrayList<IndustryCandidate> candidates = getCandidates(targetComId, result, markets, phase);
        if (candidates.isEmpty()) return false;

        sortCandidates(candidates, result.slack(), result, A);

        for (final IndustryCandidate candidate : candidates) {
            final MarketAPI market = candidate.market();
            final String targetId = candidate.industry().getSpec().getId();
            final boolean needsSlot = candidate.industry().isIndustry();
            final boolean hasFreeSlot = Misc.getMaxIndustries(market) - Misc.getNumIndustries(market) > 0;
            final IndustrySpecAPI predecessorSpec = getPredecessor(targetId, market);
            final Industry predecessor = predecessorSpec != null
                ? market.getIndustry(predecessorSpec.getId()) : null;

            if (phase == 1) {
                if (needsSlot && !hasFreeSlot) {
                    if (predecessorSpec == null) continue;
                    if (predecessor == null && !predecessorSpec.hasTag(Industries.TAG_INDUSTRY)) continue;
                    if (predecessor != null && !predecessor.isIndustry()) continue;
                }
            } else {
                final Industry removable = findRemovableIndustry(market, result.assignments(), predecessorSpec);
                if (removable == null) continue;
                recordChange(market, removable.getId(), null, ChangeType.REMOVED);
                market.removeIndustry(removable.getId(), null, false);
            }

            if (predecessorSpec == null) {
                recordChange(market, null, targetId, ChangeType.ADDED);
                market.addIndustry(targetId);
                return true;
            }
            if (predecessor == null) {
                recordChange(market, predecessorSpec.getId(), targetId, ChangeType.UPGRADED);
                market.getConstructionQueue().removeItem(predecessorSpec.getId());
                market.addIndustry(targetId);
                return true;
            }
            recordChange(market, predecessorSpec.getId(), targetId, ChangeType.UPGRADED);
            market.addIndustry(targetId);
            final Industry added = market.getIndustry(targetId);
            copyIndustryState(predecessor, added);
            market.removeIndustry(predecessorSpec.getId(), null, false);

            return true;
        }
        return false;
    }

    private static final void copyIndustryState(final Industry from, final Industry to) {
        to.setAICoreId(from.getAICoreId());
        to.setSpecialItem(from.getSpecialItem());
        to.setImproved(from.isImproved());
    }

    private static final Industry findRemovableIndustry(
        final MarketAPI market, final ArrayMap<MarketAPI, float[]> assignments, final IndustrySpecAPI excluded
    ) {
        final String excludedId = excluded != null ? excluded.getId() : null;
        for (final Industry ind : market.getIndustries()) {
            if (!ind.isIndustry()) continue; // must free a slot
            if (excludedId != null && ind.getSpec().getId().equals(excludedId)) continue;
            if (!isBalancerEligible(ind.getSpec())) continue;
            if (!IndustryConfigManager.getIndConfig(ind).workerAssignable) continue;
            if (!isDeadweight(ind, assignments)) continue;
            return ind;
        }
        return null;
    }

    private static final boolean isDeadweight(
        final Industry ind, final ArrayMap<MarketAPI, float[]> assignments
    ) {
        final MarketAPI market = ind.getMarket();
        final double poolSize = WorkerPoolRegistry.get(market).getWorkerPool();
        final double threshold = Math.max(MIN_WORKERS, poolSize * USELESS_USAGE_FRACTION);

        final float[] arr = assignments.get(market);
        final BitSet cols = PlanningData.getOutputForIndustry(ind);
        double indUsage = 0d;
        for (int col = cols.nextSetBit(0); col >= 0; col = cols.nextSetBit(col + 1)) {
            indUsage += arr[col];
        }

        double slotsFrac = 0d;
        for (Industry i : market.getIndustries()) {
            final IndustryConfig config = IndustryConfigManager.getIndConfig(i);
            if (!config.workerAssignable) continue;
            for (OutputConfig output : config.outputs.values()) {
                slotsFrac += output.workerAssignableLimit;
            }
        }
        if (slotsFrac > SLOT_SATURATION_THRESHOLD
                && spareWorkersAt(market, assignments) + indUsage < MIN_WORKERS) {
            return false;
        }

        return indUsage < threshold;
    }

    private static final ArrayList<IndustryCandidate> getCandidates(
        String targetComId, FeasibilityResult result, List<MarketAPI> markets, int phase
    ) {
        // this list is made using getBaseIndIDifNoConfig, so meaningless upgrades are excluded.
        final ArrayList<String> industries = ProductionMatrix.getWorkerIndustries();
        final ArrayList<IndustryCandidate> candidates = new ArrayList<>(32);

        for (MarketAPI market : markets) {
            if (spareWorkersAt(market, result.assignments()) < MIN_WORKERS) continue;

            for (String indId : industries) {

                if (isPresent(indId, market)) continue;
                if (getSuccessor(indId, market) != null) continue;
                if (!isBalancerEligible(settings.getIndustrySpec(indId))) continue;
                if (!IndustryIOs.hasOutput(indId, targetComId)) continue;

                final Industry instance = market.instantiateIndustry(indId);
                final BitSet columns = PlanningData.getOutputForIndustry(instance);
                final Integer colIdx = ProductionMatrix.getIndOutputPairToColumnMap().get(indId + EconomyLoop.KEY + targetComId);
                if (colIdx == null || !columns.get(colIdx)) continue;
                
                final boolean needsSlot = instance.isIndustry();
                final boolean hasFreeSlot = Misc.getMaxIndustries(market) - Misc.getNumIndustries(market) > 0;
                final IndustrySpecAPI predecessor = getPredecessor(indId, market);

                if (phase == 1 && needsSlot) {
                    if (!hasFreeSlot && predecessor != null &&
                        !predecessor.hasTag(Industries.TAG_INDUSTRY)
                    ) continue;

                    if (predecessor == null && !hasFreeSlot) continue;
                    
                } else if (phase == 2) {
                    if (!needsSlot || hasFreeSlot) continue;
                    if (predecessor != null && predecessor.hasTag(Industries.TAG_INDUSTRY)) continue;
                }

                candidates.add(new IndustryCandidate(market, instance, columns));
            }
        }

        return candidates;
    }

    private static final void sortCandidates(
        final List<IndustryCandidate> candidates, final double[] slack,
        final FeasibilityResult result, final double[][] A
    ) {
        final List<String> pairs = ProductionMatrix.getIndustryOutputPairs();
        final int n = candidates.size();
        final double[] scores = new double[n];

        for (int i = 0; i < n; i++) scores[i] = scoreCandidate(candidates.get(i), slack, result, A, pairs);

        final Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(scores[b], scores[a]));

        final List<IndustryCandidate> sorted = new ArrayList<>(n);
        for (int i : indices) sorted.add(candidates.get(i));
        candidates.clear();
        candidates.addAll(sorted);
    }

    private static final double scoreCandidate(
        final IndustryCandidate move, final double[] slack, final FeasibilityResult result,
        final double[][] A, final List<String> pairs
    ) {
        final double spare = spareWorkersAt(move.market(), result.assignments());
        final Industry ind = move.industry();
        final BitSet cols = move.outputs();

        double score = 0d;
        for (int col = cols.nextSetBit(0); col >= 0; col = cols.nextSetBit(col + 1)) {
            final String pair = pairs.get(col);
            final String outputID = pair.substring(pair.indexOf(EconomyLoop.KEY) + EconomyLoop.KEY.length());
            final double mod = CompatLayer.getModifiersMult(ind, outputID, false);

            double columnValue = 0d;
            for (int c = 0; c < A.length; c++) {
                final double coeff = A[c][col];
                if (coeff != 0d) columnValue += coeff * slack[c];
            }
            score += mod * columnValue;
        }
        return spare * score;
    }

    private static final double spareWorkersAt(MarketAPI market, ArrayMap<MarketAPI, float[]> assignments) {
        final double totalWorkers = WorkerPoolRegistry.get(market).getWorkerPool();

        double total = 0d;
        for (float assigned : assignments.get(market)) total += assigned;
        return totalWorkers - total;
    }

    private static final IndustrySpecAPI getSuccessor(String industryId, MarketAPI market)  {
        IndustrySpecAPI candidate = settings.getIndustrySpec(industryId);

        while (true) {
            final String next = candidate.getUpgrade();
            if (next == null) return null;
            candidate = settings.getIndustrySpec(next);
            if (isPresent(next, market)) return candidate;
        }
    }

    private static final IndustrySpecAPI getPredecessor(String industryId, MarketAPI market) {
        IndustrySpecAPI candidate = settings.getIndustrySpec(industryId);

        while (true) {
            final String next = candidate.getDowngrade();
            if (next == null || next.equals(candidate.getId())) return null;
            candidate = settings.getIndustrySpec(next);
            if (isPresent(next, market)) return candidate;
        }
    }

    private static final boolean isPresent(String industryId, MarketAPI market) {
        // Some mods make it so that ind.getId() and ind.getSpec().getId() are different. So this method is used instead of market.getIndustry();
        for (Industry ind : market.getIndustries()) {
            if (ind.getSpec().getId().equals(industryId)) return true;
        }

        return market.getConstructionQueue().getItem(industryId) != null;
    }

    private static final int worstCommoditySlackIndex(final double[] slack) {
        if (slack.length == 0) return -1;

        int idx = 0;
        for (int i = 1; i < slack.length; i++) {
            if (slack[idx] < slack[i]) idx = i;
        }

        return slack[idx] > SLACK_TOLERANCE ? idx : -1; // ignore up to 1 slack.
    }

    private static final int[] commoditiesWithSlackDescending(final double[] slack) {
        return IntStream.range(0, slack.length)
            .filter(i -> slack[i] > SLACK_TOLERANCE)
            .boxed().sorted((a, b) -> Double.compare(slack[b], slack[a]))
            .mapToInt(Integer::intValue).toArray();
    }

    private static final List<CommoditySlack> topSlackCommodities(final double[] slack, final int n) {
        final List<String> ids = ProductionMatrix.getWorkerRelatedCommodityIDs();
        return IntStream.range(0, slack.length)
            .filter(c -> slack[c] > SLACK_TOLERANCE).boxed()
            .sorted((a, b) -> Double.compare(slack[b], slack[a]))
            .limit(n)
            .map(c -> new CommoditySlack(ids.get(c), slack[c]))
            .collect(Collectors.toList());
    }

    private static final boolean isBalancerEligible(final IndustrySpecAPI spec) {
        return !spec.hasTag(Tags.CODEX_UNLOCKABLE);
    }

    private static final long fnv1a(long h, final String s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            h ^= c & 0xFF;
            h *= FNV_PRIME;
            h ^= (c >>> 8) & 0xFF;
            h *= FNV_PRIME;
        }
        return h;
    }

    private static final long stateHash(final List<MarketAPI> markets) {
        final Set<String> workerIndustries = new HashSet<>(ProductionMatrix.getWorkerIndustries());
        long h = FNV_OFFSET;

        for (final MarketAPI market : markets) {
            final List<String> specIds = new ArrayList<>();
            for (final Industry ind : market.getIndustries()) {
                final IndustrySpecAPI spec = ind.getSpec();
                if (!workerIndustries.contains(IndustryConfigManager.getBaseIndIDifNoConfig(spec))) continue;
                specIds.add(spec.getId());
            }
            Collections.sort(specIds);

            h = fnv1a(h, market.getId());
            h = fnv1a(h, "=");
            for (final String id : specIds) {
                h = fnv1a(h, id);
                h = fnv1a(h, ",");
            }
            h = fnv1a(h, ";");
        }

        return h;
    }

    private static final double sumArray(final double[] arr) {
        double sum = 0d;
        for (int i = 0; i < arr.length; i++) sum += arr[i];
        return sum;
    }
}
package wfg.ltv_econ.economy.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.WorkforceAllocator.FeasibilityResult;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.native_ui.util.ArrayMap;

// TODO test this
public final class IndustryBalancer {
    private IndustryBalancer() {};
    
    public record IndustryCandidate(
        MarketAPI market,
        Industry industry,
        BitSet outputs
    ) {}

    private static final int MAX_ITERATIONS = 200;
    private static final double MIN_WORKERS = 50d;
    private static final long FNV_OFFSET = 0xcbf29ce484222325l;
    private static final long FNV_PRIME  = 0x100000001b3l;

    public static final void balance(boolean includePlayerColonies) {
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        if (!includePlayerColonies) markets.removeIf(MarketAPI::isPlayerOwned);

        final double[][] A = ProductionMatrix.getMatrix();
        final List<String> outputPairs = ProductionMatrix.getIndustryOutputPairs();
        final Set<Long> visited = new HashSet<>();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            if (!visited.add(stateHash(markets))) return; // cycle detected

            final FeasibilityResult result = WorkforceAllocator.solveFeasibilityLP(
                markets, A, outputPairs, PlanningData.getOutputsPerMarket(markets)
            );

            final int worst = worstCommoditySlackIndex(result.slack());
            if (worst < 0) return; // no slack left, success

            if (tryAdd(worst, result, markets, A)) continue;
            if (tryFreeSlotAndAdd(worst, result, markets, A)) continue;

            return; // no possible change
        }
    }

    private static final boolean tryAdd(int worstCommIdx, FeasibilityResult result, List<MarketAPI> markets, double[][] A) {
        final String targetComId = ProductionMatrix.getWorkerRelatedCommodityIDs().get(worstCommIdx);
        final ArrayList<IndustryCandidate> candidates = getCandidates(targetComId, result, markets, 1);
        if (candidates.isEmpty()) return false;

        sortCandidates(candidates, result.slack(), result, A);

        for (final IndustryCandidate candidate : candidates) {
            final MarketAPI market = candidate.market();
            final String targetId = candidate.industry().getSpec().getId();
            final boolean needsSlot = candidate.industry().isIndustry();
            final boolean hasFreeSlot = Misc.getMaxIndustries(market) - Misc.getNumIndustries(market) > 0;
            final IndustrySpecAPI predecessorSpec = getPredecessor(targetId, market);

            if (predecessorSpec == null) {
                if (needsSlot && !hasFreeSlot) continue;
                market.addIndustry(targetId);
                return true;
            }

            final Industry predecessor = market.getIndustry(predecessorSpec.getId());

            if (predecessor == null) { // predecessor in queue
                final boolean predWasTakingSlot = predecessorSpec.hasTag(Industries.TAG_INDUSTRY);
                final int netDelta = (needsSlot ? 1 : 0) - (predWasTakingSlot ? 1 : 0);
                if (netDelta > 0 && !hasFreeSlot) continue;

                market.getConstructionQueue().removeItem(predecessorSpec.getId());
                market.addIndustry(targetId);
                return true;
            }

            final boolean predNeedsSlot = predecessor.isIndustry();
            if (!predNeedsSlot && needsSlot && !hasFreeSlot) continue;

            market.addIndustry(targetId);
            final Industry added = market.getIndustry(targetId);

            copyIndustryState(predecessor, added);
            market.removeIndustry(predecessorSpec.getId(), null, false);
            return true;
        }

        return false;
    }

    private static final boolean tryFreeSlotAndAdd(int worstCommIdx, FeasibilityResult result, List<MarketAPI> markets, double[][] A) {
        final String targetComId = ProductionMatrix.getWorkerRelatedCommodityIDs().get(worstCommIdx);
        final ArrayList<IndustryCandidate> candidates = getCandidates(targetComId, result, markets, 2);
        if (candidates.isEmpty()) return false;

        sortCandidates(candidates, result.slack(), result, A);

        for (final IndustryCandidate candidate : candidates) {
            final MarketAPI market = candidate.market();
            final String targetId = candidate.industry().getSpec().getId();
            final IndustrySpecAPI predecessorSpec = getPredecessor(targetId, market);

            final Industry removable = findRemovableIndustry(market, result.assignments(), predecessorSpec);
            if (removable == null) continue;

            market.removeIndustry(removable.getId(), null, false);

            if (predecessorSpec == null) {
                market.addIndustry(targetId);
                return true;
            }

            final Industry predecessor = market.getIndustry(predecessorSpec.getId());

            if (predecessor == null) {
                market.getConstructionQueue().removeItem(predecessorSpec.getId());
                market.addIndustry(targetId);
                return true;
            }

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
            if (!isDeadweight(ind, assignments)) continue;
            return ind;
        }
        return null;
    }

    private static final boolean isDeadweight(
        final Industry ind, final ArrayMap<MarketAPI, float[]> assignments
    ) {
        final float[] arr = assignments.get(ind.getMarket());
        final BitSet cols = PlanningData.getOutputForIndustry(ind);
        for (int col = cols.nextSetBit(0); col >= 0; col = cols.nextSetBit(col + 1)) {
            if (arr[col] >= MIN_WORKERS) return false;
        }
        return true;
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

        return slack[idx] > 1d ? idx : -1; // ignore up to 1 slack.
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
}
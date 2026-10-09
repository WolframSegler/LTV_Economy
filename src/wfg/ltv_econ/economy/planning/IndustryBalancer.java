package wfg.ltv_econ.economy.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.planning.WorkforceAllocator.FeasibilityResult;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry.WorkerPool;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.Manufacturing;

public final class IndustryBalancer {
    private IndustryBalancer() {};
    
    public record AddMove(
        MarketAPI market,
        String industryId
    ) {}

    public record RemoveMove(
        MarketAPI market,
        String industryId
    ) {}

    private static final int MAX_ITERATIONS = 200;
    private static final double MIN_WORKERS = 100d;
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

            if (tryAdd(worst, result, markets)) continue;
            if (tryFreeSlotAndAdd(worst, result, markets)) continue;

            return; // no possible change
        }
    }

    private static final ArrayList<AddMove> getCandidates(
        String targetComId, FeasibilityResult result, List<MarketAPI> markets, int phase
    ) {
        // this list is made using getBaseIndIDifNoConfig, so meaningless upgrades are excluded.
        final ArrayList<String> industries = ProductionMatrix.getWorkerIndustries();
        final Map<String, List<String>> inputToOutput = IndustryIOs.getInputToOutput();
        final ArrayList<AddMove> candidates = new ArrayList<>(32);

        for (MarketAPI market : markets) {
            final WorkerPool pool = WorkerPoolRegistry.get(market);
            if (pool.getWorkerPool() * pool.getFreeWorkerRatio() < MIN_WORKERS) continue;

            for (String indId : industries) {

                if (isPresent(indId, market)) continue;
                if (getSuccessor(indId, market) != null) continue;
                final Industry instance = market.instantiateIndustry(indId);
                if (!IndustryIOs.isOutputActive(instance, targetComId)) continue;
                
                final boolean needsSlot = settings.getIndustrySpec(indId).hasTag(Industries.TAG_INDUSTRY);
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

                candidates.add(new AddMove(market, indId));
            }
        }

        return candidates;
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
package wfg_ltv_econ.economy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityInfo {
    private final CommoditySpecAPI m_spec;
    private final Map<String, CommodityStats> m_comStats = new HashMap<>();

    public CommodityInfo(
        CommoditySpecAPI spec
    ) {
        m_spec = spec;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            m_comStats.put(market.getId(), new CommodityStats(m_spec.getId(), market));
        }
    }

    public final void advance(boolean fakeAdvance) {
        for (Map.Entry<String, CommodityStats> stats : m_comStats.entrySet()) {
            stats.getValue().advance(fakeAdvance);
        }
    }

    public final void reset() {
        for (Map.Entry<String, CommodityStats> stats : m_comStats.entrySet()) {
            stats.getValue().resetTradeValues();
        }
    }

    public final void addMarket(MarketAPI market) {
        if (m_comStats.containsKey(market.getId())) return;
        
        m_comStats.put(market.getId(), new CommodityStats(m_spec.getId(), market));
    }

    public final CommodityStats getStats(MarketAPI market) {

        return m_comStats.get(market.getId());
    }

    public final Collection<CommodityStats> getAllStats() {
        return m_comStats.values();
    }

    // inside CommodityInfo
    // private static final int TOP_K_PER_EXPORTER = 12; // tune: 8..20 is reasonable

    // private static class PairOffer {
    //     final CommodityStats exporterStats;
    //     final CommodityStats importerStats;
    //     final float score;

    //     PairOffer(CommodityStats exporterStats, CommodityStats importerStats, float score) {
    //         this.exporterStats = exporterStats;
    //         this.importerStats = importerStats;
    //         this.score = score;
    //     }
    // }

    // public void performGlobalTradeForCommodity() {
    //     // 1) Gather exporters and importers (tick-pre-trade numbers)
    //     List<CommodityStats> exporters = new ArrayList<>();
    //     List<CommodityStats> importers = new ArrayList<>();

    //     for (CommodityStats s : m_comStats.values()) {
    //         long exportable = s.getTotalExportable();
    //         long deficitPre = s.getDeficitPreTrade();
    //         if (exportable > 0) exporters.add(s);
    //         if (deficitPre > 0) importers.add(s);
    //     }

    //     if (exporters.isEmpty() || importers.isEmpty()) return;

    //     // Build quick lookup maps for remaining amounts
    //     Map<CommodityStats, Long> exporterRemaining = new HashMap<>();
    //     Map<CommodityStats, Long> importerRemaining = new HashMap<>();
    //     for (CommodityStats e : exporters) exporterRemaining.put(e, e.getTotalExportable());
    //     for (CommodityStats r : importers) importerRemaining.put(r, r.getDeficitPreTrade());

    //     // Priority queue of pair offers sorted by highest score first
    //     PriorityQueue<PairOffer> pq = new PriorityQueue<>(
    //         (a, b) -> Float.compare(b.score, a.score)
    //     );

    //     // 2) For each exporter, generate candidate importer pairs (prune & keep top-K)
    //     for (CommodityStats exporterStats : exporters) {
    //         MarketAPI exporterMarket = exporterStats.market;

    //         // get candidate importer markets logically (your existing method)
    //         List<MarketAPI> candidateMarkets = EconomyEngine.getInstance()
    //             .getMarketsImportingCom(m_com.getCommodity(), exporterMarket, false);

    //         // Filter to only importers that are actually in our importer list (have deficit)
    //         // and map to their CommodityStats
    //         List<CommodityStats> candidateStats = new ArrayList<>();
    //         for (MarketAPI cm : candidateMarkets) {
    //             CommodityStats s = EconomyEngine.getInstance().getComStats(m_com.getId(), cm);
    //             if (s != null && importerRemaining.containsKey(s)) candidateStats.add(s);
    //         }

    //         if (candidateStats.isEmpty()) continue;

    //         // Compute scores for these candidate pairs
    //         // We create a small list and select top-K by score
    //         List<PairOffer> localOffers = new ArrayList<>(candidateStats.size());
    //         for (CommodityStats impStats : candidateStats) {
    //             float score = computePairScore(exporterStats, impStats, m_com.getCommodity());
    //             // Ignore extremely low/negative scores if desired:
    //             if (score <= Float.NEGATIVE_INFINITY) continue; // placeholder; or use a threshold
    //             localOffers.add(new PairOffer(exporterStats, impStats, score));
    //         }

    //         // sort localOffers descending by score and push top-K into global pq
    //         localOffers.sort((a,b) -> Float.compare(b.score, a.score));
    //         int limit = Math.min(TOP_K_PER_EXPORTER, localOffers.size());
    //         for (int i=0; i<limit; i++) {
    //             pq.add(localOffers.get(i));
    //         }
    //     }

    //     // 3) Process PQ: allocate best matches first
    //     while (!pq.isEmpty()) {
    //         PairOffer offer = pq.poll();
    //         CommodityStats exp = offer.exporterStats;
    //         CommodityStats imp = offer.importerStats;

    //         Long expRemL = exporterRemaining.get(exp);
    //         Long impRemL = importerRemaining.get(imp);
    //         long expRem = expRemL == null ? 0L : expRemL;
    //         long impRem = impRemL == null ? 0L : impRemL;

    //         if (expRem <= 0 || impRem <= 0) {
    //             continue; // one side exhausted, skip
    //         }

    //         long amount = Math.min(expRem, impRem);
    //         if (amount <= 0) continue;

    //         // apply allocation
    //         if (exp.market.getFaction().equals(imp.market.getFaction())) {
    //             exp.addInFactionExport((int) amount);   // or addInFactionExport(long) if you have it
    //             imp.addInFactionImport((int) amount);
    //         } else {
    //             exp.addGlobalExport((int) amount);
    //             imp.addGlobalImport((int) amount);
    //         }

    //         expRem -= amount;
    //         impRem -= amount;
    //         exporterRemaining.put(exp, expRem);
    //         importerRemaining.put(imp, impRem);

    //         // Note: we do NOT push new offers here because we precomputed top-K per exporter.
    //         // If you want re-queuing behavior (e.g. if importer still needs and another exporter could serve),
    //         // those would already be present in the PQ from other exporter passes.
    //     }
    // }

    // private float computePairScore(CommodityStats exporterStats, CommodityStats importerStats, 
    //     CommoditySpecAPI spec) {
    //     MarketAPI exporter = exporterStats.market;
    //     MarketAPI importer = importerStats.market;

    //     float score = 0f;
    //     // in-faction
    //     if (importer.getFaction().equals(exporter.getFaction())) {
    //         score += TradeWeights.IN_FACTION;
    //     }

    //     score += relationFactor(exporter, importer) * TradeWeights.POLITICAL;
    //     if (hasTradeAgreement(exporter, importer)) score += TradeWeights.TRADE_AGREEMENTS;

    //     score += accessibilityFactor(importer) * TradeWeights.ACCESSIBILITY;
    //     score += priceFactor(spec, importer) * TradeWeights.LOCAL_PRICE;
    //     score += distanceFactor(importer, exporter) * TradeWeights.DISTANCE;
    //     score += deficitFactor(importerStats) * TradeWeights.LOCAL_DEFICIT_RATIO;
    //     score += sizeFactor(importer) * TradeWeights.MARKET_SIZE;

    //     return score;
    // }
}
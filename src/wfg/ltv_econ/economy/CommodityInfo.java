package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

public class CommodityInfo {
    private final String comID;
    private final Map<String, CommodityStats> m_comStats = new HashMap<>();

    public CommodityInfo(
        CommoditySpecAPI spec, Set<String> registeredMarkets
    ) {
        comID = spec.getId();

        for (String marketID : registeredMarkets) {
            m_comStats.put(marketID, new CommodityStats(comID, marketID));
        }
    }

    public final void advance(boolean fakeAdvance) {
        m_comStats.values().parallelStream().forEach(stats -> stats.advance(fakeAdvance));
    }

    public final void reset() {
        m_comStats.values().parallelStream().forEach(CommodityStats::reset);
    }

    public final void update() {
        m_comStats.values().parallelStream().forEach(CommodityStats::update);
    }

    public final void addMarket(String marketID) {
        m_comStats.putIfAbsent(marketID, new CommodityStats(comID, marketID));
    }

    public final void removeMarket(String marketID) {
        m_comStats.remove(marketID);
    }

    public final CommodityStats getStats(String marketID) {
        return m_comStats.get(marketID);
    }

    public final Collection<CommodityStats> getAllStats() {
        return m_comStats.values();
    }

    public final Map<String, CommodityStats> getStatsMap() {
        return m_comStats;
    }

    public static final Pair<String, String> getPairFromIndex(int index, List<MarketAPI> exporters,     
        List<MarketAPI> importers) {

        final int numImporters = importers.size();
        final int exporterIndex = index / numImporters;
        final int importerIndex = index % numImporters;

        final String exporter = exporters.get(exporterIndex).getId();
        final String importer = importers.get(importerIndex).getId();

        return new Pair<>(exporter, importer);
    }

    public final void trade() {
        List<MarketAPI> importers = getImporters();
        List<MarketAPI> exporters = getExporters();

        final int pairCount = importers.size() * exporters.size();

        int[] pairScores = new int[pairCount];
        int[] indices = new int[pairCount];

        for (int expInd = 0; expInd < exporters.size(); expInd++) {
            for (int impInd = 0; impInd < importers.size(); impInd++) {
                pairScores[expInd * importers.size() + impInd] = computePairScore(
                    exporters.get(expInd), importers.get(impInd)
                );
            }
        }

        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        radixSortIndices(indices, pairScores);

        for (int i = 0; i < indices.length; i++) {
            Pair<String, String> expImp = getPairFromIndex(
                indices[i], exporters, importers
            );

            CommodityStats expStats = getStats(expImp.one);
            CommodityStats impStats = getStats(expImp.two);

            long exportableRemaining = computeExportableRemaining(expStats);
            long deficitRemaining = computeProjectedImportAmount(impStats);

            if (exportableRemaining < 1 || deficitRemaining < 1) continue;

            boolean sameFaction = expStats.market.getFaction().equals(impStats.market.getFaction());

            long amountToSend = Math.min(exportableRemaining, deficitRemaining);

            if(sameFaction) {
                expStats.addInFactionExport(amountToSend);
                impStats.addInFactionImport(amountToSend);
            } else {
                expStats.addGlobalExport(amountToSend);
                impStats.addGlobalImport(amountToSend);
            }
        }
    }

    public final List<MarketAPI> getImporters() {
        List <MarketAPI> importers = new ArrayList<>(50);

        for (CommodityStats stats : m_comStats.values()) {

            if (stats.getDeficitPreTrade() > 0) {
                importers.add(stats.market);
            }
        }

        return importers;
    }

    public final List<MarketAPI> getExporters() {
        List <MarketAPI> exporters = new ArrayList<>(50);

        for (CommodityStats stats : m_comStats.values()) {

            if (stats.getBaseExportable() > 0 && stats.getDeficitPreTrade() <= 0) {
                exporters.add(stats.market);
            }
        }

        return exporters;
    }

    /**
     * Sorts the indices array in-place based on pairScores using radix sort with radix = 256.
     * Descending order: highest score first.
     */
    public static void radixSortIndices(int[] indices, int[] pairScores) {
        int n = indices.length;
        int[] output = new int[n];

        int maxScore = 0;
        for (int i = 0; i < n; i++) {
            if (pairScores[indices[i]] > maxScore) maxScore = pairScores[indices[i]];
        }

        int[] count = new int[256];
        int shift = 0; // start with least significant byte

        while ((maxScore >> shift) != 0) {
            for (int i = 0; i < 256; i++) count[i] = 0;

            for (int i = 0; i < n; i++) {
                int byteVal = (pairScores[indices[i]] >> shift) & 0xFF;
                count[byteVal]++;
            }

            for (int i = 254; i >= 0; i--) {
                count[i] += count[i + 1];
            }

            for (int i = 0; i < n; i++) {
                int byteVal = (pairScores[indices[i]] >> shift) & 0xFF;
                int pos = count[byteVal] - 1;
                output[pos] = indices[i];
                count[byteVal]--;
            }

            System.arraycopy(output, 0, indices, 0, n);

            shift += 8; // next byte
        }
    }

    public final int computePairScore(MarketAPI exporter, MarketAPI importer) {

        int score = 0;

        if (importer.getFaction().equals(exporter.getFaction())) {
            score += TradeWeights.IN_FACTION;
        }

        score += relationFactor(exporter, importer) * TradeWeights.POLITICAL;

        // if (hasTradeAgreement(exporter, importers)) {
        //     score += TradeWeights.TRADE_AGREEMENTS;
        // }

        score += accessibilityFactor(exporter, importer) * TradeWeights.ACCESSIBILITY;

        score += distanceFactor(exporter, importer) * TradeWeights.DISTANCE;

        score += sizeFactor(importer) * TradeWeights.MARKET_SIZE;

        score += priceFactor(comID, importer) * TradeWeights.LOCAL_PRICE;

        return score;
    }

    private static final float relationFactor(MarketAPI exporter, MarketAPI importer) {
        final float rel = exporter.getFaction().getRelationship(importer.getFaction().getId()); // [-1,1]
        final float relNorm = (rel + 1f) / 2f;
        final float weight = (float) Math.pow(relNorm, 1.7f);
        return weight; // [0,1]
    }

    private static final float accessibilityFactor(MarketAPI exporter, MarketAPI importer) {
        float expValue = smoothAroundOne(exporter.getAccessibilityMod().computeEffective(0)) * 0.3f;
        float impValue = smoothAroundOne(importer.getAccessibilityMod().computeEffective(0)) * 0.7f;
        return expValue + impValue;
    }

    private static final float smoothAroundOne(float value) {
        if (value == 1f) return 1f;
        if (value > 1f) {
            return 1f + (float)Math.pow(value - 1f, 0.8f);  // diminishing growth
        } else { // value < 1
            return 1f - (float)Math.pow(1f - value, 1.4f);  // harsher penalty
        }
    }

    private static final float priceFactor(String comID, MarketAPI importer) {
        final CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(comID); 
        final float price = importer.getDemandPrice(spec.getId(), spec.getEconUnit(), false);
        final float base = spec.getBasePrice();

        float diff = price / base - 1f; // e.g. 0.5 means 50% above base, -0.5 means 50% below

        float scaled;
        if (diff >= 0) {
            scaled = (float) Math.sqrt(diff);
        } else {
            scaled = -(float) Math.sqrt(-diff);
        }

        // Scale to desired range [-1,1] and clamp
        if (scaled > 1f) scaled = 1f;
        if (scaled < -1f) scaled = -1f;

        return (scaled + 1f) / 2f; // [0,1]
    }

    private static final float distanceFactor(MarketAPI exporter, MarketAPI importer) {
        final float dist = Misc.getDistanceLY(importer.getLocationInHyperspace(), exporter.getLocationInHyperspace());

        // Map size in vanilla is 82*52 LY
        final int maxDistance = 100; // max map diagonal

        final float normalized = Math.min(dist / maxDistance, 1f);
        final float alpha = 0.6f; // less harsh in mid-range

        final float score = 1f - (float) Math.pow(Math.sqrt(normalized), alpha); // [0, 1]
        return score;
    }

    private static final float sizeFactor(MarketAPI exporter) {
        int size = exporter.getSize();
        int maxSize = 10;
        return (float) Math.sqrt(size / (float) maxSize);
    }

    private static final long computeProjectedImportAmount(CommodityStats stats) {
        final int daysToCover = 3;
        final long targetStockpiles = daysToCover*stats.getDeficitPreTrade();

        final long delta = targetStockpiles - stats.getStoredAmount() - stats.getTotalImports();
        return Math.max(delta, 0);
    }

    private static final long computeExportableRemaining(CommodityStats stats) {
        // If there is a deficit before trade, do not export.
        if (stats.getDeficitPreTrade() > 0) {
            return 0;
        }
        return stats.getRemainingExportable();
    }
}
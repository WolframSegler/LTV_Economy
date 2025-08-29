package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

public class CommodityInfo {
    private final CommoditySpecAPI m_spec;
    private final Map<MarketAPI, CommodityStats> m_comStats = new HashMap<>();

    public CommodityInfo(
        CommoditySpecAPI spec
    ) {
        m_spec = spec;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.isHidden()) continue;
            
            m_comStats.put(market, new CommodityStats(m_spec.getId(), market));
        }
    }

    public final void advance(boolean fakeAdvance) {
        for (CommodityStats stats : m_comStats.values()) {
            stats.advance(fakeAdvance);
        }
    }

    public final void reset() {
        for (CommodityStats stats : m_comStats.values()) {
            stats.resetTradeValues();
        }
    }

    public final void update() {
        for (CommodityStats stats : m_comStats.values()) {
            stats.update();
        }
    }

    public final void addMarket(MarketAPI market) {
        if (m_comStats.containsKey(market)) return;
        
        m_comStats.put(market, new CommodityStats(m_spec.getId(), market));
    }

    public final void refreshMarkets() {
        final EconomyEngine engine = EconomyEngine.getInstance();

        List<MarketAPI> newMarkets = engine.symmetricDifference(
            engine.getMarketsCopy(),
            new ArrayList<>(m_comStats.keySet())
        );

        for (MarketAPI market : newMarkets) {
            addMarket(market);
        }
    }

    public final CommodityStats getStats(MarketAPI market) {

        return m_comStats.get(market);
    }

    public final Collection<CommodityStats> getAllStats() {
        return m_comStats.values();
    }

    public static Pair<MarketAPI, MarketAPI> getPairFromIndex(int index, List<MarketAPI> exporters,     
        List<MarketAPI> importers) {

        final int numImporters = importers.size();
        final int exporterIndex = index / numImporters;
        final int importerIndex = index % numImporters;

        final MarketAPI exporter = exporters.get(exporterIndex);
        final MarketAPI importer = importers.get(importerIndex);

        return new Pair<>(exporter, importer);
    }

    public final void trade() {
        List<MarketAPI> importers = getImporters();
        List<MarketAPI> exporters = getExporters();

        int[] pairScores = new int[importers.size() * exporters.size()];
        Integer[] indices = new Integer[importers.size() * exporters.size()];

        int expInd = 0;
        for (MarketAPI exporter : exporters) {

            int impInd = 0;
            for (MarketAPI importer : importers) {
                if (exporter.getId().contains(importer.getId())) {
                    continue;
                }

                pairScores[expInd*importers.size() + impInd] = computeStaticPairScore(exporter, importer);

                impInd++;
            }
            expInd++;
        }

        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, (a, b) -> Integer.compare(pairScores[b], pairScores[a]));

        for (int i = 0; i < indices.length; i++) {
            Pair<MarketAPI, MarketAPI> expImp = getPairFromIndex(
                indices[i], exporters, importers
            );

            CommodityStats expStats = getStats(expImp.one);
            CommodityStats impStats = getStats(expImp.two);

            long exportableRemaining = expStats.getRemainingExportable();
            long deficitRemaining = impStats.getDeficit();

            if (exportableRemaining < 1 || deficitRemaining < 1) continue;

            boolean sameFaction = expStats.market.getFaction().equals(impStats.market.getFaction());

            long amountToSend = Math.min(exportableRemaining, deficitRemaining);

            exportableRemaining -= amountToSend;
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

            if (stats.getBaseExportable() > 0) {
                exporters.add(stats.market);
            }
        }

        return exporters;
    }

    public final int computeStaticPairScore(MarketAPI exporter, MarketAPI importer) {

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

        return score;
    }

    public final int computeDynamicPairScore(MarketAPI importer) {
        return (int) priceFactor(m_spec, importer) * TradeWeights.LOCAL_PRICE;
    }

    private static final float relationFactor(MarketAPI exporter, MarketAPI importer) {
        final float rel = exporter.getFaction().getRelationship(importer.getFaction().getId());

        final float weight = (float) Math.pow(Math.abs(rel), 1.7f);
        return rel < 0 ? -weight : weight;
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

    private static final float priceFactor(CommoditySpecAPI spec, MarketAPI importer) {
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

        return scaled;
    }

    private static final float distanceFactor(MarketAPI exporter, MarketAPI importer) {
        final float dist = Misc.getDistanceLY(importer.getLocationInHyperspace(), exporter.getLocationInHyperspace());

        // Map size in vanilla is 82*52 LY
        final int maxDistance = 100; // max map diagonal

        final float normalized = Math.min(dist / maxDistance, 1f);
        final float alpha = 0.6f; // less harsh in mid-range

        final float penalty = (float) Math.pow(Math.sqrt(normalized), alpha); // [-1, 0]
        return -penalty;
    }

    private static final float sizeFactor(MarketAPI exporter) {
        int size = exporter.getSize();
        int maxSize = 10;
        return (float) Math.sqrt(size / (float) maxSize);
    }
}
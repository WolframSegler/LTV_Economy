package wfg_ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine {
    private static EconomyEngine instance;

    private final Map<String, CommoditySpecAPI> m_commoditySpecs;
    private final Map<String, CommodityInfo> m_commoditInfo;

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
        this.m_commoditySpecs = new HashMap<>();

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            m_commoditySpecs.put(spec.getId(), spec);
            m_commoditInfo.put(spec.getId(), new CommodityInfo(spec));
        }
    }

    public final void update() {
        for (Map.Entry<String, CommodityInfo> comInfo : m_commoditInfo.entrySet()) {
            for (CommodityStats stats : comInfo.getValue().getAllStats()) {
                stats.update();
            }
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

        for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {
            com.getValue().reset();
        }

        for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {
            com.getValue().advance(false);
        }
    }

    public final void fakeAdvance() {
        for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {
            com.getValue().reset();
        }

        for (Map.Entry<String, CommodityInfo> com : m_commoditInfo.entrySet()) {
            com.getValue().advance(true);
        }
    }

    public final void registerMarket(MarketAPI market) {
        for (Map.Entry<String, CommodityInfo> comInfo : m_commoditInfo.entrySet()) {
            comInfo.getValue().addMarket(market);
        }
    }

    public final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public final List<CommoditySpecAPI> getCommoditiesCopy() {
        return new ArrayList<>(m_commoditySpecs.values());
    }

    public final CommodityInfo getCommodityInfo(String comID) {
        return m_commoditInfo.get(comID);
    }

    public final CommodityStats getComStats(String comID, MarketAPI market) {
        final CommodityStats stats = m_commoditInfo.get(comID).getStats(market);
        if (stats != null) {
            stats.update();
        }
        return stats;
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

    public final List<MarketAPI> getMarketsImportingCom(
        CommoditySpecAPI spec, MarketAPI exporter, boolean onlyInFaction
    ) {
        List <MarketAPI> importers = new ArrayList<>();

        for (CommodityStats stats : m_commoditInfo.get(spec.getId()).getAllStats()) {
            if (onlyInFaction && !stats.market.getFaction().equals(exporter.getFaction())) {
                continue;
            }

            if (stats.m_com != null && stats.getDeficitPreTrade() > 0) {
                importers.add(stats.market);
            }
        }

        return importers;
    }

    public final List<MarketAPI> computeTradeScore(
        List<MarketAPI> importers, MarketAPI exporter, CommoditySpecAPI spec
    ) {
        Map<MarketAPI, Integer> tradeScores = new HashMap<>();
        for (MarketAPI importer : importers) {
            int score = 0;
    
            if (importer.getFaction().equals(exporter.getFaction())) {
                score += TradeWeights.IN_FACTION;
            }
    
            score += relationFactor(exporter, importer) * TradeWeights.POLITICAL;
    
            // if (hasTradeAgreement(exporter, importers)) {
            //     score += TradeWeights.TRADE_AGREEMENTS;
            // }
    
            score += accessibilityFactor(importer) * TradeWeights.ACCESSIBILITY;
    
            score += priceFactor(spec, importer) * TradeWeights.LOCAL_PRICE;
    
            score += distanceFactor(importer, exporter) * TradeWeights.DISTANCE;
    
            score += sizeFactor(importer) * TradeWeights.MARKET_SIZE;
    
            tradeScores.put(importer, score);
        }

        final List<MarketAPI> sortedCandidates = tradeScores.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .map(Map.Entry::getKey)
            .toList();

        return sortedCandidates;
    }

    private static final float relationFactor(MarketAPI exporter, MarketAPI importer) {
        final float rel = exporter.getFaction().getRelationship(importer.getFaction().getId());
        Global.getLogger(EconomyEngine.class).error(rel);

        final float weight = (float) Math.pow(Math.abs(rel), 1.7f);
        return rel < 0 ? -weight : weight;
    }

    private static final float accessibilityFactor(MarketAPI importer) {
        return importer.getAccessibilityMod().computeEffective(0);
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

    private static final float distanceFactor(MarketAPI importer, MarketAPI exporter) {
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

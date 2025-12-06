package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.configs.TradeWeights;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.CommodityStats.PriceType;

public class CommodityInfo {

    private final String comID;
    public transient CommoditySpecAPI spec;


    private final Map<String, CommodityStats> m_comStats = new HashMap<>();
    private final Map<String, IncomeLedger> incomeLedgers = new HashMap<>();

    private transient long marketActivity;
    private transient int currentIndex = 0;
    private transient boolean filled = false;
    private float[] lastNDaysVolume = new float[EconomyConfig.VOLATILITY_WINDOW];

    public CommodityInfo(
        CommoditySpecAPI spec, Set<String> registeredMarkets, EconomyEngine engine
    ) {
        comID = spec.getId();
        for (String marketID : registeredMarkets) {
            m_comStats.put(marketID, new CommodityStats(comID, marketID));

            if (engine.isPlayerMarket(marketID)) {
                incomeLedgers.put(marketID, new IncomeLedger());
            }
        }
    
        readResolve();
    }

    public Object readResolve() {
        marketActivity = 0;

        final float[] newArray = new float[EconomyConfig.VOLATILITY_WINDOW];

        int oldLength = 0;
        if (lastNDaysVolume != null) {
            oldLength = lastNDaysVolume.length;
            System.arraycopy(lastNDaysVolume, 0, newArray, 0,
                Math.min(oldLength, newArray.length)
            );
        }

        lastNDaysVolume = newArray;

        currentIndex = Math.min(currentIndex, lastNDaysVolume.length - 1);
        filled = oldLength >= newArray.length;

        spec = Global.getSettings().getCommoditySpec(comID);

        return this;
    }

    public final void advance() {
        m_comStats.values().parallelStream().forEach(CommodityStats::advance);

        recordDailyVolume();
    }

    public final void reset() {
        m_comStats.values().parallelStream().forEach(CommodityStats::reset);
    }

    public final void update() {
        m_comStats.values().parallelStream().forEach(CommodityStats::update);
    }

    public void endMonth() {
        for (IncomeLedger ledger : incomeLedgers.values()) {
            ledger.endMonth();
        }
    }

    public final void addMarket(String marketID) {
        m_comStats.putIfAbsent(marketID, new CommodityStats(comID, marketID));

        if (EconomyEngine.getInstance().isPlayerMarket(marketID)) {
            incomeLedgers.put(marketID, new IncomeLedger());
        }
    }

    public final void removeMarket(String marketID) {
        m_comStats.remove(marketID);

        if (EconomyEngine.getInstance().isPlayerMarket(marketID)) {
            incomeLedgers.remove(marketID);
        }
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

    public final ArrayList<CommodityStats> getSortedByProduction(int listSize) {
        return m_comStats.values()
            .stream()
            .filter(stats -> stats.getProduction(true) > 0)
            .sorted((a, b) -> Float.compare(
                b.getProduction(true), a.getProduction(true)
            ))
            .limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final ArrayList<CommodityStats> getSortedByDemand(int listSize) {
        return m_comStats.values()
            .stream()
            .filter(stats -> stats.getBaseDemand(false) > 0)
            .sorted((a, b) -> Float.compare(
                b.getBaseDemand(false), a.getBaseDemand(false)
            ))
            .limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final IncomeLedger getLedger(String marketID) {
        return incomeLedgers.get(marketID);
    }

    public final boolean hasLedger(String marketID) {
        return incomeLedgers.containsKey(marketID);
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

    /**
     * @return The total credits spent on imports in the last trade cycle.
     */
    public long getMarketActivity() {
        return marketActivity;
    }

    public float getTradeVolatility() {
        int length = filled ? EconomyConfig.VOLATILITY_WINDOW : currentIndex;
        if (length == 0) return 0f;

        float sum = 0f;
        for (int i = 0; i < length; i++) sum += lastNDaysVolume[i];
        float mean = sum / length;

        if (mean == 0f) return 0f;

        float variance = 0f;
        for (int i = 0; i < length; i++) {
            float diff = lastNDaysVolume[i] - mean;
            variance += diff * diff;
        }
        variance /= length;

        return (float) Math.sqrt(variance) / mean;
    }

    public final void trade(boolean fakeAdvance) {
        final EconomyEngine engine = EconomyEngine.getInstance();

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

        marketActivity = 0;

        for (int i = 0; i < indices.length; i++) {
            Pair<String, String> expImp = getPairFromIndex(
                indices[i], exporters, importers
            );

            final CommodityStats expStats = getStats(expImp.one);
            final CommodityStats impStats = getStats(expImp.two);

            final double exportableRemaining = expStats.getStoredRemainingExportable();
            final float deficitRemaining = computeImportAmount(impStats);

            if (exportableRemaining < 0.01f || deficitRemaining < 0.01f) continue;

            final boolean sameFaction = expStats.market.getFaction().equals(impStats.market.getFaction());
            final float amountToSend = (float) Math.min(exportableRemaining, deficitRemaining);

            // Weighted price: price leans toward importer if deficit is high, toward exporter if low;
            // models supply-demand influence on transaction.
            final float exporterPrice = expStats.getUnitPrice(PriceType.MARKET_SELLING, (int)amountToSend);
            final float importerPrice = impStats.getUnitPrice(PriceType.MARKET_BUYING, (int)amountToSend);
            final float weight = Math.min(1f, (float)impStats.getFlowDeficitPreTrade() / amountToSend);
            final float pricePerUnit = exporterPrice * (1f - weight) + importerPrice * weight;
            final float price = pricePerUnit * amountToSend;

            if(sameFaction) {
                expStats.addInFactionExport(amountToSend);
                impStats.addInFactionImport(amountToSend);

                final int credits = (int) (price * EconomyConfig.FACTION_EXCHANGE_MULT);
                marketActivity += credits;

                if (fakeAdvance) continue;
                engine.addCredits(expStats.marketID, credits);
                engine.addCredits(impStats.marketID, -credits);

                if (engine.isPlayerMarket(expStats.marketID)) getLedger(expStats.marketID).recordExport(credits);
                if (engine.isPlayerMarket(impStats.marketID)) getLedger(impStats.marketID).recordImport(credits);
                
            } else {
                expStats.addGlobalExport(amountToSend);
                impStats.addGlobalImport(amountToSend);

                marketActivity += price;
                
                if (fakeAdvance) continue;
                engine.addCredits(expStats.marketID, (int) price);
                engine.addCredits(impStats.marketID, (int) -price);

                if (engine.isPlayerMarket(expStats.marketID)) getLedger(expStats.marketID).recordExport((int) price);
                if (engine.isPlayerMarket(impStats.marketID)) getLedger(impStats.marketID).recordImport((int) price);
            }
        }
    }

    public final List<MarketAPI> getImporters() {
        List <MarketAPI> importers = new ArrayList<>(50);

        for (CommodityStats stats : m_comStats.values()) {

            if (computeImportAmount(stats) > 0) {
                importers.add(stats.market);
            }
        }

        return importers;
    }

    public final List<MarketAPI> getExporters() {
        List <MarketAPI> exporters = new ArrayList<>(50);

        for (CommodityStats stats : m_comStats.values()) {
            if (stats.getStoredRemainingExportable() > 0) exporters.add(stats.market);
        }

        return exporters;
    }

    /**
     * Sorts the indices array in-place based on pairScores using radix sort with radix = 256.
     * Descending order: highest score first.
     */
    private static void radixSortIndices(int[] indices, int[] pairScores) {
        final int n = indices.length;
        final int[] output = new int[n];

        int maxScore = 0;
        for (int i = 0; i < n; i++) {
            if (pairScores[indices[i]] > maxScore) maxScore = pairScores[indices[i]];
        }

        final int[] count = new int[256];
        int shift = 0;

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

            shift += 8;
        }
    }

    private final int computePairScore(MarketAPI exporter, MarketAPI importer) {

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
        } else {
            return 1f - (float)Math.pow(1f - value, 1.4f);  // harsher penalty
        }
    }

    /*
    * ALTERNATIVE PRICE FACTOR APPROACH:
    *
    * Current implementation is importer-centric: priceFactor is calculated based on
    * the importer’s BUYING price, reflecting how much the importing market wants
    * a commodity. This works well for general balance and keeps calculations simple.
    *
    * Alternative, more "capitalist" or exporter-centric approach:
    * - If the importer has a deficit (even projected with DAYS_TO_COVER), use the
    *   exporter’s SELLING price to calculate priceFactor. This reflects real-world
    *   producer leverage: owners of production can dictate terms, push surplus, or
    *   exploit market scarcity.
    * - If the importer has no deficit, fall back to the importer’s BUYING price, as
    *   they are not under pressure to acquire the good.
    * 
    * Pros of exporter-centric or hybrid method:
    * - Models market leverage and capitalist-style dynamics.
    * - Allows overproducing exporters to influence trade decisions and prices.
    * - Can introduce strategic behaviors like monopolies, stockpile-driven pricing, 
    *   or political dominance of certain markets.
    * 
    * Cons:
    * - More complex logic and slightly higher computation.
    *
    * Possible hybrid: mostly importer-centric for simplicity, but incorporate
    * exporter SELLING influence when projected deficits exist.
    *
    * This can be revisited later if the goal is to simulate more "realistic" trade
    * dynamics beyond simple supply/demand ratio.
    */
    private static final float priceFactor(String comID, MarketAPI importer) {
        final CommodityStats stats = EconomyEngine.getInstance().getComStats(comID, importer.getId());

        final float price = stats.getUnitPrice(PriceType.MARKET_BUYING, 1);
        final float base = stats.spec.getBasePrice();

        final float diff = price / base - 1f; // e.g. 0.5 means 50% above base, -0.5 means 50% below

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

    private static final float computeImportAmount(CommodityStats stats) {
        final float cap = EconomyConfig.DAYS_TO_COVER_PER_IMPORT * stats.getBaseDemand(false);
        final float rawTarget = stats.getPreferredStockpile();
        final int target = (int) Math.max(Math.min(rawTarget - stats.getStored(), cap), 0);
        final float exclusive = stats.getImportExclusiveDemand();

        return Math.max(target + exclusive - stats.getTotalImports(false), 0);
    }

    private void recordDailyVolume() {
        long total = 0;
        for (CommodityStats stats : m_comStats.values()) total += stats.getTotalExports();
        lastNDaysVolume[currentIndex] = total;
        currentIndex = (currentIndex + 1) % EconomyConfig.VOLATILITY_WINDOW;
        if (currentIndex == 0) filled = true;
    }
}
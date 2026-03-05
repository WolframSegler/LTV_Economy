package wfg.ltv_econ.economy.commodity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.configs.TradeWeights;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.util.Arithmetic;

public class CommodityDomain {

    private final String comID;
    public transient CommoditySpecAPI spec;

    // TODO switch static and runtime type to ArrayMap in a future incompatible update
    private final Map<String, CommodityCell> m_comCells = new HashMap<>();
    private final Map<String, IncomeLedger> incomeLedgers = new HashMap<>();
    private InformalExchangeNode informalNode;

    private transient List<CommodityTradeFlow> tradeFlows = new ArrayList<>();
    private transient long tradeCreditActivity = 0l;
    private transient int currentIndex = 0;
    private transient boolean filled = false;
    private float[] lastNDaysVolume = new float[EconomyConfig.VOLATILITY_WINDOW];

    public CommodityDomain(String comID) {
        this.comID = comID;
        informalNode = new InformalExchangeNode(comID);
    
        readResolve();
    }

    public Object readResolve() {
        tradeCreditActivity = 0l;

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
        tradeFlows = new ArrayList<>();

        // TODO remove after save incompatible update
        if (informalNode == null) informalNode = new InformalExchangeNode(comID);

        return this;
    }

    public final void advance() {
        m_comCells.values().forEach(CommodityCell::advance);

        recordDailyVolume();
    }

    public final void reset() {
        m_comCells.values().forEach(CommodityCell::reset);

        tradeCreditActivity = 0l;
        tradeFlows.clear();
    }

    public final void update() {
        m_comCells.values().forEach(CommodityCell::update);
    }

    public void endMonth() {
        for (IncomeLedger ledger : incomeLedgers.values()) {
            ledger.endMonth();
        }
    }

    public final void addMarket(String marketID) {
        m_comCells.putIfAbsent(marketID, new CommodityCell(comID, marketID));

        if (EconomyEngine.getInstance().isPlayerMarket(marketID)) {
            incomeLedgers.put(marketID, new IncomeLedger());
        }
    }

    public final void removeMarket(String marketID) {
        m_comCells.remove(marketID);

        if (EconomyEngine.getInstance().isPlayerMarket(marketID)) {
            incomeLedgers.remove(marketID);
        }
    }

    public final CommodityCell getCell(String marketID) {
        return m_comCells.get(marketID);
    }

    public final Collection<CommodityCell> getAllCells() {
        return m_comCells.values();
    }

    public final Map<String, CommodityCell> getCellsMap() {
        return m_comCells;
    }

    public final Collection<IncomeLedger> getAllLedgers() {
        return incomeLedgers.values();
    }

    public final ArrayList<CommodityCell> getSortedByProduction(int listSize) {
        return m_comCells.values().stream()
            .filter(cell -> cell.getProduction(true) > 0)
            .sorted((a, b) -> Float.compare(
                b.getProduction(true), a.getProduction(true)
            )).limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final ArrayList<CommodityCell> getSortedByDemand(int listSize) {
        return m_comCells.values().stream()
            .filter(cell -> cell.getBaseDemand(true) > 0)
            .sorted((a, b) -> Float.compare(
                b.getBaseDemand(true), a.getBaseDemand(true)
            )).limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final IncomeLedger getLedger(String marketID) {
        return incomeLedgers.get(marketID);
    }

    public final boolean hasLedger(String marketID) {
        return incomeLedgers.containsKey(marketID);
    }

    /**
     * @return The total credits spent on imports in the last trade cycle.
     */
    public long getTradeCreditActivity() {
        return tradeCreditActivity;
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

        formalTrade(fakeAdvance, engine);

        informalNode.updateBeforeTrade();
        informalTrade(fakeAdvance, engine);
    }

    public final List<CommodityCell> getImporters() {
        List <CommodityCell> importers = new ArrayList<>(32);

        for (CommodityCell cell : m_comCells.values()) {

            if (computeImportAmount(cell) > 0) {
                importers.add(cell);
            }
        }

        return importers;
    }

    public final List<CommodityCell> getExporters() {
        List <CommodityCell> exporters = new ArrayList<>(32);

        for (CommodityCell cell : m_comCells.values()) {
            if (cell.getStoredRemainingExportable() > 0) exporters.add(cell);
        }

        return exporters;
    }

    public final InformalExchangeNode getInformalNode() {
        return informalNode;
    }

    public final List<CommodityTradeFlow> getTradeFlows() {
        return tradeFlows;
    }

    private final void formalTrade(boolean fakeAdvance, final EconomyEngine engine) {
        final List<CommodityCell> importers = getImporters();
        final List<CommodityCell> exporters = getExporters();

        final int pairCount = importers.size() * exporters.size();

        final int[] pairScores = new int[pairCount];
        final int[] indices = new int[pairCount];

        for (int expInd = 0; expInd < exporters.size(); expInd++) {
            for (int impInd = 0; impInd < importers.size(); impInd++) {
                pairScores[expInd * importers.size() + impInd] = computePairScore(
                    exporters.get(expInd), importers.get(impInd)
                );
            }
        }

        for (int i = 0; i < indices.length; i++) indices[i] = i;
        radixSortIndices(indices, pairScores);

        tradeCreditActivity = 0l;

        for (int i = 0; i < indices.length; i++) {
            Pair<CommodityCell, CommodityCell> expImp = getPairFromIndex(
                indices[i], exporters, importers
            );

            final CommodityCell expCell = expImp.one;
            final CommodityCell impCell = expImp.two;

            { // ABORT CONDITIONS
                if (expCell.market.getFaction().getRelationship(impCell.market.getFactionId()) <
                    EconomyConfig.MIN_RELATION_TO_TRADE
                ) { continue;}
    
                if (expCell.market.isPlayerOwned()^impCell.market.isPlayerOwned()) {
                    final String tradingFactionID = expCell.market.isPlayerOwned() ?
                        impCell.market.getFaction().getId() : expCell.market.getFaction().getId();
    
                    if (engine.playerFactionSettings.embargoedFactions.contains(tradingFactionID)) {
                        continue;
                    }
                }
            }

            final double exportableRemaining = expCell.getStoredRemainingExportable();
            final float deficitRemaining = computeImportAmount(impCell);

            if (exportableRemaining < 0.01f || deficitRemaining < 0.01f) continue;

            final boolean sameFaction = expCell.market.getFaction().equals(impCell.market.getFaction());
            final float amountToSend = (float) Math.min(exportableRemaining, deficitRemaining);

            // Weighted price: price leans toward importer if deficit is high, toward exporter if low;
            // models supply-demand influence on transaction.
            final float exporterPrice = expCell.getUnitPrice(PriceType.MARKET_SELLING, (int)amountToSend);
            final float importerPrice = impCell.getUnitPrice(PriceType.MARKET_BUYING, (int)amountToSend);
            final float weight = Math.min(1f, (float)impCell.getFlowDeficitPreTrade() / amountToSend);
            final float pricePerUnit = exporterPrice * (1f - weight) + importerPrice * weight;
            final float price = pricePerUnit * amountToSend;

            if(sameFaction) {
                expCell.inFactionExports += amountToSend;
                impCell.inFactionImports += amountToSend;
            } else {
                expCell.globalExports += amountToSend;
                impCell.globalImports += amountToSend;
            }

            final int credits = sameFaction ? (int) (price * EconomyConfig.FACTION_EXCHANGE_MULT) : (int) price;
            tradeCreditActivity += credits;

            tradeFlows.add(new CommodityTradeFlow(
                expCell.market, impCell.market, amountToSend, sameFaction
            ));

            if (!fakeAdvance) {
                engine.addCredits(expCell.marketID, credits);
                engine.addCredits(impCell.marketID, -credits);
    
                if (hasLedger(expCell.marketID)) getLedger(expCell.marketID).recordExport(credits);
                if (hasLedger(impCell.marketID)) getLedger(impCell.marketID).recordImport(credits);
            }
        }
    }

    private final void informalTrade(boolean fakeAdvance, final EconomyEngine engine) {
        final List<CommodityCell> exporters = getExporters();
        final List<CommodityCell> importers = getImporters();

        final float sumImportable;
        final float sumExportable;
        {
            float demand = 0f;
            float excess = 0f;
            for (CommodityCell cell : exporters) excess += cell.getStoredRemainingExportable();
            for (CommodityCell cell : importers) demand += CommodityDomain.computeImportAmount(cell);

            sumImportable = demand;
            sumExportable = excess;
        }

        if (sumExportable < 1f) return;
        for (CommodityCell exporter : exporters) {
            final double exportable = exporter.getStoredRemainingExportable();
            final float share = (float) (exportable / sumExportable);
            final float amount = Math.min((float) exportable, share * informalNode.imports);
            final int price = (int) (exporter.getUnitPrice(PriceType.MARKET_SELLING, (int)amount)
                * informalNode.priceMultImporting * amount * (1f + exporter.market.getTariff().getModifiedValue() * informalNode.tariffEnforcementImporting)
            );

            exporter.informalExports += amount;
            tradeCreditActivity += price;
            
            if (fakeAdvance) continue;
            final String marketID = exporter.marketID;
            engine.addCredits(marketID, price);

            if (hasLedger(marketID)) getLedger(marketID).recordExport(price);
        }

        if (sumImportable < 1f) return;
        for (CommodityCell importer : importers) {
            final double importable = computeImportAmount(importer);
            final float share = (float) (importable / sumImportable);
            final float amount = Math.min((float) importable, importer.getInformalImportMods()
                .computeEffective(share * informalNode.exports));
            final int price = (int) (importer.getUnitPrice(PriceType.MARKET_BUYING, (int)amount)
                * informalNode.priceMultExporting * amount * (1f + importer.market.getTariff().getModifiedValue() * informalNode.tariffEnforcementExporting)
            );

            importer.informalImports += amount;
            tradeCreditActivity += price;
            
            if (fakeAdvance) continue;
            final String marketID = importer.marketID;
            engine.addCredits(marketID, -price);

            if (hasLedger(marketID)) getLedger(marketID).recordImport(price);
        }
    }

    private static final Pair<CommodityCell, CommodityCell> getPairFromIndex(int index, List<CommodityCell> exporters,     
        List<CommodityCell> importers
    ) {
        final int numImporters = importers.size();
        final int exporterIndex = index / numImporters;
        final int importerIndex = index % numImporters;

        return new Pair<>(exporters.get(exporterIndex), importers.get(importerIndex));
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

    private final int computePairScore(CommodityCell exporter, CommodityCell importer) {
        final MarketAPI expMarket = exporter.market;
        final MarketAPI impMarket = importer.market;

        int score = 0;

        if (impMarket.getFaction().equals(expMarket.getFaction())) {
            score += TradeWeights.IN_FACTION;
        }

        score += relationFactor(expMarket, impMarket) * TradeWeights.POLITICAL;

        // if (hasTradeAgreement(expMarket, impMarket)) {
        //     score += TradeWeights.TRADE_AGREEMENTS;
        // }

        score += accessibilityFactor(expMarket, impMarket) * TradeWeights.ACCESSIBILITY;

        score += distanceFactor(expMarket, impMarket) * TradeWeights.DISTANCE;

        score += sizeFactor(impMarket) * TradeWeights.MARKET_SIZE;

        score += priceFactor(importer) * TradeWeights.LOCAL_PRICE;

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
    private static final float priceFactor(CommodityCell importer) {

        final float price = importer.getUnitPrice(PriceType.MARKET_BUYING, 1);
        final float base = importer.spec.getBasePrice();

        final float diff = price / base - 1f; // e.g. 0.5 means 50% above base, -0.5 means 50% below
        final float scaled = diff >= 0 ? (float) Math.sqrt(diff) : (float) -Math.sqrt(-diff);

        return (Arithmetic.clamp(scaled, -1f, 1f) + 1f) / 2f; // [0,1]
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
        final int size = exporter.getSize();
        final int maxSize = 10;
        return (float) Math.sqrt(size / (float) maxSize);
    }

    static final float computeImportAmount(CommodityCell cell) {
        final float cap = EconomyConfig.DAYS_TO_COVER_PER_IMPORT * cell.getBaseDemand(true);
        final float rawTarget = cell.getPreferredStockpile();
        final int target = (int) Math.max(Math.min(rawTarget - cell.getStored(), cap), 0.0);
        final float exclusive = cell.getImportExclusiveDemand();

        return Math.max(target + exclusive - cell.getTotalImports(false), 0f);
    }

    private final void recordDailyVolume() {
        long total = 0;
        for (CommodityCell cell : m_comCells.values()) total += cell.getTotalExports();
        lastNDaysVolume[currentIndex] = total;
        currentIndex = (currentIndex + 1) % EconomyConfig.VOLATILITY_WINDOW;
        if (currentIndex == 0) filled = true;
    }
}
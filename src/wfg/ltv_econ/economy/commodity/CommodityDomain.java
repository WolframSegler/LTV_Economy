package wfg.ltv_econ.economy.commodity;

import static wfg.ltv_econ.constants.strings.Income.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.constants.TradeWeights;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class CommodityDomain implements Serializable {

    public final String comID;
    public transient CommoditySpecAPI spec;

    private final ArrayMap<String, CommodityCell> comCells = new ArrayMap<>(EconomyConstants.econCommodityIDs.size());
    private final List<ComTradeFlow> tradeFlows = new ArrayList<>(EconomyInfo.getMarketsCount());
    private InformalExchangeNode informalNode;

    private float[] tradeVolumeHistory = new float[EconomyConfig.HISTORY_LENGTH];
    private long[] creditActivityHistory = new long[EconomyConfig.HISTORY_LENGTH];
    private int historyIndex = 0;

    public CommodityDomain(String comID) {
        this.comID = comID;
        informalNode = new InformalExchangeNode(comID);
    
        readResolve();
    }

    public Object readResolve() {
        resizeHistoryArrays();

        spec = Global.getSettings().getCommoditySpec(comID);

        return this;
    }

    public final void advance() {
        comCells.values().forEach(CommodityCell::advance);

        historyIndex = (historyIndex + 1) % EconomyConfig.HISTORY_LENGTH;
    }

    public final void reset() {
        comCells.values().forEach(CommodityCell::reset);

        tradeVolumeHistory[historyIndex] = 0f;
        creditActivityHistory[historyIndex] = 0l;
    }

    public final void update() {
        comCells.values().forEach(CommodityCell::update);
    }

    public final void addMarket(String marketID) {
        comCells.putIfAbsent(marketID, new CommodityCell(comID, marketID));
    }

    public final void removeMarket(String marketID) {
        comCells.remove(marketID);
    }

    public final CommodityCell getCell(String marketID) {
        return comCells.get(marketID);
    }

    public final Collection<CommodityCell> getAllCells() {
        return comCells.values();
    }

    public final Map<String, CommodityCell> getCellsMap() {
        return comCells;
    }

    public final ArrayList<CommodityCell> getSortedByProduction(int listSize) {
        return comCells.values().stream()
            .filter(cell -> cell.getProduction(true) > 0)
            .sorted((a, b) -> Float.compare(
                b.getProduction(true), a.getProduction(true)
            )).limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final ArrayList<CommodityCell> getSortedByTargetQuantum(int listSize) {
        return comCells.values().stream()
            .filter(cell -> cell.getTargetQuantum(true) > 0f)
            .sorted((a, b) -> Float.compare(
                b.getTargetQuantum(true), a.getTargetQuantum(true)
            )).limit(listSize)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public final List<CommodityCell> getImporters() {
        final List <CommodityCell> importers = new ArrayList<>(32);

        for (CommodityCell cell : comCells.values()) {
            if (cell.computeImportAmount() > 0f) importers.add(cell);
        }

        return importers;
    }

    public final List<CommodityCell> getExporters() {
        final List <CommodityCell> exporters = new ArrayList<>(32);

        for (CommodityCell cell : comCells.values()) {
            if (cell.computeExportAmount() > 0f) exporters.add(cell);
        }

        return exporters;
    }

    public final InformalExchangeNode getInformalNode() {
        return informalNode;
    }

    public final List<ComTradeFlow> getTradeFlows() {
        return tradeFlows;
    }

    public final float getTradeVolumeHistory() {
        float total = 0f;
        for (int i = 0; i < tradeVolumeHistory.length; i++) total += tradeVolumeHistory[i];
        return total;
    }

    public final long getCreditActivityHistory() {
        long total = 0l;
        for (int i = 0; i < creditActivityHistory.length; i++) total += creditActivityHistory[i];
        return total;
    }

    public final float getTradeVolatility() {
        final int len = tradeVolumeHistory.length;
        if (len == 0) return 0f;

        final int head = (historyIndex - 1 + len) % len;

        float sum = 0f;
        float sumSq = 0f;
        int idx = head;
        for (int i = 0; i < len; i++) {
            float val = tradeVolumeHistory[idx];
            sum += val;
            sumSq += val * val;
            idx = (idx - 1 + len) % len;
        }

        final float mean = sum / len;
        if (mean == 0f) return 0f;
        final float variance = (sumSq / len) - (mean * mean);
        final float stdDev = (float) Math.sqrt(Math.max(0f, variance));
        return stdDev / mean;
    }

    public final void createFormalTradeFlows() {
        final MarketFinanceRegistry registry = MarketFinanceRegistry.instance();
        final PlayerFactionSettings facSettings = LtvEconSaveData.instance().playerFactionSettings;
        final List<CommodityCell> importers = getImporters();
        final List<CommodityCell> exporters = getExporters();

        tradeFlows.clear();

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

        float tradeVolume = 0f;
        long tradeCreditActivity = 0l;

        for (int i = 0; i < indices.length; i++) {
            final Pair<CommodityCell, CommodityCell> expImp = getPairFromIndex(
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
    
                    if (facSettings.embargoedFactions.contains(tradingFactionID)) {
                        continue;
                    }
                }

                if (!expCell.market.hasSpaceport() || !impCell.market.hasSpaceport()) continue;
            }

            final double exportableRemaining = expCell.computeExportAmount();
            final float deficitRemaining = impCell.computeImportAmount();

            if (exportableRemaining < 0.01f || deficitRemaining < 0.01f) continue;

            final boolean sameFaction = expCell.market.getFaction().equals(impCell.market.getFaction());
            final float amountToSend = (float) Math.min(exportableRemaining, deficitRemaining);

            // Weighted price: price leans toward importer if deficit is high, toward exporter if low;
            final float exporterPrice = expCell.getUnitPrice(PriceType.MARKET_SELLING, (int)amountToSend);
            final float importerPrice = impCell.getUnitPrice(PriceType.MARKET_BUYING, (int)amountToSend);
            final float weight = Math.min(1f, (float)impCell.getTargetQuantumPreTrade() / amountToSend);
            final float unitPrice = exporterPrice * (1f - weight) + importerPrice * weight;
            final float price = unitPrice * amountToSend;

            if(sameFaction) {
                expCell.inFactionExports += amountToSend;
            } else {
                expCell.globalExports += amountToSend;
            }

            final int credits = sameFaction ? (int) (price * EconomyConfig.FACTION_EXCHANGE_MULT) : (int) price;
            tradeCreditActivity += credits;
            tradeVolume += amountToSend;

            tradeFlows.add(new ComTradeFlow(comID,
                expCell.market, impCell.market, amountToSend, credits, sameFaction
            ));

            registry.getLedger(expCell.marketID).add(TRADE_EXPORT_KEY + comID, credits, getDesc(TRADE_EXPORT_KEY) + spec.getName());
            registry.getLedger(impCell.marketID).add(TRADE_IMPORT_KEY + comID, -credits, getDesc(TRADE_IMPORT_KEY) + spec.getName());
        }

        tradeVolumeHistory[historyIndex] += tradeVolume;
        creditActivityHistory[historyIndex] += tradeCreditActivity;
    }

    public final void informalTrade(boolean fakeAdvance) {
        informalNode.updateBeforeTrade();

        final MarketFinanceRegistry registry = MarketFinanceRegistry.instance();
        final List<CommodityCell> exporters = getExporters();
        final List<CommodityCell> importers = getImporters();

        final float sumImportable;
        final float sumExportable;
        {
            float demand = 0f;
            float excess = 0f;
            for (CommodityCell cell : exporters) excess += cell.computeExportAmount();
            for (CommodityCell cell : importers) demand += cell.computeImportAmount();

            sumImportable = demand;
            sumExportable = excess;
        }

        float tradeVolume = 0f;
        long tradeCreditActivity = 0l;

        if (sumExportable < 1f) return;
        for (CommodityCell exporter : exporters) {
            final double exportable = exporter.computeExportAmount();
            final float share = (float) (exportable / sumExportable);
            final float amount = Math.min((float) exportable, share * informalNode.imports);
            final int price = (int) (exporter.getUnitPrice(PriceType.MARKET_SELLING, (int)amount)
                * informalNode.priceMultImporting * amount * (1f + exporter.market.getTariff().getModifiedValue() * informalNode.tariffEnforcementImporting)
            );

            exporter.informalExports += amount;
            tradeVolume += amount;
            tradeCreditActivity += price;
            
            if (fakeAdvance) continue;
            registry.getLedger(exporter.marketID).add(TRADE_EXPORT_KEY + comID, price, getDesc(TRADE_EXPORT_KEY) + spec.getName());
        }

        if (sumImportable < 1f) return;
        for (CommodityCell importer : importers) {
            final double importable = importer.computeImportAmount();
            final float share = (float) (importable / sumImportable);
            final float amount = Math.min((float) importable, importer.getInformalImportMods()
                .computeEffective(share * informalNode.exports));
            final int price = (int) (importer.getUnitPrice(PriceType.MARKET_BUYING, (int)amount)
                * informalNode.priceMultExporting * amount * (1f + importer.market.getTariff().getModifiedValue() * informalNode.tariffEnforcementExporting)
            );

            importer.informalImports += amount;
            tradeCreditActivity += price;
            
            if (fakeAdvance) continue;
            registry.getLedger(importer.marketID).add(TRADE_IMPORT_KEY + comID, -price, getDesc(TRADE_IMPORT_KEY) + spec.getName());
        }

        tradeVolumeHistory[historyIndex] += tradeVolume;
        creditActivityHistory[historyIndex] += tradeCreditActivity;
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
    private static final void radixSortIndices(int[] indices, int[] pairScores) {
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

    private static final int computePairScore(CommodityCell exporter, CommodityCell importer) {
        final MarketAPI expMarket = exporter.market;
        final MarketAPI impMarket = importer.market;

        int score = 0;

        if (impMarket.getFaction().equals(expMarket.getFaction())) {
            score += TradeWeights.IN_FACTION;
        }

        score += relationFactor(expMarket, impMarket) * TradeWeights.POLITICAL;

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

    private final void resizeHistoryArrays() {
        final int newLen = EconomyConfig.HISTORY_LENGTH;
        final int oldLen = tradeVolumeHistory.length;
        final int head = (historyIndex - 1 + oldLen) % oldLen;

        if (newLen == oldLen) return;

        final float[] newVolume = new float[newLen];
        final long[] newCredit = new long[newLen];

        int copied = 0;
        int idx = head;

        while (copied < newLen) {
            newVolume[copied] = tradeVolumeHistory[idx];
            newCredit[copied] = creditActivityHistory[idx];
            copied++;

            idx = (idx - 1 + oldLen) % oldLen;
            if (idx == head) break;
        }

        tradeVolumeHistory = newVolume;
        creditActivityHistory = newCredit;
        historyIndex = (copied == newLen) ? 0 : copied;
    }
}
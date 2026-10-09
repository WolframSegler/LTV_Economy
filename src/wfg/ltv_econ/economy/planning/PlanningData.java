package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryIOs;

public final class PlanningData {
    private PlanningData() {};

    public static final ArrayList<BitSet> getOutputsPerMarket(final List<MarketAPI> markets) {
        final ArrayList<BitSet> outputsPerMarket = new ArrayList<>();

        for (int i = 0; i < markets.size(); i++) {
            outputsPerMarket.add(getOutputForMarket(markets.get(i)));
        }

        return outputsPerMarket;
    }

    public static final BitSet getOutputForMarket(final MarketAPI market) {
        final var indOutputPairToColumn = ProductionMatrix.getIndOutputPairToColumnMap();

        final BitSet outputMask = new BitSet(indOutputPairToColumn.size());
        for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
            if (!ind.isFunctional()) continue;

            final IndustryConfig config = IndustryConfigManager.getIndConfig(ind);
            if (!config.workerAssignable) continue;

            final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind.getSpec());

            for (String outputID : config.outputs.keySet()) {
                if (!IndustryIOs.isOutputValidForMarket(config.outputs.get(outputID), ind)) continue;

                final int idx = indOutputPairToColumn.getOrDefault(indID + EconomyLoop.KEY + outputID, -1);
                if (idx >= 0) outputMask.set(idx);
            }
        }

        return outputMask;
    }

    public static final BitSet getOutputForIndustry(final Industry ind) {
        final var indOutputPairToColumn = ProductionMatrix.getIndOutputPairToColumnMap();
        final BitSet outputMask = new BitSet(indOutputPairToColumn.size());

        if (!ind.isFunctional()) return outputMask;

        final IndustryConfig config = IndustryConfigManager.getIndConfig(ind);
        if (!config.workerAssignable) return outputMask;

        final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind);

        for (String outputID : config.outputs.keySet()) {
            if (!IndustryIOs.isOutputValidForMarket(config.outputs.get(outputID), ind)) continue;

            final int idx = indOutputPairToColumn.getOrDefault(indID + EconomyLoop.KEY + outputID, -1);
            if (idx >= 0) outputMask.set(idx);
        }

        return outputMask;
    }

    public static final double[] getGlobalDemandVector() {
        final ArrayList<String> commodities = ProductionMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            String commodityID = commodities.get(i);
            d[i] = EconomyEngine.instance().info.getGlobalDemand(commodityID);
        }

        return d;
    }

    public static final double[] getFactionDemandVector(final String factionID) {
        final ArrayList<String> commodities = ProductionMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            d[i] = EconomyEngine.instance().info.getFactionTargetQuantum(comID, factionID);
        }

        return d;
    }

    public static final double[][] getFactionDemandVectors() {
        final int F = EconomyConstants.factionIDs.size();
        final int C = ProductionMatrix.getWorkerRelatedCommodityIDs().size();

        final double[][] d = new double[F][C];
        for (int f = 0; f < F; f++) {
            d[f] = getFactionDemandVector(EconomyConstants.factionIDs.get(f));
        }

        return d;
    }

    public static final double[] getMarketDemandVector(final String marketID) {
        final ArrayList<String> commodities = ProductionMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            
            final CommodityCell cell = EconomyEngine.instance().getComCell(comID, marketID);
            if (cell == null) continue;

            d[i] = cell.getTargetQuantum(true);
        }

        return d;
    }

    public static final double[][] getMarketDemandVectorsFromMarkets(final List<MarketAPI> markets) {
        return getMarketDemandVectors(markets.stream().map(m -> m.getId()).toList());
    }

    public static final double[][] getMarketDemandVectors(final List<String> marketIDs) {
        final int M = marketIDs.size();
        final int C = ProductionMatrix.getWorkerRelatedCommodityIDs().size();
        final double[][] d = new double[M][C];

        for (int m = 0; m < M; m++) {
            d[m] = getMarketDemandVector(marketIDs.get(m));
        }

        return d;
    }
}
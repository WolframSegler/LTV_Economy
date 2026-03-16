package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.industry.IndustryIOs;

public class PlanningData {

    public static final List<BitSet> getOutputsPerMarket(final List<MarketAPI> markets) {
        final var indOutputPairToColumn = IndustryMatrix.getIndOutputPairToColumnMap();
        final int numOutputs = indOutputPairToColumn.size();
        final List<BitSet> outputsPerMarket = new ArrayList<>();

        for (int i = 0; i < markets.size(); i++) {
            final MarketAPI market = markets.get(i);

            final BitSet outputMask = new BitSet(numOutputs);
            for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
                if (!ind.isFunctional()) continue;

                final IndustryConfig config = IndustryIOs.getIndConfig(ind);
                if (!config.workerAssignable) continue;

                final String indID = IndustryIOs.getBaseIndIDifNoConfig(ind.getSpec());

                for (String outputID : config.outputs.keySet()) {
                    if (!CompatLayer.hasRelevantCondition(outputID, market)) continue;
                    if (!IndustryIOs.isOutputValidForMarket(config.outputs.get(outputID), ind)) continue;

                    final int idx = indOutputPairToColumn.getOrDefault(indID + EconomyLoop.KEY + outputID, -1);
                    if (idx >= 0) outputMask.set(idx);
                }
            }

            outputsPerMarket.add(outputMask);
        }

        return outputsPerMarket;
    }

    public static final double[] getGlobalDemandVector() {
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            String commodityID = commodities.get(i);
            d[i] = EconomyEngine.getInstance().info.getGlobalDemand(commodityID);
        }

        return d;
    }

    public static final double[] getFactionDemandVector(final String factionID) {
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            d[i] = EconomyEngine.getInstance().info.getFactionComDemand(comID, factionID);
        }

        return d;
    }

    public static final double[] getMarketDemandVector(final String marketID) {
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            d[i] = EconomyEngine.getInstance().getComCell(comID, marketID).getBaseDemand(true);
        }

        return d;
    }

    public static final double[][] getMarketDemandVectorsFromMarkets(final List<MarketAPI> markets) {
        return getMarketDemandVectors(markets.stream().map(m -> m.getId()).toList());
    }

    public static final double[][] getMarketDemandVectors(final List<String> marketIDs) {
        final int M = marketIDs.size();
        final int C = IndustryMatrix.getWorkerRelatedCommodityIDs().size();
        final double[][] marketDemands = new double[M][C];

        for (int m = 0; m < M; m++) {
            marketDemands[m] = getMarketDemandVector(marketIDs.get(m));
        }

        return marketDemands;
    }
}
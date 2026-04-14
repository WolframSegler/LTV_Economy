package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryIOs;

public class PlanningData {
    private PlanningData() {}

    public static final List<BitSet> getOutputsPerMarket(final List<MarketAPI> markets) {
        final var indOutputPairToColumn = IndustryMatrix.getIndOutputPairToColumnMap();
        final int numOutputs = indOutputPairToColumn.size();
        final List<BitSet> outputsPerMarket = new ArrayList<>();

        for (int i = 0; i < markets.size(); i++) {
            final MarketAPI market = markets.get(i);

            final BitSet outputMask = new BitSet(numOutputs);
            for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
                if (!ind.isFunctional()) continue;

                final IndustryConfig config = IndustryConfigManager.getIndConfig(ind);
                if (!config.workerAssignable) continue;

                final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind.getSpec());

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
            d[i] = EconomyEngine.instance().info.getGlobalDemand(commodityID);
        }

        return d;
    }

    public static final double[] getFactionDemandVector(final String factionID) {
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            d[i] = EconomyEngine.instance().info.getFactionTargetQuantum(comID, factionID);
        }

        return d;
    }

    public static final double[][] getFactionDemandVectors() {
        final int F = EconomyConstants.factionIDs.size();
        final int C = IndustryMatrix.getWorkerRelatedCommodityIDs().size();

        final double[][] d = new double[F][C];
        for (int f = 0; f < F; f++) {
            d[f] = getFactionDemandVector(EconomyConstants.factionIDs.get(f));
        }

        return d;
    }

    public static final double[] getMarketDemandVector(final String marketID) {
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final double[] d = new double[commodities.size()];
        for (int i = 0; i < commodities.size(); i++) {
            final String comID = commodities.get(i);
            
            final CommodityCell cell = EconomyEngine.instance().getComCell(comID, marketID);
            d[i] = cell.getTargetQuantum(true);
        }

        return d;
    }

    public static final double[][] getMarketDemandVectorsFromMarkets(final List<MarketAPI> markets) {
        return getMarketDemandVectors(markets.stream().map(m -> m.getId()).toList());
    }

    public static final double[][] getMarketDemandVectors(final List<String> marketIDs) {
        final int M = marketIDs.size();
        final int C = IndustryMatrix.getWorkerRelatedCommodityIDs().size();
        final double[][] d = new double[M][C];

        for (int m = 0; m < M; m++) {
            d[m] = getMarketDemandVector(marketIDs.get(m));
        }

        return d;
    }
}
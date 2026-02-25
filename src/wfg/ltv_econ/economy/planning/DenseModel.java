package wfg.ltv_econ.economy.planning;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.IndustryGrouper.IndustryMatrixGrouped;
import wfg.ltv_econ.industry.IndustryIOs;

public class DenseModel {
    public final int denseSize;
    public final long[] denseMarketCap;
    public final long[] denseWeightSum;
    public final double[] denseOutputMod;
    public final int[] marketStart;
    public final int[] denseOutputIndex;
    public final float[] denseWorkerLimitFrac;
    public final Map<String, Integer> commodityIndex;

    private DenseModel(int denseSize, long[] denseMarketCap, long[] denseWeightSum,
        double[] denseOutputMod, int[] marketStart, int[] denseOutputIndex,
        float[] denseWorkerLimitFrac, Map<String, Integer> commodityIndex
    ) {
        this.denseSize = denseSize;
        this.denseMarketCap = denseMarketCap;
        this.denseWeightSum = denseWeightSum;
        this.denseOutputMod = denseOutputMod;
        this.marketStart = marketStart;
        this.denseOutputIndex = denseOutputIndex;
        this.denseWorkerLimitFrac = denseWorkerLimitFrac;
        this.commodityIndex = commodityIndex;
    }

    public static final DenseModel createDenseData(final List<MarketAPI> markets,
        final List<String> groupedOutputPairs, final List<BitSet> outputsPerMarket
    ) {
        final IndustryMatrixGrouped MatrixData = IndustryGrouper.getStaticGrouping();
        final List<String> commodities = IndustryMatrix.getWorkerRelatedCommodityIDs();

        final int numMarkets = markets.size();
        final int numOutputs = groupedOutputPairs.size();
        final int numCommodities = commodities.size();

        int denseCount = 0;
        for (BitSet bits : outputsPerMarket) denseCount += bits.cardinality();

        final long[] denseMarketCap = new long[denseCount];
        final long[] denseWeightSum = new long[denseCount];
        final double[] denseOutputMod = new double[denseCount];
        final int[] marketStart = new int[numMarkets + 1];
        final int[] denseOutputIndex = new int[denseCount];
        final float[] denseWorkerLimitFrac = new float[denseCount];
        final Map<String, Integer> commodityIndex = new HashMap<>(numCommodities);

        final long[] baseCapacities = new long[numMarkets];
        final long[] weightSum = new long[numOutputs];

        for (int m = 0; m < numMarkets; m++) {
            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(markets.get(m));
            baseCapacities[m] = (pool != null) ? pool.getWorkerPool() : 0l;
        }

        for (int o = 0; o < numOutputs; o++) {
            long s = 0l;
            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).get(o)) continue;
                s += baseCapacities[m];
            }
            weightSum[o] = s;
        }

        for (int c = 0; c < numCommodities; c++) {
            commodityIndex.put(commodities.get(c), c);
        }

        int denseIdx = 0;
        for (int m = 0; m < numMarkets; m++) {
            marketStart[m] = denseIdx;
            
            final BitSet bits = outputsPerMarket.get(m);
            for (int o = bits.nextSetBit(0); o >= 0; o = bits.nextSetBit(o + 1)) {
                denseOutputIndex[denseIdx] = o;
                denseMarketCap[denseIdx] = baseCapacities[m];
                denseWeightSum[denseIdx] = weightSum[o];

                final String pair = groupedOutputPairs.get(o);
                final String[] split = pair.split(EconomyLoop.KEY);
                final String indGroupID = split[0];
                final String outputID = split[1];

                final Industry ind;
                if (MatrixData.groupToMembers.get(pair) == null) {
                    ind = IndustryIOs.getRealIndustryFromBaseID(markets.get(m), indGroupID);
                } else {
                    final List<String> baseIDs = MatrixData.groupToMembers.get(pair).stream()
                        .map(p -> p.split(EconomyLoop.KEY)[0]).toList();
                    ind = IndustryIOs.getRealIndustryFromBaseID(markets.get(m), baseIDs);
                }

                final float limitFrac = ind == null ? 0f :
                    IndustryIOs.getIndConfig(ind).outputs.get(outputID).workerAssignableLimit;
                denseWorkerLimitFrac[denseIdx] = limitFrac;

                denseOutputMod[denseIdx] = (ind == null) ? 1.0 :
                    CompatLayer.getModifiersMult(ind, outputID, false);

                denseIdx++;
            }
        }
        marketStart[numMarkets] = denseIdx;

        return new DenseModel(denseCount, denseMarketCap, denseWeightSum, denseOutputMod, marketStart,
            denseOutputIndex, denseWorkerLimitFrac, commodityIndex
        );
    }

    public static double[] expandDenseSolution(double[] denseSolution, final List<BitSet> outputsPerMarket,
        final int numOutputs
    ) {
        final int numMarkets = outputsPerMarket.size();
        final double[] full = new double[numMarkets * numOutputs];

        int denseIdx = 0;
        for (int m = 0; m < numMarkets; m++) {
            final BitSet bits = outputsPerMarket.get(m);
            for (int o = bits.nextSetBit(0); o >= 0; o = bits.nextSetBit(o + 1)) {
                full[m * numOutputs + o] = denseSolution[denseIdx++];
            }
        }

        return full;
    }
}
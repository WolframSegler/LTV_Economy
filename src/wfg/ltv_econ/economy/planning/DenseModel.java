package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.IndustryGrouper.IndustryMatrixGrouped;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.util.ArrayMap;

public class DenseModel {
    public final int columnSize;
    public final long[] columnMarketCap;
    public final long[] columnWeightSum;
    public final double[] columnOutputMod;
    public final int[] marketStart;
    public final int[] columnOutputIndex;
    public final float[] columnWorkerLimitFrac;
    public final int[] columnFaction;
    public final int[] columnComIdx;
    public final boolean[] columnIsOutputAbstract;
    public final Map<String, Integer> commodityIndex;

    private DenseModel(int columnSize, long[] columnMarketCap, long[] columnWeightSum,
        double[] columnOutputMod, int[] marketStart, int[] columnOutputIndex,
        float[] columnWorkerLimitFrac, Map<String, Integer> commodityIndex, int[] columnFaction,
        boolean[] columnIsAbstract, int[] columnComIdx
    ) {
        this.columnSize = columnSize;
        this.columnMarketCap = columnMarketCap;
        this.columnWeightSum = columnWeightSum;
        this.columnOutputMod = columnOutputMod;
        this.marketStart = marketStart;
        this.columnOutputIndex = columnOutputIndex;
        this.columnWorkerLimitFrac = columnWorkerLimitFrac;
        this.commodityIndex = commodityIndex;
        this.columnFaction = columnFaction;
        this.columnIsOutputAbstract = columnIsAbstract;
        this.columnComIdx = columnComIdx;
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

        final long[] columnMarketCap = new long[denseCount];
        final long[] columnWeightSum = new long[denseCount];
        final double[] columnOutputMod = new double[denseCount];
        final int[] marketStart = new int[numMarkets + 1];
        final int[] columnOutputIndex = new int[denseCount];
        final float[] columnWorkerLimitFrac = new float[denseCount];
        final int[] columnFaction = new int[denseCount];
        final int[] columnComIdx = new int[denseCount];
        final boolean[] columnIsOutputAbstract = new boolean[denseCount];
        final Map<String, Integer> commodityIndex = new ArrayMap<>(numCommodities);

        final long[] baseCapacities = new long[numMarkets];
        final long[] weightSum = new long[numOutputs];
        final String[] outputIDs = new String[numOutputs];
        final Object[] industryKeys = new Object[numOutputs]; 

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

        for (int o = 0; o < numOutputs; ++o) {
            final String pair = groupedOutputPairs.get(o);

            final int sep = pair.indexOf(EconomyLoop.KEY);
            final String indGroupID = pair.substring(0, sep);
            final String outputID = pair.substring(sep + EconomyLoop.KEY.length());

            outputIDs[o] = outputID;

            final List<String> members = MatrixData.groupToMembers.get(pair);
            if (members == null) {
                industryKeys[o] = indGroupID;
            } else {
                final ArrayList<String> baseIDs = new ArrayList<>(members.size());
                for (String p : members) {
                    final int s = p.indexOf(EconomyLoop.KEY);
                    baseIDs.add(p.substring(0, s));
                }
                industryKeys[o] = baseIDs;
            }
        }

        int denseIdx = 0;
        for (int m = 0; m < numMarkets; m++) {
            marketStart[m] = denseIdx;
            
            final long baseCapacity = baseCapacities[m];
            final MarketAPI market = markets.get(m);
            final int factionIdx = EconomyConstants.factionIDs.indexOf(market.getFactionId());
            final BitSet bits = outputsPerMarket.get(m);
            for (int o = bits.nextSetBit(0); o >= 0; o = bits.nextSetBit(o + 1)) {
                final Object indKey = industryKeys[o];
                final String outputID = outputIDs[o];

                columnOutputIndex[denseIdx] = o;
                columnMarketCap[denseIdx] = baseCapacity;
                columnWeightSum[denseIdx] = weightSum[o];
                columnFaction[denseIdx] = factionIdx;
                columnComIdx[denseIdx] = commodityIndex.getOrDefault(outputID, -1);
                
                @SuppressWarnings("unchecked")
                final Industry ind = (indKey instanceof String s)
                    ? IndustryIOs.getRealIndustryFromBaseID(market, s)
                    : IndustryIOs.getRealIndustryFromBaseID(market, (List<String>) indKey);
                final OutputConfig output = IndustryIOs.getIndConfig(ind).outputs.get(outputID);

                columnWorkerLimitFrac[denseIdx] = output.workerAssignableLimit;
                columnIsOutputAbstract[denseIdx] = output.isAbstract || columnComIdx[denseIdx] == -1;
                columnOutputMod[denseIdx] = CompatLayer.getModifiersMult(ind, outputID, false);

                denseIdx++;
            }
        }
        marketStart[numMarkets] = denseIdx;

        return new DenseModel(denseCount, columnMarketCap, columnWeightSum, columnOutputMod, marketStart,
            columnOutputIndex, columnWorkerLimitFrac, commodityIndex, columnFaction, columnIsOutputAbstract,
            columnComIdx
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
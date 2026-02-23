package wfg.ltv_econ.economy.planning;

import java.util.BitSet;
import java.util.List;

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
    public final Industry[] denseIndustry;
    public final double[] denseOutputMod;
    public final int[] marketStart; 
    public final String[] denseOutputID;
    public final int[] denseOutputIndex;

    private DenseModel(int denseSize, long[] denseMarketCap, long[] denseWeightSum,
        Industry[] denseIndustry, double[] denseOutputMod, int[] marketStart, String[] denseOutputID,
        int[]  denseOutputIndex
    ) {
        this.denseSize = denseSize;
        this.denseMarketCap = denseMarketCap;
        this.denseWeightSum = denseWeightSum;
        this.denseIndustry = denseIndustry;
        this.denseOutputMod = denseOutputMod;
        this.marketStart = marketStart;
        this.denseOutputID = denseOutputID;
        this.denseOutputIndex = denseOutputIndex;
    }

    public static final DenseModel createDenseData(final List<MarketAPI> markets,
        final List<String> groupedOutputPairs, final List<BitSet> outputsPerMarket
    ) {
        final IndustryMatrixGrouped A = IndustryGrouper.getStaticGrouping();

        final int numMarkets = markets.size();
        final int numOutputs = groupedOutputPairs.size();

        int denseCount = 0;
        for (BitSet bits : outputsPerMarket) denseCount += bits.cardinality();

        final long[] denseMarketCap = new long[denseCount];
        final long[] denseWeightSum = new long[denseCount];
        final Industry[] denseIndustry = new Industry[denseCount];
        final double[] denseOutputMod = new double[denseCount];
        final int[] marketStart = new int[numMarkets + 1];
        final String[] denseOutputID = new String[denseCount];
        final int[] denseOutputIndex = new int[denseCount];

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

                denseOutputID[denseIdx] = outputID;

                final Industry ind;
                if (A.groupToMembers.get(pair) == null) {
                    ind = IndustryIOs.getRealIndustryFromBaseID(markets.get(m), indGroupID);
                } else {
                    final List<String> baseIDs = A.groupToMembers.get(pair).stream()
                        .map(p -> p.split(EconomyLoop.KEY)[0]).toList();
                    ind = IndustryIOs.getRealIndustryFromBaseID(markets.get(m), baseIDs);
                }

                denseIndustry[denseIdx] = ind;
                denseOutputMod[denseIdx] = (ind == null) ? 1.0 :
                    CompatLayer.getModifiersMult(ind, outputID, false);

                denseIdx++;
            }
        }
        marketStart[numMarkets] = denseIdx;

        return new DenseModel(denseCount, denseMarketCap, denseWeightSum, denseIndustry,
            denseOutputMod, marketStart, denseOutputID, denseOutputIndex
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
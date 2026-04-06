package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.native_ui.util.ArrayMap;

public final class IndustryGrouper {
    private IndustryGrouper() {}
    private static IndustryMatrixGrouped STATIC_GROUPING;

    public static synchronized IndustryMatrixGrouped getStaticGrouping() {
        if (STATIC_GROUPING == null) {
            double[][] A = IndustryMatrix.getMatrix();
            List<String> pairs = IndustryMatrix.getIndustryOutputPairs();
            STATIC_GROUPING = groupSimilarIndustries(A, pairs, 0.01);
        }
        return STATIC_GROUPING;
    }

    public static void invalidate() {
        STATIC_GROUPING = null;
    }

    /**
     * Groups similar industry::output pairs, but only if they have identical input/output patterns.
     * Industries with differing outputs or inputs are never grouped,
     * even if their matrix columns are close.
     */
    public static IndustryMatrixGrouped groupSimilarIndustries(
        double[][] A,
        List<String> industryOutputPairs,
        double similarityTolerance
    ) {
        final int rows = A.length; // commodities
        final int columns = A[0].length; // industry::output pairs

        final boolean[] grouped = new boolean[columns];
        final Map<String, List<String>> groupToMembers = new LinkedHashMap<>();
        final Map<String, String> memberToGroup = new ArrayMap<>(columns);

        final List<String> groupNames = new ArrayList<>();
        final List<double[]> groupedColumns = new ArrayList<>();

        for (int j = 0; j < columns; j++) {
            if (grouped[j]) continue;

            final String groupName = "group_" + industryOutputPairs.get(j);
            final List<String> members = new ArrayList<>();
            members.add(industryOutputPairs.get(j));
            grouped[j] = true;

            final double[] baseCol = new double[rows];
            for (int i = 0; i < rows; i++) baseCol[i] = A[i][j];

            // merge similar columns only if same input/output pattern
            for (int k = j + 1; k < columns; k++) {
                final String outputJ = industryOutputPairs.get(j).split("::")[1];
                final String outputK = industryOutputPairs.get(k).split("::")[1];
                if (!outputJ.equals(outputK)) continue;

                if (grouped[k] || !haveSameCommodityPattern(A, j, k)) continue;                

                if (columnDistance(A, baseCol, k) > similarityTolerance) continue;

                members.add(industryOutputPairs.get(k));
                grouped[k] = true;
            }

            final double[] avgCol = new double[rows];
            for (String member : members) {
                int idx = industryOutputPairs.indexOf(member);
                for (int i = 0; i < rows; i++) {
                    avgCol[i] += A[i][idx];
                }
            }
            for (int i = 0; i < rows; i++) avgCol[i] /= members.size();

            groupNames.add(groupName);
            groupedColumns.add(avgCol);
            groupToMembers.put(groupName, members);
            for (String member : members) memberToGroup.put(member, groupName);
        }

        final double[][] reduced = new double[rows][groupedColumns.size()];
        for (int g = 0; g < groupedColumns.size(); g++) {
            for (int i = 0; i < rows; i++) {
                reduced[i][g] = groupedColumns.get(g)[i];
            }
        }

        return new IndustryMatrixGrouped(reduced, groupNames, groupToMembers, memberToGroup);
    }

    private static boolean haveSameCommodityPattern(double[][] A, int colJ, int colK) {
        for (int i = 0; i < A.length; i++) {
            final double vj = A[i][colJ];
            final double vk = A[i][colK];

            if ((vj > 0 && vk <= 0) || (vj < 0 && vk >= 0)) return false;
        }
        return true;
    }

    private static final double columnDistance(double[][] A, double[] base, int col) {
        double sumSq = 0.0;
        for (int i = 0; i < A.length; i++) {
            double diff = A[i][col] - base[i];
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq);
    }

    public static final Pair<List<String>, List<BitSet>> applyGroupingToMarketData(
        List<MarketAPI> markets,
        List<String> pairs,
        List<BitSet> outputsPerMarket,
        Map<String, String> memberToGroup
    ) {
        final List<String> groupedPairs = new ArrayList<>();
        final Map<String, Integer> groupIndex = new ArrayMap<>(pairs.size());

        // Step 1: create new grouped list
        for (String pair : pairs) {
            String group = memberToGroup.getOrDefault(pair, pair);
            if (!groupIndex.containsKey(group)) {
                groupIndex.put(group, groupedPairs.size());
                groupedPairs.add(group);
            }
        }

        // Step 2: remap bitsets
        final List<BitSet> newOutputsPerMarket = new ArrayList<>(outputsPerMarket.size());
        for (BitSet oldBits : outputsPerMarket) {
            final BitSet newBits = new BitSet(groupedPairs.size());

            // iterate only over set bits (this is why BitSet is used)
            for (int idx = oldBits.nextSetBit(0); idx >= 0; idx = oldBits.nextSetBit(idx + 1)) {
                final String oldPair = pairs.get(idx);
                final String group = memberToGroup.getOrDefault(oldPair, oldPair);
                final int newIdx = groupIndex.get(group);
                newBits.set(newIdx);
            }

            newOutputsPerMarket.add(newBits);
        }

        return new Pair<>(groupedPairs, newOutputsPerMarket);
    }

    public static ArrayMap<MarketAPI, float[]> expandGroupedAssignments(
        Map<MarketAPI, float[]> groupedAssignments,
        IndustryMatrixGrouped group,
        List<MarketAPI> markets,
        List<String> industryOutputPairs
    ) {
        final ArrayMap<MarketAPI, float[]> expanded = new ArrayMap<>(markets.size());

        for (MarketAPI market : markets) {
            final float[] groupedArray = groupedAssignments.get(market);

            final float[] expandedArray = new float[industryOutputPairs.size()];

            int groupIndex = 0;
            for (String groupKey : group.groupToMembers.keySet()) {
                final float value = groupedArray[groupIndex];
                final List<String> originals = group.groupToMembers.get(groupKey);

                final List<String> valid = new ArrayList<>(3);
                for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
                    for (String pair : originals) {
                        if (IndustryConfigManager.getBaseIndIDifNoConfig(ind.getSpec()).equals(
                            pair.split(EconomyLoop.KEY)[0]
                        )) valid.add(pair);
                    }
                }
                if (valid.isEmpty()) {
                    groupIndex++;
                    continue;
                }

                for (String groupOrPair : valid) {
                    List<String> members = group.groupToMembers.getOrDefault(groupOrPair, List.of(groupOrPair));
                    float share = value / members.size();
                    for (String member : members) {
                        int targetIdx = industryOutputPairs.indexOf(member);
                        if (targetIdx < 0) continue;
                        expandedArray[targetIdx] = share;
                    }
                }
                groupIndex++;
            }
            expanded.put(market, expandedArray);
        }

        return expanded;
    }

    public static class IndustryMatrixGrouped {
        public final double[][] reducedMatrix;
        public final List<String> groupNames;
        public final Map<String, List<String>> groupToMembers;
        public final Map<String, String> memberToGroup;

        public IndustryMatrixGrouped(
            double[][] reducedMatrix,
            List<String> groupNames,
            Map<String, List<String>> groupToMembers,
            Map<String, String> memberToGroup
        ) {
            this.reducedMatrix = reducedMatrix;
            this.groupNames = groupNames;
            this.groupToMembers = groupToMembers;
            this.memberToGroup = memberToGroup;
        }
    }
}
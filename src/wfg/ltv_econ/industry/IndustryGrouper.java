package wfg.ltv_econ.industry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.economy.CommodityStats;

public final class IndustryGrouper {

    public static GroupedMatrix groupSimilarIndustries(
        double[][] A,
        List<String> industryOutputPairs,
        double similarityTolerance
    ) {
        final int m = A.length;       // commodities
        final int n = A[0].length;    // industry::output pairs

        final boolean[] grouped = new boolean[n];
        final Map<String, List<String>> groupToMembers = new LinkedHashMap<>();
        final Map<String, String> memberToGroup = new HashMap<>();

        final List<String> groupNames = new ArrayList<>();
        final List<double[]> groupedColumns = new ArrayList<>();

        for (int j = 0; j < n; j++) {
            if (grouped[j]) continue;

            final String groupName = "group_" + industryOutputPairs.get(j);
            final List<String> members = new ArrayList<>();
            members.add(industryOutputPairs.get(j));
            grouped[j] = true;

            final double[] baseCol = new double[m];
            for (int i = 0; i < m; i++) baseCol[i] = A[i][j];

            // merge similar columns
            for (int k = j + 1; k < n; k++) {
                if (grouped[k]) continue;

                double dist = columnDistance(A, baseCol, k);
                if (dist < similarityTolerance) {
                    members.add(industryOutputPairs.get(k));
                    grouped[k] = true;
                }
            }

            for (int k = j + 1; k < n; k++) {
                if (grouped[k] || !haveSameCommodityPattern(A, j, k)) continue;

                final double dist = columnDistance(A, baseCol, k);
                if (dist < similarityTolerance) {
                    members.add(industryOutputPairs.get(k));
                    grouped[k] = true;
                }
            }

            final double[] avgCol = new double[m];
            for (String member : members) {
                int idx = industryOutputPairs.indexOf(member);
                for (int i = 0; i < m; i++) {
                    avgCol[i] += A[i][idx];
                }
            }
            for (int i = 0; i < m; i++) avgCol[i] /= members.size();

            groupNames.add(groupName);
            groupedColumns.add(avgCol);
            groupToMembers.put(groupName, members);
            for (String member : members) memberToGroup.put(member, groupName);
        }

        final double[][] reduced = new double[m][groupedColumns.size()];
        for (int g = 0; g < groupedColumns.size(); g++) {
            for (int i = 0; i < m; i++) {
                reduced[i][g] = groupedColumns.get(g)[i];
            }
        }

        return new GroupedMatrix(reduced, groupNames, groupToMembers, memberToGroup);
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

    public static final Pair<List<String>, List<List<Integer>>> applyGroupingToMarketData(
        List<MarketAPI> markets,
        List<String> originalPairs,
        List<List<Integer>> outputsPerMarket,
        Map<String, String> originalToGroup
    ) {
        final List<String> groupedPairs = new ArrayList<>();
        final Map<String, Integer> groupIndex = new HashMap<>();

        // Step 1: create new grouped list
        for (String pair : originalPairs) {
            String group = originalToGroup.getOrDefault(pair, pair);
            if (!groupIndex.containsKey(group)) {
                groupIndex.put(group, groupedPairs.size());
                groupedPairs.add(group);
            }
        }

        // Step 2: rewrite outputsPerMarket using new indices
        final List<List<Integer>> newOutputsPerMarket = new ArrayList<>();
        for (List<Integer> oldList : outputsPerMarket) {
            Set<Integer> newSet = new HashSet<>();
            for (int idx : oldList) {
                String oldPair = originalPairs.get(idx);
                String group = originalToGroup.getOrDefault(oldPair, oldPair);
                Integer newIdx = groupIndex.get(group);
                if (newIdx != null) newSet.add(newIdx);
            }
            newOutputsPerMarket.add(new ArrayList<>(newSet));
        }

        return new Pair<>(groupedPairs, newOutputsPerMarket);
    }

    public static Map<MarketAPI, float[]> expandGroupedAssignments(
        Map<MarketAPI, float[]> groupedAssignments,
        GroupedMatrix group,
        List<MarketAPI> markets,
        List<String> industryOutputPairs
    ) {
        final Map<MarketAPI, float[]> expanded = new HashMap<>();

        for (MarketAPI market : markets) {
            final float[] groupedArray = groupedAssignments.get(market);

            final float[] expandedArray = new float[industryOutputPairs.size()];

            int groupIndex = 0;
            for (String groupKey : group.groupToMembers.keySet()) {
                final float value = groupedArray[groupIndex];
                final List<String> originals = group.groupToMembers.get(groupKey);

                final List<String> valid = new ArrayList<>(3);
                for (Industry ind : CommodityStats.getVisibleIndustries(market)) {
                    for (String pair : originals) {
                        if (IndustryIOs.getBaseIndIDifNoConfig(ind.getSpec()).equals(
                            pair.split("::")[0]
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

    public static class GroupedMatrix {
        public final double[][] reducedMatrix;
        public final List<String> groupNames;
        public final Map<String, List<String>> groupToMembers;
        public final Map<String, String> memberToGroup;

        public GroupedMatrix(
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

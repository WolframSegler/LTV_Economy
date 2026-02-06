package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math4.legacy.optim.MaxIter;
import org.apache.commons.math4.legacy.optim.PointValuePair;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraintSet;
import org.apache.commons.math4.legacy.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math4.legacy.optim.linear.NonNegativeConstraint;
import org.apache.commons.math4.legacy.optim.linear.PivotSelectionRule;
import org.apache.commons.math4.legacy.optim.linear.Relationship;
import org.apache.commons.math4.legacy.optim.linear.SimplexSolver;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.industry.IndustryGrouper;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.IndustryGrouper.GroupedMatrix;

public class WorkforcePlanner {

    private static final Logger logger = Global.getLogger(WorkforcePlanner.class);

    public static final double WORKER_COST = 1;

    /**
     * For reduced / grouped matrixes
     */
    public static final void logInputMatrix(GroupedMatrix grouped, List<String> coms) {
        logInputMatrix(grouped.reducedMatrix, grouped.groupNames, coms);
    }

    /**
     * For non-reduced / non-grouped Matrixes
     */
    public static final void logInputMatrix(double[][] A, List<String> industryOutputs, List<String> coms) {
        StringBuilder csv = new StringBuilder(4096);
        csv.append("Commodity/IndustryOutput,");

        // Header: industry_output pairs
        for (String indOut : industryOutputs) {
            csv.append(indOut).append(",");
        }
        csv.append("\n");

        // Matrix rows
        for (int i = 0; i < coms.size(); i++) {
            String commodity = coms.get(i);
            csv.append(commodity).append(",");

            for (int o = 0; o < A[i].length; o++) {
                csv.append(String.format("%.2f", A[i][o]));
                if (o < A[i].length - 1) csv.append(",");
            }
            csv.append("\n");
        }

        logger.info("=== Input Matrix A (Industry_Output Columns) ===\n" + csv);
    }

    public static final void logDemandVector(double[] d, List<String> commodities) {
        StringBuilder demandLog = new StringBuilder("\n=== Demand Vector d ===\n");
        for (int i = 0; i < commodities.size(); i++) {
            demandLog.append(String.format("%s: %d\n", commodities.get(i), (long) d[i]));
        }

        logger.info(demandLog);
    }
    
    public static final void logWorkerAssignments(
        Map<MarketAPI, float[]> assignedWorkersPerMarket,
        List<String> industryOutputPairs
    ) {
        double totalAssigned = 0.0;
        final long workersAvailable = EconomyInfo.getGlobalWorkerCount(false);
        final StringBuilder sb = new StringBuilder("\n=== Worker Distribution Report ===\n");

        // 1. Global overview
        for (float[] arr : assignedWorkersPerMarket.values()) {
            for (float v : arr) totalAssigned += v;
        }

        sb.append(String.format(Locale.ROOT, "Total workers assigned: %.0f\n", totalAssigned));
        sb.append(String.format(Locale.ROOT, "Total workers available: %d\n", workersAvailable));
        sb.append(String.format(Locale.ROOT, "Unemployment: %.2f%%\n",
            100f - 100f*(totalAssigned/workersAvailable)
        ));

        // 2. Market-level details
        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.entrySet()) {
            final MarketAPI market = entry.getKey();
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);
            final float pool = (cond != null) ? cond.getWorkerPool() : 0f;

            float used = 0f;
            for (float v : entry.getValue()) used += v;

            final float utilization = (pool > 0) ? used / pool : 0f;
            sb.append(String.format(Locale.ROOT,
                    "[%s] used %.0f / %.0f (%.1f%%)\n",
                    market.getName(), used, pool, utilization * 100f));

            for (int i = 0; i < industryOutputPairs.size(); i++) {
                final float assigned = entry.getValue()[i];
                if (assigned > 0.01f * pool) {
                    sb.append(String.format("   - %-25s : %.0f (%.2f%%)\n",
                            industryOutputPairs.get(i),
                            assigned,
                            100f * assigned / Math.max(1f, pool)));
                }
            }

            if (used > pool + 1e-3f)
                sb.append("   ⚠ Overcapacity!\n");
            else if (used < 0)
                sb.append("   ⚠ Negative assignment!\n");

            sb.append("\n");
        }

        sb.append("=== End of Report ===\n");
        logger.info(sb.toString());
    }

    public static final void logOutputsPerMarketCSV(
        List<List<Integer>> outputsPerMarket,
        List<MarketAPI> markets,
        List<String> industryOutputPairs
    ) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Output/Market");
        for (MarketAPI market : markets) sb.append(",").append(market.getName());
        sb.append("\n");

        for (int o = 0; o < industryOutputPairs.size(); o++) {
            sb.append(industryOutputPairs.get(o));

            for (int m = 0; m < markets.size(); m++) {
                final boolean exists = outputsPerMarket.get(m).contains(o);

                sb.append(",");
                sb.append(exists ? "O" : "X");
            }
            sb.append("\n");
        }

        logger.info(sb.toString());
    }

    /**
     * Computes optimal worker assignments across markets for all outputs, 
     * satisfying commodity demands while respecting market capacities, per-output limits,
     * fair spread and shortage adjustments.
     * 
     * @param groupedMatrix Matrix of size (commodities x reduced_outputs) and grouping data
     * @param demand Vector of size (commodities)
     * @param markets List of markets to distribute to
     * @param industryOutputPairs industryID::outputID
     * @param outputsPerMarket List of markets containing list of indexes for industryOutputPairs
     * @return Map of MarketAPI to float[] arrays representing worker assignments per output
     */
    public static Map<MarketAPI, float[]> computeWorkerAllocations(
        final GroupedMatrix groupedMatrix, final double[] demand,
        List<MarketAPI> markets, List<String> industryOutputPairs,
        List<List<Integer>> outputsPerMarket, List<String> commodities
    ) {
        final Pair<List<String>, List<List<Integer>>> groupedData =
            IndustryGrouper.applyGroupingToMarketData(
                markets, industryOutputPairs,
                outputsPerMarket, groupedMatrix.memberToGroup
            );
        final double[][] A = groupedMatrix.reducedMatrix;
        final List<String> groupedOutputPairs = groupedData.one;
        outputsPerMarket = groupedData.two;

        final int numMarkets = markets.size();
        final int numOutputs = groupedOutputPairs.size();
        final int numCommodities = A.length;

        final long[] baseCapacities = new long[numMarkets];
        final long[] weightSum = new long[numOutputs];
        final Industry[][] industryCache = new Industry[numMarkets][numOutputs];
        final float[][] outputMods = new float[numMarkets][numOutputs];
        final Map<String, Integer> commodityIndex = new HashMap<>(numCommodities);

        // 1) PRE-COMPUTES
        // Market capacities
        for (int m = 0; m < numMarkets; m++) {
            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(markets.get(m));
            baseCapacities[m] = (pool != null) ? pool.getWorkerPool() : 0;
        }

        // Weight sums per output (only over markets that can produce it)
        for (int o = 0; o < numOutputs; o++) {
            long s = 0l;
            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;
                s += baseCapacities[m];
            }
            weightSum[o] = s;
        }

        // Industry instances per output per market and ind modifiers
        for (int o = 0; o < numOutputs; o++) {
            final String pair = groupedOutputPairs.get(o);
            final String indGroupID = pair.split(EconomyLoop.KEY)[0];
            final String outputID = pair.split(EconomyLoop.KEY)[1];

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;

                final MarketAPI market = markets.get(m);
                final Industry ind;
                if (groupedMatrix.groupToMembers.get(pair) == null) {
                    ind = IndustryIOs.getRealIndustryFromBaseID(market, indGroupID);
                } else {
                    final List<String> baseIDs = groupedMatrix.groupToMembers.get(pair).stream()
                        .map(p -> p.split(EconomyLoop.KEY)[0])
                        .toList();

                    ind = IndustryIOs.getRealIndustryFromBaseID(market, baseIDs);
                }

                industryCache[m][o] = ind;
                if (ind != null) outputMods[m][o] = CompatLayer.getModifiersMult(ind, outputID, false);
            }
        }
        // Reverse lookup map for commodities
        for (int c = 0; c < numCommodities; c++) {
            commodityIndex.put(commodities.get(c), c);
        }

        // INDEXES
        final int idxSStart = numMarkets * numOutputs;
        final int idxSpreadStart = idxSStart + numCommodities;
        final int idxTotalWorkersStart = idxSpreadStart + numOutputs;
        final int nVars = idxTotalWorkersStart + numOutputs;
        final Function<Integer, Integer> idxS = c -> idxSStart + c;
        final Function<Integer,Integer> idxSpread = o -> idxSpreadStart + o;
        final Function<Integer,Integer> idxTotalW = o -> idxTotalWorkersStart + o;
        final BiFunction<Integer, Integer, Integer> idxW = (m, o) -> m * numOutputs + o;

        // 2) OBJECTIVE: minimize SLACK_COST * sum(slack[c]) + WORKER_COST * sum(w)
        final double[] objective = new double[nVars];

        for (int o = 0; o < numOutputs; o++) {
            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;
                objective[idxW.apply(m, o)] = WORKER_COST;
            }
        }
        for (int o = 0; o < numOutputs; o++) objective[idxTotalW.apply(o)] = 0.0; // total workers per o
        for (int c = 0; c < numCommodities; c++) objective[idxS.apply(c)] = EconomyConfig.ECON_DEFICIT_COST;
        for (int o = 0; o < numOutputs; o++) {
            objective[idxSpread.apply(o)] = EconomyConfig.CONCENTRATION_COST;
        }

        final LinearObjectiveFunction f = new LinearObjectiveFunction(objective, 0.0);
        final List<LinearConstraint> const_constraints = new ArrayList<>();

        // 3) Market capacity constraints: sum_o w[m,o] <= baseCapacities[m]
        for (int m = 0; m < numMarkets; m++) {
            final double[] coeffs = new double[nVars];
            for (int o = 0; o < numOutputs; o++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;
                coeffs[idxW.apply(m, o)] = 1.0;
            }
            const_constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, baseCapacities[m]));
        }

        // 4) Per-(market,output) worker caps: w[m,o] <= limit * baseCapacity[m]
        final double[] workerLimitCoeffs = new double[nVars]; // allocate once
        for (int o = 0; o < numOutputs; o++) {
            final String outputID = groupedOutputPairs.get(o).split(EconomyLoop.KEY)[1];

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;

                final float limit = IndustryIOs.getIndConfig(industryCache[m][o])
                    .outputs.get(outputID).workerAssignableLimit;
                final double rhs = limit * baseCapacities[m];

                Arrays.fill(workerLimitCoeffs, 0.0); // reset array
                workerLimitCoeffs[idxW.apply(m, o)] = 1.0;

                const_constraints.add(new LinearConstraint(workerLimitCoeffs, Relationship.LEQ, rhs));
            }
        }

        // 5) Spreading assignments across markets
        for (int o = 0; o < numOutputs; o++) {
            if (weightSum[o] <= 0) continue;

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;

                final double proportion = baseCapacities[m] / (double) weightSum[o];
                final double tokenRatio = proportion * EconomyConfig.MIN_WORKER_FRACTION;

                final double[] coeffs = new double[nVars];
                coeffs[idxW.apply(m, o)] = 1.0;
                coeffs[idxSpread.apply(o)] = 1.0;
                coeffs[idxTotalW.apply(o)] = -proportion;                
                const_constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, tokenRatio));
            }
        }

        // 6) Set totalW tracking var to total workers assigned
        for (int o = 0; o < numOutputs; o++) {
            final double[] coeffs = new double[nVars];
            
            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(o)) continue;
                coeffs[idxW.apply(m,o)] = 1.0;
            }

            // subtract the tracker variable
            coeffs[idxTotalW.apply(o)] = -1.0;
            const_constraints.add(new LinearConstraint(coeffs, Relationship.EQ, 0.0));
        }

        double[] vars = null;
        final double[] netCommodity = new double[numCommodities];
        final double[] comAvailability = new double[numCommodities];
        final double[] shortage_mult = new double[numOutputs];

        for (int i = 0; i < EconomyConfig.ECON_ALLOCATION_PASSES; i++) {
            final List<LinearConstraint> constraints = new ArrayList<>();
            constraints.addAll(const_constraints);
            Arrays.fill(netCommodity, 0.0);
            Arrays.fill(comAvailability, 0.0);
            Arrays.fill(shortage_mult, 1.0);

            if (i != 0) {
                for (int c = 0; c < numCommodities; c++) {
                    for (int o = 0; o < numOutputs; o++) {
                        final double base = A[c][o];
                        if (base == 0.0) continue;
    
                        for (int m = 0; m < numMarkets; m++) {
                            if (!outputsPerMarket.get(m).contains(o)) continue;
    
                            final double w = vars[idxW.apply(m, o)];
                            if (w == 0.0) continue;
    
                            final double mod = base > 0.0 ? outputMods[m][o] :
                                CompatLayer.getModifiersMult(
                                    industryCache[m][o],
                                    commodities.get(c),
                                    true
                                );
    
                            netCommodity[c] += base * mod * w;
                        }
                    }
                }
    
                for (int c = 0; c < numCommodities; c++) {
                    if (demand[c] <= 0.0) comAvailability[c] = 1.0;
                    else comAvailability[c] = Math.min(1.0, netCommodity[c] / demand[c]);
                }
    
                for (int o = 0; o < numOutputs; o++) {
                    final String pair = groupedOutputPairs.get(o);
                    final String industryID = pair.split(EconomyLoop.KEY)[0];
                    final String outputID = pair.split(EconomyLoop.KEY)[1];
    
                    final Map<String, Map<String, Map<String, Float>>> byIndustry =
                        IndustryIOs.getBaseInputsMap();
    
                    final Map<String, Map<String, Float>> byOutput =
                        byIndustry.get(industryID);
                    if (byOutput == null) continue;
    
                    final Map<String, Float> inputs =
                        byOutput.get(outputID);
                    if (inputs == null || inputs.isEmpty()) continue;
    
                    double deficitWeightSum = 0.0;
                    for (float v : inputs.values()) deficitWeightSum += v;
                    if (deficitWeightSum <= 0.0) continue;
    
                    double deficit = 0.0;
    
                    for (Map.Entry<String, Float> e : inputs.entrySet()) {
                        final String inputID = e.getKey();
                        if (IndustryIOs.ABSTRACT_COM.contains(inputID)) continue;
    
                        final int c = commodityIndex.get(inputID);
                        final double availability = comAvailability[c];
    
                        final double weightNorm = e.getValue() / deficitWeightSum;
                        deficit += weightNorm * (1.0 - availability);
                    }
    
                    if (deficit > 0.0) {
                        shortage_mult[o] = Math.max(1.0 - deficit, 0.01);
                    }
                }
            }

            // 7) PRIMARY: Commodity production constraints:
            // For each commodity c: sum_{o,m} A[c][o] * mod[m,o] * w[m,o] + slack[c] >= demand[c] * buffer
            final double[] quotaCoeffs = new double[nVars]; // allocate once
            for (int c = 0; c < numCommodities; c++) {
                Arrays.fill(quotaCoeffs, 0.0);

                for (int o = 0; o < numOutputs; o++) {
                    for (int m = 0; m < numMarkets; m++) {
                        if (!outputsPerMarket.get(m).contains(o)) continue;
                        
                        quotaCoeffs[idxW.apply(m, o)] += A[c][o] * outputMods[m][o] * shortage_mult[o];
                    }
                }

                quotaCoeffs[idxS.apply(c)] = 1.0; // slack for commodity
                final double target = demand[c] * EconomyConfig.PRODUCTION_BUFFER;
                constraints.add(new LinearConstraint(quotaCoeffs, Relationship.GEQ, target));
            }

            final SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(
                new MaxIter(10000), f,
                new LinearConstraintSet(constraints),
                GoalType.MINIMIZE,
                new NonNegativeConstraint(true),
                PivotSelectionRule.DANTZIG
            );
            vars = solution.getPoint();
        }

        // 8) Extract assignments w[m,o] into map for grouped outputs
        final Map<MarketAPI, float[]> groupedAssignments = new HashMap<>();
        for (int m = 0; m < numMarkets; m++) {
            final float[] arr = new float[numOutputs];
            for (int o = 0; o < numOutputs; o++) {
                arr[o] = (float) vars[idxW.apply(m, o)];
            }
            groupedAssignments.put(markets.get(m), arr);
        }

        return IndustryGrouper.expandGroupedAssignments(
            groupedAssignments, groupedMatrix,
            markets, industryOutputPairs
        );
    }
}
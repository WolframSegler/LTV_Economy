package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.industry.IndustryGrouper;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.industry.IndustryGrouper.GroupedMatrix;

public class WorkforcePlanner {

    /*
    * NOTE ON SLACK / DEFICIT HANDLING
    *
    * PROBLEM:
    * Currently, the Solver treats all deficits (slack variables) equally, regardless of the commodity's price.
    * That is, a deficit of 1 unit of food is considered just as bad as a deficit of 1 unit of hand_weapons,
    * even though the economic value of a hand_weapon is much higher than a unit of food.
    *
    * CONSEQUENCE:
    * This can lead the solver to "give up" on high-value commodities that are produced in small quantities,
    * because minimizing slack in cheaper, larger-quantity commodities reduces the objective function more efficiently.
    * For example, expensive goods like hand_weapons or drugs may end up with zero assigned workers
    * while the solver prioritizes large-volume low-cost items like food or fuel.
    *
    * POTENTIAL SOLUTION:
    * Convert the demand vector from unit counts to monetary values before solving the LP:
    *   monetaryDemand[i] = unitDemand[i] * pricePerUnit[i]
    * This way, the slack penalty naturally scales with the economic value of the commodity,
    * so the solver prioritizes fulfilling deficits equally over all commodities.
    *
    * RESOLUTION:
    * Currently I am happy with the way the LP Solver treats unit deficits equally regardless of their value.
    * I might change this in the future, hence the note.
    */

    /**
    * Combined Solver Concept for Worker Assignment Optimization
    *
    * This describes a potential single-step approach to replace the current two-step process:  
    * 1. The calculateGlobalWorkerTargets solver calculates total workers needed per commodity.
    * 2. The allocateWorkersToMarkets solver assigns those workers to specific markets.
    *
    * In the combined solver idea, the objective function would directly encode the market-specific  
    * production efficiencies of each worker. Each variable x_{m,j} represents workers assigned to
    * market m for output j, and the objective would be maximized according to:
    *
    * objectiveCoeffs[m*numOutputs + j] = WORKER_COST - MARKET_MODIFIER_SCALER * outputMultiplier[m][j];
    *  
    * where outputMultiplier captures the true productivity of market m for output j.
    *
    * Notes on the matter:  
    * * Constraints must still enforce market capacity, total output caps, and ideal spread
    * * Large production multipliers may lead to over-concentration; scaling must be tuned.
    * * This approach allows the solver to "see" the real benefit of each worker assignment directly,
    * removing the need for the separate global-demand step.
    * * Potentially faster convergence with fewer solver steps.
    * * May complicate constraint balancing and require careful tuning to avoid pathological assignments.
    *
    * RESOLUTION:
    * Currently, the two-step solution is sufficient. This is kept as a reference for future me.
    */


    public static final double WORKER_COST = 1;     // penatly for a unit of worker used.
    public static final double SLACK_COST = 1300;   // penalty for a unit of deficit regardless of value.

    // Any lower than 1300 causes the solver to not produce hand_weapons

    /**
     * Calculate global worker requirements per output using linear programming with slack variables.
     * Ensures each output's demand is met if possible, or distributes shortfall via slack.
     *
     * @param A Matrix of size (commodities x outputs)
     * @param d Demand vector of size (commodities)
     * @return Non-negative worker assignments to outputs
     */
    public static double[] calculateGlobalWorkerTargets(double[][] A, double[] d) {
        final int m = A.length;       // number of commodities
        final int n = A[0].length;    // number of outputs

        final double[] objectiveCoeffs = new double[n + m];

        for (int j = 0; j < n; j++) {
            objectiveCoeffs[j] = WORKER_COST;
        }
        for (int i = 0; i < m; i++) {
            objectiveCoeffs[n + i] = SLACK_COST;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoeffs, 0.0);
        List<LinearConstraint> constraints = new ArrayList<>();

        // Constraints: A*x + s >= d * buffer  ->  -A*x - s <= -d * buffer
        for (int i = 0; i < m; i++) {
            final double[] coeffs = new double[n + m];
            for (int j = 0; j < n; j++) coeffs[j] = -A[i][j]; // industry coefficients
            coeffs[n + i] = -1.0; // slack variable coefficient
            constraints.add(new LinearConstraint(
                coeffs, Relationship.LEQ, -d[i] * EconomyConfig.PRODUCTION_BUFFER
            ));
        }

        // total assigned workers ≤ total available workers
        final double[] totalWorkerCoeffs = new double[n + m];
        for (int j = 0; j < n; j++) {
            totalWorkerCoeffs[j] = 1.0;
        }
        constraints.add(new LinearConstraint(
            totalWorkerCoeffs, Relationship.LEQ, EconomyEngine.getGlobalWorkerCount(false)
        ));

        // Non-negative net production for all commodities
        for (int i = 0; i < m; i++) {
            double[] coeffs = new double[n + m];
            for (int j = 0; j < n; j++) {
                coeffs[j] = A[i][j];
            }
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0.0));
        }

        // Solve
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(
            new MaxIter(1000),
            f,
            new LinearConstraintSet(constraints),
            GoalType.MINIMIZE,
            new NonNegativeConstraint(true)
        );

        final double[] vars = solution.getPoint();
        // First n variables are the worker assignments
        final double[] workers = new double[n];
        System.arraycopy(vars, 0, workers, 0, n);
        return workers;
    }

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

            for (int j = 0; j < A[i].length; j++) {
                csv.append(String.format("%.2f", A[i][j]));
                if (j < A[i].length - 1) csv.append(",");
            }
            csv.append("\n");
        }

        Global.getLogger(WorkforcePlanner.class)
            .info("=== Input Matrix A (Industry_Output Columns) ===\n" + csv);
    }

    public static final void logDemandVector(double[] d, List<String> commodities) {
        StringBuilder demandLog = new StringBuilder("\n=== Demand Vector d ===\n");
        for (int i = 0; i < commodities.size(); i++) {
            demandLog.append(String.format("%s: %d\n", commodities.get(i), (long) d[i]));
        }

        Global.getLogger(WorkforcePlanner.class).info(demandLog);
    }

    /**
     * For reduced / grouped matrixes
     */
    public static final void logWorkerTargets(double[] workerVector, GroupedMatrix groupedMatrix) {
        logWorkerTargets(workerVector, groupedMatrix.groupNames);
    }

    /**
     * For non-reduced / non-grouped Matrixes
     */
    public static final void logWorkerTargets(double[] workerVector, List<String> industryOutputPairs) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < workerVector.length; i++) {
            String line = industryOutputPairs.get(i) + ": " + Math.round(workerVector[i]);
            lines.add(line);
        }
        String logString = lines.stream()
            .map(s -> "    " + s)
            .collect(Collectors.joining("\n"));
        Global.getLogger(WorkforcePlanner.class).info(logString);
    }

    public static final void logCommodityResults(GroupedMatrix group, double[] workerVector, List<String> commodities) {
        logCommodityResults(group.reducedMatrix, workerVector, commodities);
    }

    public static final void logCommodityResults(double[][] A, double[] workerVector, List<String> commodities) {
        int rows = A.length;
        int cols = A[0].length;
        double[] result = new double[rows];

        for (int i = 0; i < rows; i++) {
            double sum = 0;
            for (int j = 0; j < cols; j++) {
                sum += A[i][j] * workerVector[j];
            }
            result[i] = sum;
        }

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            String line = commodities.get(i) + ": " + Math.round(result[i]);
            lines.add(line);
        }

        StringBuilder builder = new StringBuilder("\n=== Commodity Results ===\n");
        builder.append(lines.stream()
                .map(s -> "    " + s)
                .collect(Collectors.joining("\n")));
        Global.getLogger(WorkforcePlanner.class).info(builder.toString());
    }

    /**
     * Allocate global worker targets to individual markets fairly, respecting market capacities.
     *
     * @param targetVector Global worker assignments per output (from calculateGlobalWorkerTargets)
     * @param markets List of markets to distribute to
     * @param industryOutputPairs industryID::outputID
     * @param outputsPerMarket List of markets containing list of indexes for industryOutputPairs
     * @param reducedMatrix containts the group data of the reduced matrix.
     * @return Map from market to worker assignments array (length = industryOutputPairs.size())
     */
    public static Map<MarketAPI, float[]> allocateWorkersToMarkets(
        double[] targetVector,
        List<MarketAPI> markets,
        List<String> industryOutputPairs,
        List<List<Integer>> outputsPerMarket,
        GroupedMatrix groupingData
    ) {
        Pair<List<String>, List<List<Integer>>> groupedData =
        IndustryGrouper.applyGroupingToMarketData(
            markets,
            industryOutputPairs,
            outputsPerMarket,
            groupingData.memberToGroup
        );
        final List<String> groupedOutputPairs = groupedData.one;
        outputsPerMarket = groupedData.two;

        final int numMarkets = markets.size();
        final int numOutputs = groupedOutputPairs.size();
        final int nVars = numMarkets * numOutputs + numOutputs; // original + slack per output
        final int slackStart = numMarkets * numOutputs;

        final long[] baseCapacities = new long[numMarkets];
        for (int m = 0; m < numMarkets; m++) {
            final MarketAPI market = markets.get(m);
            final WorkerPoolCondition pool = WorkerPoolCondition.getPoolCondition(market);
            baseCapacities[m] = (pool != null) ? pool.getWorkerPool() : 0;
        }

        final double[] objectiveCoeffs = new double[nVars];

        for (int j = 0; j < numOutputs; j++) {
            final String pair = groupedOutputPairs.get(j);
            final String indGroupID = pair.split(EconomyEngine.KEY)[0];
            final String outputID = pair.split(EconomyEngine.KEY)[1];

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(j)) continue;

                final MarketAPI market = markets.get(m);
                final Industry ind;
                if (groupingData.groupToMembers.get(pair) == null) {
                    ind = IndustryIOs.getRealIndustryFromBaseID(
                        market, indGroupID
                    );
                } else {
                    final List<String> baseIDs = groupingData.groupToMembers.get(pair).stream()
                        .map(p -> p.split(EconomyEngine.KEY)[0]).toList();

                    ind = IndustryIOs.getRealIndustryFromBaseID(market, baseIDs);
                }
                final float outputMultiplier = CompatLayer.getModifiersMult(ind, outputID, false);

                objectiveCoeffs[m * numOutputs + j] = WORKER_COST +
                    EconomyConfig.MARKET_MODIFIER_SCALER * outputMultiplier;
            }
        }
        for (int j = 0; j < numOutputs; j++) {
            objectiveCoeffs[slackStart + j] = -EconomyConfig.CONCENTRATION_COST;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoeffs, 0.0);
        final List<LinearConstraint> constraints = new ArrayList<>();

        // 1) Market capacity constraint: sum over outputs <= workerPool
        for (int m = 0; m < numMarkets; m++) {
            final double[] coeffs = new double[nVars];
            for (int j = 0; j < numOutputs; j++) {
                if (outputsPerMarket.get(m).contains(j)) {
                    coeffs[m * numOutputs + j] = 1.0;
                }
            }
            constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, baseCapacities[m]));
        }

        // 2) Output cap (targetVector): sum_m x_mj <= targetVector[j]
        for (int j = 0; j < numOutputs; j++) {
            final double[] coeffs = new double[nVars];
            for (int m = 0; m < numMarkets; m++) {
                if (outputsPerMarket.get(m).contains(j)) {
                    coeffs[m * numOutputs + j] = 1.0;
                }
            }

            constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, targetVector[j]));
        }

        // 3) Output capacity constraint: workersAssigned <= workerLimit
        for (int j = 0; j < numOutputs; j++) {
            final String pair = groupedOutputPairs.get(j);
            final String indGroupID = pair.split(EconomyEngine.KEY)[0];
            final String outputID = pair.split(EconomyEngine.KEY)[1];

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(j)) continue;

                final MarketAPI market = markets.get(m);

                final Industry ind;
                if (groupingData.groupToMembers.get(pair) == null) {
                    ind = IndustryIOs.getRealIndustryFromBaseID(
                        market, indGroupID
                    );
                } else {
                    final List<String> baseIDs = groupingData.groupToMembers.get(pair).stream()
                        .map(p -> p.split(EconomyEngine.KEY)[0])
                        .toList();

                    ind = IndustryIOs.getRealIndustryFromBaseID(market, baseIDs);
                }

                final float limit = IndustryIOs.getIndConfig(ind)
                    .outputs.get(outputID).workerAssignableLimit;

                // create constraint: x_{m,j} <= limit * baseCapacity
                final double[] coeffsLimit = new double[nVars];
                coeffsLimit[m * numOutputs + j] = 1.0;
                constraints.add(new LinearConstraint(
                    coeffsLimit,
                    Relationship.LEQ,
                    limit * baseCapacities[m]
                ));
            }
        }

        // 4) Spreading assignments across markets
        for (int j = 0; j < numOutputs; j++) {
            double totalCapacity = 0.0;
            double[] effectiveCapacities = new double[numMarkets];

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(j)) continue;

                final long baseCapacity = baseCapacities[m];

                effectiveCapacities[m] = baseCapacity;
                totalCapacity += baseCapacity;
            }

            for (int m = 0; m < numMarkets; m++) {
                if (!outputsPerMarket.get(m).contains(j)) continue;

                final double preferredWorkers = (totalCapacity > 0) ?
                    effectiveCapacities[m] / totalCapacity * targetVector[j] : 0.0;

                // x_{m,j} >= preferred - s_j
                final double[] coeffsGE = new double[nVars];
                coeffsGE[m * numOutputs + j] = 1.0;
                coeffsGE[slackStart + j] = 1.0;
                constraints.add(new LinearConstraint(
                    coeffsGE, Relationship.GEQ, preferredWorkers * (1 - EconomyConfig.IDEAL_SPREAD_TOLERANCE)
                ));
            }
        }

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(
            new MaxIter(2000),
            f,
            new LinearConstraintSet(constraints),
            GoalType.MAXIMIZE,
            new NonNegativeConstraint(true),
            PivotSelectionRule.DANTZIG
        );

        final double[] vars = solution.getPoint();
        final Map<MarketAPI, float[]> marketAssignments = new HashMap<>();

        for (int m = 0; m < numMarkets; m++) {
            float[] assignment = new float[numOutputs];
            for (int j = 0; j < numOutputs; j++) {
                assignment[j] = (float) vars[m * numOutputs + j];
            }
            marketAssignments.put(markets.get(m), assignment);
        }

        final Map<MarketAPI, float[]> expandedAssignments =
        IndustryGrouper.expandGroupedAssignments(
            marketAssignments,
            groupingData,
            markets,
            industryOutputPairs
        );

        return expandedAssignments;
    }

    public static final void logWorkerAssignments(
        Map<MarketAPI, float[]> assignedWorkersPerMarket,
        List<String> industryOutputPairs,
        double[] workerVector
    ) {
        float totalAssigned = 0f;
        float totalRequired = 0f;
        StringBuilder sb = new StringBuilder("\n=== Worker Distribution Report ===\n");

        // 1. Global overview
        for (double w : workerVector) totalRequired += w;
        for (float[] arr : assignedWorkersPerMarket.values()) {
            for (float v : arr) totalAssigned += v;
        }

        sb.append(String.format(Locale.ROOT, "Total workers required: %.0f\n", totalRequired));
        sb.append(String.format(Locale.ROOT, "Total workers assigned: %.0f\n", totalAssigned));
        sb.append(String.format(Locale.ROOT, "Discrepancy: %.2f%%\n\n",
                100f * (totalAssigned - totalRequired) / Math.max(1f, totalRequired)));

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
        Global.getLogger(WorkforcePlanner.class).info(sb.toString());
    }

    public static final void logOutputsPerMarketCSV(
        List<List<Integer>> outputsPerMarket,
        List<MarketAPI> markets,
        List<String> industryOutputPairs
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("Output/Market");
        for (MarketAPI market : markets) {
            sb.append(",").append(market.getName());
        }
        sb.append("\n");

        for (int j = 0; j < industryOutputPairs.size(); j++) {
            sb.append(industryOutputPairs.get(j));

            for (int m = 0; m < markets.size(); m++) {
                List<Integer> available = outputsPerMarket.get(m);
                boolean exists = available.contains(j);

                sb.append(",");
                sb.append(exists ? "O" : "X");
            }
            sb.append("\n");
        }

        Global.getLogger(WorkforcePlanner.class).info(sb.toString());
    }
}
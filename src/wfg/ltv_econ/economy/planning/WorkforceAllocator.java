package wfg.ltv_econ.economy.planning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math4.legacy.optim.MaxIter;
import org.apache.commons.math4.legacy.optim.PointValuePair;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraintSet;
import org.apache.commons.math4.legacy.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math4.legacy.optim.linear.NonNegativeConstraint;
import org.apache.commons.math4.legacy.optim.linear.PivotSelectionRule;
import org.apache.commons.math4.legacy.optim.linear.Relationship;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.IndustryGrouper.IndustryMatrixGrouped;
import wfg.ltv_econ.economy.planning.optim.CustomSimplexSolver;
import wfg.ltv_econ.economy.planning.optim.SimplexTableau;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.native_ui.util.ArrayMap;

public class WorkforceAllocator {
    private static final Logger logger = Global.getLogger(WorkforceAllocator.class);

    public static final double WORKER_COST = 1.0;

    /**
     * For reduced / grouped matrixes
     */
    public static final void logInputMatrix(IndustryMatrixGrouped grouped, List<String> coms) {
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

    /**
     * Computes optimal worker assignments across markets for all outputs, 
     * satisfying commodity demands while respecting market capacities, per-output limits,
     * fair spread and shortage adjustments.
     * 
     * @param markets List of markets to distribute to
     * @param industryOutputPairs industryID::outputID
     * @return Map of MarketAPI to float[] arrays representing worker assignments per output
     */
    public static Map<MarketAPI, float[]> computeWorkerAllocations(List<MarketAPI> markets,
        List<String> industryOutputPairs
    ) {
        // DATA COLLECTION
        final IndustryMatrixGrouped groupedMatrix = IndustryGrouper.getStaticGrouping();
        final double[] demand = PlanningData.getGlobalDemandVector();

        final Pair<List<String>, List<BitSet>> groupedData = IndustryGrouper.applyGroupingToMarketData(
            markets, industryOutputPairs, PlanningData.getOutputsPerMarket(markets),
            groupedMatrix.memberToGroup
        );
        final double[][] A = groupedMatrix.reducedMatrix;
        final List<String> groupedOutputPairs = groupedData.one;
        final List<BitSet> outputsPerMarket = groupedData.two;
        final DenseModel denseData = DenseModel.createDenseData(markets, groupedOutputPairs, outputsPerMarket);

        final int N = denseData.columnSize;
        final int C = A.length;
        final int O = groupedOutputPairs.size();
        final int M = markets.size();
        final int F = EconomyConstants.factionIDs.size();

        // INDEXES
        final int idxSStr = N;
        final int idxTotalWStr = idxSStr + C;
        final int nVars = idxTotalWStr + O;
        final Function<Integer, Integer> idxS = c -> idxSStr + c;
        final Function<Integer,Integer> idxTotalW = o -> idxTotalWStr + o;

        // 1) OBJECTIVE: minimize DEFICIT_COST * sum(slack[c]) + WORKER_COST * sum(w)
        final double[] objective = new double[nVars];

        Arrays.fill(objective, 0, N, WORKER_COST);
        Arrays.fill(objective, idxSStr, idxTotalWStr, EconomyConfig.ECON_DEFICIT_COST);
        Arrays.fill(objective, idxTotalWStr, nVars, 0.0);

        final LinearObjectiveFunction objFunc = new LinearObjectiveFunction(objective, 0.0);
        final List<LinearConstraint> const_constraints = new ArrayList<>();

        { // 2) Market capacity constraints: sum_o w[m,o] <= baseCapacities[m]
            final double[] coeffs = new double[nVars];
            for (int m = 0; m < denseData.marketStart.length - 1; m++) {
                final int start = denseData.marketStart[m];
                final int end = denseData.marketStart[m + 1];
                if (start == end) continue;
    
                Arrays.fill(coeffs, 0.0);
                for (int i = start; i < end; i++) coeffs[i] = 1.0;
    
                const_constraints.add(
                    new LinearConstraint(coeffs, Relationship.LEQ, denseData.columnMarketCap[start])
                );
            }
        }

        { // 3) Per-(market,output) worker caps: w[m,o] <= limit * baseCapacity[m]
            final double[] coeffs = new double[nVars]; // reused buffer
            for (int i = 0; i < N; i++) {
                Arrays.fill(coeffs, 0.0);
                coeffs[i] = 1.0;

                final double rhs = denseData.columnWorkerLimitFrac[i] * denseData.columnMarketCap[i];

                const_constraints.add(
                    new LinearConstraint(coeffs, Relationship.LEQ, rhs)
                );
            }
        }
        
        { // 4) Set totalW tracking var to total workers assigned
            final double[] coeffs = new double[nVars];
            for (int o = 0; o < O; o++) {
                Arrays.fill(coeffs, 0.0);

                for (int i = 0; i < N; i++) {
                    if (denseData.columnOutputIndex[i] == o) coeffs[i] = 1.0;
                }

                coeffs[idxTotalW.apply(o)] = -1.0;
                const_constraints.add(
                    new LinearConstraint(coeffs, Relationship.EQ, 0.0)
                );
            }
        }

        { // 5) Fair share hard floor
            final double[] coeffs = new double[nVars];
            for (int i = 0; i < N; i++) {
                final long weightSum = denseData.columnWeightSum[i];
                if (weightSum <= 0) continue;

                final long marketCap = denseData.columnMarketCap[i];
                final double proportion = marketCap / (double) weightSum;

                final int o = denseData.columnOutputIndex[i];
                final double floorMultiplier = EconomyConfig.USE_PRODUCTION_FAIRNESS ?
                    EconomyConfig.MIN_WORKER_FRACTION / denseData.columnOutputMod[i]:
                    EconomyConfig.MIN_WORKER_FRACTION;

                Arrays.fill(coeffs, 0.0);
                coeffs[i] = 1.0;
                coeffs[idxTotalW.apply(o)] = -proportion;
                coeffs[idxTotalW.apply(o)] = -proportion * floorMultiplier;

                const_constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, 0.0));
            }
        }

        double[] vars = null;
        SimplexTableau previousTableau = null;
        final double[] netCommodity = new double[C];
        final double[] comAvailability = new double[C];
        final double[] shortage_mult = new double[O];

        final double[][] baselineDemandFC = new double[F][C];
        final double[][] demandFC = new double[F][C];
        final double[][] rhoFC = new double[F][C];

        for (int f = 0; f < F; f++) {
            final String factionID = EconomyConstants.factionIDs.get(f);
            baselineDemandFC[f] = PlanningData.getFactionDemandVector(factionID); // copy or clone
        }

        for (int pass = 0; pass < EconomyConfig.ECON_ALLOCATION_PASSES; pass++) {
            final List<LinearConstraint> constraints = new ArrayList<>();
            constraints.addAll(const_constraints);
            Arrays.fill(netCommodity, 0.0);
            Arrays.fill(comAvailability, 0.0);
            Arrays.fill(shortage_mult, 1.0);
            for (int f = 0; f < F; f++) Arrays.fill(demandFC[f], 0.0);
            for (int f = 0; f < F; f++) System.arraycopy(baselineDemandFC[f], 0, demandFC[f], 0, C);

            if (pass != 0 && vars != null) {
                final double[][] producedDemandAdd = new double[F][C];

                for (int i = 0; i < N; i++) {
                    final double w = vars[i];
                    if (w == 0.0) continue;

                    final int o = denseData.columnOutputIndex[i];
                    final double mod = denseData.columnOutputMod[i];
                    final int f = denseData.columnFaction[i];
                    final double effectiveW = mod * w;

                    for (int c = 0; c < C; c++) {
                        final double base = A[c][o];
                        if (base == 0.0) continue;
                        netCommodity[c] += base * effectiveW;
                        producedDemandAdd[f][c] = base * effectiveW;
                    }
                }
    
                for (int c = 0; c < C; c++) {
                    if (demand[c] <= 0.0) comAvailability[c] = 1.0;
                    else comAvailability[c] = Math.min(1.0, netCommodity[c] / demand[c]);
                }
    
                final var byIndustry = IndustryIOs.getBaseInputsMap();
                for (int o = 0; o < O; o++) {
                    final String pair = groupedOutputPairs.get(o);
                    final String industryID = pair.split(EconomyLoop.KEY)[0];
                    final String outputID = pair.split(EconomyLoop.KEY)[1];
    
                    final Map<String, Map<String, Float>> byOutput = byIndustry.get(industryID);
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
    
                        final int c = denseData.commodityIndex.get(inputID);
                        final double availability = comAvailability[c];
    
                        final double weightNorm = e.getValue() / deficitWeightSum;
                        deficit += weightNorm * (1.0 - availability);
                    }
    
                    if (deficit > 0.0) {
                        shortage_mult[o] = Math.max(1.0 - deficit, 0.01);
                    }
                }
            
                for (int f = 0; f < F; f++) {
                    for (int c = 0; c < C; c++) {
                        final double newDemand = baselineDemandFC[f][c] + producedDemandAdd[f][c];
                        final double beta = 0.4;
                        demandFC[f][c] = (1.0 - beta) * demandFC[f][c] + beta * newDemand;
                    }
                }
            }

            for (int c = 0; c < C; c++) {
                double total = 0.0;
                for (int f = 0; f < F; f++) total += demandFC[f][c];
                if (total > 0.0) {
                    for (int f = 0; f < F; f++) rhoFC[f][c] = demandFC[f][c] / total;
                } else {
                    for (int f = 0; f < F; f++) rhoFC[f][c] = 0.0;
                }
            }

            for (int i = 0; i < N; i++) {
                if (denseData.columnIsOutputAbstract[i]) continue;
                final int f = denseData.columnFaction[i];
                final int c = denseData.columnComIdx[i];

                final double strength = EconomyConfig.SELF_SUFFICIENCY_REWARD_STRENGTH;
                final double antiDemandShare = Math.max(1f - rhoFC[f][c], 0.01f);

                final double value = WORKER_COST * (1f - strength + strength * antiDemandShare);
                objFunc.getCoefficients().setEntry(i, value);
            }

            { // 6) PRIMARY: Commodity production constraints:
                final double[] coeffs = new double[nVars];
                for (int c = 0; c < C; c++) {
                    Arrays.fill(coeffs, 0.0);
    
                    for (int i = 0; i < N; i++) {
                        final int o = denseData.columnOutputIndex[i];
                        final double base = A[c][o];
                        if (base == 0.0) continue;
    
                        final double coeff = base * denseData.columnOutputMod[i] * shortage_mult[o];
                        coeffs[i] += coeff;
                    }
    
                    coeffs[idxS.apply(c)] = 1.0; // slack
                    final double target = demand[c] * EconomyConfig.PRODUCTION_BUFFER;
                    constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, target));
                }
            }

            final CustomSimplexSolver solver = new CustomSimplexSolver(
                1e-4, 30, 1e-6, previousTableau
            );
            final PointValuePair solution = solver.optimize(
                new MaxIter(50000), objFunc,
                new LinearConstraintSet(constraints),
                GoalType.MINIMIZE,
                new NonNegativeConstraint(true),
                PivotSelectionRule.DANTZIG
            );
            vars = solution.getPoint();
            previousTableau = solver.getPrevTableau();
        }

        final Map<MarketAPI, float[]> groupedAssignments = new ArrayMap<>();
        { // 7) Extract assignments w[m,o] into map for grouped outputs
            final double[] denseWorkerVars = new double[N];
            System.arraycopy(vars, 0, denseWorkerVars, 0, N);

            final double[] fullWorkers = DenseModel.expandDenseSolution(
                denseWorkerVars, outputsPerMarket, O
            );

            for (int m = 0; m < M; m++) {
                final float[] arr = new float[O];
                final int base = m * O;
                for (int o = 0; o < O; o++) {
                    arr[o] = (float) fullWorkers[base + o];
                }
                groupedAssignments.put(markets.get(m), arr);
            }
        }

        return IndustryGrouper.expandGroupedAssignments(
            groupedAssignments, groupedMatrix,
            markets, industryOutputPairs
        );
    }
}
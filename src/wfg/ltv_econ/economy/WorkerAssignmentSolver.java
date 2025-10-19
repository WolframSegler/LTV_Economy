package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math4.legacy.optim.MaxIter;
import org.apache.commons.math4.legacy.optim.PointValuePair;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.LinearConstraintSet;
import org.apache.commons.math4.legacy.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math4.legacy.optim.linear.NonNegativeConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;
import org.apache.commons.math4.legacy.optim.linear.SimplexSolver;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;

import com.fs.starfarer.api.Global;

public class WorkerAssignmentSolver {

    public static final double WORKER_COST = 1;
    public static final double SLACK_COST = 150;

    /**
     * Solve A*x >= d using linear programming with slack variables.
     * @param A Matrix of size (commodities x industries)
     * @param d Demand vector of size (commodities)
     * @return Non-negative worker assignments to industries
     */
    public static double[] solveLPWithSlack(double[][] A, double[] d) {
        int m = A.length;       // number of commodities
        int n = A[0].length;    // number of industries

        double[] objectiveCoeffs = new double[n + m];

        for (int j = 0; j < n; j++) {
            objectiveCoeffs[j] = WORKER_COST;
        }
        for (int i = 0; i < m; i++) {
            objectiveCoeffs[n + i] = SLACK_COST;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoeffs, 0.0);
        List<LinearConstraint> constraints = new ArrayList<>();

        // Constraints: A*x + s >= d  ->  -A*x - s <= -d
        for (int i = 0; i < m; i++) {
            double[] coeffs = new double[n + m];
            for (int j = 0; j < n; j++) coeffs[j] = -A[i][j]; // industry coefficients
            coeffs[n + i] = -1.0; // slack variable coefficient
            constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, -d[i]));
        }

        // Non-negativity for industries and slacks
        for (int i = 0; i < n + m; i++) {
            double[] coeffs = new double[n + m];
            coeffs[i] = 1.0;
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

        double[] vars = solution.getPoint();
        // First n variables are the worker assignments
        double[] workers = new double[n];
        System.arraycopy(vars, 0, workers, 0, n);
        return workers;
    }

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

            for (int j = 0; j < industryOutputs.size(); j++) {
                csv.append(String.format("%.6f", A[i][j]));
                if (j < industryOutputs.size() - 1) csv.append(",");
            }
            csv.append("\n");
        }

        Global.getLogger(WorkerAssignmentSolver.class)
            .info("=== Input Matrix A (Industry_Output Columns) ===\n" + csv);
    }

    public static final void logDemandVector(double[] d, List<String> commodities) {
        StringBuilder demandLog = new StringBuilder("=== Demand Vector d ===\n");
        for (int i = 0; i < commodities.size(); i++) {
            demandLog.append(String.format("%s: %.3f\n", commodities.get(i), d[i]));
        }

        Global.getLogger(WorkerAssignmentSolver.class).info(demandLog);
    }

    public static final void logWorkerAssignments(double[] workerVector, List<String> industryOutputPairs) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < industryOutputPairs.size(); i++) {
            String line = industryOutputPairs.get(i) + ": " + Math.round(workerVector[i]);
            lines.add(line);
        }
        String logString = lines.stream()
                .map(s -> "    " + s)
                .collect(Collectors.joining("\n"));
        Global.getLogger(WorkerAssignmentSolver.class).info(logString);
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

        String logString = lines.stream()
                .map(s -> "    " + s)
                .collect(Collectors.joining("\n"));
        Global.getLogger(WorkerAssignmentSolver.class).info(logString);
    }
}
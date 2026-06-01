package wfg.ltv_econ.config.planning;

import java.util.*;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;

import wfg.ltv_econ.economy.planning.DenseModel;
import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.CustomObjective;
import wfg.ltv_econ.economy.planning.custom.PlanningContext;
import wfg.ltv_econ.economy.planning.custom.VariableLayout;
import wfg.ltv_econ.economy.planning.custom.goalParams.DoubleParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

/**
 * Soft goal: produce exactly the player faction's demand for selected commodities.
 * Penalises under‑production heavily, over‑production lightly.
 */
public class FactionDemandCoverageGoal implements CustomObjective, CustomConstraint {
    public static final String SERIAL_ID = "faction_demand_coverage";

    private double penalty;

    public ObjectiveAllocation allocateVariables(PlanningContext context) {
        final int C = context.commodityCount;
        final double[] coeffs = new double[C];
        Arrays.fill(coeffs, penalty);
        return new ObjectiveAllocation(SERIAL_ID, coeffs);
    }

    public List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives) {
        final DenseModel dense = context.denseData;
        final double[][] A = context.A;
        final int T = layout.tierCount;
        final int C = context.commodityCount;

        final ObjectiveAllocation alloc = objectives.get(SERIAL_ID);
        final int slackStart = alloc.startIndex;

        final List<Integer> tierIndices = new ArrayList<>();
        for (String label : getRequiredSegmentIds()) {
            tierIndices.add(context.segments.indexOf(label));
        }

        final List<LinearConstraint> constraints = new ArrayList<>();

        for (int c = 0; c < C; c++) {
            final double[] coeffs = new double[layout.totalVars];

            for (int col = 0; col < dense.columnSize; col++) {
                final int o = dense.columnOutputIndex[col];
                final double base = A[c][o];
                if (base == 0d) continue;

                final double coeff = base * dense.columnOutputMod[col];
                final int varBase = col * T;
                for (int tier : tierIndices) {
                    coeffs[varBase + tier] += coeff;
                }
            }

            coeffs[slackStart + c] = 1d;

            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, context.factionDemand[c]));
        }

        return constraints;
    }

    public String getSerializationId() { return SERIAL_ID; }
    public List<String> getRequiredSegmentIds() {
        return Arrays.asList("fair", "local", "faction");
    }
    public List<String> getRequiredObjectiveIds() {
        return Collections.singletonList(SERIAL_ID);
    }

    @Override
    public List<GoalParameter> getParameters() {
        return Collections.singletonList(
            new DoubleParameter(
                "penalty",
                "Under-production penalty",
                1d, 5000d,
                () -> FactionDemandCoverageGoal.this.penalty,
                (v) -> FactionDemandCoverageGoal.this.penalty = v
            )
        );
    }
}
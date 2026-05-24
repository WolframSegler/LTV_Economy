package wfg.ltv_econ.config.planning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;

import wfg.ltv_econ.economy.planning.DenseModel;
import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.PlanningContext;
import wfg.ltv_econ.economy.planning.custom.VariableLayout;
import wfg.ltv_econ.economy.planning.custom.CustomObjective.ObjectiveAllocation;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public class HardAutarkyConstraint implements CustomConstraint {
    public static final String SERIAL_ID = "hard_autarky";

    public List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives) {
        final DenseModel dense = context.denseData;
        final double[][] A = context.A;
        final int T = layout.tierCount;
        final int C = context.commodityCount;

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

            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, context.factionDemand[c]));
        }

        return constraints;
    }

    public String getSerializationId() { return SERIAL_ID; }
    public List<String> getRequiredSegmentIds() {
        return Arrays.asList("fair", "local", "faction");
    }
    public List<String> getRequiredObjectiveIds() {
        return Collections.emptyList();
    }

    @Override
    public List<GoalParameter> getParameters() {
        return Collections.emptyList();
    }
}
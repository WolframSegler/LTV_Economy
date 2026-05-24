package wfg.ltv_econ.config.planning;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;

import wfg.ltv_econ.economy.planning.DenseModel;
import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.CustomObjective;
import wfg.ltv_econ.economy.planning.custom.PlanningContext;
import wfg.ltv_econ.economy.planning.custom.VariableLayout;
import wfg.ltv_econ.economy.planning.custom.goalParams.DoubleParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public class ExportTargetGoal implements CustomObjective, CustomConstraint {
    public static final String SERIAL_ID = "export_target";

    private final String comID;
    private double targetAmount;
    private double penalty;
    private final String allocationId;

    public ExportTargetGoal(String comID, double targetAmount, double penalty) {
        this.comID = comID;
        this.targetAmount = targetAmount;
        this.penalty = penalty;
        this.allocationId = SERIAL_ID + "_" + comID;
    }

    public ObjectiveAllocation allocateVariables(PlanningContext context) {
        return new ObjectiveAllocation(allocationId, new double[] { penalty });
    }

    public List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives) {
        final DenseModel dense = context.denseData;
        final double[][] A = context.A;
        final int T = layout.tierCount;

        final Integer c = dense.commodityIndex.get(comID);
        if (c == null) throw new IllegalArgumentException("Unknown commodity: " + comID);

        final ObjectiveAllocation alloc = objectives.get(allocationId);
        final int slackIdx = alloc.startIndex;

        // 1) Soft target: total_production + slack >= targetAmount
        final double[] coeffsSoft = new double[layout.totalVars];
        for (int col = 0; col < dense.columnSize; col++) {
            final int o = dense.columnOutputIndex[col];
            final double base = A[c][o];
            if (base == 0d) continue;

            final double coeff = base * dense.columnOutputMod[col];
            final int varBase = col * T;
            for (int t = 0; t < T; t++) coeffsSoft[varBase + t] += coeff;
        }
        coeffsSoft[slackIdx] = 1d;
        final LinearConstraint softConstraint = new LinearConstraint(coeffsSoft, Relationship.GEQ, targetAmount);

        // 2) Hard ceiling: total_production <= globalDemand[c]
        final double[] coeffsHard = new double[layout.totalVars];
        for (int col = 0; col < dense.columnSize; col++) {
            final int o = dense.columnOutputIndex[col];
            final double base = A[c][o];
            if (base == 0d) continue;

            final double coeff = base * dense.columnOutputMod[col];
            final int varBase = col * T;
            for (int t = 0; t < T; t++) coeffsHard[varBase + t] += coeff;
        }
        final LinearConstraint hardConstraint = new LinearConstraint(coeffsHard, Relationship.LEQ, context.globalDemand[c]);

        return Arrays.asList(softConstraint, hardConstraint);
    }

    public String getSerializationId() { return SERIAL_ID; }
    public List<String> getRequiredSegmentIds() { return Collections.emptyList(); }
    public List<String> getRequiredObjectiveIds() { return Collections.singletonList(allocationId); }

    @Override
    public List<GoalParameter> getParameters() {
        return Arrays.asList(
            new DoubleParameter("target", "Export target amount", 0d, 1_000_000_000d, 10d, () -> targetAmount, v -> targetAmount = v),
            new DoubleParameter("penalty", "Shortfall penalty", 1d, 1_000_000d, 100d, () -> penalty, v -> penalty = v)
        );
    }
}
package wfg.ltv_econ.config.planning;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;
import static wfg.native_ui.util.Globals.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;

import com.fs.starfarer.api.graphics.SpriteAPI;

import wfg.ltv_econ.economy.engine.EconomyLoop;
import wfg.ltv_econ.economy.planning.DenseModel;
import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.CustomObjective;
import wfg.ltv_econ.economy.planning.custom.PlanningContext;
import wfg.ltv_econ.economy.planning.custom.VariableLayout;
import wfg.ltv_econ.economy.planning.custom.goalParams.DoubleParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public class CommodityTargetGoal implements CustomObjective, CustomConstraint {
    public static final String SERIAL_ID = "commodity_target";

    private final String comID;
    private final String comName;
    private double targetAmount;
    private double penalty;
    private final String allocationId;

    public CommodityTargetGoal(String comID, String comName) {
        this.comID = comID;
        this.comName = comName;
        this.allocationId = SERIAL_ID + EconomyLoop.KEY + comName;
    }

    public ObjectiveAllocation allocateVariables(PlanningContext context) {
        return new ObjectiveAllocation(allocationId, new double[] { penalty });
    }

    public List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives) {
        final DenseModel dense = context.denseData;
        final double[][] A = context.A;
        final int T = layout.tierCount;

        final Integer c = dense.commodityIndex.get(comID);
        if (c == null) throw new IllegalArgumentException("Unknown commodity: " + comName);

        final ObjectiveAllocation alloc = objectives.get(allocationId);
        final double[] coeffs = new double[layout.totalVars];

        for (int col = 0; col < dense.columnSize; col++) {
            final int o = dense.columnOutputIndex[col];
            final double base = A[c][o];
            if (base == 0d) continue;

            final double coeff = base * dense.columnOutputMod[col];
            final int varBase = col * T;
            for (int t = 0; t < T; t++) coeffs[varBase + t] += coeff;
        }

        coeffs[alloc.startIndex] = 1d;

        return Collections.singletonList(new LinearConstraint(coeffs, Relationship.GEQ, targetAmount));
    }

    @Override
    public SpriteAPI getIcon() {
        return settings.getSprite(settings.getCommoditySpec(comID).getIconName());
    }
    public String getSerializationId() { return SERIAL_ID + EconomyLoop.KEY + comName; }
    public List<String> getRequiredSegmentIds() { return Collections.emptyList(); }
    public List<String> getRequiredObjectiveIds() { return Collections.singletonList(allocationId); }

    @Override
    public List<GoalParameter> getParameters() {
        return Arrays.asList(
            new DoubleParameter(
                "target", str("uiGoalParamTargetAmount"),
                0d, 50_000d,
                () -> CommodityTargetGoal.this.targetAmount,
                v -> CommodityTargetGoal.this.targetAmount = v
            ),
            new DoubleParameter(
                "penalty", str("uiGoalParamShortfallPenalty"),
                1d, 5000d,
                () -> CommodityTargetGoal.this.penalty,
                v -> CommodityTargetGoal.this.penalty = v
            )
        );
}
}
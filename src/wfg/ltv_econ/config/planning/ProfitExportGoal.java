package wfg.ltv_econ.config.planning;

import static wfg.native_ui.util.Globals.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.economy.planning.DenseModel;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.CustomObjective;
import wfg.ltv_econ.economy.planning.custom.PlanningContext;
import wfg.ltv_econ.economy.planning.custom.VariableLayout;
import wfg.ltv_econ.economy.planning.custom.goalParams.DoubleParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;
import wfg.ltv_econ.economy.planning.custom.goalParams.RadioParameter;
import wfg.native_ui.util.Arithmetic;

public class ProfitExportGoal implements CustomObjective, CustomConstraint {
    public static final String SERIAL_ID = "profit_export";

    public enum Metric { BASE_VALUE, MARGIN }

    private Metric metric = Metric.BASE_VALUE;
    private double weight = 1d;

    @Override
    public void modifyWorkerObjective(double[] objective, VariableLayout layout, PlanningContext context) {
        final DenseModel dense = context.denseData;
        final int T = layout.tierCount;

        final double[] scores = new double[dense.columnSize];
        double maxScore = 0d;
        for (int col = 0; col < dense.columnSize; col++) {
            final String comID = IndustryMatrix.getWorkerRelatedCommodityIDs().get(dense.columnComIdx[col]);
            final String indID = dense.columnIndustryId[col];
            final double s = computeScore(comID, indID);
            scores[col] = s;
            if (s > maxScore) maxScore = s;
        }
        if (maxScore == 0d) return;

        // 2) Apply multiplicative discount: objective[i] *= (1 - weight * (score / maxScore))
        final double strength = Arithmetic.clamp(weight, 0d, 1d);
        for (int col = 0; col < dense.columnSize; col++) {
            final double normScore = scores[col] / maxScore; // [0, 1]
            final double multiplier = 1d - strength * normScore;
            if (multiplier >= 1d) continue;

            final int varBase = col * T;
            for (int t = 0; t < T; t++) {
                objective[varBase + t] *= multiplier;
            }
        }
    }

    public List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives) {
        return Collections.emptyList();
    }

    public List<String> getRequiredSegmentIds() { return Collections.emptyList(); }
    public List<String> getRequiredObjectiveIds() { return Collections.emptyList(); }
    public String getSerializationId() { return SERIAL_ID; }
    public ObjectiveAllocation allocateVariables(PlanningContext context) {
        return new ObjectiveAllocation(SERIAL_ID, new double[0]);
    }
    public List<GoalParameter> getParameters() {
        return Arrays.asList(
            new RadioParameter("metric", "Value metric",
                Arrays.asList("Base value", "Production margin"),
                () -> metric == Metric.BASE_VALUE ? "Base value" : "Production margin",
                v -> metric = "Base value".equals(v) ? Metric.BASE_VALUE : Metric.MARGIN
            ),
            new DoubleParameter("weight", "Discount strength", 0d, 1d, 0.01d,
                () -> weight,
                v -> weight = v
            )
        );
    }

    private double computeScore(String comID, String indID) {
        return switch (metric) {
            case BASE_VALUE -> settings.getCommoditySpec(comID).getBasePrice();
            case MARGIN -> LaborConfig.getRoVC(IndustryConfigManager.getIndConfig(indID).occTag);
        };
    }
}
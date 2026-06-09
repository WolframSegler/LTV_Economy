package wfg.ltv_econ.config.planning;

import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;
import org.apache.commons.math4.legacy.optim.linear.Relationship;

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
    private double targetFraction = 1d;
    private double penalty = 100d;

    @Override
    public ObjectiveAllocation allocateVariables(PlanningContext context) {
        final int C = context.commodityCount;
        final double[] coeffs = new double[C];
        Arrays.fill(coeffs, penalty);
        return new ObjectiveAllocation(SERIAL_ID, coeffs);
    }

    @Override
    public void modifyWorkerObjective(double[] objective, VariableLayout layout, PlanningContext context) {
        final DenseModel dense = context.denseData;
        final int T = layout.tierCount;

        final double[] scores = new double[dense.columnSize];
        double maxScore = 0d;
        for (int col = 0; col < dense.columnSize; col++) {
            final int comIndex = dense.columnComIdx[col];
            if (comIndex < 0) continue;

            final String comID = IndustryMatrix.getWorkerRelatedCommodityIDs().get(comIndex);
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
        final DenseModel dense = context.denseData;
        final double[][] A = context.A;
        final int T = layout.tierCount;
        final int C = context.commodityCount;

        final ObjectiveAllocation alloc = objectives.get(SERIAL_ID);
        final int slackStart = alloc.startIndex;

        final List<LinearConstraint> constraints = new ArrayList<>();

        for (int c = 0; c < C; c++) {
            final double[] coeffs = new double[layout.totalVars];

            // Sum production of this commodity across all markets and all tiers
            for (int col = 0; col < dense.columnSize; col++) {
                final int o = dense.columnOutputIndex[col];
                final double base = A[c][o];
                if (base == 0.0) continue;

                final double coeff = base * dense.columnOutputMod[col];
                final int varBase = col * T;
                for (int t = 0; t < T; t++) {
                    coeffs[varBase + t] += coeff;
                }
            }

            coeffs[slackStart + c] = 1.0;

            final double target = context.globalDemand[c] * targetFraction;
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, target));
        }
        return constraints;
    }

    public List<String> getRequiredSegmentIds() { return Collections.emptyList(); }
    public List<String> getRequiredObjectiveIds() { return Collections.emptyList(); }
    public String getSerializationId() { return SERIAL_ID; }
    public List<GoalParameter> getParameters() {
        return Arrays.asList(
            new RadioParameter(
                "metric", str("uiGoalParamValueMetric"),
                Arrays.asList(
                    str("uiGoalParamValueMetricBaseValue"),
                    str("uiGoalParamValueMetricProductionMargin")
                ),
                () -> metric == Metric.BASE_VALUE
                        ? str("uiGoalParamValueMetricBaseValue")
                        : str("uiGoalParamValueMetricProductionMargin"),
                v -> metric = str("uiGoalParamValueMetricBaseValue").equals(v)
                        ? Metric.BASE_VALUE : Metric.MARGIN
            ),
            new DoubleParameter(
                "weight", str("uiGoalParamDiscountStrength"),
                0d, 1d,
                () -> weight, v -> weight = v
            ),
            new DoubleParameter(
                "targetFraction", str("uiGoalParamTargetFraction"),
                0d, 1d,
                () -> targetFraction, v -> targetFraction = v
            ),
            new DoubleParameter(
                "penalty", str("uiGoalParamShortfallPenalty"),
                1d, 5000d,
                () -> penalty, v -> penalty = v
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
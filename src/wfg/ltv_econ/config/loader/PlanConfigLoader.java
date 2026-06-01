package wfg.ltv_econ.config.loader;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.str;
import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.config.PlanConfig;
import wfg.ltv_econ.config.PlanConfig.WorkerAllocationPlan;
import wfg.ltv_econ.economy.planning.custom.CustomGoal;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments.PiecewiseSegment;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;
import wfg.ltv_econ.economy.registry.PlanningGoalRegistry;
import wfg.native_ui.ui.dialog.DialogPanel;

public class PlanConfigLoader {
    private PlanConfigLoader() {}
    private static final Logger log = Global.getLogger(IndustryConfigLoader.class);

    private static final String CONFIG_PATH = "./data/config/ltvEcon/plan_config.json";
    private static final String DYNAMIC_CONFIG_NAME = "custom_plan_config.json";

    private static JSONObject config;
    private static JSONObject dynamic_config;    

    public static final void loadConfig() {
        final Set<String> seenIds = new HashSet<>();

        processConfig(getConfig(false), false, seenIds);

        processConfig(getConfig(true), true, seenIds);
    }

    private static final void processConfig(JSONObject config, boolean isCustom, Set<String> seenIds) {
        final JSONArray plansArray = config.optJSONArray("worker_allocation_plans");
        if (plansArray == null) return;

        for (int i = 0; i < plansArray.length(); i++) {
            try {
                final JSONObject planJson = plansArray.getJSONObject(i);
                final WorkerAllocationPlan plan = new WorkerAllocationPlan();

                final String planId = planJson.getString("id");
                if (!seenIds.add(planId)) {
                    throw new RuntimeException("Duplicate worker allocation plan ID: " + planId);
                }
                plan.id = planId;
                plan.description = planJson.optString("description", "");
                plan.isCustom = isCustom;

                final JSONArray segs = planJson.getJSONArray("segments");
                for (int j = 0; j < segs.length(); j++) {
                    final JSONObject segJson = segs.getJSONObject(j);
                    final String label = segJson.getString("id");
                    final double cost = segJson.getDouble("cost");
                    plan.segments.segments.put(label, new PiecewiseSegments.PiecewiseSegment(cost, label));
                }

                final JSONObject objConfJson = planJson.optJSONObject("objective_config");
                plan.objConfig.maxIter = objConfJson.getInt("maxIter");
                final String goalTypeName = objConfJson.getString("goalType");
                plan.objConfig.goal = GoalType.valueOf(goalTypeName);

                final JSONArray goalsJson = planJson.optJSONArray("goals");
                for (int j = 0; j < goalsJson.length(); j++) {
                    final JSONObject goalJson = goalsJson.getJSONObject(j);
                    final String goalId = goalJson.getString("id");
                    final CustomGoal goal = PlanningGoalRegistry.createGoal(goalId);
                    if (goal == null) {
                        throw new RuntimeException("Unknown custom goal id: " + goalId + " in plan " + planId);
                    }

                    final JSONArray params = goalJson.optJSONArray("params");
                    for (int k = 0; k < params.length(); k++) {
                        final JSONObject pJson = params.getJSONObject(k);
                        final String paramId = pJson.getString("id");
                        final String valueStr = pJson.getString("value");
                        for (GoalParameter param : goal.getParameters()) {
                            if (param.id.equals(paramId)) {
                                param.setValueFromString(valueStr);
                                break;
                            }
                        }
                    }

                    plan.goals.add(goal);
                }

                PlanConfig.map.put(planId, plan);
            } catch (Exception e) {
                log.error("Exception loading worker allocation plan at index " + i, e);
                new DialogPanel(400, 100, null, str("uiTitleFailedToLoadWorkerAllocationPlan") + e.toString(), str("dismissTxt")).show(0.2f, 0.2f);
            }
        }
    }

    public static final void serializeAndWriteToCommon() {
        final JSONObject json = serializeCustomConfigs();

        try {
            settings.writeJSONToCommon(
                DYNAMIC_CONFIG_NAME,
                json, false
            );
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to write custom plan configuration to common JSON file '"
                + DYNAMIC_CONFIG_NAME, e
            );
        }
    }

    private static final JSONObject serializeCustomConfigs() {
        final JSONObject root = new JSONObject();

        try {
        final List<JSONObject> plans = new ArrayList<>();
        for (WorkerAllocationPlan plan : PlanConfig.map.values()) {
            if (!plan.isCustom) continue;

            final JSONObject planJson = new JSONObject();
            planJson.put("id", plan.id);
            planJson.put("description", plan.description);

            final List<JSONObject> segmentsJson = new ArrayList<>(4);
            for (PiecewiseSegment seg : plan.segments.segments.values()) {
                final JSONObject segJson = new JSONObject();
                segJson.put("id", seg.id);
                segJson.put("cost", seg.cost);
                segmentsJson.add(segJson);
            }
            planJson.put("segments", segmentsJson);

            final JSONObject objectiveConfigJson = new JSONObject();
            objectiveConfigJson.put("maxIter", plan.objConfig.maxIter);
            objectiveConfigJson.put("goalType", plan.objConfig.goal.name());
            planJson.put("objective_config", objectiveConfigJson);
            
            final List<JSONObject> goalsJson = new ArrayList<>(8);
            for (CustomGoal goal : plan.goals) {
                final JSONObject goalJson = new JSONObject();
                goalJson.put("id", goal.getSerializationId());
                final List<JSONObject> parametersJson = new ArrayList<>(2);
                for (GoalParameter param : goal.getParameters()) {
                    final JSONObject paramJson = new JSONObject();
                    paramJson.put("id", param.id);
                    paramJson.put("name", param.name);
                    paramJson.put("value", param.getValueAsString());
                    
                    parametersJson.add(paramJson);
                }
                goalJson.put("params", parametersJson);
                goalsJson.add(goalJson);
            }

            planJson.put("goals", goalsJson);

            plans.add(planJson);
        }
        root.put("worker_allocation_plans", plans);
        } catch (JSONException e) {
            log.error("Failed to serialize custom plan configuration to JSON", e);
        }

        return root;
    }

    private static final void load() {
        try {
            config = settings.getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load plan config: " + CONFIG_PATH, ex);
        }

        try {
            dynamic_config = settings.readJSONFromCommon(DYNAMIC_CONFIG_NAME, false);

            if (dynamic_config == null || dynamic_config.length() < 1) {
                log.info("Custom plan config missing or empty. Creating new JSONObject.");
                dynamic_config = new JSONObject();
            }
        } catch (Exception ex) {
            log.warn("Failed to read custom plan config, creating new JSONObject."
            );
            dynamic_config = new JSONObject();
        }
    }

    private static final JSONObject getConfig(boolean dynamicConfig) {
        if (config == null || dynamic_config == null) load();
        return dynamicConfig ? dynamic_config : config;
    }
}
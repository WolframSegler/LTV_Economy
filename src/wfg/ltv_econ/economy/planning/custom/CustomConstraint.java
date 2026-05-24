package wfg.ltv_econ.economy.planning.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;

import wfg.ltv_econ.economy.planning.custom.CustomObjective.ObjectiveAllocation;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public interface CustomConstraint {
    String getSerializationId();
    List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives);
    List<String> getRequiredSegmentIds();
    List<String> getRequiredObjectiveIds();
    List<GoalParameter> getParameters();

    public static List<String> getRequiredSegmentIds(List<CustomConstraint> customConstraints) {
        final List<String> ids = new ArrayList<>();

        for (CustomConstraint cc : customConstraints) {
            for (String id : cc.getRequiredSegmentIds()) {
                if (!ids.contains(id)) ids.add(id);
            }
        }

        return ids;
    }

    public static List<String> getRequiredObjectiveIds(List<CustomConstraint> customConstraints) {
        final List<String> ids = new ArrayList<>();

        for (CustomConstraint cc : customConstraints) {
            for (String id : cc.getRequiredObjectiveIds()) {
                if (!ids.contains(id)) ids.add(id);
            }
        }

        return ids;
    }
}
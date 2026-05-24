package wfg.ltv_econ.economy.planning.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.linear.LinearConstraint;

import wfg.ltv_econ.economy.planning.custom.CustomObjective.ObjectiveAllocation;

public interface CustomConstraint {
    String getSerializationId();
    List<LinearConstraint> buildConstraints(VariableLayout layout, PlanningContext context, Map<String, ObjectiveAllocation> objectives);
    List<String> getRequiredSegmentIds();

    public static List<String> getSegmentIds(List<CustomConstraint> customConstraints) {
        final List<String> ids = new ArrayList<>();

        for (CustomConstraint cc : customConstraints) {
            for (String id : cc.getRequiredSegmentIds()) {
                if (!ids.contains(id)) ids.add(id);
            }
        }

        return ids;
    }
}
package wfg.ltv_econ.economy.planning.custom;

import java.util.List;

import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public interface CustomObjective {
    String getSerializationId();
    ObjectiveAllocation allocateVariables(PlanningContext context);
    default void modifyWorkerObjective(double[] objective, VariableLayout layout, PlanningContext context) {}
    List<GoalParameter> getParameters();

    public static class ObjectiveAllocation {
        /** dense array of coefficients */
        public final double[] coefficients;
        public final String id;
        public int startIndex;

        public ObjectiveAllocation(String id, double[] coefficients) {
            this.id = id;
            this.coefficients = coefficients;
        }
    }
}
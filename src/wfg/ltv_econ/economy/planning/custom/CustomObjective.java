package wfg.ltv_econ.economy.planning.custom;

public interface CustomObjective {
    String getSerializationId();
    ObjectiveAllocation allocateVariables(PlanningContext context);

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
package wfg.ltv_econ.economy.planning.custom;

import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;

public class ObjectiveConfig {
    public final GoalType goal;
    public final int maxIter;

    public ObjectiveConfig(GoalType goal, int maxIter) {
        this.goal = goal;
        this.maxIter = maxIter;
    }
}
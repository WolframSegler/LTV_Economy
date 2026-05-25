package wfg.ltv_econ.economy.planning.custom;

import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;

public class ObjectiveConfig {
    public GoalType goal = GoalType.MINIMIZE;
    public int maxIter = 5000;
}
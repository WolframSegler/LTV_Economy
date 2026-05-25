package wfg.ltv_econ.economy.planning.custom;

import java.util.List;
import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public interface CustomGoal {
    String getSerializationId();
    List<GoalParameter> getParameters();
}
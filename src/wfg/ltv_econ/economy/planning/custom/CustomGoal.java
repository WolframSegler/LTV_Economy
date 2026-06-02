package wfg.ltv_econ.economy.planning.custom;

import java.util.List;

import com.fs.starfarer.api.graphics.SpriteAPI;

import wfg.ltv_econ.economy.planning.custom.goalParams.GoalParameter;

public interface CustomGoal {
    String getSerializationId();
    List<GoalParameter> getParameters();
    default SpriteAPI getIcon() { return null; };
}
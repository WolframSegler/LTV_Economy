package wfg.ltv_econ.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import wfg.ltv_econ.config.loader.PlanConfigLoader;
import wfg.ltv_econ.economy.planning.custom.CustomGoal;
import wfg.ltv_econ.economy.planning.custom.ObjectiveConfig;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments;

public class PlanConfig {
    private PlanConfig() {}
    public static final Map<String, WorkerAllocationPlan> map = new LinkedHashMap<>(16);

    static {
        PlanConfigLoader.loadConfig();
    }
    
    public static final List<WorkerAllocationPlan> getPlansCopy() {
        return new ArrayList<>(map.values());
    }

    public static class WorkerAllocationPlan {
        public final List<CustomGoal> goals = new ArrayList<>(8);
        public final ObjectiveConfig objConfig = new ObjectiveConfig();
        public final PiecewiseSegments segments = new PiecewiseSegments();
        public final Set<String> excludedMarkets = new HashSet<>();
        public String id;
        public boolean isCustom;
    }
}
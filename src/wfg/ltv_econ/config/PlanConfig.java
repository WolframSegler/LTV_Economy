package wfg.ltv_econ.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.loader.PlanConfigLoader;
import wfg.ltv_econ.economy.planning.custom.CustomGoal;
import wfg.ltv_econ.economy.planning.custom.ObjectiveConfig;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments;
import wfg.ltv_econ.economy.planning.custom.PiecewiseSegments.PiecewiseSegment;

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
        private final long copySafeKey;

        public final List<CustomGoal> goals = new ArrayList<>(8);
        public final ObjectiveConfig objConfig = new ObjectiveConfig();
        public final PiecewiseSegments segments = new PiecewiseSegments();
        public String id;
        public boolean isCustom;
        public String description = "";


        public WorkerAllocationPlan() {
            copySafeKey = Misc.genRandomSeed();
        }

        public WorkerAllocationPlan(long copySafeKey) {
            this.copySafeKey = copySafeKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof WorkerAllocationPlan plan)) return false;
            return plan.copySafeKey == copySafeKey;
        }

        public final WorkerAllocationPlan copy() {
            final WorkerAllocationPlan plan = new WorkerAllocationPlan(copySafeKey);
            plan.goals.addAll(goals);
            plan.objConfig.goal = objConfig.goal;
            plan.objConfig.maxIter = objConfig.maxIter;

            plan.id = id;
            plan.isCustom = isCustom;
            plan.description = description;

            for (PiecewiseSegment seg : segments.segments.values()) {
                final PiecewiseSegment copySeg = seg.copy();
                plan.segments.segments.put(copySeg.id, seg);
            }

            return plan;
        }
    }
}
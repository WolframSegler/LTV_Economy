package wfg.ltv_econ.economy.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import wfg.ltv_econ.economy.planning.custom.CustomConstraint;
import wfg.ltv_econ.economy.planning.custom.CustomGoal;
import wfg.ltv_econ.economy.planning.custom.CustomObjective;

public final class PlanningGoalRegistry {
    private static final Map<String, GoalEntry> registry = new HashMap<>();

    public static final void register(String id, Supplier<CustomGoal> factory) {
        registry.put(id, new GoalEntry(factory));
    }

    public static final boolean hasGoal(String id) {
        return registry.get(id) != null;
    }

    public static final CustomGoal createGoal(String id) throws IllegalArgumentException {
        final GoalEntry entry = registry.get(id);
        if (entry == null) throw new IllegalArgumentException("No such custom goal: " + id);
        return entry.factory.get();
    }

    public static final Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public static final Collection<GoalEntry> getRegistry() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public static List<Supplier<CustomGoal>> getFactories() {
        final List<Supplier<CustomGoal>> list = new ArrayList<>();
        for (GoalEntry e : registry.values()) list.add(e.factory);

        return list;
    }

    public static List<Supplier<CustomGoal>> getObjectiveFactories() {
        final List<Supplier<CustomGoal>> list = new ArrayList<>();
        for (GoalEntry e : registry.values()) {
            if (e.isObjective) list.add(e.factory);
        }
        return list;
    }

    public static List<Supplier<CustomGoal>> getConstraintFactories() {
        final List<Supplier<CustomGoal>> list = new ArrayList<>();
        for (GoalEntry e : registry.values()) {
            if (e.isConstraint) list.add(e.factory);
        }
        return list;
    }

    private static class GoalEntry {
        final Supplier<CustomGoal> factory;
        final boolean isObjective;
        final boolean isConstraint;

        GoalEntry(Supplier<CustomGoal> factory) {
            this.factory = factory;
            final CustomGoal sample = factory.get();
            this.isObjective = sample instanceof CustomObjective;
            this.isConstraint = sample instanceof CustomConstraint;
        }
    }
}
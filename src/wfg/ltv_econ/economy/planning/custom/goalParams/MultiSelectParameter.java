package wfg.ltv_econ.economy.planning.custom.goalParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiSelectParameter extends GoalParameter {
    private final Supplier<Set<String>> getter;
    private final Consumer<Set<String>> setter;
    private final List<String> allOptions;

    public MultiSelectParameter(String id, String name, List<String> allOptions, Supplier<Set<String>> getter, Consumer<Set<String>> setter) {
        super(id, name);
        this.allOptions = Collections.unmodifiableList(allOptions);
        this.getter = getter;
        this.setter = setter;
    }

    public Set<String> getValue() { return getter.get(); }
    public void setValue(Set<String> value) { setter.accept(value); }
    public List<String> getAllOptions() { return allOptions; }

    @Override public String getValueAsString() { return String.join(",", getValue()); }
    @Override public void setValueFromString(String s) {
        setValue(new HashSet<>(Arrays.asList(s.split(","))));
    }

    @Override public ParamType getParamType() { return ParamType.MULTI_SELECT; }
}
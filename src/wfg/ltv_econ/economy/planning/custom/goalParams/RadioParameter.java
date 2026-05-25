package wfg.ltv_econ.economy.planning.custom.goalParams;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RadioParameter extends GoalParameter {
    public final Supplier<String> getter;
    public final Consumer<String> setter;
    public final List<String> allOptions;

    public RadioParameter(String id, String name, List<String> allOptions,
        Supplier<String> getter, Consumer<String> setter
    ) {
        super(id, name);
        this.allOptions = Collections.unmodifiableList(allOptions);
        this.getter = getter;
        this.setter = setter;
    }

    @Override public String getValueAsString() { return getValue(); }
    @Override public void setValueFromString(String s) { setValue(s); }

    public String getValue() { return getter.get(); }
    public void setValue(String value) { setter.accept(value); }
    public List<String> getAllOptions() { return allOptions; }
    @Override public WidgetType getWidgetType() { return WidgetType.MULTI_SELECT; }
    @Override public ParamType getParamType() { return ParamType.RADIO; }
}
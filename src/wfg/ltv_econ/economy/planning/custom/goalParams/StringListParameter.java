package wfg.ltv_econ.economy.planning.custom.goalParams;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringListParameter extends GoalParameter {
    public final Supplier<String> getter;
    public final Consumer<String> setter;
    public final List<String> allowedValues;

    public StringListParameter(String id, String name, List<String> allowedValues,
        Supplier<String> getter, Consumer<String> setter
    ) {
        super(id, name);
        this.allowedValues = Collections.unmodifiableList(allowedValues);
        this.getter = getter;
        this.setter = setter;
    }

    public String getValue() { return getter.get(); }
    public void setValue(String value) { setter.accept(value); }

    @Override public String getValueAsString() { return getValue(); }
    @Override public void setValueFromString(String s) { setValue(s); }
    @Override public WidgetType getWidgetType() { return WidgetType.MULTI_SELECT; }
}
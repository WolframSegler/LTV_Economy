package wfg.ltv_econ.economy.planning.custom.goalParams;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class BooleanParameter extends GoalParameter {
    public final BooleanSupplier getter;
    public final Consumer<Boolean> setter;

    public BooleanParameter(String id, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(id, name);
        this.getter = getter;
        this.setter = setter;
    }

    public boolean getValue() { return getter.getAsBoolean(); }
    public void setValue(boolean value) { setter.accept(value); }

    @Override public String getValueAsString() { return Boolean.toString(getValue()); }
    @Override public void setValueFromString(String s) { setValue(Boolean.parseBoolean(s)); }
    @Override public ParamType getParamType() { return ParamType.BOOLEAN; }
}
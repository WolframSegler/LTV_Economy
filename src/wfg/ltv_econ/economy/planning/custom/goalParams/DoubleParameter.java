package wfg.ltv_econ.economy.planning.custom.goalParams;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class DoubleParameter extends GoalParameter {
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final double min, max;

    public DoubleParameter(String id, String name, double min, double max,
        DoubleSupplier getter, DoubleConsumer setter
    ) {
        super(id, name);
        this.min = min; this.max = max;
        this.getter = getter;
        this.setter = setter;
    }

    public double getValue() { return getter.getAsDouble(); }
    public void setValue(double value) { setter.accept(value); }
    public double getMin() { return min; }
    public double getMax() { return max; }

    @Override public String getValueAsString() { return Double.toString(getValue()); }
    @Override public void setValueFromString(String s) { setValue(Double.parseDouble(s)); }
    @Override public ParamType getParamType() { return ParamType.DOUBLE; }
}
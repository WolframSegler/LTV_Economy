package wfg.ltv_econ.economy.planning.custom.goalParams;

public abstract class GoalParameter {
    public final String id;
    public final String name;

    protected GoalParameter(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Used for serialisation – the current value as a string. */
    public abstract String getValueAsString();
    /** Restore the value from a previously serialised string. */
    public abstract void setValueFromString(String value);

    public abstract WidgetType getWidgetType();
    public abstract ParamType getParamType();

    public enum WidgetType {
        SLIDER,
        DROPDOWN,
        CHECKBOX,
        MULTI_SELECT,
        TEXT_FIELD,
    }

    public enum ParamType {
        BOOLEAN,
        DOUBLE,
        MULTI_SELECT,
        RADIO
    }
}
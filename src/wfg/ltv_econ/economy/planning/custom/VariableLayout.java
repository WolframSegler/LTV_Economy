package wfg.ltv_econ.economy.planning.custom;

public class VariableLayout {
    public final int workerVars;
    public final int tierCount;
    public final int totalVars;

    public VariableLayout(int workerVars, int tierCount, int totalVars) {
        this.workerVars = workerVars;
        this.tierCount = tierCount;
        this.totalVars = totalVars;
    }
}
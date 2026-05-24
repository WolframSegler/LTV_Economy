package wfg.ltv_econ.economy.planning.custom;

import wfg.ltv_econ.economy.planning.DenseModel;

public class PlanningContext {
    public final DenseModel denseData;
    public final double[][] A;
    public final double[] globalDemand;
    public final double[][] marketDemand;
    public final double[] factionDemand;
    public final int outputCount;
    public final int marketCount;
    public final int commodityCount;
    public final PiecewiseSegments segments;

    public PlanningContext(
        DenseModel denseData, double[][] A, double[] globalDemand, double[][] marketDemand,
        double[] factionDemand, int outputCount, int marketCount, int commodityCount,
        PiecewiseSegments segments
    ) {
        this.denseData = denseData;
        this.A = A;
        this.globalDemand = globalDemand;
        this.marketDemand = marketDemand;
        this.factionDemand = factionDemand;
        this.outputCount = outputCount;
        this.marketCount = marketCount;
        this.commodityCount = commodityCount;
        this.segments = segments;
    }
}
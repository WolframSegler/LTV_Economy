package wfg.ltv_econ.economy.fleet;

import wfg.native_ui.util.ArrayMap;

public class PlannedOrder {
    public final String hullId;
    public final ArrayMap<String, Float> commodities;
    public final long credits;
    public final int days;

    public PlannedOrder(String hullId, long credits, ArrayMap<String, Float> commodities, int days) {
        this.hullId = hullId;
        this.credits = credits;
        this.commodities = commodities;
        this.days = days;
    }
}
package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;

public class ShipProductionOrder implements Serializable {
    public final String hullId;
    public int daysRemaining;

    public ShipProductionOrder(String hullId, int daysRemaining) {
        this.hullId = hullId;
        this.daysRemaining = daysRemaining;
    }
}
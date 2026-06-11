package wfg.ltv_econ.economy.fleet;

import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.EconomyRouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;

import wfg.ltv_econ.economy.engine.EconomyEngine;

public class LtvEconomyRouteData extends EconomyRouteData {
    public final long uniqueID;
    private transient TradeMission mission;

    public LtvEconomyRouteData(TradeMission mission) {
        uniqueID = mission.uniqueID;
        this.mission = mission;

        from = mission.src;
        to = mission.dest;
        smuggling = mission.smuggling;
    }

    public final TradeMission getMission() {
        if (mission == null) {
            for (TradeMission m : EconomyEngine.instance().getActiveMissions()) {
                if (m.uniqueID == uniqueID) {
                    mission = m;
                    break;
                }
            }
        }
        return mission;
    }

    public static final TradeMission getMission(RouteData route) {
        if (route.getCustom() instanceof LtvEconomyRouteData data) {
            return data.getMission();
        }
        return null;
    }
}
package wfg.ltv_econ.economy.fleet;

import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.EconomyRouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;

import wfg.ltv_econ.economy.engine.EconomyEngine;

// TODO After incompatible update change route manager access TradeMission through this class.
public class LtvEconomyRouteData extends EconomyRouteData {
    private transient TradeMission mission;
    public final long uniqueID;

    public LtvEconomyRouteData(TradeMission mission) {
        uniqueID = mission.uniqueID;
        this.mission = mission;
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

    public static final LtvEconomyRouteData createData(TradeMission mission) {
        final LtvEconomyRouteData data = new LtvEconomyRouteData(mission);
        data.from = mission.src;
        data.to = mission.dest;
        data.smuggling = mission.smuggling;

        return data;
    }

    public static final TradeMission getMission(RouteData route) {
        if (route.getCustom() instanceof LtvEconomyRouteData data) {
            return data.mission;
        }
        return null;
    }
}
package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;

import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.native_ui.util.ArrayMap;

public class PatrolFleetRouteManager {
    private final HashMap<Long, PatrolMission> missions = new HashMap<>(48);

    public final synchronized void freePatrol(RouteData route) {
        final PatrolMission mission = missions.get(route.getSeed());
        if (mission == null) return;

        missions.remove(route.getSeed());
        if (!mission.usedFactionFleet) return;

        final FactionShipInventory inv = EconomyEngine.instance().getFactionShipInventory(mission.factionID);
        for (Map.Entry<String, Integer> entry : mission.allocatedShips.singleEntrySet()) {
            inv.freeShip(entry.getKey(), entry.getValue());
        }
    }

    public final synchronized void registerPatrolLoss(RouteData route) {
        final PatrolMission mission = missions.get(route.getSeed());
        if (mission == null) return;

        missions.remove(route.getSeed());
        if (!mission.usedFactionFleet) return;

        final FactionShipInventory inv = EconomyEngine.instance().getFactionShipInventory(mission.factionID);
        for (Map.Entry<String, Integer> entry : mission.allocatedShips.singleEntrySet()) {
            inv.registerShipLoss(entry.getKey(), entry.getValue());
        }
    }

    public final synchronized void registerMission(RouteData route) {
        // TODO calculate target combat power and allocate either from faction inventory or independet captain.
        // TODO create missions and add to the map.
    }

    public final synchronized PatrolMission getMission(RouteData route) {
        return missions.get(route.getSeed());
    }

    public class PatrolMission implements Serializable {
        public final ArrayMap<String, Integer> allocatedShips = new ArrayMap<>(8);
        public final String factionID;
        public final double fleetPoints;
        
        public boolean usedFactionFleet = false;

        public PatrolMission(MarketAPI source, double fp) {
            factionID = source.getFactionId();
            fleetPoints = fp;
        }
    }
}
package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constant.strings.Income.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
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
        final float fp = route.getExtra().getStrengthModifiedByDamage();
        final PatrolMission mission = new PatrolMission(route.getMarket());
        missions.put(route.getSeed(), mission);

        final FactionShipInventory inv = EconomyEngine.instance().getFactionShipInventory(mission.factionID);
        final float idleFp = inv.getIdleFleetPoints();

        if (idleFp >= fp) { 
            ShipAllocator.allocateShipsForPatrol(inv, mission, fp);
        } else {
            ShipAllocator.allocateShipsForFleetPoints(Global.getSector().getFaction(mission.factionID), mission, fp);

            float totalValue = 0f;
            for (Entry<String, Integer> e : mission.allocatedShips.singleEntrySet()) totalValue += inv.get(e.getKey()).spec.getFleetPoints() * e.getValue();

            final float fee = EconConfig.INDEPENDENT_PATROL_FLEET_FEE_PER_100_FP * totalValue / 100f;

            MarketFinanceRegistry.instance().getLedger(inv.getCapital()).add(
                INDEPENDENT_PATROL_COST_KEY, -fee, getDesc(INDEPENDENT_PATROL_COST_KEY)
            );
        }
    }

    public final synchronized PatrolMission getMission(RouteData route) {
        return missions.get(route.getSeed());
    }

    public class PatrolMission implements Serializable {
        public final ArrayMap<String, Integer> allocatedShips = new ArrayMap<>(8);
        public final String factionID;
        
        public boolean usedFactionFleet = false;

        public PatrolMission(MarketAPI source) {
            factionID = source.getFactionId();
        }
    }
}
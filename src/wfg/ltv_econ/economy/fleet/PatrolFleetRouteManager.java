package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constant.strings.Income.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType;
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
        final FactionAPI faction = Global.getSector().getFaction(mission.factionID);
        final float idleFp = inv.getIdleFleetPoints();

        final PatrolFleetData custom = (PatrolFleetData) route.getCustom();
        final PatrolType type = custom.type;
        final Predicate<ShipHullSpecAPI> filter = PatrolFilters.forType(type);

        final List<InventoryShip> candidates = new ArrayList<>();
        if (fp > idleFp) {
            for (String hullId : faction.getKnownShips()) {
                final InventoryShip dummy = new InventoryShip(hullId);
                if (!filter.test(dummy.spec)) continue;
                dummy.addShip(4096);
                candidates.add(dummy);
            }
        } else {
            for (InventoryShip data : inv.getShips().values()) {
                if (data.getIdle() <= 0) continue;
                if (!filter.test(data.spec)) continue;
                candidates.add(data);
            }
        }

        ShipAllocator.allocateShipsForFleetPoints(fp, faction, mission.allocatedShips, candidates);

        if (fp > idleFp) {
            float totalValue = 0f;
            for (Entry<String, Integer> e : mission.allocatedShips.singleEntrySet()) {
                totalValue += inv.get(e.getKey()).spec.getFleetPoints() * e.getValue();
            }
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

    public static final class PatrolFilters {
        private PatrolFilters() {};

        public static final Predicate<ShipHullSpecAPI> forType(PatrolType type) {
            return switch (type) {
                case FAST -> PatrolFilters::isFastEligible;
                case COMBAT, HEAVY -> PatrolFilters::isCombatEligible;
                default -> s -> true;
            };
        }

        private static final boolean isFastEligible(ShipHullSpecAPI spec) {
            if (InventoryShip.isCivilian(spec)) return false;
            final HullSize size = spec.getHullSize();
            return size == HullSize.FRIGATE
                || size == HullSize.DESTROYER
                || size == HullSize.CRUISER;
        }

        private static final boolean isCombatEligible(ShipHullSpecAPI spec) {
            return !InventoryShip.isCivilian(spec);
        }
    }
}
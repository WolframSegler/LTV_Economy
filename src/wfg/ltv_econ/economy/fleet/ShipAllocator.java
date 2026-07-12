package wfg.ltv_econ.economy.fleet;

import static wfg.native_ui.util.Globals.settings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.fleet.PatrolFleetRouteManager.PatrolMission;
import wfg.native_ui.util.ArrayMap;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class ShipAllocator {
    private ShipAllocator() {}
    private static final Logger log = Global.getLogger(ShipAllocator.class);
    private static final double eps = 1e-3;
    private static final HashMap<String, Double> DOCTRINE_PREF_CACHE = new HashMap<>();
    private static final char DOCT_PREF_KEY = '|';

    private static final double DIVERSITY_PENALTY = 0.4;
    private static final double REF_SHIPMENT = 500d;
    private static final float COMBAT_POWER_BASE_PER_100_TONS = 2f;
    private static final float[] COMBAT_POWER_DANGER_MULT = {
        0.1f, // NONE
        0.4f, // MINIMAL
        0.8f, // LOW
        1.4f, // MEDIUM
        2.5f, // HIGH
        3.5f  // EXTREME
    };

    public static final float getRequiredCombatPower(TradeMission mission) {
        final float totalShipment = mission.getTotalAmount();
        if (totalShipment <= 0) throw new IllegalArgumentException("Total shipment value is: " + totalShipment);

        final StarSystemAPI srcSys = mission.src.getStarSystem();
        final StarSystemAPI dstSys = mission.dest.getStarSystem();
        
        final int dangerOrdinal = getHighestLocationDangerInRoute(mission.src.getFaction().getId(), srcSys, dstSys).ordinal();

        final float dangerMult = COMBAT_POWER_DANGER_MULT[dangerOrdinal];
        final float required = (totalShipment / 100f) * COMBAT_POWER_BASE_PER_100_TONS * dangerMult;
        return Math.max(required, EconConfig.SHIP_ALLOC_MIN_COMBAT_POWER);
    }

    public static final float getRequiredFuelForShips(ArrayMap<String, Integer> allocatedShips, float distLY) {
        float cost = 0f;
        for (Map.Entry<String, Integer> entry : allocatedShips.singleEntrySet()) {
            cost += settings.getHullSpec(entry.getKey()).getFuelPerLY() * entry.getValue() * distLY;
        }
        return cost;
    }

    public static final LocationDanger getHighestLocationDangerInRoute(final String factionID, final StarSystemAPI src, final StarSystemAPI dest) {
        LocationDanger danger = LocationDanger.NONE;
        try {
            if (src != null) {
                final LocationDanger d = WarSimScript.getDangerFor(factionID, src);
                if (d.ordinal() > danger.ordinal()) danger = d;
            }
            if (dest != null) {
                final LocationDanger d = WarSimScript.getDangerFor(factionID, dest);
                if (d.ordinal() > danger.ordinal()) danger = d;
            }
        } catch (ConcurrentModificationException | NullPointerException e) {}

        return danger;
    } 

    /**
     * Allocate ships to meet given targets. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForTrade(
        FactionShipInventory inventory, TradeMission mission
    ) {
        final List<ShipTypeData> ships = new ArrayList<>(inventory.getShips().values());
        ships.removeIf(s -> s.getIdle() <= 0);
        allocateShipsForTarget(mission.cargoAmount, mission.fuelAmount, mission.crewAmount, mission.combatPowerTarget,
            Global.getSector().getFaction(inventory.factionID), mission.allocatedShips, ships
        );
    }

    /**
     * Allocate ships to meet given fleet points. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForPatrol(
        FactionShipInventory inventory, PatrolMission mission, double fleetPoints
    ) {
        final List<ShipTypeData> candidates = new ArrayList<>(inventory.getShips().values());
        candidates.removeIf(s -> s.getIdle() <= 0);
        allocateShipsForFleetPoints(fleetPoints,
            Global.getSector().getFaction(inventory.factionID), mission.allocatedShips, candidates
        );
    }

    /**
     * Allocate ships to meet given targets. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForTarget(FactionAPI faction, TradeMission mission) {
        allocateShipsForTarget(mission.cargoAmount, mission.fuelAmount, mission.crewAmount, mission.combatPowerTarget,
            faction, mission.allocatedShips
        );
    }

    /**
     * Allocate ships to meet given fleet points. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForFleetPoints(FactionAPI faction, PatrolMission mission, double fleetPoints) {
        allocateShipsForFleetPoints(fleetPoints, faction, mission.allocatedShips);
    }

    /**
     * Allocate ships to meet given targets. Populates the allocation map.
     * 
     * @param targetCargo cargo capacity (tons)
     * @param targetFuel fuel capacity (tons)
     * @param targetCrew crew transport capacity
     * @param targetCombat combat power
     * @param faction used for doctrine preferences
     * @param allocation the allocation to be populated.
     */
    public static final void allocateShipsForTarget(
        double targetCargo, double targetFuel, double targetCrew, double targetCombat,
        FactionAPI faction, Map<String, Integer> allocation
    ) {
        final List<ShipTypeData> candidates = new ArrayList<>();
        for (String hullId : faction.getKnownShips()) {
            final ShipTypeData dummy = new ShipTypeData(hullId);
            dummy.addShip(4096);
            candidates.add(dummy);
        }
        if (candidates.isEmpty()) return;

        allocateShipsForTarget(targetCargo, targetFuel, targetCrew, targetCombat,
            faction, allocation, candidates
        );
    }

    /**
     * Allocate ships to meet given fleet points. Populates the allocation map.
     * 
     * @param fleetPoints combat power
     * @param faction used for doctrine preferences
     * @param allocation the allocation to be populated.
     */
    public static final void allocateShipsForFleetPoints(double fleetPoints, FactionAPI faction, Map<String, Integer> allocation) {
        final List<ShipTypeData> candidates = new ArrayList<>();
        for (String hullId : faction.getKnownShips()) {
            final ShipTypeData dummy = new ShipTypeData(hullId);
            dummy.addShip(4096);
            candidates.add(dummy);
        }
        if (candidates.isEmpty()) return;

        allocateShipsForFleetPoints(fleetPoints, faction, allocation, candidates);
    }

    /**
     * Allocate ships to meet given targets. Populates the allocation map.
     * 
     * @param targetCargo cargo capacity (tons)
     * @param targetFuel fuel capacity (tons)
     * @param targetCrew crew transport capacity
     * @param targetCombat combat power
     * @param faction used for doctrine preferences
     * @param allocation the allocation to be populated.
     * @param candidates the pool of hulls to choose from
     */
    public static final void allocateShipsForTarget(
        double targetCargo, double targetFuel, double targetCrew, double targetCombat,
        FactionAPI faction, Map<String, Integer> allocation, List<ShipTypeData> candidates
    ) {
        final double totalTarget = targetCargo + targetFuel + targetCrew + targetCombat;
        if (totalTarget <= 0d) return;

        final int N = candidates.size();
        if (N <= 0) throw new IllegalStateException("No ship candidates: " + faction.getId());

        final int[] idleRemaining = new int[N];
        final double[] weight = buildTargetWeights(candidates, faction, targetCargo, targetFuel, targetCrew, targetCombat);
        final double[] cargoCap = new double[N];
        final double[] fuelCap = new double[N];
        final double[] crewCap = new double[N];
        final double[] combatCap = new double[N];

        for (int i = 0; i < N; i++) {
            final ShipTypeData data = candidates.get(i);
            cargoCap[i] = data.spec.getCargo();
            fuelCap[i] = data.spec.getFuel();
            crewCap[i] = data.getCrewCapacityPerShip();
            combatCap[i] = data.getCombatPower();
            idleRemaining[i] = data.getIdle();
        }

        final int[] counts = new int[N];
        double remCargo = targetCargo;
        double remFuel = targetFuel;
        double remCrew = targetCrew;
        double remCombat = targetCombat;

        while (remCargo > eps || remFuel > eps || remCrew > eps || remCombat > eps) {
            final double combatNeed = (targetCombat > eps) ? remCombat / targetCombat : 0d;
            final double cargoNeed = (targetCargo > eps) ? remCargo / targetCargo : 0d;
            final double fuelNeed = (targetFuel > eps) ? remFuel / targetFuel : 0d;
            final double crewNeed = (targetCrew > eps) ? remCrew / targetCrew : 0d;

            double totalWeight = 0d;
            double[] weights = new double[N];

            for (int i = 0; i < N; i++) {
                if (idleRemaining[i] <= 0) continue;

                boolean useful = false;
                if (remCargo > 0d && cargoCap[i] > 0d) useful = true;
                if (remFuel > 0d && fuelCap[i] > 0d) useful = true;
                if (remCrew > 0d && crewCap[i] > 0d) useful = true;
                if (remCombat > 0d && combatCap[i] > 0d) useful = true;
                if (!useful) continue;

                final double cargoContrib = (targetCargo > 0) ? Math.min(1d, cargoCap[i] / targetCargo) : 0d;
                final double fuelContrib = (targetFuel > 0) ? Math.min(1d, fuelCap[i] / targetFuel) : 0d;
                final double crewContrib = (targetCrew > 0) ? Math.min(1d, crewCap[i] / targetCrew) : 0d;
                final double combatContrib = (targetCombat > 0) ? Math.min(1d, combatCap[i] / targetCombat) : 0d;

                final double utility = cargoContrib * cargoNeed
                    + fuelContrib * fuelNeed
                    + crewContrib * crewNeed
                    + combatContrib * combatNeed;

                final double w = weight[i] * Math.pow(utility, 0.75) / (1d + DIVERSITY_PENALTY * counts[i]);

                weights[i] = w;
                totalWeight += w;
            }
            if (totalWeight <= 0d) break;

            final double r = Math.random() * totalWeight;
            int picked = -1;
            double accum = 0;
            for (int i = 0; i < N; i++) {
                accum += weights[i];
                if (r <= accum) {
                    picked = i;
                    break;
                }
            }
            if (picked < 0) break;

            counts[picked]++;
            idleRemaining[picked]--;

            remCargo = Math.max(0d, remCargo - cargoCap[picked]);
            remFuel = Math.max(0d, remFuel - fuelCap[picked]);
            remCrew = Math.max(0d, remCrew - crewCap[picked]);
            remCombat = Math.max(0d, remCombat- combatCap[picked]);
        }

        if (remCargo > eps) log.warn(faction.getId() + " - Not enough cargo capacity after allocation, remaining: " + (long) remCargo);
        if (remFuel > eps) log.warn(faction.getId() + " - Not enough fuel capacity after allocation, remaining: " + (long) remFuel);
        if (remCrew > eps) log.warn(faction.getId() + " - Not enough crew capacity after allocation, remaining: " + (long) remCrew);

        for (int i = 0; i < N; i++) {
            final int count = counts[i];
            if (count <= 0) continue;

            final ShipTypeData data = candidates.get(i);
            data.useShip(count);
            allocation.put(data.hullID, count);
        }
    }

    /**
     * Allocate ships to meet given fleet points. Populates the allocation map.
     * 
     * @param targetFp combat power
     * @param faction used for doctrine preferences
     * @param allocation the allocation to be populated.
     * @param candidates the pool of hulls to choose from
     */
    public static final void allocateShipsForFleetPoints(
        double targetFp, FactionAPI faction, Map<String, Integer> allocation, List<ShipTypeData> candidates
    ) {
        if (targetFp <= 0d) return;

        final int N = candidates.size();
        if (N <= 0) throw new IllegalStateException("No ship candidates: " + faction.getId());

        final double[] baseWeights = buildFleetPointWeights(candidates, faction);
        final float[] fleetPoints = new float[N];
        final int[] idleRemaining = new int[N];

        for (int i = 0; i < N; i++) {
            final ShipTypeData ship = candidates.get(i);
            idleRemaining[i] = ship.getIdle();
            fleetPoints[i] = ship.spec.getFleetPoints();
        }

        final int[] counts = new int[N];
        double remFp = targetFp;

        while (remFp > eps) {
            double totalWeight = 0d;
            double[] weights = new double[N];

            for (int i = 0; i < N; i++) {
                if (idleRemaining[i] <= 0) continue;

                final double w = baseWeights[i] / (1d + DIVERSITY_PENALTY * counts[i]);

                weights[i] = w;
                totalWeight += w;
            }
            if (totalWeight <= 0d) break;

            final double r = Math.random() * totalWeight;
            int picked = -1;
            double accum = 0;
            for (int i = 0; i < N; i++) {
                accum += weights[i];
                if (r <= accum) {
                    picked = i;
                    break;
                }
            }
            if (picked < 0) break;

            counts[picked]++;
            idleRemaining[picked]--;

            remFp -= fleetPoints[picked];
        }

        if (remFp > eps) log.warn(faction.getId() + " - Not enough fleet points after allocation, remaining: " + remFp);

        for (int i = 0; i < N; i++) {
            final int count = counts[i];
            if (count <= 0) continue;

            final ShipTypeData data = candidates.get(i);
            data.useShip(count);
            allocation.put(data.hullID, count);
        }
    }

    private static final double getDoctrinePreference(FactionAPI faction, ShipTypeData data) {
        final String key = faction.getId() + DOCT_PREF_KEY + data.hullID;
        final Double cached = DOCTRINE_PREF_CACHE.get(key);
        if (cached != null) return cached.doubleValue();

        double mult = 1d;

        final FactionDoctrineAPI doctrine = faction.getDoctrine();
        if (faction.knowsShip(data.hullID)) {
            mult *= 1.8d;
        }

        final int shipSizeWeight = hullSizeToWeight(data.spec.getHullSize());
        final int diff = Math.abs(doctrine.getShipSize() - shipSizeWeight); // [1, 5]
        mult *= getSizeMatchMultiplier(diff);
        mult *= getHullTypePreferenceMult(doctrine, data.spec);
        mult *= faction.isShipPriority(data.hullID) ? 1.8d : 1d;

        if (!faction.getId().equals(Factions.PLAYER)) DOCTRINE_PREF_CACHE.put(key, mult);
        return mult;
    }

    private static final int hullSizeToWeight(HullSize size) {
        switch (size) {
            default: case FRIGATE: return 1;
            case DESTROYER: return 2;
            case DEFAULT: return 3;
            case CRUISER: return 4;
            case CAPITAL_SHIP: return 5;
        }
    }

    private static final double getSizeMatchMultiplier(int diff) {
        switch (diff) {
            case 0: return 1.5d;
            case 1: return 1.25d;
            case 2: return 1d;
            case 3: return 0.75d;
            default: return 0.5d;
        }
    }

    private static final double getHullTypePreferenceMult(FactionDoctrineAPI doctrine, ShipHullSpecAPI spec) {
        double mult = 1d;

        if (spec.getShieldSpec().getType() == ShieldType.PHASE) {
            mult *= 0.4 + doctrine.getPhaseShips() * 0.3; // [1, 5]
        }

        if (spec.getHints().contains(ShipTypeHints.CARRIER)) {
            mult *= 0.4 + doctrine.getCarriers() * 0.3; // [1, 5]
        }

        if (!spec.getHints().contains(ShipTypeHints.CIVILIAN)) {
            mult *= 0.4 + doctrine.getWarships() * 0.3; // [1, 5]
        }

        return mult;
    }

    private static final double[] buildTargetWeights(List<ShipTypeData> candidates, FactionAPI faction,
        double targetCargo, double targetFuel, double targetCrew, double targetCombat
    ) {
        final int N = candidates.size();
        final double totalShipment = targetCargo + targetFuel + targetCrew;
        final double totalTarget = totalShipment + targetCombat;

        final double transportWeight = totalShipment / totalTarget;
        final double combatWeight = targetCombat / totalTarget;
        final double cargoWeight = targetCargo / totalShipment;
        final double fuelWeight = targetFuel / totalShipment;
        final double crewWeight = targetCrew / totalShipment;

        final double[] coeffs = new double[N];
        for (int i = 0; i < N; i++) {
            final ShipTypeData data = candidates.get(i);
            final ShipHullSpecAPI spec = data.spec;
            
            final double transportScore = 1d
                + crewWeight * (data.getCrewCapacityPerShip() / REF_SHIPMENT)
                + cargoWeight * (spec.getCargo() / REF_SHIPMENT)
                + fuelWeight * (spec.getFuel() / REF_SHIPMENT);

            final double combatScore = data.getCombatPower() / targetCombat;
            
            final double combinedScore = Math.pow(transportScore, transportWeight)
                * Math.pow(combatScore, combatWeight);
            
            final double baseCost = combinedScore / data.spec.getFleetPoints();
            final double doctrineFactor = getDoctrinePreference(faction, data);
            final double randFactor = 0.75d + 0.5d * Math.random();
            
            coeffs[i] = baseCost * doctrineFactor * randFactor;
        }

        return coeffs;
    }

    private static final double[] buildFleetPointWeights(List<ShipTypeData> candidates, FactionAPI faction) {
        final int N = candidates.size();

        final double[] coeffs = new double[N];
        for (int i = 0; i < N; i++) {
            final ShipTypeData data = candidates.get(i);

            final double baseCost = data.getCombatPower() / Math.pow(data.spec.getFleetPoints(), 2d);
            final double doctrineFactor = getDoctrinePreference(faction, data);
            final double randFactor = 0.75d + 0.5d * Math.random();
            
            coeffs[i] = baseCost * doctrineFactor * randFactor;
        }

        return coeffs;
    }
}
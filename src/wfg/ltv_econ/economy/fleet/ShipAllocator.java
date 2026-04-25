package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.economy.fleet.ShipTypeData.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.config.EconConfig;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipAllocator {
    private ShipAllocator() {}
    private static final double eps = 1e-3;
    private static final HashMap<String, Double> DOCTRINE_PREF_CACHE = new HashMap<>();
    private static final String DOCT_PREF_KEY = "|";

    private static final double DIVERSITY_PENALTY = 0.2;
    private static final double REF_SHIPMENT = 500.0;
    private static final float COMBAT_POWER_BASE_PER_100_TONS = 5.0f;
    private static final float[] COMBAT_POWER_DANGER_MULT = {
        0.2f, // NONE
        0.8f, // MINIMAL
        1.5f, // LOW
        2.2f, // MEDIUM
        3.2f, // HIGH
        5.0f  // EXTREME
    };

    public static final float getRequiredCombatPower(TradeMission mission) {
        final float totalShipment = mission.getTotalAmount();
        if (totalShipment <= 0) throw new IllegalArgumentException("Total shipment value is: " + totalShipment);

        final StarSystemAPI srcSys = mission.src.getStarSystem();
        final StarSystemAPI dstSys = mission.dest.getStarSystem();
        
        int dangerOrdinal = 0;
        try {
            if (srcSys != null) {
                final LocationDanger d = WarSimScript.getDangerFor(mission.src.getFaction().getId(), srcSys);
                dangerOrdinal = Math.max(dangerOrdinal, d.ordinal());
            }
            if (dstSys != null) {
                final LocationDanger d = WarSimScript.getDangerFor(mission.dest.getFaction().getId(), dstSys);
                dangerOrdinal = Math.max(dangerOrdinal, d.ordinal());
            }
        } catch (ConcurrentModificationException e) {}

        final float dangerMult = COMBAT_POWER_DANGER_MULT[dangerOrdinal];
        final float required = (totalShipment / 100f) * COMBAT_POWER_BASE_PER_100_TONS * dangerMult;
        return Math.max(required, EconConfig.SHIP_ALLOC_MIN_COMBAT_POWER);
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
     * Allocate ships to meet given targets. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForTarget(
        FactionAPI faction, TradeMission mission
    ) {
        final List<ShipTypeData> candidates = new ArrayList<>();
        for (String hullId : faction.getKnownShips()) {
            final ShipTypeData dummy = new ShipTypeData(hullId);
            dummy.addShip(4096);
            candidates.add(dummy);
        }
        if (candidates.isEmpty()) return;

        allocateShipsForTarget(mission.cargoAmount, mission.fuelAmount, mission.crewAmount, mission.combatPowerTarget,
            faction, mission.allocatedShips, candidates
        );
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
    public static void allocateShipsForTarget(
        double targetCargo, double targetFuel, double targetCrew, double targetCombat,
        FactionAPI faction, Map<String, Integer> allocation, List<ShipTypeData> candidates
    ) {
        final double totalTarget = targetCargo + targetFuel + targetCrew + targetCombat;
        if (totalTarget <= 0) return;

        final int N = candidates.size();
        if (N <= 0) throw new IllegalStateException("No ship candidates: " + faction.getId());

        final int[] idleRemaining = new int[N];
        final double[] weight = buildTargetObjective(candidates, faction, targetCargo, targetFuel, targetCrew, targetCombat);
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

        while ((remCargo > eps || remFuel > eps || remCrew > eps || remCombat > eps)) {
            final double combatNeed = (targetCombat > eps) ? remCombat / targetCombat : 0.0;
            final double cargoNeed = (targetCargo > eps) ? remCargo / targetCargo : 0.0;
            final double fuelNeed = (targetFuel > eps) ? remFuel / targetFuel : 0.0;
            final double crewNeed = (targetCrew > eps) ? remCrew / targetCrew : 0.0;

            double totalWeight = 0.0;
            double[] weights = new double[N];

            for (int i = 0; i < N; i++) {
                if (idleRemaining[i] <= 0) continue;

                boolean useful = false;
                if (remCargo > 0.0 && cargoCap[i] > 0.0) useful = true;
                if (remFuel > 0.0 && fuelCap[i] > 0.0) useful = true;
                if (remCrew > 0.0 && crewCap[i] > 0.0) useful = true;
                if (remCombat > 0.0 && combatCap[i] > 0.0) useful = true;
                if (!useful) continue;

                final double cargoContrib = (targetCargo > 0) ? Math.min(1.0, cargoCap[i] / targetCargo) : 0.0;
                final double fuelContrib = (targetFuel > 0) ? Math.min(1.0, fuelCap[i] / targetFuel) : 0.0;
                final double crewContrib = (targetCrew > 0) ? Math.min(1.0, crewCap[i] / targetCrew) : 0.0;
                final double combatContrib = (targetCombat > 0) ? Math.min(1.0, combatCap[i] / targetCombat) : 0.0;

                final double utility = cargoContrib * cargoNeed
                    + fuelContrib * fuelNeed
                    + crewContrib * crewNeed
                    + combatContrib * combatNeed;

                final double w = weight[i] * Math.pow(utility, 1.8) / (1.0 + DIVERSITY_PENALTY * counts[i]);

                weights[i] = w;
                totalWeight += w;
            }

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

            remCargo = Math.max(0.0, remCargo - cargoCap[picked]);
            remFuel = Math.max(0.0, remFuel - fuelCap[picked]);
            remCrew = Math.max(0.0, remCrew - crewCap[picked]);
            remCombat = Math.max(0.0, remCombat- combatCap[picked]);
        }

        if (remCargo > eps) throw new IllegalStateException("Not enough cargo capacity after allocation");
        if (remFuel > eps) throw new IllegalStateException("Not enough fuel capacity after allocation");
        if (remCrew > eps) throw new IllegalStateException("Not enough crew capacity after allocation");

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

        double mult = 1.0;

        final FactionDoctrineAPI doctrine = faction.getDoctrine();
        if (faction.knowsShip(data.hullID)) {
            mult *= 0.7;
        }

        final int shipSizeWeight = hullSizeToWeight(data.spec.getHullSize());
        final int diff = Math.abs(doctrine.getShipSize() - shipSizeWeight); // [1, 5]
        mult *= getSizeMatchMultiplier(diff);
        mult *= getHullSizePreferenceMult(doctrine, data.spec.getDesignation());
        mult *= faction.isShipPriority(data.hullID) ? 0.5 : 1.0;

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
            case 0: return 0.6;
            case 1: return 0.8;
            case 2: return 1.0;
            default: return 1.2;
        }
    }

    private static final double getHullSizePreferenceMult(FactionDoctrineAPI doctrine, String designation) {
        if (designation == null) return 1.0;

        return 1.0 / switch (designation) {
            case CIVILIAN -> 1.0 + doctrine.getCombatFreighterProbability();
            case FRIGATES -> 1.0 + doctrine.getNumShips() * 0.3; // [1, 5]
            case DESTROYERS -> 1.0 + doctrine.getNumShips() * 0.5; // [1, 5]
            case CAPITALS -> 1.0 + doctrine.getWarships() * 0.3; // [1, 5]
            case PHASE_SHIPS -> 1.0 + doctrine.getPhaseShips() * 0.3; // [1, 5]
            case CARRIERS -> 1.0 + doctrine.getCarriers() * 0.3; // [1, 5]
            default -> 1.0;
        };
    }

    private static final double[] buildTargetObjective(List<ShipTypeData> candidates, FactionAPI faction,
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
            
            final double transportScore = 1.0
                + crewWeight * (data.getCrewCapacityPerShip() / REF_SHIPMENT)
                + cargoWeight * (spec.getCargo() / REF_SHIPMENT)
                + fuelWeight * (spec.getFuel() / REF_SHIPMENT);

            final double combatScore = data.getCombatPower() / targetCombat;
            
            final double combinedScore = Math.pow(transportScore, transportWeight)
                * Math.pow(combatScore, combatWeight);
            
            final double baseCost = 1.0 / (1.0 + combinedScore * data.spec.getFleetPoints());
            final double doctrineFactor = getDoctrinePreference(faction, data);
            final double randFactor = 0.7 + 0.6 * Math.random();
            
            coeffs[i] = baseCost * doctrineFactor * randFactor;
        }

        return coeffs;
    }
}
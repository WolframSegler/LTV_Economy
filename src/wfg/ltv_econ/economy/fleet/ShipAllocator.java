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

import wfg.native_ui.util.ArrayMap;

import org.apache.commons.math4.legacy.optim.linear.*;
import org.apache.commons.math4.legacy.optim.nonlinear.scalar.GoalType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math4.legacy.optim.MaxIter;

public class ShipAllocator {
    private ShipAllocator() {}
    private static final double eps = 1e-2;
    
    private static final HashMap<String, Double> DOCTRINE_PREF_CACHE = new HashMap<>();
    private static final String DOCT_PREF_KEY = "|";

    // Doctrine preferences
    private static final float DOCTRINE_KNOWN_SHIP_MULT = 0.7f;
    private static final float DOCTRINE_SIZE_EXACT_MULT = 0.8f;
    private static final float DOCTRINE_SIZE_CLOSE_MULT = 0.9f;
    private static final float DOCTRINE_SIZE_FAR_MULT = 1.0f;
    private static final float DOCTRINE_SIZE_VERY_FAR_MULT = 1.1f;

    private static final float COMBAT_POWER_BASE_PER_100_TONS = 1.0f;
    private static final float MIN_COMBAT_POWER = 10f;
    private static final float[] COMBAT_POWER_DANGER_MULT = {
        0.0f,   // NONE
        0.5f,   // MINIMAL
        1.0f,   // LOW
        1.5f,   // MEDIUM
        2.0f,   // HIGH
        3.0f    // EXTREME
    };

    /**
     * Allocate ships from the faction inventory to meet given targets. Updates the tracking values inside the inventory and the mission.
     * 
     * @param inventory the faction's ship inventory
     * @param mission the mission to be used and modified
     * @throws IllegalStateException if after greedy fill cargo target is still not met
     */
    public static final void allocateShipsForTrade(FactionShipInventory inventory, TradeMission mission) {
        allocateShipsForTrade(mission.cargoAmount, mission.fuelAmount, mission.crewAmount, mission.combatPowerTarget,
            inventory, mission.allocatedShips, Global.getSector().getFaction(inventory.factionID)
        );
    }

    /**
     * Allocate ships from the faction inventory to meet given targets. Updates the tracking values inside the inventory and the mission.
     *
     * @param targetCargo cargo capacity (tons)
     * @param targetFuel fuel capacity (tons)
     * @param targetCrew crew (persons)
     * @param targetCombat combat power
     * @param inventory the faction's ship inventory
     * @param allocation the map to be filled with allocations
     * @param faction the faction (for doctrine preferences)
     * @throws IllegalStateException if after greedy fill targets are still not met
     */
    public static final void allocateShipsForTrade(
        double targetCargo, double targetFuel, double targetCrew, double targetCombat,
        FactionShipInventory inventory, ArrayMap<ShipTypeData, Integer> allocation, FactionAPI faction
    ) {
        final double totalShipment = targetCargo + targetFuel + targetCrew;
        if (totalShipment <= 0) throw new IllegalArgumentException("Total shipment value is 0");

        final ArrayList<ShipTypeData> candidates = new ArrayList<>();
        for (ShipTypeData data : inventory.getShips().values()) {
            if (data.getIdle() > 0) candidates.add(data);
        }
        if (candidates.isEmpty()) return;

        final int N = candidates.size();

        final double[] objective = buildObjectiveWithInv(
            N, candidates, faction, targetCargo, targetFuel, targetCrew, targetCombat
        );
        final ArrayList<LinearConstraint> constraints = new ArrayList<>(N + 4);

        constraints.addAll(buildTargetConstraintsWithInv(N, candidates, targetCargo, targetFuel, targetCrew, targetCombat));

        { // Upper Bound by Idle Constraint 
            final double[] coeffs = new double[N];
            for (int i = 0; i < N; i++) {
                Arrays.fill(coeffs, 0.0);
                coeffs[i] = 1.0;
                constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, candidates.get(i).getIdle()));
            }
        }
        
        final double[] x = simplexSolveWithInv(objective, constraints);
        
        final ArrayMap<ShipTypeData, Integer> idleCopy = new ArrayMap<>(candidates.size());
        for (ShipTypeData data : candidates) {
            idleCopy.put(data, data.getIdle());
        }

        double remainingCargo = targetCargo;
        double remainingFuel = targetFuel;
        double remainingCrew = targetCrew;

        for (int i = 0; i < N; i++) { // Track updated targets
            final ShipTypeData data = candidates.get(i);
            final int allocated = (int) Math.floor(x[i]);
            if (allocated < 1) continue;

            allocation.put(data, allocated);
            idleCopy.put(data, idleCopy.get(data) - allocated);

            remainingCargo -= allocated * data.spec.getCargo();
            remainingFuel -= allocated * data.spec.getFuel();
            remainingCrew -= allocated * data.getCrewCapacityPerShip();
        }

        if (remainingCargo > 0) { // Greedy fill for cargo
            candidates.sort((a, b) -> Float.compare(b.spec.getCargo(), a.spec.getCargo()));
            for (ShipTypeData data : candidates) {
                final int idle = idleCopy.get(data);
                if (idle == 0) continue;

                final int needed = (int) Math.ceil(remainingCargo / data.spec.getCargo());
                final int take = Math.min(needed, idle);
                if (take < 1) continue;

                allocation.merge(data, take, Integer::sum);
                idleCopy.put(data, idle - take);

                remainingCargo -= take * data.spec.getCargo();
                remainingFuel -= take * data.spec.getFuel();
                remainingCrew -= take * data.getCrewCapacityPerShip();

                if (remainingCargo <= 0) break;
            }
        }

        if (remainingFuel > 0) { // Greedy fill for fuel
            candidates.sort((a, b) -> Float.compare(b.spec.getFuel(), a.spec.getFuel()));
            for (ShipTypeData data : candidates) {
                final int idle = idleCopy.get(data);
                if (idle == 0) continue;

                final int needed = (int) Math.ceil(remainingFuel / data.spec.getFuel());
                final int take = Math.min(needed, idle);
                if (take < 1) continue;

                allocation.merge(data, take, Integer::sum);
                idleCopy.put(data, idle - take);

                remainingCargo -= take * data.spec.getCargo();
                remainingFuel -= take * data.spec.getFuel();
                remainingCrew -= take * data.getCrewCapacityPerShip();

                if (remainingFuel <= 0) break;
            }
        }

        if (remainingCrew > 0) { // Greedy fill for crew
            candidates.sort((a, b) -> Integer.compare(b.getCrewCapacityPerShip(), a.getCrewCapacityPerShip()));
            for (ShipTypeData data : candidates) {
                final int idle = idleCopy.get(data);
                if (idle == 0) continue;

                final int needed = (int) Math.ceil(remainingCrew / data.getCrewCapacityPerShip());
                final int take = Math.min(needed, idle);
                if (take < 1) continue;

                allocation.merge(data, take, Integer::sum);
                idleCopy.put(data, idle - take);

                remainingCargo -= take * data.spec.getCargo();
                remainingFuel -= take * data.spec.getFuel();
                remainingCrew -= take * data.getCrewCapacityPerShip();

                if (remainingCrew <= 0) break;
            }
        }

        if (remainingCargo > eps) throw new IllegalStateException("Not enough cargo capacity after greedy fill");
        if (remainingFuel > eps) throw new IllegalStateException("Not enough fuel capacity after greedy fill");
        if (remainingCrew > eps) throw new IllegalStateException("Not enough crew capacity after greedy fill");

        for (Map.Entry<ShipTypeData, Integer> entry : allocation.singleEntrySet()) {
            inventory.useShips(entry.getKey().hullID, entry.getValue());
        }
    }

    public static final float getRequiredCombatPower(TradeMission mission) {
        final float totalShipment = mission.getTotalAmount();
        if (totalShipment <= 0) throw new IllegalArgumentException("Total shipment value is 0");

        final StarSystemAPI srcSys = mission.src.getStarSystem();
        final StarSystemAPI dstSys = mission.dest.getStarSystem();
        
        int dangerOrdinal = 0;
        if (srcSys != null) {
            final LocationDanger d = WarSimScript.getDangerFor(mission.src.getFaction().getId(), srcSys);
            dangerOrdinal = Math.max(dangerOrdinal, d.ordinal());
        }
        if (dstSys != null) {
            final LocationDanger d = WarSimScript.getDangerFor(mission.dest.getFaction().getId(), dstSys);
            dangerOrdinal = Math.max(dangerOrdinal, d.ordinal());
        }

        final float dangerMult = COMBAT_POWER_DANGER_MULT[dangerOrdinal];
        final float required = (totalShipment / 100f) * COMBAT_POWER_BASE_PER_100_TONS * dangerMult;
        return Math.max(required, MIN_COMBAT_POWER);
    }

    /**
     * Allocate ships to meet given targets. Updates the tracking values inside the mission.
     * 
     * @param faction used for doctrine preferences
     * @param mission the mission to be updated.
     */
    public static final void allocateShipsForTrade(
        FactionAPI faction, TradeMission mission
    ) {
        allocateShipsForTarget(mission.cargoAmount, mission.fuelAmount, mission.crewAmount, mission.combatPowerTarget,
            faction, mission.allocatedShips
        );
    }

    /**
     * Allocate ships to meet given targets. Populates the allocation map.
     * 
     * @param targetCargo cargo capacity (tons)
     * @param targetFuel fuel capacity (tons)
     * @param targetCrew crew (persons)
     * @param targetCombat combat power
     * @param faction used for doctrine preferences
     * @param allocation the allocation to be populated.
     */
    public static final void allocateShipsForTarget(
        double targetCargo, double targetFuel, double targetCrew, double targetCombat,
        FactionAPI faction, ArrayMap<ShipTypeData, Integer> allocation
    ) {
        final double totalShipment = targetCargo + targetFuel + targetCrew;
        if (totalShipment <= 0) throw new IllegalArgumentException("Total shipment value is 0");

        final List<ShipTypeData> candidates = new ArrayList<>();
        
        for (String hullId : faction.getKnownShips()) {
            final ShipTypeData dummy = new ShipTypeData(hullId);
            dummy.addShip(1023);
            candidates.add(dummy);
        }
        if (candidates.isEmpty()) return;

        final int N = candidates.size();

        final double[] objective = buildObjectiveWithInv(
            N, candidates, faction, targetCargo, targetFuel, targetCrew, targetCombat
        );
        final List<LinearConstraint> constraints = buildTargetConstraintsWithInv(
            N, candidates, targetCargo, targetFuel, targetCrew, targetCombat
        );

        final double[] x = simplexSolveWithInv(objective, constraints);

        for (int i = 0; i < N; i++) {
            final int count = (int) Math.ceil(x[i]);
            if (count < 1) continue;

            final ShipTypeData data = candidates.get(i);
            data.useShip(count);
            allocation.put(data, count);
        }
    }

    private static final double getDoctrinePreference(FactionAPI faction, ShipTypeData data) {
        final String key = faction.getId() + DOCT_PREF_KEY + data.hullID;
        final Double cached = DOCTRINE_PREF_CACHE.get(key);
        if (cached != null) return cached.doubleValue();

        double mult = 1.0;

        final FactionDoctrineAPI doctrine = faction.getDoctrine();
        if (faction.knowsShip(data.hullID)) {
            mult *= DOCTRINE_KNOWN_SHIP_MULT;
        }

        final int shipSizeWeight = hullSizeToWeight(data.spec.getHullSize());
        final int diff = Math.abs(doctrine.getShipSize() - shipSizeWeight); // [1, 5]
        mult *= getSizeMatchMultiplier(diff);
        mult *= getHullSizePreferenceMult(doctrine, data.spec.getDesignation());
        mult *= faction.isShipPriority(data.hullID) ? 1.3 : 1.0;

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
            case 0: return DOCTRINE_SIZE_EXACT_MULT;
            case 1: return DOCTRINE_SIZE_CLOSE_MULT;
            case 2: return DOCTRINE_SIZE_FAR_MULT;
            default: return DOCTRINE_SIZE_VERY_FAR_MULT;
        }
    }

    private static final double getHullSizePreferenceMult(FactionDoctrineAPI doctrine, String designation) {
        if (designation == null) return 1.0;

        return 1.0 / switch (designation) {
            case CIVILIAN -> 1.0 + doctrine.getCombatFreighterProbability();
            case FRIGATES -> 1.0 + doctrine.getNumShips() * 0.2; // [1, 5]
            case DESTROYERS -> 1.0 + doctrine.getNumShips() * 0.1; // [1, 5]
            case CAPITALS -> 1.0 + doctrine.getWarships() * 0.2; // [1, 5]
            case PHASE_SHIPS -> 1.0 + doctrine.getPhaseShips() * 0.2; // [1, 5]
            case CARRIERS -> 1.0 + doctrine.getCarriers() * 0.2; // [1, 5]
            default -> 1.0;
        };
    }

    private static final List<LinearConstraint> buildTargetConstraintsWithInv(int N, List<ShipTypeData> candidates,
        double targetCargo, double targetFuel, double targetCrew, double targetCombat
    ) {
        final List<LinearConstraint> constraints = new ArrayList<>(4);
        { // Cargo Capacity Constraint
            final double[] coeffs = new double[N];
            for (int i = 0; i < N; i++) coeffs[i] = candidates.get(i).spec.getCargo();
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, targetCargo));
        }

        { // Fuel Capacity Constraint
            final double[] coeffs = new double[N];
            for (int i = 0; i < N; i++) coeffs[i] = candidates.get(i).spec.getFuel();
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, targetFuel));
        }

        { // Crew Capacity Constraint
            final double[] coeffs = new double[N];
            for (int i = 0; i < N; i++) coeffs[i] = candidates.get(i).getCrewCapacityPerShip();
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, targetCrew));
        }

        if (targetCombat > 0f) { // Combat Capacity Constraint
            final double[] coeffs = new double[N];
            for (int i = 0; i < N; i++) coeffs[i] = candidates.get(i).getCombatPower();
            constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, targetCombat));
        }

        return constraints;
    }

    private static final double[] buildObjectiveWithInv(int N, List<ShipTypeData> candidates, FactionAPI faction,
        double targetCargo, double targetFuel, double targetCrew, double targetCombat
    ) {
        final double totalShipment = targetCargo + targetFuel + targetCrew;
        final double cargoWeight = targetCargo / totalShipment;
        final double fuelWeight = targetFuel / totalShipment;
        final double crewWeight = targetCrew / totalShipment;

        final double[] objective = new double[N];
        for (int i = 0; i < N; i++) {
            final ShipTypeData data = candidates.get(i);
            final ShipHullSpecAPI spec = data.spec;

            final double effCapacity = cargoWeight * spec.getCargo()
                + fuelWeight * spec.getFuel()
                + crewWeight * data.getCrewCapacityPerShip();

            final double baseCost = 1.0 / (1.0 + effCapacity * data.getCombatPower());
            final double randFactor = 0.7 + Math.random() * 0.6;
            final double doctrineFactor = getDoctrinePreference(faction, data);

            objective[i] = baseCost * randFactor * doctrineFactor;
        }

        return objective;
    }

    private static final double[] simplexSolveWithInv(final double[] objective, final List<LinearConstraint> constraints) {
        final LinearObjectiveFunction objFunc = new LinearObjectiveFunction(objective, 0);
        final SimplexSolver solver = new SimplexSolver(
            eps, 30, 1e-4
        );
        return solver.optimize(
            new MaxIter(1000), objFunc,
            new LinearConstraintSet(constraints),
            GoalType.MINIMIZE,
            new NonNegativeConstraint(true),
            PivotSelectionRule.DANTZIG
        ).getPoint();
    }
}
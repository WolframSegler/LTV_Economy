package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.EconomyConstants.MONTH;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import wfg.ltv_econ.config.EconomyConfig;

public class ShipTypeData implements Serializable {
    public static final String FRIGATES = "Frigates";
	public static final String DESTROYERS = "Destroyers";
	public static final String CRUISERS = "Cruisers";
	public static final String CAPITALS = "Capitals";
	public static final String COMBAT_SHIPS = "Warships";
	public static final String PHASE_SHIPS = "Phase ships";
	public static final String CARRIERS = "Carriers";
	public static final String CIVILIAN = "Civilian";
    
    public final String hullID;
    public transient ShipHullSpecAPI spec;

    private int idle, inUse;

    public ShipTypeData(String hullID) {
        this.hullID = hullID;

        readResolve();
    }

    private final Object readResolve() {
        spec = Global.getSettings().getHullSpec(hullID);

        return this;
    }

    public final int getOwned() {
        return idle + inUse;
    }

    public final int getIdle() {
        return idle;
    }

    public final int getInUse() {
        return inUse;
    }

    /** Negative values to remove */
    public final void addShip(int amount) {
        idle = Math.max(0, idle + amount);
    }

    public final void registerShipLoss(int amount) {
        inUse = Math.max(0, inUse - Math.max(0, amount));
    }

    public final void useShip(int amount) {
        final int used = Math.min(Math.max(0, amount), idle);
        inUse += used;
        idle -= used;
    }

    public final void freeShip(int amount) {
        final int freed = Math.min(Math.max(0, amount), inUse);
        idle += freed;
        inUse -= freed;
    }

    public final float getMonthlyMaintenanceCost() {
        return (idle * EconomyConfig.IDLE_SHIP_MAINTENANCE_MULT + inUse) * spec.getSuppliesPerMonth();
    }

    public final float getDailyMaintenanceCost() {
        return getMonthlyMaintenanceCost() / MONTH;
    }

    public final int getCrewPerShip() {
        return (int) spec.getMinCrew();
    }

    public final int getTotalCrew() {
        return (idle + inUse) * getCrewPerShip();
    }

    public final int getIdleCrew() {
        return idle * getCrewPerShip();
    }

    public final int getCrewCapacityPerShip() {
        return (int) Math.max(0f, spec.getMaxCrew() - getCrewPerShip());
    }

    public final float getMonthlyCrewWages() {
        return (idle * EconomyConfig.IDLE_CREW_WAGE_MULT + inUse) * getCrewPerShip() * EconomyConfig.CREW_WAGE_PER_MONTH;
    }

    public final float getCombatPower() {
        final float mult = getCombatMult(spec.getDesignation())
            + spec.getFighterBays() * 0.04f;
        return spec.getFleetPoints() * mult;
    }

    public static final int getCrewPerShip(ShipHullSpecAPI spec) {
        return (int) spec.getMinCrew();
    }

    public static final int getCrewCapacityPerShip(ShipHullSpecAPI spec) {
        return (int) spec.getMaxCrew() - getCrewPerShip(spec);
    }

    private static final float getCombatMult(String designation) {
        if (designation == null) return 0f;

        return switch (designation) {
            case CIVILIAN -> 0.08f;
            case COMBAT_SHIPS -> 1f;
            case FRIGATES -> 0.7f;
            case DESTROYERS -> 1.2f;
            case CRUISERS -> 1f;
            case CAPITALS -> 1.1f;
            case PHASE_SHIPS -> 0.5f;
            case CARRIERS -> 0.3f;
            default -> 0.5f;
        };
    }
}
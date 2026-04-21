package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.EconomyConstants.MONTH;
import static wfg.native_ui.util.Globals.settings;

import java.io.Serializable;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import wfg.ltv_econ.config.EconConfig;

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

    private transient float combatPower = 0f;

    public ShipTypeData(String hullID) {
        this.hullID = hullID;

        readResolve();
    }

    private final Object readResolve() {
        spec = settings.getHullSpec(hullID);

        combatPower = getCombatPower(spec);

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
        return (idle * EconConfig.IDLE_SHIP_MAINTENANCE_MULT + inUse) * spec.getSuppliesPerMonth();
    }

    public final float getDailyMaintenanceCost() {
        return getMonthlyMaintenanceCost() / MONTH;
    }

    public final int getCrewPerShip() {
        return getCrewPerShip(spec);
    }

    public final int getTotalCrew() {
        return getOwned() * getCrewPerShip();
    }

    public final int getIdleCrew() {
        return idle * getCrewPerShip();
    }

    public final int getCrewCapacityPerShip() {
        return getCrewCapacityPerShip(spec);
    }

    public final float getMonthlyCrewWages() {
        return (idle * EconConfig.IDLE_CREW_WAGE_MULT + inUse) * getCrewPerShip() * EconConfig.CREW_WAGE_PER_MONTH;
    }

    public final float getCombatPower() {
        return combatPower;
    }

    public final float getTotalCombatPower() {
        return getCombatPower() * getOwned();
    }

    public static final int getCrewPerShip(ShipHullSpecAPI spec) {
        return (int) spec.getMinCrew();
    }

    public static final int getCrewCapacityPerShip(ShipHullSpecAPI spec) {
        return (int) spec.getMaxCrew() - getCrewPerShip(spec);
    }

    public static final float getCombatPower(ShipHullSpecAPI spec) {
        final float mult = getCombatMult(spec.getDesignation())
            + spec.getFighterBays() * 0.04f
            + spec.getArmorRating() / 500f
            + spec.getFluxCapacity() / 6000f
            + spec.getFluxDissipation() / 750f
            + (1f - spec.getShieldSpec().getFluxPerDamageAbsorbed()) * 2f
            + (spec.getEngineSpec().getMaxSpeed() - 70) / 70f;
        return spec.getFleetPoints() * mult;
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
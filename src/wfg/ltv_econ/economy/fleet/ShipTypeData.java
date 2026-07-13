package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constant.EconomyConstants.MONTH;
import static wfg.native_ui.util.Globals.settings;

import java.io.Serializable;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;

import wfg.ltv_econ.config.EconConfig;
import wfg.native_ui.util.Arithmetic;

public class ShipTypeData implements Serializable {
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
        final int used = Arithmetic.clamp(amount, 0, idle);
        inUse += used;
        idle -= used;
    }

    public final void freeShip(int amount) {
        final int freed = Arithmetic.clamp(amount, 0, inUse);
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
        return (int) Math.max(0f, spec.getMaxCrew() - getCrewPerShip(spec));
    }

    public static final float getCombatPower(ShipHullSpecAPI spec) {
        final float stats = 1f
            + spec.getFighterBays() * 0.04f
            + spec.getArmorRating() / 750f
            + spec.getFluxCapacity() / 7500f
            + spec.getFluxDissipation() / 750f
            + (1f - spec.getShieldSpec().getFluxPerDamageAbsorbed()) * 3f
            + (spec.getEngineSpec().getMaxSpeed() - 80) / 30f;
            
        final float mult = getCombatMult(spec) * stats;
        return spec.getFleetPoints() * mult;
    }

    private static final float getCombatMult(ShipHullSpecAPI spec) {
        if (spec.getShieldSpec().getType() == ShieldType.PHASE) {
            return 0.7f;
        }

        if (spec.getHints().contains(ShipTypeHints.CIVILIAN)) {
            return 0.05f;
        }

        if (spec.getHints().contains(ShipTypeHints.CARRIER)) {
            return 0.5f;
        }

        return 1f;
    }
}
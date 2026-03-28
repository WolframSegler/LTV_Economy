package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.EconomyConstants.MONTH;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;

public class ShipTypeData implements Serializable {
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

    public final float getMonthlyCrewWages() {
        return (idle * EconomyConfig.IDLE_CREW_WAGE_MULT + inUse) * getCrewPerShip() * EconomyConfig.CREW_WAGE_PER_MONTH;
    }
}
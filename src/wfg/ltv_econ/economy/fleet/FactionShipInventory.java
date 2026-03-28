package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import wfg.native_ui.util.ArrayMap;

public class FactionShipInventory implements Serializable {
    public final String factionID;
    private final ArrayMap<String, ShipTypeData> ships = new ArrayMap<>();

    public FactionShipInventory(String factionID) {
        this.factionID = factionID;

        readResolve();
    }

    private Object readResolve() {
        ships.entrySet().removeIf(entry -> entry.getValue().spec == null); // Remove invalids

        return this;
    }

    public final ShipTypeData get(final String hullId) {
        return ships.computeIfAbsent(hullId, k -> new ShipTypeData(hullId));
    }

    public final void addShips(final String hullId, int count) {
        get(hullId).addShip(count);
    }

    public final void removeShips(String hullId, int count) {
        get(hullId).addShip(-count);
    }

    public final void useShips(String hullId, int count) {
        get(hullId).useShip(count);
    }

    public final void freeShips(String hullId, int count) {
        get(hullId).freeShip(count);
    }

    public final void registerShipLoss(String hullId, int count) {
        get(hullId).registerShipLoss(count);
    }

    public final float getTotalCargoCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getOwned() * data.spec.getCargo();
        }
        return total;
    }

    public final float getIdleCargoCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdle() * data.spec.getCargo();
        }
        return total;
    }

    public final float getTotalFleetPoints() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getOwned() * data.spec.getFleetPoints();
        }
        return total;
    }

    public final float getIdleFleetPoints() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdle() * data.spec.getFleetPoints();
        }
        return total;
    }

    public final float getTotalMonthlyMaintenance() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getMonthlyMaintenanceCost();
        }
        return total;
    }

    public final float getTotalMonthlyCrewWage() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getMonthlyCrewWages();
        }
        return total;
    }

    public final int getTotalCrew() {
        int total = 0;
        for (ShipTypeData data : ships.values()) {
            total += data.getTotalCrew();
        }
        return total;
    }

    public final int getIdleCrew() {
        int total = 0;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdleCrew();
        }
        return total;
    }

    public final Map<String, ShipTypeData> getShips() {
        return Collections.unmodifiableMap(ships);
    }
}
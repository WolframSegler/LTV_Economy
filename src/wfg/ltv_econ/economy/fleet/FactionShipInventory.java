package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.strings.Income.FACTION_CREW_WAGES_KEY;
import static wfg.ltv_econ.constants.strings.Income.getDesc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.native_ui.util.ArrayMap;

public class FactionShipInventory implements Serializable {
    // TODO make this a config entry
    private static final int MAX_SIMULTANEOUS_SHIP_PROD = 5;
    private static final String DAILY_SUPPLIES_DEMAND_KEY = "dsdk";
    private static final String DAILY_SUPPLIES_DEMAND_DESC = "Maintenance of faction ships";

    private final ArrayMap<String, ShipTypeData> ships = new ArrayMap<>();
    final List<ShipProductionOrder> activeQueue = new ArrayList<>();
    final List<PlannedOrder> plannedOrders = new ArrayList<>();

    public final String factionID;
    String capitalID;
    transient EconomyAPI econ;
    transient EconomyEngine engine;

    public FactionShipInventory(String factionID) {
        this.factionID = factionID;

        readResolve();
    }

    private Object readResolve() {
        ships.entrySet().removeIf(entry -> entry.getValue().spec == null); // Remove invalids

        econ = Global.getSector().getEconomy();
        engine = EconomyEngine.instance();

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

    public final float getTotalFuelCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getOwned() * data.spec.getFuel();
        }
        return total;
    }

    public final float getIdleFuelCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdle() * data.spec.getFuel();
        }
        return total;
    }

    public final float getTotalCrewCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getOwned() * data.getCrewCapacityPerShip();
        }
        return total;
    }

    public final float getIdleCrewCapacity() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdle() * data.getCrewCapacityPerShip();
        }
        return total;
    }

    public final float getTotalCombatPower() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getOwned() * data.getCombatPower();
        }
        return total;
    }

    public final float getIdleCombatPower() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getIdle() * data.getCombatPower();
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

    public final float getTotalDailyMaintenance() {
        float total = 0f;
        for (ShipTypeData data : ships.values()) {
            total += data.getDailyMaintenanceCost();
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

    public final List<ShipProductionOrder> getActiveProductionQueue() {
        return Collections.unmodifiableList(activeQueue);
    }

    public final List<PlannedOrder> getPlannedOrders() {
        return Collections.unmodifiableList(plannedOrders);
    }

    public final MarketAPI getCapital() {
        final MarketAPI capital;
        if (capitalID == null || econ.getMarket(capitalID) == null) {
            capital = computeCapital();
            setCapital(capital.getId());
        } else {
            capital = econ.getMarket(capitalID);
        }

        return capital;
    }

    public final void addToActiveQueue(String hullId, int constructionDays) {
        activeQueue.add(new ShipProductionOrder(hullId, constructionDays));
    }

    public final ShipProductionOrder cancelActiveOrder(int index) {
        if (index < 0 || index >= activeQueue.size()) return null;
        return activeQueue.remove(index);
    }

    public final void swapActiveOrders(int idxA, int idxB) {
        Collections.swap(activeQueue, idxA, idxB);
    }

    public final void addPlannedOrder(PlannedOrder order) {
        plannedOrders.add(order);
    }

    public final PlannedOrder removePlannedOrder(int index) {
        if (index < 0 || index >= plannedOrders.size()) return null;
        return plannedOrders.remove(index);
    }

    public final void swapPlannedOrders(int idxA, int idxB) {
        Collections.swap(plannedOrders, idxA, idxB);
    }

    public final void update() {
        final String capitalID = getCapital().getId();
        final CommodityCell suppliesCell = engine.getComCell(Commodities.SUPPLIES, capitalID);

        suppliesCell.getConsumptionStat().modifyFlat(DAILY_SUPPLIES_DEMAND_KEY, getTotalDailyMaintenance(), DAILY_SUPPLIES_DEMAND_DESC);
        suppliesCell.getTargetQuantumStat().modifyFlat(DAILY_SUPPLIES_DEMAND_KEY, getTotalDailyMaintenance(), DAILY_SUPPLIES_DEMAND_DESC);
    }

    public final void advance() {
        advanceProduction(1);

        ShipProductionManager.tryStartPlannedOrders(this, capitalID);
    }

    public final void endMonth() {
        MarketFinanceRegistry.instance().getLedger(getCapital()).add(
            FACTION_CREW_WAGES_KEY, -getTotalMonthlyCrewWage(), getDesc(FACTION_CREW_WAGES_KEY)
        );
    }

    public final void advanceProduction(int days) {
        if (activeQueue.isEmpty()) return;
        final int activeCount = Math.min(MAX_SIMULTANEOUS_SHIP_PROD, activeQueue.size());
        final List<Integer> completedIndices = new ArrayList<>(MAX_SIMULTANEOUS_SHIP_PROD);

        for (int i = 0; i < activeCount; i++) {
            final ShipProductionOrder order = activeQueue.get(i);
            order.daysRemaining -= days;
            if (order.daysRemaining <= 0) completedIndices.add(i);
        }

        for (int i = completedIndices.size() - 1; i >= 0; i--) {
            final int idx = completedIndices.get(i);
            final ShipProductionOrder completed = activeQueue.remove(idx);
            addShips(completed.hullId, 1);
        }
    }

    public final void setCapital(String marketID) {
        final MarketAPI oldCapital = econ.getMarket(capitalID);

        if (oldCapital != null) {
            final CommodityCell suppliesCell = engine.getComCell(Commodities.SUPPLIES, capitalID);
            suppliesCell.getConsumptionStat().unmodifyFlat(DAILY_SUPPLIES_DEMAND_KEY);
            suppliesCell.getTargetQuantumStat().unmodifyFlat(DAILY_SUPPLIES_DEMAND_KEY);
        }

        capitalID = marketID;
    } 

    private final MarketAPI computeCapital() {
        MarketAPI best = null;
        for (MarketAPI market : econ.getMarketsCopy()) {
            if (!market.getFactionId().equals(factionID)) continue;

            if (best == null) {
                best = market;

            } else if (market.getSize() > best.getSize()) {
                best = market;

            } else if (market.getShipQualityFactor() > best.getShipQualityFactor()) {
                best = market;

            } else if (market.getHazardValue() < best.getHazardValue()) {
                best = market;

            } else if (market.getDaysInExistence() > best.getDaysInExistence()) {
                best = market;

            }
        }

        return best;
    }
}
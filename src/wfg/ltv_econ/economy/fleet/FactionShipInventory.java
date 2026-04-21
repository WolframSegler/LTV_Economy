package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.strings.Income.FACTION_CREW_WAGES_KEY;
import static wfg.ltv_econ.constants.strings.Income.getDesc;
import static wfg.ltv_econ.constants.strings.Consumption.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.ArrayMap;

public class FactionShipInventory implements Serializable {
    private final ArrayMap<String, ShipTypeData> ships = new ArrayMap<>(4);
    final List<ShipProductionOrder> activeQueue = new ArrayList<>();
    final List<PlannedOrder> plannedOrders = new ArrayList<>();

    private final Set<String> lastDemandCommodities = new HashSet<>();

    public final String factionID;
    private String capitalID;
    private int assemblyLines;

    private transient EconomyAPI econ;

    public FactionShipInventory(String factionID) {
        this.factionID = factionID;

        if (factionID.equals(Factions.PLAYER)) {
            assemblyLines = EconConfig.PLAYER_FACTION_ASSEMBLY_LINES;
        } else {
            assemblyLines = EconConfig.NPC_FACTION_ASSEMBLY_LINES;
        }

        readResolve();
    }

    private Object readResolve() {
        ships.entrySet().removeIf(entry -> entry.getValue().spec == null); // Remove invalids

        econ = Global.getSector().getEconomy();

        return this;
    }

    public final ShipTypeData get(final String hullId) {
        return ships.computeIfAbsent(hullId, k -> new ShipTypeData(hullId));
    }

    public final void addShip(final String hullId, int count) {
        get(hullId).addShip(count);
    }

    public final void removeShip(String hullId, int count) {
        get(hullId).addShip(-count);
    }

    public final void useShip(String hullId, int count) {
        get(hullId).useShip(count);
    }

    public final void freeShip(String hullId, int count) {
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

    public final void clearActiveOrders() {
        activeQueue.clear();
    }

    public final void clearPlannedOrders() {
        plannedOrders.clear();
    }

    public final int getAssemblyLines() {
        return assemblyLines;
    }

    public final MarketAPI getCapital() {
        final MarketAPI capital;
        if (capitalID == null || econ.getMarket(capitalID) == null) {
            capital = computeCapital();
            if (capital != null) setCapital(capital.getId());
        } else {
            capital = econ.getMarket(capitalID);
        }

        return capital;
    }

    public final void addToActiveQueue(String hullId, int constructionDays) {
        activeQueue.add(new ShipProductionOrder(hullId, constructionDays));
    }

    public final ShipProductionOrder removeActiveOrder(int index) {
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
        final MarketAPI capital = getCapital();
        if (capital == null) return;

        final EconomyEngine engine = EconomyEngine.instance();
        engine.getComCell(Commodities.SUPPLIES, capital.getId()).getConsumptionStat().modifyFlat(
            FACTION_FLEET_MAINTENANCE_KEY, getTotalDailyMaintenance(), FACTION_FLEET_MAINTENANCE_DESC
        );
    
        final ArrayMap<String, Float> demands = new ArrayMap<>(4);
        for (PlannedOrder order : plannedOrders) {
            for (var e : order.commodities.singleEntrySet()) {
                demands.merge(e.getKey(), e.getValue(), Float::sum);
            }
        }

        for (String comID : lastDemandCommodities) {
            final CommodityCell cell = engine.getComCell(comID, capital.getId());
            cell.getTargetQuantumStat().unmodifyFlat(ORDERS_DEMAND_KEY);
        }
        lastDemandCommodities.clear();

        for (var e : demands.singleEntrySet()) {
            final String comID = e.getKey();
            final float value = e.getValue() / EconConfig.DAYS_TO_COVER;
            lastDemandCommodities.add(comID);
            engine.getComCell(comID, capital.getId()).getTargetQuantumStat().modifyFlat(
                ORDERS_DEMAND_KEY, value, ORDERS_DEMAND_DESC
            );
        }
    }

    public final void advance() {
        advanceProduction(1);

        if (factionID.equals(Factions.PLAYER)) {
            final PlayerFactionSettings factionSettings = LtvEconSaveData.instance().playerFactionSettings;
            if (factionSettings.automaticShipProductionForFaction) {
                ShipProductionManager.planOrders(this);
            }
        } else {
            ShipProductionManager.planOrders(this);
        }

        ShipProductionManager.tryStartPlannedOrders(this, capitalID);
    }

    public final void endMonth() {
        final MarketAPI capital = getCapital();
        if (capital == null) return;

        MarketFinanceRegistry.instance().getLedger(capital).add(
            FACTION_CREW_WAGES_KEY, -getTotalMonthlyCrewWage(), getDesc(FACTION_CREW_WAGES_KEY)
        );
    }

    public final void advanceProduction(int days) {
        if (activeQueue.isEmpty()) return;
        final int activeCount = Math.min(assemblyLines, activeQueue.size());
        final List<Integer> completedIndices = new ArrayList<>(assemblyLines);

        for (int i = 0; i < activeCount; i++) {
            final ShipProductionOrder order = activeQueue.get(i);
            order.daysRemaining -= days;
            if (order.daysRemaining <= 0) completedIndices.add(i);
        }

        for (int i = completedIndices.size() - 1; i >= 0; i--) {
            final int idx = completedIndices.get(i);
            final ShipProductionOrder completed = activeQueue.remove(idx);
            addShip(completed.hullId, 1);
        }
    }

    public final void setCapital(String marketID) {
        final MarketAPI oldCapital = econ.getMarket(capitalID);

        if (oldCapital != null) {
            final CommodityCell suppliesCell = EconomyEngine.instance().getComCell(Commodities.SUPPLIES, capitalID);
            suppliesCell.getConsumptionStat().unmodifyFlat(FACTION_FLEET_MAINTENANCE_KEY);
            suppliesCell.getTargetQuantumStat().unmodifyFlat(FACTION_FLEET_MAINTENANCE_KEY);
        }

        capitalID = marketID;
    } 

    private MarketAPI computeCapital() {
        MarketAPI best = null;
        for (MarketAPI market : econ.getMarketsCopy()) {
            if (!market.getFactionId().equals(factionID)) continue;

            if (best == null) {
                best = market;
                continue;
            }

            if (market.getSize() > best.getSize()) {
                best = market;
            } else if (market.getSize() < best.getSize()) {
                continue;
            }

            if (market.getShipQualityFactor() > best.getShipQualityFactor()) {
                best = market;
            } else if (market.getShipQualityFactor() < best.getShipQualityFactor()) {
                continue;
            }

            if (market.getHazardValue() < best.getHazardValue()) {
                best = market;
            } else if (market.getHazardValue() > best.getHazardValue()) {
                continue;
            }

            if (market.getDaysInExistence() > best.getDaysInExistence()) {
                best = market;
            }
        }
        return best;
    }
}
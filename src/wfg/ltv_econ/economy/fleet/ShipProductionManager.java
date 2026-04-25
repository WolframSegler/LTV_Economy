package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.strings.Income.FACTION_SHIP_PRODUCTION_KEY;
import static wfg.ltv_econ.constants.strings.Income.getDesc;
import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.native_ui.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class ShipProductionManager {
    private ShipProductionManager() {}
    private static final float MAX_TARGET_FOR_PLANNED_ORDERS = 100_000f;

    private static final CommoditySpecAPI shipSpec = settings.getCommoditySpec(Commodities.SHIPS);
    private static final CommoditySpecAPI metalsSpec = settings.getCommoditySpec(Commodities.METALS);
    private static final CommoditySpecAPI rareMetalsSpec = settings.getCommoditySpec(Commodities.RARE_METALS);

    public static final void injectShipGameStart(FactionShipInventory inv) {
        final String factionID = inv.factionID;
        final FactionAPI faction = Global.getSector().getFaction(factionID);

        long pop = 0l;
        long workers = 0l;
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (!market.getFaction().equals(faction)) continue;

            pop += Math.pow(10, market.getSize());
            workers += WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
        }

        final long shipmentTarget = workers / 25l;
        final long combatTarget = pop / 90l;

        final ArrayMap<String, Integer> buildList = new ArrayMap<>(32);
        ShipAllocator.allocateShipsForTarget(shipmentTarget, shipmentTarget, shipmentTarget, combatTarget, faction, buildList);

        for (Map.Entry<String, Integer> entry : buildList.singleEntrySet()) {
            inv.addShip(entry.getKey(), entry.getValue());
        }
    }

    public static final void planOrders(FactionShipInventory inv) {
        final String factionID = inv.factionID;
        final FactionAPI faction = Global.getSector().getFaction(factionID);
        final List<PlannedOrder> plannedOrders = inv.plannedOrders;
        final List<ShipProductionOrder> activeQueue = inv.activeQueue;
        final List<TradeMission> missions = new ArrayList<>(EconomyEngine.instance().getActiveMissions());
        missions.removeIf(m -> !m.src.getFactionId().equals(factionID));

        float deficitCargo = computeDesiredCargo(missions, faction) - inv.getTotalCargoCapacity();
        float deficitFuel = computeDesiredFuel(missions, faction) - inv.getTotalFuelCapacity();
        float deficitCrew = computeDesiredCrew(missions, faction) - inv.getTotalCrewCapacity();
        float deficitCombat = computeDesiredCombat(missions, faction) - inv.getTotalCombatPower();
        deficitCargo = Math.min(deficitCargo, MAX_TARGET_FOR_PLANNED_ORDERS);
        deficitFuel = Math.min(deficitFuel, MAX_TARGET_FOR_PLANNED_ORDERS);
        deficitCrew = Math.min(deficitCrew, MAX_TARGET_FOR_PLANNED_ORDERS);
        deficitCombat = Math.min(deficitCombat, MAX_TARGET_FOR_PLANNED_ORDERS);

        for (ShipProductionOrder order : activeQueue) {
            final ShipTypeData data = inv.get(order.hullId);
            deficitCargo -= data.spec.getCargo();
            deficitFuel -= data.spec.getFuel();
            deficitCrew -= data.getCrewCapacityPerShip();
            deficitCombat -= data.getCombatPower();
        }

        for (PlannedOrder order : plannedOrders) {
            final ShipTypeData data = inv.get(order.hullId);
            deficitCargo -= data.spec.getCargo();
            deficitFuel -= data.spec.getFuel();
            deficitCrew -= data.getCrewCapacityPerShip();
            deficitCombat -= data.getCombatPower();
        }

        if (deficitCargo <= 0f && deficitFuel <= 0f && deficitCrew <= 0f && deficitCombat <= 0f) return;

        deficitCargo = Math.max(0f, deficitCargo);
        deficitFuel = Math.max(0f, deficitFuel);
        deficitCrew = Math.max(0f, deficitCrew);
        deficitCombat = Math.max(0f, deficitCombat);

        final ArrayMap<String, Integer> buildList = new ArrayMap<>(12);
        ShipAllocator.allocateShipsForTarget(deficitCargo, deficitFuel, deficitCrew, deficitCombat, faction, buildList);

        for (Map.Entry<String, Integer> entry : buildList.singleEntrySet()) {
            final String hullID = entry.getKey();

            final PlannedOrder cost = getProductionCost(settings.getHullSpec(hullID));
            for (int i = 0; i < entry.getValue(); i++) {
                inv.addPlannedOrder(new PlannedOrder(hullID, cost.credits, cost.commodities, cost.days));
            }
        }
    }

    public static final void tryStartPlannedOrders(FactionShipInventory inv, String capitalID) {
        final MarketFinanceRegistry register = MarketFinanceRegistry.instance();
        final EconomyEngine engine = EconomyEngine.instance();
        final Iterator<PlannedOrder> it = inv.plannedOrders.iterator();

        while (it.hasNext()) {
            final PlannedOrder order = it.next();

            boolean canAfford = true;
            for (Map.Entry<String, Float> entry : order.commodities.singleEntrySet()) {
                final CommodityCell cell = engine.getComCell(entry.getKey(), capitalID);
                if (cell == null || cell.getStored() < entry.getValue()) {
                    canAfford = false;
                    break;
                }
            }
            if (!canAfford) continue;

            register.getLedger(capitalID).add(FACTION_SHIP_PRODUCTION_KEY, -order.credits, getDesc(FACTION_SHIP_PRODUCTION_KEY));
            for (Map.Entry<String, Float> entry : order.commodities.singleEntrySet()) {
                final CommodityCell cell = engine.getComCell(entry.getKey(), capitalID);
                cell.addStoredAmount(-entry.getValue());
            }

            inv.addToActiveQueue(order.hullId, order.days);
            it.remove();
        }
    }

    public static final PlannedOrder getProductionCost(ShipHullSpecAPI spec) {
        final ArrayMap<String, Float> commodities = new ArrayMap<>(1);
        final float rocc = LaborConfig.getRoCC("industry");
        final float rovc = LaborConfig.getRoVC("industry");

        final int days = getBaseDays(spec);
        final long credits = (long) (spec.getBaseValue() * rovc);
        final float shipsCost = (spec.getBaseValue() / shipSpec.getBasePrice()) * rocc;
        final float crewCost = ShipTypeData.getCrewPerShip(spec);
        
        commodities.put(Commodities.SHIPS, shipsCost);
        commodities.put(Commodities.CREW, crewCost);

        return new PlannedOrder(spec.getHullId(), credits, commodities, days);
    }

    public static final void addScrapsToCapital(FactionShipInventory inv, int amount, ShipHullSpecAPI spec) {
        addScrapsToCapital(inv, spec.getBaseValue() * amount);
    }

    public static final void addScrapsToCapital(FactionShipInventory inv, float creditValue) {
        final MarketAPI capital = inv.getCapital();
        if (capital == null) return;
        
        final ArrayMap<String, Float> scraps = ShipProductionManager.getScrapAmounts(creditValue);
        final EconomyEngine engine = EconomyEngine.instance();
        for (var e : scraps.singleEntrySet()) {
            engine.getComCell(e.getKey(), capital.getId()).addStoredAmount(e.getValue());
        }
    }

    private static final int getBaseDays(ShipHullSpecAPI spec) {
        if (spec.getHullSize() == HullSize.FIGHTER) return 1;
        return Arithmetic.clamp(spec.getFleetPoints() * 2, 1, 180);
    }

    private static final float computeDesiredCargo(List<TradeMission> missions, FactionAPI faction) {
        float cargo = 0f;

        for (TradeMission mission : missions) {
            cargo += mission.cargoAmount;
        }

        return cargo * EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER;
    }

    private static final float computeDesiredFuel(List<TradeMission> missions, FactionAPI faction) {
        float fuel = 0f;

        for (TradeMission mission : missions) {
            fuel += mission.fuelAmount;
            fuel += mission.fuelCost;
        }

        return fuel * EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER;
    }

    private static final float computeDesiredCrew(List<TradeMission> missions, FactionAPI faction) {
        float crew = 0f;

        for (TradeMission mission : missions) {
            crew += mission.crewAmount;
        }

        return crew * EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER;
    }

    private static final float computeDesiredCombat(List<TradeMission> missions, FactionAPI faction) {
        float tradeCombat = 0f;
        for (TradeMission mission : missions) {
            tradeCombat += mission.combatPowerTarget;
        }
        tradeCombat *= EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER;

        float colonyCombat = 0f;
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (!market.getFaction().equals(faction)) continue;

            colonyCombat += (float) Math.pow(market.getSize() - 2, EconConfig.SHIP_ALLOC_MARKET_SIZE_EXPONENT)
                * EconConfig.SHIP_ALLOC_MARKET_WEIGHT_PER_SIZE;
        }

        final float aggressionFactor = 1f + (faction.getDoctrine().getAggression() / 5f)
            * EconConfig.SHIP_ALLOC_AGGRESSION_COMBAT_MULT;

        final float threatFactor = 1f + (1f - computeAvgRel(faction)) * EconConfig.SHIP_ALLOC_THREAT_RELATIONSHIP_MULT;

        final float desiredCombat = (colonyCombat * aggressionFactor * threatFactor) + tradeCombat;
        return Math.max(desiredCombat, EconConfig.SHIP_ALLOC_MIN_COMBAT_POWER);
    }

    private static final float computeAvgRel(FactionAPI faction) {
        float total = 0f;
        int count = 0;
        for (FactionAPI other : Global.getSector().getAllFactions()) {
            if (other == faction) continue;
            total += faction.getRelationship(other.getId()); // [-1,1]
            count++;
        }
        if (count == 0) return 0.5f;

        // [-1,1] -> [0,1]
        return (total / count + 1f) / 2f;
    }

    private static final ArrayMap<String, Float> getScrapAmounts(float baseValue) {
        final ArrayMap<String, Float> map = new ArrayMap<>(2);

        final float materialValuePerShip = baseValue * LaborConfig.getRoCC("industry");
        final float scrapValue = materialValuePerShip * EconConfig.SCRAP_REFUND_FRACTION;

        map.put(Commodities.METALS, (scrapValue * 0.5f) / metalsSpec.getBasePrice());
        map.put(Commodities.RARE_METALS, (scrapValue * 0.5f) / rareMetalsSpec.getBasePrice());

        return map;
    }
}
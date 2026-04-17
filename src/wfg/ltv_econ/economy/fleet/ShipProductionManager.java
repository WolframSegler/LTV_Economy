package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.strings.Income.FACTION_SHIP_PRODUCTION_KEY;
import static wfg.ltv_econ.constants.strings.Income.getDesc;
import static wfg.native_ui.util.Globals.settings;

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

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class ShipProductionManager {
    private ShipProductionManager() {}
    
    // TODO move these to economy config
    private static final float CARGO_SAFETY_MULT = 1.5f;
    private static final float FUEL_SAFETY_MULT = 1.2f;
    private static final float CREW_SAFETY_MULT = 1.2f;

    private static final float COLONY_WEIGHT_PER_SIZE = 100f;
    private static final float COLONY_SIZE_EXPONENT = 1.5f;
    private static final float AGGRESSION_COMBAT_MULT = 0.2f;
    private static final float THREAT_RELATIONSHIP_MULT = 2f;
    private static final float TRADE_COMBAT_SAFETY_MULT = 1.2f;
    private static final float MIN_COMBAT_POWER = 100f;

    private static final float SHIP_PROD_CREDIT_COST_MULT = 0.35f;
    private static final float SHIP_PROD_SHIPS_COST_MULT = 1.4f;
    private static final CommoditySpecAPI shipSpec = settings.getCommoditySpec(Commodities.SHIPS);

    public static final void planOrders(FactionShipInventory inv) {
        final FactionAPI faction = Global.getSector().getFaction(inv.factionID);
        final List<PlannedOrder> plannedOrders = inv.plannedOrders;
        final List<ShipProductionOrder> activeQueue = inv.activeQueue;
        final List<TradeMission> missions = EconomyEngine.instance().getActiveMissions();

        float deficitCargo = computeDesiredCargo(missions, faction) - inv.getTotalCargoCapacity();
        float deficitFuel = computeDesiredFuel(missions, faction) - inv.getTotalFuelCapacity();
        float deficitCrew = computeDesiredCrew(missions, faction) - inv.getTotalCrewCapacity();
        float deficitCombat = computeDesiredCombat(missions, faction) - inv.getTotalCombatPower();

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

        final ArrayMap<String, Integer> buildList = new ArrayMap<>(8);
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
        final int days = getBaseDays(spec);
        final long credits = (long) (spec.getBaseValue() * SHIP_PROD_CREDIT_COST_MULT);
        final float shipsCost = (spec.getBaseValue() / shipSpec.getBasePrice()) * SHIP_PROD_SHIPS_COST_MULT;
        
        commodities.put(Commodities.SHIPS, shipsCost);

        return new PlannedOrder(spec.getHullId(), credits, commodities, days);
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

        return cargo * CARGO_SAFETY_MULT;
    }

    private static final float computeDesiredFuel(List<TradeMission> missions, FactionAPI faction) {
        float fuel = 0f;

        for (TradeMission mission : missions) {
            fuel += mission.fuelAmount;
            fuel += mission.fuelCost;
        }

        return fuel * FUEL_SAFETY_MULT;
    }

    private static final float computeDesiredCrew(List<TradeMission> missions, FactionAPI faction) {
        float crew = 0f;

        for (TradeMission mission : missions) {
            crew += mission.crewAmount;
        }

        return crew * CREW_SAFETY_MULT;
    }

    private static final float computeDesiredCombat(List<TradeMission> missions, FactionAPI faction) {
        float tradeCombat = 0f;
        for (TradeMission mission : missions) {
            tradeCombat += mission.combatPowerTarget;
        }
        tradeCombat *= TRADE_COMBAT_SAFETY_MULT;

        float colonyCombat = 0f;
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            if (!market.getFaction().equals(faction)) continue;

            colonyCombat += (float) Math.pow(market.getSize() - 2, COLONY_SIZE_EXPONENT) * COLONY_WEIGHT_PER_SIZE;
        }

        final float aggressionFactor = 1f + (faction.getDoctrine().getAggression() / 5f) * AGGRESSION_COMBAT_MULT;

        final float threatFactor = 1f + (1f - computeAvgRel(faction)) * THREAT_RELATIONSHIP_MULT;

        final float desiredCombat = (colonyCombat * aggressionFactor * threatFactor) + tradeCombat;
        return Math.max(desiredCombat, MIN_COMBAT_POWER);
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
}
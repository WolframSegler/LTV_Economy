package wfg.ltv_econ.economy.fleet;

import static wfg.ltv_econ.constants.strings.Income.FACTION_SHIP_PRODUCTION_KEY;
import static wfg.ltv_econ.constants.strings.Income.getDesc;

import java.util.Iterator;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class ShipProductionManager {
    private ShipProductionManager() {}

    private static final float SHIP_PROD_COST_MULT = 0.35f;
    private static final CommoditySpecAPI shipSpec = Global.getSettings().getCommoditySpec(Commodities.SHIPS);

    public static final void tryStartPlannedOrders(FactionShipInventory inv, String capitalID) {
        final MarketFinanceRegistry register = MarketFinanceRegistry.instance();
        final EconomyEngine engine = inv.engine;
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
        final ArrayMap<String, Float> commodities = new ArrayMap<>();
        final int days = getBaseDays(spec);
        final long credits = (long) (spec.getBaseValue() * SHIP_PROD_COST_MULT);
        final float shipCost = spec.getBaseValue() / shipSpec.getBasePrice();
        
        commodities.put(Commodities.SHIPS, shipCost);

        return new PlannedOrder(spec.getHullId(), credits, commodities, days);
    }

    private static final int getBaseDays(ShipHullSpecAPI spec) {
        if (spec.getHullSize() == HullSize.FIGHTER) return 1;
        return Arithmetic.clamp(spec.getFleetPoints() * 2, 1, 180);
    }
}
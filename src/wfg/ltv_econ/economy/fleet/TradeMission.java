package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class TradeMission implements Serializable {
    public final ArrayMap<ShipTypeData, Integer> allocatedShips = new ArrayMap<>();
    public final List<ComTradeFlow> cargo = new ArrayList<>();
    public final StatBonus credits = new StatBonus();
    public final MarketAPI sourceMarket;
    public final MarketAPI destMarket;
    public final boolean inFaction;
    public final float dist;
    public final int totalTravelTime;
    
    public float cargoAmount = 0f;
    public float crewAmount = 0f;
    public float fuelAmount = 0f;
    public float combatPowerTarget = 0f; // value depends on the above three
    public float fuelCost = 0f; // operation cost in units
    public boolean usedFactionFleet = false;
    public boolean usedFuelFromStockpiles = false;

    public int departureDay = -1;
    public int travelTimeRemaining;
    public MissionStatus status = MissionStatus.SCHEDULED;

    public TradeMission(MarketAPI source, MarketAPI dest, boolean inFaction) {
        this.sourceMarket = source;
        this.destMarket = dest;
        this.inFaction = inFaction;

        dist = Arithmetic.dist(source.getLocationInHyperspace(), dest.getLocationInHyperspace());
        totalTravelTime = (int) Math.ceil(dist / EconomyConfig.TRAVEL_SPEED_LY_DAY);
        
        travelTimeRemaining = totalTravelTime;
    }

    public enum MissionStatus {
        SCHEDULED,
        IN_TRANSIT,
        DELIVERED,
        LOST
    }
}
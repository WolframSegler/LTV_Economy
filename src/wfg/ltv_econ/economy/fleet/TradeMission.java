package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.commodity.TradeCargo;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class TradeMission implements Serializable {
    public final ArrayMap<ShipTypeData, Integer> allocatedShips = new ArrayMap<>();
    public final List<TradeCargo> cargo = new ArrayList<>();
    public final MarketAPI sourceMarket;
    public final MarketAPI destMarket;
    public final boolean inFaction;
    public final float dist;
    public final int totalTravelTime;
    public final float distPerDay;

    public float totalAmount = 0f;
    public float fuelCost = 0f;
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
        distPerDay = dist / totalTravelTime;
        
        travelTimeRemaining = totalTravelTime;
    }

    public enum MissionStatus {
        SCHEDULED,
        IN_TRANSIT,
        DELIVERED,
        LOST
    }
}
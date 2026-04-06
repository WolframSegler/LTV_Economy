package wfg.ltv_econ.economy.fleet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;

import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;

public class TradeMission implements Serializable {
    public final ArrayMap<ShipTypeData, Integer> allocatedShips = new ArrayMap<>(8);
    public final List<ComTradeFlow> cargo = new ArrayList<>();
    public final StatBonus credits = new StatBonus();
    public final boolean inFaction;
    public final float dist;

    /** nullable */
    public final MarketAPI src;
    /** nullable */
    public final MarketAPI dest;

    public final float transferDur;
    public final float travelDur;
    public final int totalDur;
    
    public float cargoAmount = 0f;
    public float crewAmount = 0f;
    public float fuelAmount = 0f;
    public float combatPowerTarget = 0f; // value depends on the above three
    public float fuelCost = 0f; // operation cost in units
    public float spawnedFleetCargoCapRatio = 0f;
    public float spawnedFleetFuelCapRatio = 0f;
    public float spawnedFleetCrewCapRatio = 0f;

    public boolean usedFactionFleet = false;
    public boolean usedFuelFromStockpiles = false;
    public boolean spawnedFleetFinishedJob = true;
    public boolean smuggling = false;

    public int startOffset = -1;
    public int durRemaining;
    public MissionStatus status = MissionStatus.SCHEDULED;

    public TradeMission(MarketAPI source, MarketAPI dest, boolean inFaction) {
        this.src = source;
        this.dest = dest;
        this.inFaction = inFaction;

        final float meanSize = (float) Math.sqrt(source.getSize()*source.getSize() + dest.getSize()*dest.getSize());
        dist = Arithmetic.dist(source.getLocationInHyperspace(), dest.getLocationInHyperspace());
        travelDur = dist / EconomyConfig.TRAVEL_SPEED_LY_DAY;
        transferDur = meanSize * (0.75f + (float) Math.random() * 0.5f);
        totalDur = (int) Math.ceil(travelDur + transferDur * 2);
        
        durRemaining = totalDur;
    }

    public final float getTotalAmount() {
        return cargoAmount + fuelAmount + crewAmount;
    }

    public final void setSpawnedFleetCapRatios(final CargoAPI cargo) {
		spawnedFleetCargoCapRatio = Math.min(1f, cargo.getMaxCapacity() / cargoAmount);
        spawnedFleetFuelCapRatio = Math.min(1f, cargo.getMaxFuel() / fuelAmount);
		spawnedFleetCrewCapRatio = Math.min(1f, cargo.getFreeCrewSpace() / crewAmount);
    }

    /** Order matters for the ordinal */
    public enum MissionStatus {
        SCHEDULED,
        IN_SRC_ORBIT_LOADING,
        IN_TRANSIT,
        IN_DST_ORBIT_UNLOADING,
        DELIVERED,
        CANCELLED,
        LOST
    }
}
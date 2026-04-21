package wfg.ltv_econ.economy.fleet;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.constants.UIColors;
import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.native_ui.util.ArrayMap;

public class TradeMission implements Serializable {
    public final ArrayMap<String, Integer> allocatedShips = new ArrayMap<>(8);
    public final List<TradeCom> cargo = new ArrayList<>();
    public final StatBonus credits = new StatBonus();
    public final boolean inFaction;
    public final float dist;
    public final String srcID;
    public final String destID;

    public transient MarketAPI src;
    public transient MarketAPI dest;

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
        this.srcID = source.getId();
        this.destID = dest.getId();
        this.src = source;
        this.dest = dest;
        this.inFaction = inFaction;

        final float meanSize = (float) Math.sqrt(source.getSize()*source.getSize() + dest.getSize()*dest.getSize());
        dist = Misc.getDistanceLY(source.getLocationInHyperspace(), dest.getLocationInHyperspace());
        travelDur = dist / EconConfig.TRAVEL_SPEED_LY_DAY;
        transferDur = meanSize * (0.75f + (float) Math.random() * 0.5f);
        totalDur = (int) Math.ceil(travelDur + transferDur * 2);
        
        durRemaining = totalDur;
    }

    private final Object readResolve() {
        final EconomyAPI econ = Global.getSector().getEconomy();

        src = econ.getMarket(srcID);
        dest = econ.getMarket(destID);

        if (src == null || dest == null) {
            status = MissionStatus.CANCELLED;
        }

        return this;
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
        SCHEDULED("Scheduled"),
        IN_SRC_ORBIT_LOADING("Loading at Source"),
        IN_TRANSIT("In Transit"),
        IN_DST_ORBIT_UNLOADING("Unloading at Destination"),
        DELIVERED("Delivered"),
        CANCELLED("Cancelled"),
        LOST("Lost");
        
        private final String displayText;
        
        MissionStatus(String displayText) {
            this.displayText = displayText;
        }
        
        public String getDisplayText() {
            return displayText;
        }
        
        public Color getDisplayColor() {
            switch (this) {
                case SCHEDULED: return UIColors.STOCKPILES_TARGET;
                case IN_TRANSIT: return UIColors.CARGO_COLOR;
                case DELIVERED: return UIColors.COM_EXPORT;
                case CANCELLED: return UIColors.COM_IMPORT;
                case LOST: return UIColors.COM_DEFICIT;
                default: return UIColors.COM_FACTION_IMPORT;
            }
        }
        
        public final boolean isActive() {
            return this != DELIVERED && this != CANCELLED && this != LOST;
        }
        
        public final boolean isTerminal() {
            return this == DELIVERED || this == CANCELLED || this == LOST;
        }
    }
}
package wfg.ltv_econ.serializable;

import com.fs.starfarer.api.impl.campaign.ids.Factions;

import wfg.ltv_econ.economy.fleet.FactionShipInventory;

public class StaticData {
    private StaticData() {}
    
    public static FactionShipInventory inv;

    public static final void loadData(LtvEconSaveData data) {
        inv = data.economyEngine.getFactionShipInventory(Factions.PLAYER);
    }

    public static final void resetData(LtvEconSaveData data) {
        inv = null;
    }
}
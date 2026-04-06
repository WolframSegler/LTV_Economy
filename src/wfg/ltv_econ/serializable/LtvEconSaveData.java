package wfg.ltv_econ.serializable;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;

public class LtvEconSaveData implements Serializable {
    private static final String LtvEconSaveDataSerialID = "ltv_econ_save_data_id"; 
    private static LtvEconSaveData instance;

    // SERIALIZABLE DATA
    public PlayerFactionSettings playerFactionSettings;
    public WorkerRegistry workerRegistry;
    public MarketFinanceRegistry financeRegistry;
    public EconomyEngine economyEngine;

    private LtvEconSaveData() {
        instance = this;
        playerFactionSettings = new PlayerFactionSettings();
        workerRegistry = new WorkerRegistry();
        financeRegistry = new MarketFinanceRegistry();
        economyEngine = new EconomyEngine();
    }

    public static final LtvEconSaveData loadInstance(boolean forceRefresh,
        boolean newGame
    ) {
        final SectorAPI sector = Global.getSector();

        LtvEconSaveData data = (LtvEconSaveData) sector.getPersistentData().get(
            LtvEconSaveDataSerialID
        );

        if (data == null || forceRefresh) {
            data = new LtvEconSaveData();

            if (Global.getSettings().isDevMode()) {
                Global.getLogger(EconomyEngine.class).info("LtvEconomySaveData constructed");
            }
        }
        instance = data;

        // SETUP
        if (EconomyConfig.ASSIGN_WORKERS_ON_LOAD || newGame) {
            data.economyEngine.fakeAdvanceWithAssignWorkers();
        } else {
            data.economyEngine.fakeAdvance();
        }

        sector.addTransientScript(data.economyEngine);
        sector.getListenerManager().addListener(data.economyEngine, true);

        return data;
    }

    public static final void saveInstance() {
        Global.getSector().getPersistentData().put(LtvEconSaveDataSerialID, instance);
        instance = null;
    }

    public static final LtvEconSaveData instance() {
        if (instance == null) LtvEconSaveData.loadInstance(false, false);
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }
}
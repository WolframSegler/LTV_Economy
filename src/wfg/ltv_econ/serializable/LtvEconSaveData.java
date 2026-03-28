package wfg.ltv_econ.serializable;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.engine.EconomyEngine;

public class LtvEconSaveData implements Serializable {
    private static final String LtvEconSaveDataSerialID = "ltv_econ_save_data_id"; 
    private static LtvEconSaveData instance;

    // SERIALIZABLE DATA
    public WorkerRegistry workerRegistry = new WorkerRegistry();
    public EconomyEngine economyEngine = new EconomyEngine();
    public PlayerFactionSettings playerFactionSettings = new PlayerFactionSettings();

    private LtvEconSaveData() {}

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
        sector.addTransientListener(data.economyEngine);
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
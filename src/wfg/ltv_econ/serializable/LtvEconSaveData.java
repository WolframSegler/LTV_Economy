package wfg.ltv_econ.serializable;

import static wfg.native_ui.util.Globals.settings;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.fleet.LtvEconFleetRouteManager;
import wfg.ltv_econ.economy.fleet.PatrolFleetRouteManager;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;

public class LtvEconSaveData implements Serializable {
    private static final String LtvEconSaveDataSerialID = "ltv_econ_save_data_id"; 
    private static LtvEconSaveData instance;

    // SERIALIZABLE DATA
    public final PlayerFactionSettings playerFactionSettings;
    public final WorkerRegistry workerRegistry;
    public WorkerPoolRegistry poolRegistry; // TODO make final after incompat update
    public final MarketFinanceRegistry financeRegistry;
    public final EconomyEngine economyEngine;
    public final LtvEconFleetRouteManager econRouteManager;
    public final PatrolFleetRouteManager patrolRouteManager;

    private LtvEconSaveData() {
        instance = this;
        playerFactionSettings = new PlayerFactionSettings();
        workerRegistry = new WorkerRegistry();
        poolRegistry = new WorkerPoolRegistry();
        financeRegistry = new MarketFinanceRegistry();
        economyEngine = new EconomyEngine();
        econRouteManager = new LtvEconFleetRouteManager();
        patrolRouteManager = new PatrolFleetRouteManager();
    }

    // TODO remove after incompat update
    private Object readResolve() {
        poolRegistry = new WorkerPoolRegistry();

        return this;
    }

    public static final LtvEconSaveData loadInstance(boolean forceRefresh, boolean newGame) {
        final SectorAPI sector = Global.getSector();

        LtvEconSaveData data = (LtvEconSaveData) sector.getPersistentData().get(
            LtvEconSaveDataSerialID
        );

        if (data == null || forceRefresh) {
            data = new LtvEconSaveData();

            if (settings.isDevMode()) {
                Global.getLogger(EconomyEngine.class).info("LtvEconomySaveData constructed");
            }
        }
        instance = data;

        // SETUP
        if (EconConfig.ASSIGN_WORKERS_ON_LOAD || newGame) {
            data.economyEngine.fakeAdvanceWithAssignWorkers();
        } else {
            data.economyEngine.fakeAdvance();
        }

        sector.getListenerManager().addListener(data.economyEngine, true);
        sector.addTransientScript(data.economyEngine);
        sector.addTransientScript(data.econRouteManager);

        StaticData.loadData(data);

        return data;
    }

    public static final void saveInstance() {
        Global.getSector().getPersistentData().put(LtvEconSaveDataSerialID, instance);

        StaticData.resetData();

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
package wfg_ltv_econ.plugins;

import java.util.Map;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import wfg_ltv_econ.conditions.NoRestockCondition;
import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.economy.WorkerRegistry;

public class LtvEconomyModPlugin extends BaseModPlugin {

    public static final String EconEngine = "ltv_econ_econ_engine";
    public static final String WorkerReg = "ltv_econ_worker_registry";

    @Override
    public void onApplicationLoad() throws Exception {
        // Force early load large classes.
        Class.forName("wfg_ltv_econ.util.ListenerFactory");
        Class.forName("wfg_ltv_econ.util.RfReflectionUtils");
        Class.forName("wfg_ltv_econ.util.ReflectionUtils");
    }

    @Override
    public void onNewGame() {
        NoRestockCondition.initialize();
        WorkerPoolCondition.initialize();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        NoRestockCondition.initialize();
        WorkerPoolCondition.initialize();

        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        final EconomyEngine engine = (EconomyEngine) persistentData.get(EconEngine);
        final WorkerRegistry workerRegistry = (WorkerRegistry) persistentData.get(WorkerReg);

        if (engine != null) {
            EconomyEngine.setInstance(engine);
        } else {
            EconomyEngine.createInstance();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Economy Engine constructed");
            }
        }
        if (workerRegistry != null) {
            WorkerRegistry.setInstance(workerRegistry);
        } else {
            WorkerRegistry.createInstance();
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Worker Registery constructed");
            }
        }

        Global.getSector().getListenerManager().addListener(new AddWorkerIndustryOption(), true);
        Global.getSector().addTransientScript(new LtvMarketReplacer());
        Global.getSector().addTransientScript(new EconomyEngineScript());
    }

    @Override
    public void beforeGameSave() {
        final Map<String, Object> persistentData = Global.getSector().getPersistentData();

        persistentData.put(EconEngine, EconomyEngine.getInstance());
        persistentData.put(WorkerReg, WorkerRegistry.getInstance());
    }
}
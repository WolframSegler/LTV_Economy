package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import wfg_ltv_econ.economy.EconomyEngine;
import wfg_ltv_econ.economy.WorkerRegistry;

public class EconomyEngineScript implements EveryFrameScript{

    private boolean engineInitialized = false;
    private boolean workerRegInitialized = false;

    @Override
    public void advance(float amount) {

        if (!engineInitialized) {

            EconomyEngine.createInstance();
            engineInitialized = true;
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Economy Engine initialized.");
            }

        } else {
            EconomyEngine.getInstance().advance(amount); 
        }

        if (!workerRegInitialized && engineInitialized) {
            WorkerRegistry.createInstance();
            workerRegInitialized = true;
            if (Global.getSettings().isDevMode()) {
                Global.getLogger(getClass()).info("Worker Registery initialized.");
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}

package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import wfg_ltv_econ.economy.EconomyEngine;

public class EconomyEngineScript implements EveryFrameScript{

    private boolean initialized = false;

    @Override
    public void advance(float amount) {

        if (!initialized) {
            if (!Global.getSector().getEconomy().getMarketsCopy().isEmpty()) {
                EconomyEngine.createInstance();
                initialized = true;
                if (Global.getSettings().isDevMode()) {
                    Global.getLogger(getClass()).info("EconomyEngine initialized.");
                }
            }
        } else {
            EconomyEngine.getInstance().advance(amount); 
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

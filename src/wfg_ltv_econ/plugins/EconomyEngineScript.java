package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.EveryFrameScript;

import wfg_ltv_econ.economy.EconomyEngine;

public class EconomyEngineScript implements EveryFrameScript{

    @Override
    public void advance(float amount) {
        if (EconomyEngine.isInitialized()) {
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

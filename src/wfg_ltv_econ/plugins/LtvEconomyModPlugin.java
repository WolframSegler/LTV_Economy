package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import wfg_ltv_econ.util.LtvMarketReplacer;

public class LtvEconomyModPlugin extends BaseModPlugin {

    // Global.getLogger(this.getClass()).info("message");

    @Override
    public void onApplicationLoad() throws Exception {
        
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
        Global.getSector().getListenerManager().addListener(new AddWorkerIndustryOption(), true);
        
        Global.getSector().addTransientScript(new LtvMarketReplacer());
    }
}
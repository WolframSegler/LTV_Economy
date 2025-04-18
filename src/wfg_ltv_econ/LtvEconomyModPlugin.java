package wfg_ltv_econ;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class LtvEconomyModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {
        //Global.getLogger(this.getClass()).info("LTV Economy Mod loaded!");
    }

    @Override
    public void onNewGame() {
        //Global.getLogger(this.getClass()).info("Starting new game with LTV Economy!");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        //Global.getLogger(this.getClass()).info("Game loaded. LTV Economy active.");
    }
}
package wfg_ltv_econ;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class LtvEconomyModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {

    }

    @Override
    public void onNewGame() {
        Global.getLogger(this.getClass()).info("Starting new game with LTV Economy!");

        // All existing markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            market.addCondition("no_restock_condition");
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {

        // All existing markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            market.addCondition("no_restock_condition");
        }
    }

    
}
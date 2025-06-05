package wfg_ltv_econ.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;

public class NoRestockCondition extends BaseMarketConditionPlugin {

    @Override
    public void apply(String id) {
        super.apply(id);
        freezeAllCommodities();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    private void freezeAllCommodities() {
        for (CommodityOnMarketAPI commodity : market.getAllCommodities()) {
            if (commodity == null) continue;

            // Remove base restock and multipliers
            commodity.getAvailableStat().unmodify("base");
            commodity.getAvailableStat().unmodify("mult");
        }
    }

    @Override
    public boolean showIcon() {
        return false;
    }

    public final static void initialize() {
        // All existing markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market.hasCondition("no_restock_condition")) { continue;}
            market.addCondition("no_restock_condition");
        }
    }
}
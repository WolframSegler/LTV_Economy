package wfg_ltv_econ;

import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
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
}
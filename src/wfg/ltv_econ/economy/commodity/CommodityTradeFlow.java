package wfg.ltv_econ.economy.commodity;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class CommodityTradeFlow {
    public final MarketAPI exporter;
    public final MarketAPI importer;
    public final float amount;
    public final boolean inFaction;

    public CommodityTradeFlow(MarketAPI exporter, MarketAPI importer, float amount, boolean inFaction) {
        this.exporter = exporter;
        this.importer = importer;
        this.amount = amount;
        this.inFaction = inFaction;
    }
}
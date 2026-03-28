package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class ComTradeFlow implements Serializable {
    public final MarketAPI exporter;
    public final MarketAPI importer;
    public final float amount;
    public final float unitPrice;
    public final boolean inFaction;

    public ComTradeFlow(MarketAPI exporter, MarketAPI importer, float amount, float unitPrice,
        boolean inFaction
    ) {
        this.exporter = exporter;
        this.importer = importer;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.inFaction = inFaction;
    }
}
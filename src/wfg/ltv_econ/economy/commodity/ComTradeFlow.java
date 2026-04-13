package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class ComTradeFlow implements Serializable {
    public final String comID;
    public final MarketAPI exporter;
    public final MarketAPI importer;
    public final float unitPrice;
    public final boolean inFaction;
    public double amount;

    public ComTradeFlow(String comID, MarketAPI exporter, MarketAPI importer, double amount,
        float unitPrice, boolean inFaction
    ) {
        this.comID = comID;
        this.exporter = exporter;
        this.importer = importer;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.inFaction = inFaction;
    }
}
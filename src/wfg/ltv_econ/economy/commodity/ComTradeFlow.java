package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

public class ComTradeFlow implements Serializable {
    public final String comID;
    public final String exporterID;
    public final String importerID;
    public final float totalPrice;
    public final boolean inFaction;
    public transient MarketAPI exporter;
    public transient MarketAPI importer;
    public double amount;

    public ComTradeFlow(String comID, MarketAPI exporter, MarketAPI importer, double amount,
        float totalPrice, boolean inFaction
    ) {
        this.comID = comID;
        this.exporterID = exporter.getId();
        this.importerID = importer.getId();
        this.exporter = exporter;
        this.importer = importer;
        this.amount = amount;
        this.totalPrice = totalPrice;
        this.inFaction = inFaction;
    }

    private final Object readResolve() {
        final EconomyAPI econ = Global.getSector().getEconomy();

        exporter = econ.getMarket(exporterID);
        importer = econ.getMarket(importerID);

        return this;
    }
}
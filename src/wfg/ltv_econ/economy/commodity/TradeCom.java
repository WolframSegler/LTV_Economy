package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

public class TradeCom implements Serializable {
    public final String comID;
    public final float unitPrice;
    public double amount;

    public TradeCom(String comID, double amount, float unitPrice) {
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.comID = comID;
    }
}
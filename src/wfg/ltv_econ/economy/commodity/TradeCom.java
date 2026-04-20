package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

public class TradeCom implements Serializable {
    public final String comID;
    public final float totalPrice;
    public double amount;

    public TradeCom(String comID, double amount, float totalPrice) {
        this.totalPrice = totalPrice;
        this.amount = amount;
        this.comID = comID;
    }
}
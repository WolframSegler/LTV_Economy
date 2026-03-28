package wfg.ltv_econ.economy.commodity;

import java.io.Serializable;

public class TradeCargo implements Serializable {
    public final String comID;
    public final float amount;
    public final float unitPrice;

    public TradeCargo(String comID, float amount, float unitPrice) {
        this.comID = comID;
        this.amount = amount;
        this.unitPrice = unitPrice;
    }
}
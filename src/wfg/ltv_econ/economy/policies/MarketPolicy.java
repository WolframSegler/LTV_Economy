package wfg.ltv_econ.economy.policies;

import wfg.ltv_econ.economy.PlayerMarketData;

public abstract class MarketPolicy {
    public String id;
    public String name;
    public String description;
    public float duration; // in days

    public abstract void applyEffect(PlayerMarketData data);

    public abstract void preAdvance(PlayerMarketData data);
    public abstract void postAdvance(PlayerMarketData data);

    public void advanceDuration(int days) {
        duration -= days;
    }
}
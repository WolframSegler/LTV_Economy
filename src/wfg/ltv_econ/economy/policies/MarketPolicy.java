package wfg.ltv_econ.economy.policies;

import wfg.ltv_econ.economy.PlayerMarketData;

public abstract class MarketPolicy {
    public enum PolicyState { ACTIVE, COOLDOWN, AVAILABLE }
    
    public String id;
    public String name;
    public String description;

    public PolicyState state;
    public float duration; // in days
    public float cooldown; // remaining days until reusable

    public abstract void applyEffect(PlayerMarketData data);
    public abstract void preAdvance(PlayerMarketData data);
    public abstract void postAdvance(PlayerMarketData data);
    public boolean isActive() { return state == PolicyState.ACTIVE; }
    public boolean isAvailable() { return state == PolicyState.AVAILABLE; }

    public final void advanceTime(int days) {
        switch (state) {
        case ACTIVE:
            duration -= days;
            if (duration <= 0) {
                duration = 0;
                state = PolicyState.COOLDOWN;
            }
            break;
        case COOLDOWN:
            cooldown -= days;
            if (cooldown <= 0) {
                cooldown = 0;
                state = PolicyState.AVAILABLE;
            }
            break;
        default:
            break;
        }
    }

    public final void activate(float durationDays, float cooldownDays) {
        if (state != PolicyState.AVAILABLE) return;

        this.duration = durationDays;
        this.cooldown = cooldownDays;
        state = PolicyState.ACTIVE;
    }
}
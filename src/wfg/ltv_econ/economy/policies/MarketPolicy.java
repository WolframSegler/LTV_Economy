package wfg.ltv_econ.economy.policies;

import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.configs.PolicyConfigLoader.PolicyConfig;
import wfg.ltv_econ.configs.PolicyConfigLoader.PolicySpec;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.intel.PolicyNotificationIntel;

/**
 * Subclasses can use {@link #apply(PlayerMarketData data)} as a sort of constructor.
 */
public abstract class MarketPolicy {
    public enum PolicyState { ACTIVE, COOLDOWN, AVAILABLE }
    
    public String id;
    public transient PolicySpec spec;

    public PolicyState state = PolicyState.AVAILABLE;
    public int activeDaysRemaining;
    public int cooldownDaysRemaining;
    public boolean notifyWhenAvailable = false;
    public boolean notifyWhenFinished = false;
    public boolean repeatAfterCooldown = false;

    public abstract void apply(PlayerMarketData data);
    public abstract void unapply(PlayerMarketData data);
    public void preAdvance(PlayerMarketData data) {};
    public void postAdvance(PlayerMarketData data) {};
    public boolean isEnabled(PlayerMarketData data) { return true; }
    public boolean isActive() { return state == PolicyState.ACTIVE; }
    public boolean isAvailable() { return state == PolicyState.AVAILABLE; }
    public boolean isOnCooldown() { return state == PolicyState.COOLDOWN; }

    public Object readResolve() {
        spec = PolicyConfig.map.get(id);

        return this;
    }

    public final void advanceTime(PlayerMarketData data, int days) {
        switch (state) {
        case ACTIVE:
            activeDaysRemaining -= days;
            if (activeDaysRemaining <= 0) {
                deactivate(data);
                if (notifyWhenFinished) notifyFinished(data);
            }
            break;

        case COOLDOWN:
            cooldownDaysRemaining -= days;
            if (cooldownDaysRemaining <= 0) {
                state = PolicyState.AVAILABLE;
                if (notifyWhenAvailable) notifyAvailable(data);
                if (repeatAfterCooldown) activate(data);
            }
            break;

        default:
            break;
        }
    }

    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.setParaFontOrbitron();
        tp.addPara(spec.name, pad, base);
        
        tp.setParaFontDefault();
        tp.addPara(spec.description, pad);

        if (state == PolicyState.COOLDOWN) {
            tp.addPara(String.format(
                "Policy currently on cooldown. Available in %d days.", cooldownDaysRemaining), pad
            );
        }
    }

    public void notifyAvailable(PlayerMarketData data) {
        Global.getSector().getIntelManager().addIntel(
            new PolicyNotificationIntel(data, this, true),
            false
        );
    }

    public void notifyFinished(PlayerMarketData data) {
        Global.getSector().getIntelManager().addIntel(
            new PolicyNotificationIntel(data, this, false),
            false
        );
    }

    public final void activate(PlayerMarketData data) {
        activate(data, spec.durationDays);
    }

    public final void activate(PlayerMarketData data, int durationDays) {
        if (state != PolicyState.AVAILABLE) return;
        if (EconomyEngine.getInstance().getCredits(data.marketID) < spec.cost &&
            DebugFlags.COLONY_DEBUG
        ) return;

        EconomyEngine.getInstance().addCredits(data.marketID, -spec.cost);
        activeDaysRemaining = durationDays;
        state = PolicyState.ACTIVE;
        apply(data);
    }

    public final void deactivate(PlayerMarketData data) {
        deactivate(data, spec.cooldownDays);
    }

    public final void deactivate(PlayerMarketData data, int cooldownDays) {
        if (state != PolicyState.ACTIVE) return;

        cooldownDaysRemaining = cooldownDays;
        state = PolicyState.COOLDOWN;
        unapply(data);
    }
}
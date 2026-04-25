package wfg.ltv_econ.intel.market.events;

import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

import wfg.ltv_econ.config.EventConfig;
import wfg.ltv_econ.config.EventConfig.EventSpec;
import wfg.ltv_econ.economy.PlayerMarketData;

public abstract class MarketEvent {
    public String id;
    public transient EventSpec spec;
    protected boolean active = false;

    public void preAdvance(PlayerMarketData data) {};
    public void postAdvance(PlayerMarketData data) {};
    public boolean isActive() { return active; }
    public boolean isVisible(PlayerMarketData data) { return active; }

    public Object readResolve() {
        spec = EventConfig.map.get(id);

        return this;
    }

    public void createTooltip(PlayerMarketData data, TooltipMakerAPI tp) {
        tp.setTitleSmallOrbitron();
        tp.addTitle(spec.name, base);
        
        tp.addPara(spec.description, pad);
    }
}
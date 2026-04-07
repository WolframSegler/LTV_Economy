package wfg.ltv_econ.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import wfg.ltv_econ.config.loader.EventConfigLoader;
import wfg.ltv_econ.intel.market.events.MarketEvent;
import wfg.native_ui.util.ArrayMap;

public class EventConfig {
    public static final Map<String, EventSpec> map = new ArrayMap<>(16);

    static {
        EventConfigLoader.loadConfig();
    }
    
    public static final List<EventSpec> getEventsCopy() {
        return new ArrayList<>(map.values());
    }

    public static class EventSpec {
        public String id;
        public String name;
        public Class<? extends MarketEvent> marketEventClass; 
        public String iconPath;
        public String description;
        public final List<String> tags = new ArrayList<>();
    }
}
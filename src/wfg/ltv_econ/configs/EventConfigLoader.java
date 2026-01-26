package wfg.ltv_econ.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import wfg.ltv_econ.intel.market.events.MarketEvent;

public class EventConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/event_config.json";

    private static JSONObject config;

    private static final void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load event config: " + CONFIG_PATH, ex);
        }
    }

    public static final JSONObject getConfig() {
        if (config == null) load();
        return config;
    }

    @SuppressWarnings("unchecked")
    public static final void loadConfig() {
        try {
            final SettingsAPI settings = Global.getSettings();
            final JSONArray root = getConfig().getJSONArray("events");

            for (int i = 0; i < root.length(); i++) {
                final JSONObject obj = root.getJSONObject(i);
                final EventSpec spec = new EventSpec();

                spec.id = obj.getString("id");
                spec.name = obj.getString("name");
                spec.marketEventClass = (Class<? extends MarketEvent>)
                    settings.getScriptClassLoader().loadClass(obj.getString("class")
                );
                spec.iconPath = obj.getString("iconPath");
                spec.description = obj.getString("description");

                final JSONArray tags = obj.getJSONArray("tags");
                for (int j = 0; j < tags.length(); j++) {
                    spec.tags.add(tags.getString(j));
                }

                if (EventConfig.map.containsKey(spec.id)) {
                    throw new RuntimeException(
                        "Duplicate id '" + spec.id + "' in " + CONFIG_PATH
                    );
                }
                EventConfig.map.put(spec.id, spec);
            }


        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load event configuration from " + CONFIG_PATH + ": "
                + e.getMessage(), e
            );
        }
    }

    public static class EventConfig {
        public static final HashMap<String, EventSpec> map = new HashMap<>(16);
        public static final List<EventSpec> getEventsCopy() {
            return new ArrayList<>(map.values());
        }

        static {
            EventConfigLoader.loadConfig();
        }
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
package wfg.ltv_econ.configs;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import wfg.ltv_econ.economy.policies.MarketPolicy;

public class PolicyConfigLoader {
    private static final String CONFIG_PATH = "./data/config/policy_config.json";

    private static JSONObject config;

    private static void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load policy config: " + CONFIG_PATH, ex);
        }
    }

    public static JSONObject getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    public static void loadConfig() {
        try {
            final SettingsAPI settings = Global.getSettings();
            final JSONArray root = getConfig().getJSONArray("policies");

            for (int i = 0; i < root.length(); i++) {
                final JSONObject obj = root.getJSONObject(i);
                final PolicySpec spec = new PolicySpec();

                spec.id = obj.getString("id");
                spec.name = obj.getString("name");
                spec.marketPolicyClass = (Class<? extends MarketPolicy>)
                    settings.getScriptClassLoader().loadClass(obj.getString("class")
                );
                spec.iconPath = obj.getString("iconPath");
                spec.cost = obj.getInt("cost");
                spec.durationDays = obj.getInt("durationDays");
                spec.cooldownDays = obj.getInt("cooldownDays");
                spec.description = obj.getString("description");

                PolicyConfig.map.put(spec.id, spec);
            }


        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load policy configuration from " + CONFIG_PATH + ": "
                + e.getMessage(), e
            );
        }
    }

    public static class PolicyConfig {
        public static final HashMap<String, PolicySpec> map = new HashMap<>(16);
        public static final HashMap<String, PolicySpec> getPoliciesCopy() {
            return new HashMap<>(map);
        }

        static {
            PolicyConfigLoader.loadConfig();
        }
    }

    public static class PolicySpec {
        public String id;
        public String name;
        public Class<? extends MarketPolicy> marketPolicyClass; 
        public String iconPath;
        public int cost;
        public int durationDays;
        public int cooldownDays;
        public String description;
    }
}
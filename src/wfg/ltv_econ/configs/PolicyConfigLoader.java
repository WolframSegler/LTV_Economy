package wfg.ltv_econ.configs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import wfg.ltv_econ.intel.market.policies.MarketPolicy;
import wfg.ltv_econ.util.ArrayMap;

public class PolicyConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/policy_config.json";

    private static JSONObject config;

    private static final void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load policy config: " + CONFIG_PATH, ex);
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
            final JSONArray root = getConfig().getJSONArray("policies");

            for (int i = 0; i < root.length(); i++) {
                final JSONObject obj = root.getJSONObject(i);
                final PolicySpec spec = new PolicySpec();

                spec.id = obj.getString("id");
                spec.name = obj.getString("name");
                spec.marketPolicyClass = (Class<? extends MarketPolicy>)
                    settings.getScriptClassLoader().loadClass(obj.getString("class")
                );
                spec.posterPath = obj.getString("posterPath");
                spec.cost = obj.getInt("cost");
                spec.durationDays = obj.getInt("durationDays");
                spec.cooldownDays = obj.getInt("cooldownDays");
                spec.description = obj.getString("description");

                if (PolicyConfig.map.containsKey(spec.id)) {
                    throw new RuntimeException(
                        "Duplicate id '" + spec.id + "' in " + CONFIG_PATH
                    );
                }
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
        public static final Map<String, PolicySpec> map = new ArrayMap<>(16);
        public static final List<PolicySpec> getPoliciesCopy() {
            return new ArrayList<>(map.values());
        }

        static {
            PolicyConfigLoader.loadConfig();
        }
    }

    public static class PolicySpec {
        public String id;
        public String name;
        public Class<? extends MarketPolicy> marketPolicyClass; 
        public String posterPath;
        public int cost;
        public int durationDays;
        public int cooldownDays;
        public String description;
    }
}
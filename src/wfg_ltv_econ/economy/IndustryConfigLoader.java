package wfg_ltv_econ.economy;

import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IndustryConfigLoader {

    private static final String CONFIG_PATH = "./data/config/industry_config.json";

    private static JSONObject config;

    private static void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load industry config: " + CONFIG_PATH, ex);
        }
    }

    public static JSONObject getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, OutputCom>> loadAsMap() {
        JSONObject root = IndustryConfigLoader.getConfig();
        Map<String, Map<String, OutputCom>> result = new HashMap<>();

        try {
        for (Iterator<String> itIndustry = root.keys(); itIndustry.hasNext();) {
            String industryId = itIndustry.next();
            JSONObject industryJson = root.getJSONObject(industryId);

            Map<String, OutputCom> commodityMap = new HashMap<>();

            for (Iterator<String> itCommodity = industryJson.keys(); itCommodity.hasNext();) {
                String commodityId = itCommodity.next();
                JSONObject commodityJson = industryJson.getJSONObject(commodityId);

                float baseProd = (float) commodityJson.optDouble("base_prod", 0);

                Map<String, Float> demandMap = new HashMap<>();
                JSONObject demandJson = commodityJson.optJSONObject("demand");
                if (demandJson != null) {
                    for (Iterator<String> itDemand = demandJson.keys(); itDemand.hasNext();) {
                        String inputId = itDemand.next();
                        float weight = (float) demandJson.getDouble(inputId);
                        demandMap.put(inputId, weight);
                    }
                }

                commodityMap.put(commodityId, new OutputCom(baseProd, demandMap));
            }

            result.put(industryId, commodityMap);
        }
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load industry configuration from 'data/config/industry_config.json': "
                + e.getMessage(), e
            );
        }

        return result;
    }

    public static class OutputCom {
        public final float baseProd;
        public final Map<String, Float> demand;

        public OutputCom(float baseProd, Map<String, Float> demand) {
            this.baseProd = baseProd;
            this.demand = demand;
        }
    }
}


package wfg_ltv_econ.economy;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import java.util.List;
import java.util.ArrayList;
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
        final JSONObject root = IndustryConfigLoader.getConfig();
        final Map<String, Map<String, OutputCom>> result = new HashMap<>();

        try {
        for (Iterator<String> itIndustry = root.keys(); itIndustry.hasNext();) {
            String industryId = itIndustry.next();
            JSONObject industryJson = root.getJSONObject(industryId);

            Map<String, OutputCom> commodityMap = new HashMap<>();

            for (Iterator<String> itCommodity = industryJson.keys(); itCommodity.hasNext();) {
                String commodityId = itCommodity.next();
                Object value = industryJson.get(commodityId);

                // if (value instanceof Boolean booleanJson) {
                //     // TODO: use workerAssignable field later
                // }

                if (value instanceof JSONObject commodityJson) {
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

                    List<String> marketCondsFalse = new ArrayList<>();
                    JSONArray condArray = commodityJson.optJSONArray("ifMarketConditionsFalse");
                    if (condArray != null) {
                        for (int i = 0; i < condArray.length(); i++) {
                            marketCondsFalse.add(condArray.getString(i));
                        }
                    }

                    List<String> marketCondsTrue = new ArrayList<>();
                    condArray = commodityJson.optJSONArray("ifMarketConditionsTrue");
                    if (condArray != null) {
                        for (int i = 0; i < condArray.length(); i++) {
                            marketCondsTrue.add(condArray.getString(i));
                        }
                    }

                    boolean scaleWSize = commodityJson.optBoolean("scaleWithMarketSize");
                    boolean useWorkers = commodityJson.optBoolean("usesWorkers");
                    boolean isAbstract = commodityJson.optBoolean("isAbstract");
                    boolean checkLegality = commodityJson.optBoolean("checkLegality");

                    OutputCom otp = new OutputCom(
                        commodityId,
                        baseProd,
                        demandMap,
                        scaleWSize,
                        useWorkers,
                        isAbstract,
                        checkLegality,
                        marketCondsFalse,
                        marketCondsTrue
                    );

                    commodityMap.put(commodityId, otp);
                }
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
        public final String comID;
        public final float baseProd;
        public final Map<String, Float> demand;
        public final List<String> ifMarketCondsFalse;
        public final List<String> ifMarketCondsTrue;

        public final boolean scaleWithMarketSize; // Base size where no scaling happens is 3.
        public final boolean usesWorkers;
        public final boolean isAbstract;
        public final boolean checkLegality;

        public OutputCom(
            String comID, float baseProd, Map<String, Float> demand, boolean scaleWithMarketSize,
            boolean useWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketConditionsFalse,
            List<String> ifMarketConditionsTrue
        ) {
            this.comID = comID;
            this.baseProd = baseProd;
            this.demand = demand;
            this.ifMarketCondsFalse = ifMarketConditionsFalse;
            this.ifMarketCondsTrue = ifMarketConditionsTrue;
            this.scaleWithMarketSize = scaleWithMarketSize;
            this.usesWorkers = useWorkers;
            this.isAbstract = isAbstract;
            this.checkLegality = checkLegality;
        }

        @Override
        public final String toString() {
            return comID + " {" +
                "baseProd=" + baseProd +
                ", demand=" + demand +
                ", ifMarketConditionsFalse=" + ifMarketCondsFalse +
                ", ifMarketConditionsTrue=" + ifMarketCondsTrue +
                ", scaleWithMarketSize=" + scaleWithMarketSize +
                ", usesWorkers=" + usesWorkers +
                ", isAbstract=" + isAbstract +
                ", checkLegality=" + checkLegality +
                '}';
        }
    }
}


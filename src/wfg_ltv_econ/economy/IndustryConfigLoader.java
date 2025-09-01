package wfg_ltv_econ.economy;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import wfg_ltv_econ.economy.LaborConfigLoader.OCCTag;

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

                OCCTag occTag = OCCTag.AVERAGE;
                if (value instanceof String occTagStr && occTagStr.equals("occTag")) {
                    switch (occTagStr) {
                    case "average":
                        break;
                    case "industry":
                        occTag = OCCTag.INDUSTRY;
                        break;
                    case "service":
                        occTag = OCCTag.SERVICE;
                        break;
                    case "agriculture":
                        occTag = OCCTag.AGRICULTURE;
                        break;
                    case "manual":
                        occTag = OCCTag.MANUAL;
                        break;
                    case "space":
                        occTag = OCCTag.SPACE;
                        break;
                    }
                }

                if (value instanceof JSONObject commodityJson) {
                    float baseProd = (float) commodityJson.optDouble("base_prod", 1);

                    Map<String, Float> CCMoneyDist = new HashMap<>();
                    JSONObject mapJson = commodityJson.optJSONObject("constantCapitalMoneySplit");
                    if (mapJson != null) {
                        for (Iterator<String> itDemand = mapJson.keys(); itDemand.hasNext();) {
                            String inputId = itDemand.next();
                            float weight = (float) mapJson.getDouble(inputId);
                            CCMoneyDist.put(inputId, weight);
                        }
                    }

                    Map<String, Float> ConsumptionMap = new HashMap<>();
                    mapJson = commodityJson.optJSONObject("consumptionRequirements");
                    if (mapJson != null) {
                        for (Iterator<String> itDemand = mapJson.keys(); itDemand.hasNext();) {
                            String inputId = itDemand.next();
                            float weight = (float) mapJson.getDouble(inputId);
                            ConsumptionMap.put(inputId, weight);
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
                        CCMoneyDist,
                        scaleWSize,
                        useWorkers,
                        isAbstract,
                        checkLegality,
                        marketCondsFalse,
                        marketCondsTrue,
                        occTag,
                        ConsumptionMap
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
        public final float baseProd; // Used when the output does not depend on workers
        public final OCCTag occTag; // Used to determine the RoVC of the industry

        public final Map<String, Float> CCMoneyDist; // Determines the share of money spent on each input
        public final Map<String, Float> ConsumptionMap; // Flat input amounts that are independent of workers

        public final List<String> ifMarketCondsFalse;
        public final List<String> ifMarketCondsTrue;

        public final boolean scaleWithMarketSize; // Base size where no scaling happens is 3.
        public final boolean usesWorkers;
        public final boolean isAbstract; // Abstract outputs have no output, only inputs
        public final boolean checkLegality;

        public OutputCom(
            String comID, float baseProd, Map<String, Float> CCMoneyDist, boolean scaleWithMarketSize,
            boolean useWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketConditionsFalse,
            List<String> ifMarketConditionsTrue, OCCTag occTag, Map<String, Float> ConsumptionMap
        ) {
            this.comID = comID;
            this.baseProd = baseProd;
            this.CCMoneyDist = CCMoneyDist;
            this.ConsumptionMap = ConsumptionMap;
            this.ifMarketCondsFalse = ifMarketConditionsFalse;
            this.ifMarketCondsTrue = ifMarketConditionsTrue;
            this.scaleWithMarketSize = scaleWithMarketSize;
            this.usesWorkers = useWorkers;
            this.isAbstract = isAbstract;
            this.checkLegality = checkLegality;
            this.occTag = occTag;
        }

        @Override
        public final String toString() {
            return comID + " {" +
                "baseProd=" + baseProd +
                ", CCMoneyDist=" + CCMoneyDist +
                ", ConsumptionMap=" + ConsumptionMap +
                ", ifMarketConditionsFalse=" + ifMarketCondsFalse +
                ", ifMarketConditionsTrue=" + ifMarketCondsTrue +
                ", scaleWithMarketSize=" + scaleWithMarketSize +
                ", usesWorkers=" + usesWorkers +
                ", isAbstract=" + isAbstract +
                ", checkLegality=" + checkLegality +
                ", occTag=" + occTag +
                '}';
        }
    }
}


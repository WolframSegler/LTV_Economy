package wfg.ltv_econ.economy;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.economy.LaborConfigLoader.OCCTag;

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
    public static Map<String, IndustryConfig> loadAsMap() {
        final JSONObject root = IndustryConfigLoader.getConfig();
        final Map<String, IndustryConfig> result = new HashMap<>();

        try {
        for (Iterator<String> itIndustry = root.keys(); itIndustry.hasNext();) {
            String industryId = itIndustry.next();
            JSONObject industryJson = root.getJSONObject(industryId);

            boolean workerAssignable = industryJson.optBoolean("workerAssignable", false);
            float limit = (float) industryJson.optDouble("workerAssignableLimit", 1);

            String occTagStr = industryJson.optString("occTag", null);
            OCCTag occTag = OCCTag.AVERAGE;

            if (occTagStr != null) {
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

            JSONObject outputInfo = industryJson.getJSONObject("outputList");
            Map<String, OutputCom> commodityMap = new HashMap<>();

            Iterator<String> outputIds = outputInfo.keys();
            while (outputIds.hasNext()) {
                String outputId = outputIds.next();
                JSONObject outputData = outputInfo.getJSONObject(outputId);

                float baseProd = 1;
                if (outputData.has("base_prod")) {
                    baseProd = (float) outputData.optDouble("base_prod", 1);
                }

                boolean scaleWSize = outputData.optBoolean("scaleWithMarketSize", false);
                boolean isAbstract = outputData.optBoolean("isAbstract", false);
                boolean useWorkers = outputData.optBoolean("usesWorkers", false);
                boolean checkLegality = outputData.optBoolean("checkLegality", false);

                List<String> marketCondsFalse = new ArrayList<>();
                if (outputData.has("ifMarketConditionsFalse")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketConditionsFalse");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsFalse.add(conds.getString(i));
                    }
                }

                List<String> marketCondsTrue = new ArrayList<>();
                if (outputData.has("ifMarketConditionsTrue")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketConditionsFalse");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsTrue.add(conds.getString(i));
                    }
                }

                Map<String, Float> ConsumptionMap = new HashMap<>();
                if (outputData.has("consumptionRequirements")) {
                    JSONObject consumption = outputData.getJSONObject("consumptionRequirements");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float weight = (float) consumption.getDouble(inputId);
                        ConsumptionMap.put(inputId, weight);
                    }
                }

                Map<String, Float> CCMoneyDist = new HashMap<>();
                if (outputData.has("constantCapitalMoneySplit")) {
                    JSONObject consumption = outputData.getJSONObject("constantCapitalMoneySplit");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float alloc = (float) consumption.getDouble(inputId);
                        CCMoneyDist.put(inputId, alloc);
                    }
                }

                OutputCom otp = new OutputCom(
                    outputId,
                    baseProd,
                    CCMoneyDist,
                    scaleWSize,
                    useWorkers,
                    isAbstract,
                    checkLegality,
                    marketCondsFalse,
                    marketCondsTrue,
                    ConsumptionMap
                );

                commodityMap.put(outputId, otp);
            }
            
            IndustryConfig indConfig = new IndustryConfig(workerAssignable, commodityMap, occTag, limit);
            result.put(industryId, indConfig);
        }
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load industry configuration from 'data/config/industry_config.json': "
                + e.getMessage(), e
            );
        }

        return result;
    }

    public static class IndustryConfig {
        public final boolean workerAssignable;
        public final float workerAssignableLimit;
        public final OCCTag occTag; // Used to determine the RoVC of the industry
        public final Map<String, OutputCom> outputs;

        public IndustryConfig(boolean workerAssignable, Map<String, OutputCom> outputs, OCCTag occTag,
            float limit) {

            this.workerAssignable = workerAssignable;
            this.outputs = outputs;
            this.occTag = occTag;
            this.workerAssignableLimit = limit;
        }

        @Override
        public final String toString() {
            return '{' + " ,\n"
                + "workerAssignable: " + workerAssignable + " ,\n"
                + "workerAssignableLimit: " + workerAssignableLimit + " ,\n"
                + "occTag: " + occTag.toString() + " ,\n"
                + outputs.toString()
                + '}';
        }
    }

    public static class OutputCom {
        public final String comID;
        public final float baseProd; // Used when the output does not depend on workers

        public final Map<String, Float> CCMoneyDist; // Determines the share of money spent on each input
        public final Map<String, Float> StaticInputsPerUnit; // Flat input amounts independent of workers
        public final Map<String, Float> DynamicInputsPerUnit = new HashMap<>(); // Populated dynamically

        public final List<String> ifMarketCondsFalse;
        public final List<String> ifMarketCondsTrue;

        public final boolean scaleWithMarketSize; // Base size where no scaling happens is 3.
        public final boolean usesWorkers;
        public final boolean isAbstract; // Abstract outputs have no output, only inputs
        public final boolean checkLegality;

        public OutputCom(
            String comID, float baseProd, Map<String, Float> CCMoneyDist, boolean scaleWithMarketSize,
            boolean useWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketConditionsFalse,
            List<String> ifMarketConditionsTrue, Map<String, Float> ConsumptionMap
        ) {
            this.comID = comID;
            this.baseProd = baseProd;
            this.CCMoneyDist = CCMoneyDist;
            this.StaticInputsPerUnit = ConsumptionMap;
            this.ifMarketCondsFalse = ifMarketConditionsFalse;
            this.ifMarketCondsTrue = ifMarketConditionsTrue;
            this.scaleWithMarketSize = scaleWithMarketSize;
            this.usesWorkers = useWorkers;
            this.isAbstract = isAbstract;
            this.checkLegality = checkLegality;
        }

        @Override
        public final String toString() {
            return '{' +  " ,\n" +
                "baseProd=" + baseProd + " ,\n" +
                ", CCMoneyDist=" + CCMoneyDist + " ,\n" +
                ", ConsumptionMap=" + StaticInputsPerUnit + " ,\n" +
                ", ifMarketConditionsFalse=" + ifMarketCondsFalse + " ,\n" +
                ", ifMarketConditionsTrue=" + ifMarketCondsTrue + " ,\n" +
                ", scaleWithMarketSize=" + scaleWithMarketSize + " ,\n" +
                ", usesWorkers=" + usesWorkers + " ,\n" +
                ", isAbstract=" + isAbstract + " ,\n" +
                ", checkLegality=" + checkLegality + " ,\n" +
                '}';
        }
    }
}


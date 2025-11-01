package wfg.ltv_econ.configs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.OCCTag;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IndustryConfigManager {

    public static final String CONFIG_PATH = "./data/config/industry_config.json";
    public static final String CONFIG_NAME = "industry_config.json";
    public static final String DYNAMIC_CONFIG_PATH = "./saves/common/dynamic_industry_config.json";
    public static final String DYNAMIC_CONFIG_NAME = "dynamic_industry_config.json";

    private static JSONObject config;
    private static JSONObject dynamic_config;

    private static final void load() {
        final SettingsAPI settings = Global.getSettings();
        try {
            config = settings.getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load industry config: " + CONFIG_PATH, ex);
        }

        try {
            dynamic_config = settings.readJSONFromCommon(DYNAMIC_CONFIG_NAME, false);

            if (dynamic_config == null || dynamic_config.length() < 1) {
                Global.getLogger(IndustryConfigManager.class).warn(
                    "Dynamic industry config missing or empty. Creating new JSONObject."
                );
                dynamic_config = new JSONObject();
            }
        } catch (Exception ex) {
            Global.getLogger(IndustryConfigManager.class).warn(
                "Failed to read dynamic industry config, creating new JSONObject: " +
                DYNAMIC_CONFIG_PATH + ".data", ex
            );
            dynamic_config = new JSONObject();
        }
    }

    public static final JSONObject getConfig(boolean dynamicConfig) {
        if (config == null || dynamic_config == null) {
            load();
        }
        return dynamicConfig ? dynamic_config : config;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, IndustryConfig> loadAsMap(boolean dynamicConfig) {
        final JSONObject root = getConfig(dynamicConfig);
        final Map<String, IndustryConfig> result = new HashMap<>();

        try {
        for (Iterator<String> itIndustry = root.keys(); itIndustry.hasNext();) {
            String industryId = itIndustry.next();
            JSONObject industryJson = root.getJSONObject(industryId);

            boolean workerAssignable = industryJson.optBoolean("workerAssignable", false);

            String occTagStr = industryJson.optString("occTag", null);
            OCCTag occTag = OCCTag.AVERAGE;

            if (occTagStr != null) {
                switch (occTagStr) {
                case "average":
                    break;
                case "industry":
                    occTag = OCCTag.INDUSTRY;
                    break;
                case "manufacture":
                    occTag = OCCTag.MANUFACTURE;
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

            JSONObject outputList = industryJson.getJSONObject("outputList");
            Map<String, OutputConfig> commodityMap = new HashMap<>();

            Iterator<String> outputIds = outputList.keys();
            while (outputIds.hasNext()) {
                String outputId = outputIds.next();
                JSONObject outputData = outputList.getJSONObject(outputId);

                float baseProd = (float) outputData.optDouble("baseProd", 1);
                long target = outputData.optLong("target", -1);
                float workerAssignableLimit = (float) outputData.optDouble(
                    "workerAssignableLimit", LaborConfig.defaultWorkerCapPerOutput
                );
                float marketScaleBase = (float) outputData.optDouble(
                    "marketScaleBase", 10
                );

                boolean scaleWSize = outputData.optBoolean("scaleWithMarketSize", false);
                boolean isAbstract = outputData.optBoolean("isAbstract", false);
                boolean useWorkers = outputData.optBoolean("usesWorkers", false);
                boolean checkLegality = outputData.optBoolean("checkLegality", false);

                List<String> marketCondsAllFalse = new ArrayList<>();
                if (outputData.has("ifMarketCondsAllFalse")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketCondsAllFalse");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsAllFalse.add(conds.getString(i));
                    }
                }

                List<String> marketCondsAllTrue = new ArrayList<>();
                if (outputData.has("ifMarketCondsAllTrue")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketCondsAllTrue");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsAllTrue.add(conds.getString(i));
                    }
                }

                Map<String, Float> ConsumptionMap = new HashMap<>();
                if (outputData.has("InputsPerUnitOutput")) {
                    JSONObject consumption = outputData.getJSONObject("InputsPerUnitOutput");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float weight = (float) consumption.getDouble(inputId);
                        ConsumptionMap.put(inputId, weight);
                    }
                }

                Map<String, Float> CCMoneyDist = new HashMap<>();
                if (outputData.has("CCMoneyDist")) {
                    JSONObject consumption = outputData.getJSONObject("CCMoneyDist");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float alloc = (float) consumption.getDouble(inputId);
                        CCMoneyDist.put(inputId, alloc);
                    }
                }

                OutputConfig otp = new OutputConfig(
                    outputId,
                    baseProd,
                    CCMoneyDist,
                    scaleWSize,
                    useWorkers,
                    isAbstract,
                    checkLegality,
                    marketCondsAllFalse,
                    marketCondsAllTrue,
                    ConsumptionMap,
                    workerAssignableLimit,
                    marketScaleBase,
                    target
                );

                commodityMap.put(outputId, otp);
            }
            
            IndustryConfig indConfig = new IndustryConfig(workerAssignable, commodityMap, occTag);
            result.put(industryId, indConfig);
        }
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load industry configuration from 'data/config/': "
                + e.getMessage(), e
            );
        }

        return result;
    }

    public static JSONObject serializeIndustryConfigs(Map<String, IndustryConfig> configs) {
        JSONObject root = new JSONObject();

        try {
            for (Map.Entry<String, IndustryConfig> entry : configs.entrySet()) {
                String industryId = entry.getKey();
                IndustryConfig ind = entry.getValue();

                JSONObject indJson = new JSONObject();
                indJson.put("workerAssignable", ind.workerAssignable);

                if (ind.occTag != null) {
                    indJson.put("occTag", ind.occTag.name().toLowerCase());
                }

                JSONObject outputList = new JSONObject();
                for (Map.Entry<String, OutputConfig> outputEntry : ind.outputs.entrySet()) {
                    String outputId = outputEntry.getKey();
                    OutputConfig opt = outputEntry.getValue();

                    JSONObject optJson = new JSONObject();
                    optJson.put("baseProd", opt.baseProd);
                    optJson.put("target", opt.target);
                    optJson.put("workerAssignableLimit", opt.workerAssignableLimit);
                    optJson.put("marketScaleBase", opt.marketScaleBase);

                    if (opt.CCMoneyDist != null && !opt.CCMoneyDist.isEmpty()) {
                        JSONObject ccJson = new JSONObject();
                        for (Map.Entry<String, Float> e : opt.CCMoneyDist.entrySet()) {
                            ccJson.put(e.getKey(), e.getValue());
                        }
                        optJson.put("CCMoneyDist", ccJson);
                    }

                    if (opt.InputsPerUnitOutput != null && !opt.InputsPerUnitOutput.isEmpty()) {
                        JSONObject inputsJson = new JSONObject();
                        for (Map.Entry<String, Float> e : opt.InputsPerUnitOutput.entrySet()) {
                            inputsJson.put(e.getKey(), e.getValue());
                        }
                        optJson.put("InputsPerUnitOutput", inputsJson);
                    }

                    if (opt.ifMarketCondsAllFalse != null && !opt.ifMarketCondsAllFalse.isEmpty()) {
                        optJson.put("ifMarketCondsAllFalse", new JSONArray(opt.ifMarketCondsAllFalse));
                    }

                    if (opt.ifMarketCondsAllTrue != null && !opt.ifMarketCondsAllTrue.isEmpty()) {
                        optJson.put("ifMarketCondsAllTrue", new JSONArray(opt.ifMarketCondsAllTrue));
                    }

                    optJson.put("scaleWithMarketSize", opt.scaleWithMarketSize);
                    optJson.put("usesWorkers", opt.usesWorkers);
                    optJson.put("isAbstract", opt.isAbstract);
                    optJson.put("checkLegality", opt.checkLegality);

                    outputList.put(outputId, optJson);
                }

                indJson.put("outputList", outputList);
                root.put(industryId, indJson);
            }
        } catch (JSONException e) {
            Global.getLogger(IndustryConfigManager.class)
                .error("Failed to serialize industry configs to JSON: " + e.getMessage(), e);
        }

        return root;
    }

    public static class IndustryConfig {
        public final boolean workerAssignable;
        public final OCCTag occTag;
        public final Map<String, OutputConfig> outputs;

        public boolean dynamic = false;

        public IndustryConfig(boolean workerAssignable, Map<String, OutputConfig> outputs, OCCTag occTag) {
            this.workerAssignable = workerAssignable;
            this.outputs = outputs;
            this.occTag = occTag;
        }

        /**
         * Copy Constructor
         */
        public IndustryConfig(IndustryConfig config) {
            this.workerAssignable = config.workerAssignable;
            this.occTag = config.occTag;
            this.dynamic = config.dynamic;

            // Deep copy outputs map
            if (config.outputs != null) {
                Map<String, OutputConfig> copy = new HashMap<>();
                for (Map.Entry<String, OutputConfig> e : config.outputs.entrySet()) {
                    copy.put(e.getKey(), new OutputConfig(e.getValue()));
                }
                this.outputs = copy;
            } else {
                this.outputs = new HashMap<>();
            }
        }

        @Override
        public final String toString() {
            return '{' + " ,\n"
                + "workerAssignable: " + workerAssignable + " ,\n"
                + "occTag: " + occTag.toString() + " ,\n"
                + outputs.toString()
                + '}';
        }
    }

    public static class OutputConfig {
        public final String comID;
        public final float baseProd;
        public final long target;
        public final float workerAssignableLimit;
        public final float marketScaleBase;

        public final Map<String, Float> CCMoneyDist; // Determines the share of money spent on each input
        public final Map<String, Float> InputsPerUnitOutput;

        public List<String> ifMarketCondsAllFalse;
        public List<String> ifMarketCondsAllTrue;

        public final boolean scaleWithMarketSize; // Base size where no scaling happens is 3.
        public final boolean usesWorkers;
        public final boolean isAbstract; // Abstract outputs have no output, only inputs
        public final boolean checkLegality;

        public OutputConfig(
            String comID, float baseProd, Map<String, Float> CCMoneyDist, boolean scaleWithMarketSize,
            boolean usesWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketCondsAllFalse,
            List<String> ifMarketCondsAllTrue, Map<String, Float> InputsPerUnitOutput, float workerAssignableLimit,
            float marketScaleBase, long target   
        ) {
            this.comID = comID;
            this.baseProd = baseProd;
            this.target = target;
            this.CCMoneyDist = CCMoneyDist;
            this.InputsPerUnitOutput = InputsPerUnitOutput;
            this.ifMarketCondsAllFalse = ifMarketCondsAllFalse;
            this.ifMarketCondsAllTrue = ifMarketCondsAllTrue;
            this.scaleWithMarketSize = scaleWithMarketSize;
            this.usesWorkers = usesWorkers;
            this.workerAssignableLimit = workerAssignableLimit;
            this.isAbstract = isAbstract;
            this.checkLegality = checkLegality;
            this.marketScaleBase = marketScaleBase;
        }

        /**
         * Copy Constructor
         */
        public OutputConfig(OutputConfig other) {
            this.comID = other.comID;
            this.baseProd = other.baseProd;
            this.target = other.target;

            this.CCMoneyDist = (other.CCMoneyDist == null) ? null : new HashMap<>(other.CCMoneyDist);
            this.InputsPerUnitOutput = (other.InputsPerUnitOutput == null) ? null : new HashMap<>(other.InputsPerUnitOutput);

            this.ifMarketCondsAllFalse = (other.ifMarketCondsAllFalse == null)
                ? null : new ArrayList<>(other.ifMarketCondsAllFalse);
            this.ifMarketCondsAllTrue = (other.ifMarketCondsAllTrue == null)
                ? null : new ArrayList<>(other.ifMarketCondsAllTrue);

            this.scaleWithMarketSize = other.scaleWithMarketSize;
            this.usesWorkers = other.usesWorkers;
            this.workerAssignableLimit = other.workerAssignableLimit;
            this.isAbstract = other.isAbstract;
            this.checkLegality = other.checkLegality;
            this.marketScaleBase = other.marketScaleBase;
        }

        @Override
        public final String toString() {
            return '{' +  " ,\n" +
                "baseProd=" + baseProd + " ,\n" +
                ", target=" + target + " ,\n" +
                ", CCMoneyDist=" + CCMoneyDist + " ,\n" +
                ", ConsumptionMap=" + InputsPerUnitOutput + " ,\n" +
                ", ifMarketCondsAllFalse=" + ifMarketCondsAllFalse + " ,\n" +
                ", ifMarketCondsAllTrue=" + ifMarketCondsAllTrue + " ,\n" +
                ", scaleWithMarketSize=" + scaleWithMarketSize + " ,\n" +
                ", marketScaleBase=" + marketScaleBase + " ,\n" +
                ", usesWorkers=" + usesWorkers + " ,\n" +
                "workerAssignableLimit: " + workerAssignableLimit + " ,\n" +
                ", isAbstract=" + isAbstract + " ,\n" +
                ", checkLegality=" + checkLegality + " ,\n" +
                '}';
        }
    }
}
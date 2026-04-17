package wfg.ltv_econ.config.loader;

import static wfg.ltv_econ.constants.Mods.LTV_ECON;
import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.native_ui.util.ArrayMap;

public class IndustryConfigLoader {
    private static final Logger log = Global.getLogger(IndustryConfigLoader.class);

    private static final String CONFIG_PATH = "./data/config/ltvEcon/industry_config.json";
    private static final String DYNAMIC_CONFIG_PATH = "./saves/common/dynamic_industry_config.json";
    private static final String DYNAMIC_CONFIG_NAME = "dynamic_industry_config.json";

    private static JSONObject config;
    private static JSONObject dynamic_config;

    public static final String getDynamicConfigVersion() {
        final JSONObject root = getConfig(true);
        try {
            final String key = "modVersion";
            return root.has(key) ? root.getString(key) : "";
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to retrieve industry configuration version from " + DYNAMIC_CONFIG_PATH, e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static final ArrayMap<String, IndustryConfig> loadAsMap(boolean dynamicConfig) {
        final JSONObject root = getConfig(dynamicConfig);
        final ArrayMap<String, IndustryConfig> result = new ArrayMap<>(32);

        try { if (root.has("industryList")) {

        final JSONArray industries = root.getJSONArray("industryList");
        for (int indIdx = 0; indIdx < industries.length(); indIdx++) {
            final JSONObject indJson = industries.getJSONObject(indIdx);

            final String indID = indJson.getString("industryId");
            final boolean workerAssignable = indJson.optBoolean("workerAssignable", false);
            final boolean demandOnly = indJson.optBoolean("demandOnly", false);
            final String occTag = indJson.optString("occTag", LaborConfigLoader.AVERAGE_OCC_TAG);

            final JSONObject outputList = indJson.getJSONObject("outputList");
            final ArrayMap<String, OutputConfig> commodityMap = new ArrayMap<>(4);

            final Iterator<String> outputIds = outputList.keys();
            while (outputIds.hasNext()) {
                final String outputId = outputIds.next();
                final JSONObject outputData = outputList.getJSONObject(outputId);

                final float baseProd = (float) outputData.optDouble("baseProd", 1);
                final long target = outputData.optLong("target", -1);
                final float workerAssignableLimit = (float) outputData.optDouble(
                    "workerAssignableLimit", LaborConfig.defaultWorkerCapPerOutput
                );
                final float marketScaleBase = (float) outputData.optDouble(
                    "marketScaleBase", 10.0
                );

                final boolean isAbstract = !EconomyConstants.econCommodityIDs.contains(outputId);
                final boolean scaleWSize = outputData.optBoolean("scaleWithMarketSize", false);
                final boolean useWorkers = outputData.optBoolean("usesWorkers", false);
                final boolean checkLegality = outputData.optBoolean("checkLegality", false);
                final boolean activeBuilding = outputData.optBoolean("activeDuringBuilding", false);

                final List<String> marketCondsAllFalse = new ArrayList<>();
                if (outputData.has("ifMarketCondsAllFalse")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketCondsAllFalse");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsAllFalse.add(conds.getString(i));
                    }
                }

                final List<String> marketCondsAllTrue = new ArrayList<>();
                if (outputData.has("ifMarketCondsAllTrue")) {
                    JSONArray conds = outputData.getJSONArray("ifMarketCondsAllTrue");
                    for (int i = 0; i < conds.length(); i++) {
                        marketCondsAllTrue.add(conds.getString(i));
                    }
                }

                final ArrayMap<String, Float> ConsumptionMap = new ArrayMap<>(4);
                if (outputData.has("InputsPerUnitOutput")) {
                    JSONObject consumption = outputData.getJSONObject("InputsPerUnitOutput");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float weight = (float) consumption.getDouble(inputId);
                        ConsumptionMap.put(inputId, weight);
                    }
                }

                final ArrayMap<String, Float> CCMoneyDist = new ArrayMap<>(4);
                if (outputData.has("CCMoneyDist")) {
                    JSONObject consumption = outputData.getJSONObject("CCMoneyDist");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float alloc = (float) consumption.getDouble(inputId);
                        CCMoneyDist.put(inputId, alloc);
                    }
                }

                final OutputConfig opt = new OutputConfig(
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
                    target,
                    activeBuilding
                );

                commodityMap.put(outputId, opt);
            }
            
            IndustryConfig indConfig = new IndustryConfig(
                workerAssignable, commodityMap, occTag, demandOnly
            );
            result.put(indID, indConfig);
        }
        
        }} catch (Exception e) {
            throw new RuntimeException(
                "Failed to load industry configuration from " +
                (dynamicConfig ? DYNAMIC_CONFIG_PATH : CONFIG_PATH), e
            );
        }

        return result;
    }

    public static final void serializeAndWriteToCommon(ArrayMap<String, IndustryConfig> configs) {
        final JSONObject json = serializeIndustryConfigs(configs);

        try {
            settings.writeJSONToCommon(
                DYNAMIC_CONFIG_NAME,
                json,
                false
            );
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to write dynamic industry configuration to common JSON file '"
                + DYNAMIC_CONFIG_NAME, e
            );
        }
    }

    private static final JSONObject serializeIndustryConfigs(ArrayMap<String, IndustryConfig> configs) {
        final JSONObject root = new JSONObject();

        try {
        root.put("modVersion", settings.getModManager().getModSpec(LTV_ECON).getVersion());

        final List<JSONObject> industries = new ArrayList<>();
        for (Entry<String, IndustryConfig> entry : configs.singleEntrySet()) {
            final String indID = entry.getKey();
            final IndustryConfig ind = entry.getValue();

            final JSONObject indJson = new JSONObject();
            indJson.put("industryId", indID);
            indJson.put("workerAssignable", ind.workerAssignable);
            indJson.put("demandOnly", ind.demandOnly);
            indJson.put("occTag", ind.occTag);

            final JSONObject outputMap = new JSONObject();
            for (Entry<String, OutputConfig> outputEntry : ind.outputs.singleEntrySet()) {
                final String outputId = outputEntry.getKey();
                final OutputConfig opt = outputEntry.getValue();

                final JSONObject optJson = new JSONObject();
                optJson.put("baseProd", opt.baseProd);
                optJson.put("target", opt.target);
                optJson.put("workerAssignableLimit", opt.workerAssignableLimit);
                optJson.put("marketScaleBase", opt.marketScaleBase);

                if (opt.CCMoneyDist != null && !opt.CCMoneyDist.isEmpty()) {
                    JSONObject ccJson = new JSONObject();
                    for (Entry<String, Float> e : opt.CCMoneyDist.singleEntrySet()) {
                        ccJson.put(e.getKey(), e.getValue());
                    }
                    optJson.put("CCMoneyDist", ccJson);
                }

                if (opt.InputsPerUnitOutput != null && !opt.InputsPerUnitOutput.isEmpty()) {
                    JSONObject inputsJson = new JSONObject();
                    for (Entry<String, Float> e : opt.InputsPerUnitOutput.singleEntrySet()) {
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
                optJson.put("checkLegality", opt.checkLegality);
                optJson.put("activeDuringBuilding", opt.activeDuringBuilding);

                outputMap.put(outputId, optJson);
            }
            indJson.put("outputList", outputMap);

            industries.add(indJson);
        }
        root.put("industryList", industries);
        } catch (JSONException e) {
            Global.getLogger(IndustryConfigManager.class)
                .error("Failed to serialize industry configs to JSON", e);
        }

        return root;
    }

    private static final void load() {
        try {
            config = settings.getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load industry config: " + CONFIG_PATH, ex);
        }

        try {
            dynamic_config = settings.readJSONFromCommon(DYNAMIC_CONFIG_NAME, false);

            if (dynamic_config == null || dynamic_config.length() < 1) {
                log.info("Dynamic industry config missing or empty. Creating new JSONObject.");
                dynamic_config = new JSONObject();
            }
        } catch (Exception ex) {
            log.warn("Failed to read dynamic industry config, creating new JSONObject: " +
                DYNAMIC_CONFIG_PATH + ".data"
            );
            dynamic_config = new JSONObject();
        }
    }

    private static final JSONObject getConfig(boolean dynamicConfig) {
        if (config == null || dynamic_config == null) load();
        return dynamicConfig ? dynamic_config : config;
    }
}
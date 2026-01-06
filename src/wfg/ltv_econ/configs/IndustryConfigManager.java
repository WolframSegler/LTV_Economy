package wfg.ltv_econ.configs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.industry.IndustryIOs;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IndustryConfigManager {

    public static final String BASE_MAINTENANCE_ID = "maintenance_base";

    public static final String CONFIG_PATH = "./data/config/industry_config.json";
    public static final String CONFIG_NAME = "industry_config.json";
    public static final String DYNAMIC_CONFIG_PATH = "./saves/common/dynamic_industry_config.json";
    public static final String DYNAMIC_CONFIG_NAME = "dynamic_industry_config.json";
    public static final int TEST_MARKET_SIZE = 6;

    private static JSONObject config;
    private static JSONObject dynamic_config;

    public static Map<String, IndustryConfig> ind_config;

    static {
        ind_config = IndustryConfigManager.loadAsMap(false);

        validateOrRebuildDynamicConfigs();
    }

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
            final String industryId = itIndustry.next();
            final JSONObject industryJson = root.getJSONObject(industryId);

            final boolean workerAssignable = industryJson.optBoolean("workerAssignable", false);
            final boolean ignoreLocalStockpiles = industryJson.optBoolean("ignoreLocalStockpiles", false);

            final String occTagStr = industryJson.optString("occTag", null);
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

            final JSONObject outputList = industryJson.getJSONObject("outputList");
            final Map<String, OutputConfig> commodityMap = new HashMap<>();

            Iterator<String> outputIds = outputList.keys();
            while (outputIds.hasNext()) {
                final String outputId = outputIds.next();
                final JSONObject outputData = outputList.getJSONObject(outputId);

                final float baseProd = (float) outputData.optDouble("baseProd", 1);
                final long target = outputData.optLong("target", -1);
                final float workerAssignableLimit = (float) outputData.optDouble(
                    "workerAssignableLimit", LaborConfig.defaultWorkerCapPerOutput
                );
                final float marketScaleBase = (float) outputData.optDouble(
                    "marketScaleBase", 10
                );

                final boolean scaleWSize = outputData.optBoolean("scaleWithMarketSize", false);
                final boolean isAbstract = outputData.optBoolean("isAbstract", false);
                final boolean useWorkers = outputData.optBoolean("usesWorkers", false);
                final boolean checkLegality = outputData.optBoolean("checkLegality", false);

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

                final Map<String, Float> ConsumptionMap = new HashMap<>();
                if (outputData.has("InputsPerUnitOutput")) {
                    JSONObject consumption = outputData.getJSONObject("InputsPerUnitOutput");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float weight = (float) consumption.getDouble(inputId);
                        ConsumptionMap.put(inputId, weight);
                    }
                }

                final Map<String, Float> CCMoneyDist = new HashMap<>();
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
                    target
                );

                commodityMap.put(outputId, opt);
            }
            
            IndustryConfig indConfig = new IndustryConfig(
                workerAssignable, commodityMap, occTag, ignoreLocalStockpiles
            );
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
                indJson.put("ignoreLocalStockpiles", ind.ignoreLocalStockpiles);

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
        public final boolean ignoreLocalStockpiles;
        public final OCCTag occTag;
        public final Map<String, OutputConfig> outputs;

        public boolean dynamic = false;

        public IndustryConfig(boolean workerAssignable, Map<String, OutputConfig> outputs, OCCTag occTag,
            boolean ignoreLocalStockpiles
        ) {
            this.workerAssignable = workerAssignable;
            this.ignoreLocalStockpiles = ignoreLocalStockpiles;
            this.outputs = outputs;
            this.occTag = occTag;
        }

        /**
         * Copy Constructor
         */
        public IndustryConfig(IndustryConfig config) {
            this.workerAssignable = config.workerAssignable;
            this.ignoreLocalStockpiles = config.ignoreLocalStockpiles;
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
                + "ignoreLocalStockpiles: " + ignoreLocalStockpiles + " ,\n"
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

    /**
     * Scans all known industry configurations, generates dynamic configurations
     * for those lacking one, merges them into the main configuration map, and writes
     * the updated dynamic configuration map to disk.
     * <p>
     * This ensures that all industries — including modded ones — have valid configuration
     * entries, even if no explicit config was provided.
     * </p>
     */
    private static final void validateOrRebuildDynamicConfigs() {
        final SettingsAPI settings = Global.getSettings();
        final Map<String, IndustryConfig> dynamic_config = IndustryConfigManager.loadAsMap(true);

        boolean allIndustriesHaveConfig = true;
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            if (!(dynamic_config.containsKey(spec.getId()) ||
                dynamic_config.containsKey(IndustryIOs.getBaseIndustryID(spec))) &&
                !IndustryIOs.hasConfig(spec)
            ) {
                allIndustriesHaveConfig = false;
                break;
            }
        }

        if (allIndustriesHaveConfig) {
            ind_config.putAll(dynamic_config);
            return;
        }

        dynamic_config.clear();

        final String factionID = "ltv_test";
        final String marketID1 = "ltv_dynamic_ind_test_market1";
        final String marketID2 = "ltv_dynamic_ind_test_market2";
        final String testEntity = "ltv_dynamic_entity";
        final String abstractOutput = "atLeastOneOutputForAbstractInputs";
        final FactoryAPI factory = Global.getFactory();
        final SectorAPI sector = Global.getSector();
        final MarketAPI testMarket1 = factory.createMarket(marketID1, marketID1, TEST_MARKET_SIZE);
        final MarketAPI testMarket2 = factory.createMarket(marketID2, marketID2, 5);
        final StarSystemAPI testStarSystem = sector.createStarSystem(testEntity);
        final SectorEntityToken testPlanet = testStarSystem.addPlanet(
            testEntity, testStarSystem.getCenter(), testEntity, "frozen", 0f, 1f, 1f, 1f
        );
        testMarket1.setFactionId(factionID);
        testMarket2.setFactionId(factionID);
        testMarket1.setPrimaryEntity(testPlanet);
        testMarket2.setPrimaryEntity(testPlanet);
        final FactionAPI testFaction = testMarket1.getFaction();

        final Set<String> scaleWithMarketSize = new HashSet<>(8);
        final Map<String, Map<String, Float>> inputCache = new HashMap<>();
        final List<String> emptyList = new ArrayList<>();

        // Make every commodity illegal to observe industry behaviour
        for (CommoditySpecAPI spec : settings.getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            testFaction.makeCommodityIllegal(spec.getId());
        }

        for (IndustrySpecAPI indSpec : settings.getAllIndustrySpecs()) { 
            if (IndustryIOs.getIndConfig(indSpec) != null) continue;

            final String indID = indSpec.getId();
            final Map<String, OutputConfig> configOutputs = new HashMap<>();
            
            testMarket1.addIndustry(indID);
            testMarket2.addIndustry(indID);

            final Industry ind1 = testMarket1.getIndustry(indID);
            final Industry ind2 = testMarket1.getIndustry(indID);
            
            final List<String> outputs = new ArrayList<>(6);
            final Set<String> illegalOutputs = new HashSet<>(6);

            for (MutableCommodityQuantity mutable : ind1.getAllSupply()) {
                outputs.add(mutable.getCommodityId());
            }

            testMarket1.setFreePort(true);
            testMarket2.setFreePort(true);
            ind1.apply();
            ind2.apply();

            for (MutableCommodityQuantity mutable : ind1.getAllSupply()) {
                illegalOutputs.add(mutable.getCommodityId());
            }
            illegalOutputs.removeAll(outputs);

            scaleWithMarketSize.clear();
            for (MutableCommodityQuantity mutable : ind1.getAllSupply()) {
                String comID = mutable.getCommodityId();
                boolean scaleWithSize = Math.abs(ind1.getSupply(comID).getQuantity().getModifiedValue() -
                    ind2.getSupply(comID).getQuantity().getModifiedValue()
                ) > 0.01f;

                if (scaleWithSize) scaleWithMarketSize.add(comID);
            }

            final boolean hasNoRealOutputs = outputs.isEmpty() && illegalOutputs.isEmpty();
            final boolean usesWorkers = EconomyEngine.isWorkerAssignable(ind1) && !hasNoRealOutputs;
            if (hasNoRealOutputs) {
                outputs.add(abstractOutput);

                Optional<MutableCommodityQuantity> firstDemand = ind1.getAllDemand().stream().findFirst();
                if (firstDemand.isPresent()) {
                    String comID = firstDemand.get().getCommodityId();
                    boolean scaleWithSize = Math.abs(
                        ind1.getDemand(comID).getQuantity().getModifiedValue() -
                        ind2.getDemand(comID).getQuantity().getModifiedValue()
                    ) > 0.01f;
                    if (scaleWithSize) scaleWithMarketSize.add(abstractOutput);
                }
            }

            // In vanilla, each output uses each input
            final Map<String, Float> inputs = new HashMap<>(6);
            populateInputs(ind1, inputs, scaleWithMarketSize);
            inputCache.put(indID, new HashMap<>(inputs));

            final Map<String, Float> CCMoneyDist = usesWorkers ?
                inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> 1f)) : null;

            final Map<String, Float> InputsPerUnitOutput = !usesWorkers ?
                inputs.entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ) : null;

            final Consumer<String> addOutput = (outputID) -> {
                final boolean isIllegal = illegalOutputs.contains(outputID);
                final boolean isAbstract = settings.getCommoditySpec(outputID) == null;

                OutputConfig optCom = new OutputConfig(
                        outputID,
                        1,
                        CCMoneyDist,
                        scaleWithMarketSize.contains(outputID),
                        usesWorkers,
                        isAbstract,
                        isIllegal,
                        emptyList,
                        emptyList,
                        InputsPerUnitOutput,
                        LaborConfig.dynamicWorkerCapPerOutput,
                        8.5f,
                        -1
                );
                configOutputs.put(outputID, optCom);
            };

            outputs.forEach(addOutput);
            illegalOutputs.forEach(addOutput);

            final IndustryConfig config = new IndustryConfig(
                usesWorkers, configOutputs, OCCTag.AVERAGE, false
            );
            config.dynamic = true;

            dynamic_config.put(indID, config);
        }

        final List<String> conds = settings.getAllMarketConditionSpecs()
            .stream().map(MarketConditionSpecAPI::getId).collect(Collectors.toList());

        conds.removeIf(condID -> {
            MarketConditionSpecAPI spec = settings.getMarketConditionSpec(condID);
            String cls = spec.getScriptClass();
            return cls.contains("FoodShortage") || cls.contains("LuddicPathCells") || cls.contains("PirateActivity");
        });

        final Map<String, IndustryConfig> new_dynamic_config = new HashMap<>();
        for (Map.Entry<String, IndustryConfig> e : dynamic_config.entrySet()) {
            new_dynamic_config.put(e.getKey(), new IndustryConfig(e.getValue()));
        }

        // applyAndTestCombo variables for reuse
        final Set<String> appearingOutputs = new HashSet<>();
        final Set<String> disappearingOutputs = new HashSet<>();

        final Consumer<List<String>> applyAndTestCombo = condCombo -> {
            testMarket1.getConditions().clear();
            testMarket2.getConditions().clear();

            try {
                for (String condID : condCombo) {
                    testMarket1.addCondition(condID);
                    testMarket2.addCondition(condID);
                }
            } catch (Exception e) {
                for (String condID : condCombo) {
                    testMarket1.removeCondition(condID);
                    testMarket2.removeCondition(condID);
                }
                return;
            }
            
            testMarket1.reapplyConditions();
            testMarket2.reapplyConditions();
            testMarket1.reapplyIndustries();
            testMarket2.reapplyIndustries();

            for (Map.Entry<String, IndustryConfig> entry : dynamic_config.entrySet()) {
                final String indID = entry.getKey();
                final IndustryConfig config = entry.getValue();
                final Industry ind1 = testMarket1.getIndustry(indID);
                final Industry ind2 = testMarket2.getIndustry(indID);

                final Set<String> currentOutputs = new HashSet<>();
                final Set<String> baselineOutputs = new HashSet<>(config.outputs.keySet());
                final Map<String, OutputConfig> new_outputs = new_dynamic_config.get(indID).outputs;

                for (MutableCommodityQuantity mutable : ind1.getAllSupply()) {
                    currentOutputs.add(mutable.getCommodityId());
                }
                
                scaleWithMarketSize.clear();
                for (String comID : appearingOutputs) {
                    boolean scaleWithSize = Math.abs(ind1.getSupply(comID).getQuantity().getModifiedValue() -
                        ind2.getSupply(comID).getQuantity().getModifiedValue()
                    ) > 0.01f;

                    if (scaleWithSize) scaleWithMarketSize.add(comID);
                }

                final Map<String, Float> inputs;
                if (inputCache.containsKey(indID)) {
                    inputs = inputCache.get(indID);
                } else {
                    inputs = new HashMap<>(6);
                    populateInputs(ind1, inputs, scaleWithMarketSize);
                    inputCache.put(indID, inputs);
                }

                final Map<String, Float> CCMoneyDist = config.workerAssignable ? new HashMap<>() : null;
                if (config.workerAssignable) {
                    for (Map.Entry<String, Float> dist : inputs.entrySet()) {
                        CCMoneyDist.put(dist.getKey(), 1f);
                    }
                }

                final Map<String, Float> InputsPerUnitOutput = !config.workerAssignable ? new HashMap<>() : null;
                if (!config.workerAssignable) {
                    for (Map.Entry<String, Float> dist : inputs.entrySet()) {
                        InputsPerUnitOutput.put(dist.getKey(), dist.getValue());
                    }
                }

                appearingOutputs.clear();
                disappearingOutputs.clear();

                baselineOutputs.remove(abstractOutput);

                appearingOutputs.addAll(currentOutputs);
                appearingOutputs.removeAll(baselineOutputs);

                disappearingOutputs.addAll(baselineOutputs);
                disappearingOutputs.removeAll(currentOutputs);

                if (!appearingOutputs.isEmpty()) new_outputs.remove(abstractOutput);

                for (String newOutput : appearingOutputs) {
                    final boolean isAbstract = settings.getCommoditySpec(newOutput) == null;
                    final OutputConfig optCom = new OutputConfig(
                        newOutput,
                        1,
                        CCMoneyDist,
                        scaleWithMarketSize.contains(newOutput),
                        config.workerAssignable && !isAbstract,
                        isAbstract,
                        false,
                        emptyList,
                        condCombo,
                        InputsPerUnitOutput,
                        LaborConfig.dynamicWorkerCapPerOutput,
                        8f,
                        -1
                    );
                    new_outputs.put(newOutput, optCom);
                }

                for (String missingOutput : disappearingOutputs) {
                    final OutputConfig outputCom = new_outputs.get(missingOutput);
                    if (outputCom.ifMarketCondsAllFalse == null || outputCom.ifMarketCondsAllFalse.isEmpty()) {
                        outputCom.ifMarketCondsAllFalse = condCombo;
                    }
                }
            }
        };

        // Test single conditions
        for (int i = 0; i < conds.size(); i++) {
            applyAndTestCombo.accept(Collections.singletonList(conds.get(i)));
        }

        // Test pairs
        for (int i = 0; i < conds.size(); i++) {
            for (int j = i + 1; j < conds.size(); j++) {
                applyAndTestCombo.accept(Arrays.asList(conds.get(i), conds.get(j)));
            }
        }

        // Test triplets
        // for (int i = 0; i < conds.size(); i++) {
        //     for (int j = i + 1; j < conds.size(); j++) {
        //         for (int k = j + 1; k < conds.size(); k++) {
        //             applyAndTestCombo.accept(Arrays.asList(conds.get(i), conds.get(j), conds.get(k)));
        //         }
        //     }
        // }
    
        ind_config.putAll(new_dynamic_config);

        final JSONObject json = IndustryConfigManager.serializeIndustryConfigs(new_dynamic_config);

        try {
            settings.writeJSONToCommon(
                IndustryConfigManager.DYNAMIC_CONFIG_NAME,
                json,
                false
            );
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to write dynamic industry configuration to common JSON file '"
                + IndustryConfigManager.DYNAMIC_CONFIG_NAME, e
            );
        }
    
        // CLEANUP
        testStarSystem.removeEntity(testPlanet);
        sector.removeStarSystem(testStarSystem);
    }

    private static final void populateInputs(
        Industry ind, Map<String, Float> inputs, Set<String> scaleWithMarketSize
    ) {
        ind.getAllDemand().forEach(mutable -> {
            MutableStat base = mutable.getQuantity();
            StatMod baseMod = null;
            float cumulativeBase = 0f;

            for (StatMod mod : base.getFlatMods().values()) {
                if (mod.source.endsWith(CompatLayer.BASE_MOD_SUFFIX) && mod.value > 0) {
                    baseMod = baseMod == null ? mod : baseMod;
    
                } else {
                    if (!mod.source.equals(CompatLayer.DEMAND_RED_MOD) && 
                        !mod.source.endsWith(CompatLayer.MARKET_COND_MOD_SUFFIX) &&
                        mod.value >= 0
                    ) {
                        cumulativeBase += mod.value;
                    }
                }
            }

            float value = (baseMod != null ? baseMod.value : cumulativeBase);
            if (!scaleWithMarketSize.isEmpty()) {
                value = value - (TEST_MARKET_SIZE - 3);
                if (value <= 1f) {
                    value = (float) Math.pow(10f, -1f * (1f - value)); 
                }
            }
            inputs.put(mutable.getCommodityId(), value);
        });
    }
}
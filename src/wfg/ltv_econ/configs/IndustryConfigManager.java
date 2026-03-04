package wfg.ltv_econ.configs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.util.ArrayMap;
import wfg.ltv_econ.util.ConfigUtils;

import java.util.List;

import static wfg.ltv_econ.constants.Mods.LTV_ECON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IndustryConfigManager {
    private static final SettingsAPI settings = Global.getSettings();

    public static final String BASE_MAINTENANCE_ID = "maintenance_base";

    public static final String CONFIG_PATH = "./data/config/ltvEcon/industry_config.json";
    public static final String CONFIG_NAME = "industry_config.json";
    public static final String DYNAMIC_CONFIG_PATH = "./saves/common/dynamic_industry_config.json";
    public static final String DYNAMIC_CONFIG_NAME = "dynamic_industry_config.json";
    public static final float dynamicIndMarketScaleBase = 6f;

    private static JSONObject config;
    private static JSONObject dynamic_config;

    public static Map<String, IndustryConfig> ind_config;

    static {
        ind_config = IndustryConfigManager.loadAsMap(false);

        validateOrRebuildDynamicConfigs();
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
                Global.getLogger(IndustryConfigManager.class).info(
                    "Dynamic industry config missing or empty. Creating new JSONObject."
                );
                dynamic_config = new JSONObject();
            }
        } catch (Exception ex) {
            Global.getLogger(IndustryConfigManager.class).warn(
                "Failed to read dynamic industry config, creating new JSONObject: " +
                DYNAMIC_CONFIG_PATH + ".data"
            );
            dynamic_config = new JSONObject();
        }
    }

    public static final JSONObject getConfig(boolean dynamicConfig) {
        if (config == null || dynamic_config == null) load();
        return dynamicConfig ? dynamic_config : config;
    }

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
    public static final Map<String, IndustryConfig> loadAsMap(boolean dynamicConfig) {
        final JSONObject root = getConfig(dynamicConfig);
        final Map<String, IndustryConfig> result = new HashMap<>();

        try { if (root.has("industryList")) {

        final JSONArray industries = root.getJSONArray("industryList");
        for (int indIdx = 0; indIdx < industries.length(); indIdx++) {
            final JSONObject indJson = industries.getJSONObject(indIdx);

            final String indID = indJson.getString("industryId");
            final boolean workerAssignable = indJson.optBoolean("workerAssignable", false);
            final boolean ignoreLocalStockpiles = indJson.optBoolean("ignoreLocalStockpiles", false);
            final String occTag = indJson.optString("occTag", LaborConfigLoader.AVERAGE_OCC_TAG);

            final JSONObject outputList = indJson.getJSONObject("outputList");
            final Map<String, OutputConfig> commodityMap = new ArrayMap<>();

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

                final Map<String, Float> ConsumptionMap = new ArrayMap<>();
                if (outputData.has("InputsPerUnitOutput")) {
                    JSONObject consumption = outputData.getJSONObject("InputsPerUnitOutput");
                    Iterator<String> inputIds = consumption.keys();
                    while (inputIds.hasNext()) {
                        String inputId = inputIds.next();
                        float weight = (float) consumption.getDouble(inputId);
                        ConsumptionMap.put(inputId, weight);
                    }
                }

                final Map<String, Float> CCMoneyDist = new ArrayMap<>();
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
                workerAssignable, commodityMap, occTag, ignoreLocalStockpiles
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

    public static final JSONObject serializeIndustryConfigs(Map<String, IndustryConfig> configs) {
        final JSONObject root = new JSONObject();

        try {
        root.put("modVersion", settings.getModManager().getModSpec(LTV_ECON).getVersion());

        final List<JSONObject> industries = new ArrayList<>();
        for (Map.Entry<String, IndustryConfig> entry : configs.entrySet()) {
            final String indID = entry.getKey();
            final IndustryConfig ind = entry.getValue();

            final JSONObject indJson = new JSONObject();
            indJson.put("industryId", indID);
            indJson.put("workerAssignable", ind.workerAssignable);
            indJson.put("ignoreLocalStockpiles", ind.ignoreLocalStockpiles);
            indJson.put("occTag", ind.occTag);

            final JSONObject outputMap = new JSONObject();
            for (Map.Entry<String, OutputConfig> outputEntry : ind.outputs.entrySet()) {
                final String outputId = outputEntry.getKey();
                final OutputConfig opt = outputEntry.getValue();

                final JSONObject optJson = new JSONObject();
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

    public static class IndustryConfig {
        public final boolean workerAssignable;
        public final boolean ignoreLocalStockpiles;
        public final String occTag;
        public final Map<String, OutputConfig> outputs;

        public boolean dynamic = false;

        public IndustryConfig(boolean workerAssignable, Map<String, OutputConfig> outputs, String occTag,
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
                Map<String, OutputConfig> copy = new ArrayMap<>();
                for (Map.Entry<String, OutputConfig> e : config.outputs.entrySet()) {
                    copy.put(e.getKey(), new OutputConfig(e.getValue()));
                }
                this.outputs = copy;
            } else {
                this.outputs = new ArrayMap<>();
            }
        }

        @Override
        public final String toString() {
            return '{' + " ,\n"
                + "workerAssignable: " + workerAssignable + " ,\n"
                + "occTag: " + occTag + " ,\n"
                + "ignoreLocalStockpiles: " + ignoreLocalStockpiles + " ,\n"
                + "dynamicConfig: " + dynamic + " ,\n"
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

        public final Map<String, Float> CCMoneyDist; // Determines the share of money spent on each input.
        public final Map<String, Float> InputsPerUnitOutput;

        public List<String> ifMarketCondsAllFalse;
        public List<String> ifMarketCondsAllTrue;

        public final boolean scaleWithMarketSize; // Base size where no scaling happens is 3.
        public final boolean usesWorkers;
        public final boolean isAbstract; // Abstract outputs have no output, only inputs.
        public final boolean checkLegality;
        public final boolean activeDuringBuilding; // will be inactive during normal operations.

        private static final BooleanSupplier dynamicOutputActiveDefault = () -> true;
        public BooleanSupplier dynamicOutputActive = dynamicOutputActiveDefault;
        public boolean dynamic = false;

        public OutputConfig(
            String comID, float baseProd, Map<String, Float> CCMoneyDist, boolean scaleWithMarketSize,
            boolean usesWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketCondsAllFalse,
            List<String> ifMarketCondsAllTrue, Map<String, Float> InputsPerUnitOutput, float workerAssignableLimit,
            float marketScaleBase, long target, boolean activeDuringBuilding
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
            this.activeDuringBuilding = activeDuringBuilding;
        }

        /**
         * Copy Constructor
         */
        public OutputConfig(OutputConfig other) {
            this.comID = other.comID;
            this.baseProd = other.baseProd;
            this.target = other.target;

            this.CCMoneyDist = (other.CCMoneyDist == null) ? null : new ArrayMap<>(other.CCMoneyDist);
            this.InputsPerUnitOutput = (other.InputsPerUnitOutput == null) ? null : new ArrayMap<>(other.InputsPerUnitOutput);

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
            this.activeDuringBuilding = other.activeDuringBuilding;
        }

        @Override
        public final String toString() {
            return '{' +  " ,\n" +
                "baseProd=" + baseProd + " ,\n" +
                "target=" + target + " ,\n" +
                "CCMoneyDist=" + CCMoneyDist + " ,\n" +
                "ConsumptionMap=" + InputsPerUnitOutput + " ,\n" +
                "ifMarketCondsAllFalse=" + ifMarketCondsAllFalse + " ,\n" +
                "ifMarketCondsAllTrue=" + ifMarketCondsAllTrue + " ,\n" +
                "scaleWithMarketSize=" + scaleWithMarketSize + " ,\n" +
                "marketScaleBase=" + marketScaleBase + " ,\n" +
                "usesWorkers=" + usesWorkers + " ,\n" +
                "workerAssignableLimit: " + workerAssignableLimit + " ,\n" +
                "isAbstract=" + isAbstract + " ,\n" +
                "checkLegality=" + checkLegality + " ,\n" +
                "activeDuringBuilding=" + activeDuringBuilding + " ,\n" +
                "dynamicOutputActive=" + dynamicOutputActive.getAsBoolean() + " ,\n" +
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
        final Map<String, IndustryConfig> dynamic_config =
            IndustryConfigManager.loadAsMap(true);
        final Set<String> validIndustryIds = settings.getAllIndustrySpecs().stream()
            .map(IndustrySpecAPI::getId).collect(Collectors.toSet());

        boolean allIndustriesHaveConfig = true;
        final boolean current = settings.getModManager().getModSpec(LTV_ECON).getVersion()
            .equals(getDynamicConfigVersion());

        // 1) Check that every existing spec has a config
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            String id = spec.getId();
            String baseId = IndustryIOs.getBaseIndustryID(spec);

            if (!(dynamic_config.containsKey(id) ||
                (baseId != null && dynamic_config.containsKey(baseId))) &&
                !IndustryIOs.hasConfig(spec)
            ) {
                allIndustriesHaveConfig = false;
                break;
            }
        }

        // 2) Check that dynamic configs don’t reference missing industries
        if (allIndustriesHaveConfig) {
            for (String cfgId : dynamic_config.keySet()) {
                if (!validIndustryIds.contains(cfgId)) {
                    allIndustriesHaveConfig = false;
                    break;
                }
            }
        }

        if (allIndustriesHaveConfig && current) {
            ind_config.putAll(dynamic_config);
            return;
        }

        dynamic_config.clear();

        final String abstractOutput = "outputForAbstractInputs";
        final SectorAPI sector = Global.getSector();
        final FactionAPI testFaction = sector.getFaction(ConfigUtils.TEST_FACTION_ID);
        final MarketAPI testMarket1 = ConfigUtils.getTestMarket1();
        final MarketAPI testMarket2 = ConfigUtils.getTestMarket2();

        final Set<String> scaleWithMarketSize = new HashSet<>(8);

        // Make every commodity illegal to observe industry behaviour
        for (CommoditySpecAPI spec : settings.getAllCommoditySpecs()) {
            if (spec.isNonEcon()) continue;

            testFaction.makeCommodityIllegal(spec.getId());
        }

        for (IndustrySpecAPI indSpec : settings.getAllIndustrySpecs()) { 
            if (IndustryIOs.getIndConfig(indSpec) != null) continue;

            final String indID = indSpec.getId();
            final Map<String, OutputConfig> configOutputs = new ArrayMap<>();
            
            testMarket1.addIndustry(indID);
            testMarket2.addIndustry(indID);

            Industry ind1 = testMarket1.getIndustry(indID);
            Industry ind2 = testMarket2.getIndustry(indID);

            if (ind1 == null || ind2 == null) {
                for (Industry ind : testMarket1.getIndustries()) {
                    if (ind.getSpec().getId().equals(indID)) {
                        ind1 = ind; break;
                    }
                }
                for (Industry ind : testMarket2.getIndustries()) {
                    if (ind.getSpec().getId().equals(indID)) {
                        ind2 = ind; break;
                    }
                }
            }
            
            if (ind1 != null && ind2 != null) {
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
                    final String comID = mutable.getCommodityId();
                    final boolean scaleWithSize = Math.abs(
                        ind1.getSupply(comID).getQuantity().getModifiedValue() -
                        ind2.getSupply(comID).getQuantity().getModifiedValue()
                    ) > 0.01f;
    
                    if (scaleWithSize) scaleWithMarketSize.add(comID);
                }
    
                final boolean hasNoRealOutputs = outputs.isEmpty() && illegalOutputs.isEmpty();
                final boolean usesWorkers = EconomyInfo.isWorkerAssignable(ind1) && !hasNoRealOutputs;
                if (hasNoRealOutputs) {
                    outputs.add(abstractOutput);
    
                    final Optional<MutableCommodityQuantity> firstDemand = ind1.getAllDemand()
                        .stream().findFirst();
                    if (firstDemand.isPresent()) {
                        final String comID = firstDemand.get().getCommodityId();
                        final boolean scaleWithSize = Math.abs(
                            ind1.getDemand(comID).getQuantity().getModifiedValue() -
                            ind2.getDemand(comID).getQuantity().getModifiedValue()
                        ) > 0.01f;
                        if (scaleWithSize) scaleWithMarketSize.add(abstractOutput);
                    }
                }
    
                // In vanilla, each output uses each input
                final Map<String, Float> inputs = new ArrayMap<>(4);
                populateInputs(ind1, inputs, !scaleWithMarketSize.isEmpty());
    
                final Map<String, Float> CCMoneyDist = usesWorkers ?
                    inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> 1f)) : null;
    
                final Map<String, Float> InputsPerUnitOutput = !usesWorkers ?
                    inputs.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                    ) : null;
    
                final Consumer<String> addOutput = (outputID) -> {
                    final boolean isIllegal = illegalOutputs.contains(outputID);
                    final boolean isAbstract = !EconomyConstants.econCommodityIDs.contains(outputID);
    
                    final OutputConfig optCom = new OutputConfig(
                        outputID, 1, CCMoneyDist,
                        scaleWithMarketSize.contains(outputID),
                        usesWorkers, isAbstract, isIllegal,
                        Collections.emptyList(), Collections.emptyList(),
                        InputsPerUnitOutput, LaborConfig.dynamicWorkerCapPerOutput,
                        dynamicIndMarketScaleBase, -1, false
                    );
                    configOutputs.put(outputID, optCom);
                };
    
                outputs.forEach(addOutput);
                illegalOutputs.forEach(addOutput);
    
                final IndustryConfig config = new IndustryConfig(
                    usesWorkers, configOutputs, LaborConfigLoader.AVERAGE_OCC_TAG, false
                );
                config.dynamic = true;
    
                dynamic_config.put(indID, config);

            } else {
                final IndustryConfig config = new IndustryConfig(
                    false, configOutputs, LaborConfigLoader.AVERAGE_OCC_TAG, false
                );
                config.dynamic = true;
    
                dynamic_config.put(indID, config);
            }
        
            final Iterator<Industry> indRemoveIterator1 = testMarket1.getIndustries().iterator();
            while (indRemoveIterator1.hasNext()) {
                final Industry industry = indRemoveIterator1.next();
                // industry.notifyBeingRemoved(null, false);
                industry.unapply();
                indRemoveIterator1.remove();
            }

            final Iterator<Industry> indRemoveIterator2 = testMarket2.getIndustries().iterator();
            while (indRemoveIterator2.hasNext()) {
                final Industry industry = indRemoveIterator2.next();
                // industry.notifyBeingRemoved(null, false);
                industry.unapply();
                indRemoveIterator2.remove();
            }
        }
    
        ind_config.putAll(dynamic_config);

        final JSONObject json = IndustryConfigManager.serializeIndustryConfigs(dynamic_config);

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
    }

    public static final void populateInputs(final Industry ind, final Map<String, Float> inputs,
        boolean scaleWithMarketSize
    ) {
        ind.getAllDemand().forEach(mutable -> {
            inputs.put(mutable.getCommodityId(), populateInput(mutable.getQuantity(), scaleWithMarketSize));
        });
    }

    public static final float populateInput(final MutableStat base, boolean scaleWithMarketSize) {
        StatMod baseMod = null;
        float cumulativeBase = 0f;

        for (StatMod mod : base.getFlatMods().values()) {
            if (mod.source.endsWith(CompatLayer.BASE_MOD_SUFFIX) && mod.value > 0) {
                baseMod = baseMod == null ? mod : baseMod;

            } else {
                if (!mod.source.equals(CompatLayer.DEMAND_RED_MOD) && 
                    !mod.source.endsWith(CompatLayer.MARKET_COND_MOD_SUFFIX) &&
                    mod.value >= 0
                ) { cumulativeBase += mod.value; }
            }
        }

        // Since vanilla values are discrete integers
        final int vanillaValue = Math.round(baseMod != null ? baseMod.value : cumulativeBase);
        final double expBase = 2;

        float value = vanillaValue;
        if (scaleWithMarketSize) {
            value = value - (ConfigUtils.TEST_MARKET_SIZE - 3);
            final float zeroCount = Math.max(0f, value) - 1f;
            if (value <= 1f) {
                value = (float) Math.pow(10f, -1f * (1f - value)); 
            }
            value = value * (float) Math.max(1f, Math.pow(expBase, zeroCount));
        } else {
            value *= Math.pow(expBase, Math.max(0, vanillaValue - 1));
        }
        return value;
    }
}
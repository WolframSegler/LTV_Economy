package wfg.ltv_econ.industry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.campaign.econ.Market;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.configs.IndustryConfigManager;
import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.configs.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;

/**
 * <h3>IndustryIOs</h3>
 * 
 * <p>This class serves as a precomputed wrapper around <code>IndustryConfig</code> data. 
 * It stores base production values and input requirements for all industries, 
 * allowing fast, reusable access without recalculating values each game cycle.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Pre-calculated mapping of industry outputs: 
 *       <code>Map&lt;industryID, Map&lt;outputID, baseOutput&gt;&gt;</code></li>
 *   <li>Pre-calculated mapping of inputs per output: 
 *       <code>Map&lt;industryID, Map&lt;outputID, Map&lt;inputID, baseInput&gt;&gt;&gt;</code></li>
 *   <li>Reverse lookup table from inputs to dependent outputs: 
 *       <code>Map&lt;inputID, List&lt;outputID&gt;&gt;</code></li>
 *   <li>Singleton-style access for global usage across the mod</li>
 *   <li>Getter methods that respect market conditions, legality, and market size scaling</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <ol>
 *   <li>Query output values using <code>getOutput(Industry, MarketAPI, String)</code>.</li>
 *   <li>Query input requirements using <code>getInput(Industry, MarketAPI, String, String)</code> or 
 *       <code>getInput(Industry, MarketAPI, String)</code> for cumulative input demand.</li>
 * </ol>
 */
public class IndustryIOs {
    private static final Object MARKET_SIZE_FIELD = RolfLectionUtil.getFieldByName("size", Market.class);
    private static final SettingsAPI settings = Global.getSettings();

    /**
     * Map(industryID, Map(outputID, baseOutput))
     */
    private static final Map<String, Map<String, Float>> baseOutputs = new HashMap<>(); 

    /**
     * <code>Map(industryID, Map(outputID, Map(inputID, baseInput)))</code>
     */
    private static final Map<String, Map<String, Map<String, Float>>> baseInputs = new HashMap<>();

    /**
     * Map(inputID, List(outputID))
     */
    private static final Map<String, List<String>> inputToOutput = new HashMap<>();

    // Map<commodityID, Set<industryID>>
    private static final Map<String, Set<String>> inputToInd = new HashMap<>();
    private static final Map<String, Set<String>> outputToInd = new HashMap<>();

    private static final Map<String, String> IndToBaseInd = new HashMap<>();

    public static final String ABSTRACT_COM = "abstract";
    public static final String DYNAMIC_OUTPUT = "output_for_input::";

    private IndustryIOs() {}
    static {
        buildBaseIdMapping();

        ConfigInputOutputMaps();

        buildInputToOutput();

        buildInputOutputToIndustries();
    }

    private static final void ConfigInputOutputMaps() {
        for (Map.Entry<String, IndustryConfig> entry : IndustryConfigManager.ind_config.entrySet()) {
            final Map<String, Float> outputMap = baseOutputs.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            final Map<String, Map<String, Float>> inputOuterMap = baseInputs.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            final Map<String, OutputConfig> outputs = entry.getValue().outputs;

            for (Map.Entry<String, OutputConfig> outputEntry : outputs.entrySet()) {
                fillOutputMaps(entry.getValue().occTag, outputEntry.getValue(), outputMap, inputOuterMap);
            }
        }
    }

    private static final void fillOutputMaps(OCCTag occTag, OutputConfig output,
        Map<String, Float> outputMap, Map<String, Map<String, Float>> inputOuterMap
    ) {
        final String outputID = output.comID;
        final Map<String, Float> inputMap = inputOuterMap.computeIfAbsent(outputID, k -> new HashMap<>());

        float base = output.baseProd;

        if (output.usesWorkers && !output.isAbstract) {
            base /= EconomyInfo.getWorkersPerUnit(outputID, occTag);
        }

        if (output.CCMoneyDist != null && !output.CCMoneyDist.isEmpty() && !output.isAbstract) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(outputID);
            final float Vcc = spec.getBasePrice() * LaborConfig.getRoCC(occTag);

            float totalWeight = 0;
            for (float weight : output.CCMoneyDist.values()) totalWeight += weight;

            boolean hasAbstractInput = false;
            for (Map.Entry<String, Float> inputEntry : output.CCMoneyDist.entrySet()) {
                final String inputID = inputEntry.getKey();
                if (inputID.equals(ABSTRACT_COM)) {
                    hasAbstractInput = true;
                    continue;
                }
                final float weight = inputEntry.getValue() / totalWeight;
                final float inputValue = Vcc * weight;
                final float unitPrice = settings.getCommoditySpec(inputID).getBasePrice();
                final float qty = inputValue * base / unitPrice;
                inputMap.put(inputID, qty);
            }

            if (hasAbstractInput) {
                float realUnits = 0;
                for (float w : inputMap.values()) realUnits += w;
                final float abs_weight = output.CCMoneyDist.get(ABSTRACT_COM);
                final float value = abs_weight * realUnits / (totalWeight - abs_weight);
                inputMap.put(ABSTRACT_COM, value);
            }

        } else if (output.InputsPerUnitOutput != null && !output.InputsPerUnitOutput.isEmpty()) {
            for (Map.Entry<String, Float> demandEntry : output.InputsPerUnitOutput.entrySet()) {
                final String inputID = demandEntry.getKey();
                if (inputID.equals(ABSTRACT_COM)) continue;
                inputMap.put(inputID, demandEntry.getValue() * base);
            }
        }

        outputMap.put(outputID, base);
    }

    private static final void buildInputToOutput() {
        inputToOutput.clear();

        for (Map.Entry<String, Map<String, Map<String, Float>>> indEntry : baseInputs.entrySet()) {
            for (Map.Entry<String, Map<String, Float>> outputEntry : indEntry.getValue().entrySet()) {
                String outputID = outputEntry.getKey();
                Map<String, Float> inputs = outputEntry.getValue();

                for (String inputID : inputs.keySet()) {
                    inputToOutput
                        .computeIfAbsent(inputID, k -> new ArrayList<>())
                        .add(outputID);
                }
            }
        }
    }

    private static final void buildInputOutputToIndustries() {
        outputToInd.clear();
        inputToInd.clear();

        for (Map.Entry<String, IndustryConfig> e : IndustryConfigManager.ind_config.entrySet()) {
            String indID = e.getKey();
            IndustryConfig cfg = e.getValue();

            for (String outputID : cfg.outputs.keySet()) {
                outputToInd.computeIfAbsent(outputID, k -> new HashSet<>()).add(indID);

                Map<String, Float> inputs = baseInputs.get(indID).get(outputID);
                if (inputs != null) {
                    for (String inputID : inputs.keySet()) {
                        inputToInd.computeIfAbsent(inputID, k -> new HashSet<>()).add(indID);
                    }
                }
            }
        }
    }

    private static final void buildBaseIdMapping() {
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            IndToBaseInd.put(spec.getId(), getBaseIndustryIDSpec(spec));
        }
    }

    private static final String getBaseIndustryIDSpec(IndustrySpecAPI ind) {
        IndustrySpecAPI currentInd = ind;

        while (true) {
            String downgradeId = currentInd.getDowngrade();
            if (downgradeId == null || downgradeId.equals(currentInd.getId())) break;

            currentInd = settings.getIndustrySpec(downgradeId);
        }

        return currentInd.getId();
    }

    /**
     * @return <ul>
     *             <li>{@code 0} – success</li>
     *             <li>{@code 1} – industry is {@code null}</li>
     *             <li>{@code 2} – industry does not have a config</li>
     *             <li>{@code 3} – output entry already exists</li>
     *             <li>{@code 4} – output is abstract</li>
     *             <li>{@code 5} – industry supply list does not have output</li>
     *         </ul>
     */
    public static final int createAndRegisterDynamicOutput(final Industry ind, String outputID,
        boolean indHasSupply
    ) {
        if (ind == null) return 1;
        final IndustryConfig cfg = getIndConfig(ind);
        if (cfg == null) return 2;
        if (cfg.outputs.containsKey(outputID)) return 3;
        if (Global.getSettings().getCommoditySpec(outputID) == null) return 4;
        final MutableStat output = ind.getSupply(outputID).getQuantity();
        if (indHasSupply && output.getModifiedValue() < 0.01f) return 5;

        final boolean scaleWithSize;
        if (indHasSupply) {
            final MarketAPI market = ind.getMarket();
            final float value_org = output.getModifiedValue();

            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, market.getSize() + 1);
            ind.reapply();
            final float value_new = output.getModifiedValue();
            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, market.getSize() - 1);
            ind.reapply();

            scaleWithSize = Math.abs(value_new - value_org) > 0.01f;
        } else {
            scaleWithSize = false;
        }

        final boolean useWorkers = cfg.workerAssignable;

        final Map<String, Float> inputs = new HashMap<>(6);
            IndustryConfigManager.populateInputs(ind, inputs, scaleWithSize);

            final Map<String, Float> CCMoneyDist = useWorkers ?
                inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> 1f)) : null;

            final Map<String, Float> InputsPerUnitOutput = !useWorkers ?
                inputs.entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ) : null;

        final OutputConfig outputConfig = new OutputConfig(
            outputID, 1, CCMoneyDist,
            scaleWithSize, useWorkers, false, false,
            Collections.emptyList(), Collections.emptyList(),
            InputsPerUnitOutput, LaborConfig.dynamicWorkerCapPerOutput,
            IndustryConfigManager.dynamicIndMarketScaleBase, -1, false
        );
        outputConfig.dynamic = true;
        outputConfig.dynamicOutputActive = () -> {
            return output.getModifiedValue() > 0.01f;
        };

        cfg.outputs.put(outputID, outputConfig);

        final String indID = getBaseIndIDifNoConfig(ind.getSpec());
        final Map<String, Float> outputMap = baseOutputs.get(indID);
        final Map<String, Map<String, Float>> inputOuterMap = baseInputs.get(indID);
        fillOutputMaps(cfg.occTag, outputConfig, outputMap, inputOuterMap);

        buildInputToOutput();
        buildInputOutputToIndustries();

        return 0;
    }

    /**
     * @return <ul>
     *             <li>{@code 0} – success</li>
     *             <li>{@code 1} – industry is {@code null}</li>
     *             <li>{@code 2} – industry does not have a config</li>
     *             <li>{@code 3} – input entry already exists</li>
     *             <li>{@code 4} – input is abstract</li>
     *             <li>{@code 5} – industry demand list does not have input</li>
     *         </ul>
     */
    public static final int createAndRegisterDynamicInput(final Industry ind, String inputID,
        boolean indHasDemand
    ) {
        if (ind == null) return 1;
        final IndustryConfig cfg = getIndConfig(ind);
        if (cfg == null) return 2;
        if (hasInput(ind, inputID)) return 3;
        if (Global.getSettings().getCommoditySpec(inputID) == null) return 4;
        final MutableStat input = ind.getDemand(inputID).getQuantity();
        if (indHasDemand && input.getModifiedValue() < 0.01f) return 5;

        final boolean scaleWithSize;
        if (indHasDemand) {
            final MarketAPI market = ind.getMarket();
            final float value_org = input.getModifiedValue();

            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, market.getSize() + 1);
            ind.reapply();
            final float value_new = input.getModifiedValue();
            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, market.getSize() - 1);
            ind.reapply();

            scaleWithSize = Math.abs(value_new - value_org) > 0.01f;
        } else {
            scaleWithSize = false;
        }

        final String outputID = DYNAMIC_OUTPUT + inputID;
        final OutputConfig output = new OutputConfig(
            outputID, 1, null,
            scaleWithSize, false, true, false,
            Collections.emptyList(), Collections.emptyList(),
            new HashMap<>(), LaborConfig.dynamicWorkerCapPerOutput,
            IndustryConfigManager.dynamicIndMarketScaleBase, -1, false
        );
        output.dynamic = true;
        output.dynamicOutputActive = () -> {
            return input.getModifiedValue() > 0.01f;
        };

        cfg.outputs.put(outputID, output);

        final float inputValue = IndustryConfigManager.populateInput(input, scaleWithSize);
        output.InputsPerUnitOutput.put(inputID, inputValue);

        final String indID = getBaseIndIDifNoConfig(ind.getSpec());
        final Map<String, Float> outputMap = baseOutputs.get(indID);
        final Map<String, Map<String, Float>> inputOuterMap = baseInputs.get(indID);
        fillOutputMaps(cfg.occTag, output, outputMap, inputOuterMap);

        buildInputToOutput();
        buildInputOutputToIndustries();

        return 0;
    }

    public static final boolean isOutputValidForMarket(final OutputConfig output, Industry ind) {
        final MarketAPI market = ind.getMarket();
        if (output.checkLegality && market.isIllegal(output.comID)) return false;
        if (output.dynamic && !output.dynamicOutputActive.getAsBoolean()) return false;

        if (output == null || ind.isDisrupted() ||
            (ind.isFunctional() && output.activeDuringBuilding)
        ) return false;

        if (output.activeDuringBuilding && !ind.isBuilding() &&
            !ind.getId().contains(Industries.POPULATION)
        ) return false;

        for (String cond : output.ifMarketCondsAllFalse) {
            if (market.hasCondition(cond)) return false;
        }

        for (String cond : output.ifMarketCondsAllTrue) {
            if (!market.hasCondition(cond)) return false;
        }

        return true;
    }

    public static final float calculateScale(OutputConfig output, Industry ind) {
        final MarketAPI market = ind.getMarket();
        if (!output.isAbstract) {
            final CommodityCell cell = EconomyEngine.getInstance().getComCell(output.comID, market.getId()); 
            if (cell != null && output.target > 0 && output.target < cell.getStored()) return 0f;
        }

        float scale = 1f;

        if (output.usesWorkers && !output.isAbstract) {
            final WorkerIndustryData data = WorkerRegistry.getInstance().getData(ind);
            if (data != null) {
                scale *= data.getAssignedForOutput(output.comID);
            }
        }

        if (output.scaleWithMarketSize) scale *= Math.pow(output.marketScaleBase, market.getSize() - 3);

        return scale;
    }

    /**
     * Get the output of an industry in a given market.
     * Returns 0 if the output does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getRealOutput(Industry ind, String outputID) {
        final IndustryConfig cfg = getIndConfig(ind);
        final OutputConfig output = cfg.outputs.get(outputID);

        if (output == null || output.isAbstract) return 0f;
        final float value = getBaseOutput(ind.getSpec(), outputID);
        if (value == 0) return 0f;

        if (!isOutputValidForMarket(output, ind)) return 0f;

        final float scale = calculateScale(output, ind);

        return value * scale;
    }

    /**
     * Get the input required by a specific output of an industry in a given market.
     * Returns 0 if the output or input does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getRealInput(Industry ind, String outputID, String inputID) {
        final IndustryConfig cfg = getIndConfig(ind);
        final OutputConfig output = cfg.outputs.get(outputID);

        final float value = getBaseInput(ind.getSpec(), outputID, inputID);
        if (value == 0f) return 0f;

        if (!isOutputValidForMarket(output, ind)) return 0f;

        final float scale = calculateScale(output, ind);

        return value * scale;
    }

    /**
     * Get the total demand of an industry for a given commodity across all outputs.
     */
    public static final float getRealSumInput(Industry ind, String inputID) {
        final Map<String, Map<String, Float>> indMap = getBaseInputs(ind.getSpec());
        final IndustryConfig cfg = getIndConfig(ind);

        float total = 0f;
        for (Map.Entry<String,Map<String,Float>> inputMap : indMap.entrySet()) {
            final OutputConfig output = cfg.outputs.get(inputMap.getKey());

            for (Map.Entry<String, Float> entry : inputMap.getValue().entrySet()) {
                if (entry.getKey().equals(inputID)) {
                    if (!isOutputValidForMarket(output, ind)) continue;

                    final float scale = calculateScale(output, ind);

                    total += entry.getValue() * scale;
                }
            }
        }
        return total;
    }

    /** 
     * Returns the modified output map for a given industry.
     */
    public static final Map<String, Float> getRealOutputs(Industry ind, boolean includeAbstract) {
        final Map<String, Float> outputs = getBaseOutputs(ind.getSpec());

        Map<String, Float> scaledOutputs = new HashMap<>();
        for (String output : outputs.keySet()) {
            float value = getRealOutput(ind, output);
            if (includeAbstract || value > 0) scaledOutputs.put(output, value);
        }

        return scaledOutputs;
    }

    /** 
     * Returns the modified inputs map for a given industry and output.
     */
    public static final Map<String, Float> getRealInputs(Industry ind, String outputID, boolean includeAbstract) {
        final Map<String, Map<String, Float>> outputs = getBaseInputs(ind.getSpec());

        Map<String, Float> scaledInputs = new HashMap<>();
        for (String output : outputs.get(outputID).keySet()) {
            float value = getRealInput(ind, outputID, output);
            if (includeAbstract || value > 0) scaledInputs.put(output, value);
        }

        return scaledInputs;
    }

    /**
     * Returns a list of all the inputs for a given industry.
     */
    public static final Set<String> getRealInputs(Industry ind, boolean includeAbstract) {
        if (ind == null || ind.getId() == null) return Collections.emptySet();

        final String indID = getBaseIndIDifNoConfig(ind.getSpec());

        final Map<String, Map<String, Float>> outputs = baseInputs.get(indID);
        if (outputs == null) return Collections.emptySet();

        final Set<String> inputSet = new HashSet<>();
        for (Map<String, Float> inputMap : outputs.values()) {
            for (String input : inputMap.keySet()) {
                if (ABSTRACT_COM.contains(input) && !includeAbstract) continue;
                inputSet.add(input);
            }
        }

        return inputSet;
    }

    /**
     * Get the pre-calculated base output of an industry for a given commodity.
     * Ignores market conditions, scaling, and legality.
     */
    public static final float getBaseOutput(IndustrySpecAPI ind, String outputID) {
        final Map<String, Float> indMap = baseOutputs.get(getBaseIndIDifNoConfig(ind));
        if (indMap == null) return 0f;
        return indMap.getOrDefault(outputID, 0f);
    }

    /**
     * Get the pre-calculated base input required by an industry for a specific output.
     * Ignores market context.
     */
    public static final float getBaseInput(IndustrySpecAPI ind, String outputID, String inputID) {
        final Map<String, Map<String, Float>> indMap = baseInputs.get(getBaseIndIDifNoConfig(ind));
        if (indMap == null) return 0f;
        final Map<String, Float> inputMap = indMap.get(outputID);
        if (inputMap == null) return 0f;
        return inputMap.getOrDefault(inputID, 0f);
    }

    /**
     * Get total pre-calculated demand for a specific input across all outputs.
     */
    public static final float getBaseSumInput(IndustrySpecAPI ind, String inputID) {
        final Map<String, Map<String, Float>> indMap = baseInputs.get(getBaseIndIDifNoConfig(ind));
        if (indMap == null) return 0f;

        float total = 0f;
        for (Map<String, Float> inputMap : indMap.values()) {
            total += inputMap.getOrDefault(inputID, 0f);
        }
        return total;
    }

    /**
     * Get the map of pre-calculated outputs for an industry.
     */
    public static final Map<String, Float> getBaseOutputs(IndustrySpecAPI ind) {
        final Map<String, Float> map = baseOutputs.get(getBaseIndIDifNoConfig(ind));
        return (map != null) ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }

    /**
     * Get the map of pre-calculated inputs for an industry.
     */
    public static final Map<String, Map<String, Float>> getBaseInputs(IndustrySpecAPI ind) {
        final Map<String, Map<String, Float>> map = baseInputs.get(getBaseIndIDifNoConfig(ind));
        return (map != null) ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }

    public static final Map<String, List<String>> getInputToOutput() {
        Map<String, List<String>> immutableMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : inputToOutput.entrySet()) {
            immutableMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(immutableMap);
    }

    /**
     * Checks whether a precomputed configuration exists for the given industry.
     */
    public static final boolean hasConfig(Industry ind) {
        return hasConfig(ind.getSpec());
    }

    public static final boolean hasConfig(IndustrySpecAPI ind) {
        return IndustryConfigManager.ind_config.containsKey(ind.getId()) 
            || IndustryConfigManager.ind_config.containsKey(getBaseIndustryID(ind.getId()));
    }

    public static final boolean hasOutput(Industry ind, String comID) {
        final String id = getBaseIndIDifNoConfig(ind.getSpec());
        return outputToInd.getOrDefault(comID, Collections.emptySet()).contains(id);
    }

    public static final boolean hasInput(Industry ind, String comID) {
        final String id = getBaseIndIDifNoConfig(ind.getSpec());
        return inputToInd.getOrDefault(comID, Collections.emptySet()).contains(id);
    }

    public static final IndustryConfig getIndConfig(Industry ind) {
        return getIndConfig(ind.getSpec());
    }

    public static final IndustryConfig getIndConfig(IndustrySpecAPI ind) {
        final IndustryConfig indConfig = IndustryConfigManager.ind_config.get(ind.getId());

        return indConfig != null ? indConfig :
            IndustryConfigManager.ind_config.get(getBaseIndustryID(ind.getId()));
    }

    public static String getBaseIndustryID(String id) {
        return IndToBaseInd.get(id);
    }

    public static String getBaseIndustryID(IndustrySpecAPI ind) {
        return getBaseIndustryID(ind.getId());
    }

    public static String getBaseIndustryID(Industry ind) {
        return getBaseIndustryID(ind.getId());
    }

    public static final String getBaseIndIDifNoConfig(IndustrySpecAPI ind) {
        if (IndustryConfigManager.ind_config.containsKey(ind.getId())) {
            return ind.getId();
        }
        return getBaseIndustryID(ind.getId());
    }

    /**
     * Retrieves the real Industry instance from a market that matches the given base ID.
     *
     * @param market the market to search for industries
     * @param baseID the base industry ID to match against
     * @return the Industry in the market whose base ID matches baseID
     * @throws IllegalStateException if no matching industry is found in the market
     */
    public static Industry getRealIndustryFromBaseID(MarketAPI market, String baseID) {
        return getRealIndustryFromBaseID(market, List.of(baseID));
    }

    /**
     * Retrieves the real Industry instance from a market that matches any of the given base IDs.
     *
     * @param market the market to search for industries
     * @param baseIDList a list of base industry IDs to match against
     * @return the first Industry in the market whose base ID is in baseIDList
     * @throws IllegalStateException if no matching industry is found in the market
     */
    public static Industry getRealIndustryFromBaseID(MarketAPI market, List<String> baseIDList) {
        for (Industry ind : market.getIndustries()) {
            final String realBaseID = IndustryIOs.getBaseIndIDifNoConfig(ind.getSpec());
            for (String baseID : baseIDList) {
                if (realBaseID.equals(baseID)) return ind;
            }
        }
        throw new IllegalStateException(
            "Expected market '" + market.getId() + "' to contain an industry with baseID '"
            + baseIDList.toString() + "', but none was found."
        );
    }

    /**
     * Do not modify the content of this map
     */
    public static final Map<String, Map<String, Float>> getBaseOutputsMap() {
        return Collections.unmodifiableMap(baseOutputs);
    }

    /**
     * Do not modify the content of this map
     */
    public static final Map<String, Map<String, Map<String, Float>>> getBaseInputsMap() {
        return Collections.unmodifiableMap(baseInputs);
    }

    public static final List<Industry> getIndustriesForOutput(String outputID, MarketAPI market) {
        return outputToInd.get(outputID).stream().map(market::getIndustry).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static final void logMaps() {
        Global.getLogger(IndustryIOs.class).info(
            "==== IndustryIOs Map Log ====" + "\n" +
            "baseOutputs" + "\n" +
            baseOutputs.toString() + "\n" +
            "--------------------------" + "\n" +
            "baseInputs" + "\n" +
            baseInputs.toString() + "\n" +
            "--------------------------" + "\n" +
            "inputsToInd" + "\n" +
            inputToInd.toString() + "\n" +
            "--------------------------" + "\n" +
            "outputsToInd" + "\n" +
            outputToInd.toString() + "\n" +
            "--------------------------" + "\n" +
            "IndToBaseInd" + "\n" +
            IndToBaseInd.toString()
        );
    }
}
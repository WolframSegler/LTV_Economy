package wfg.ltv_econ.industry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.EconomyEngine;
import wfg.ltv_econ.economy.IndustryConfigLoader;
import wfg.ltv_econ.economy.LaborConfigLoader;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.IndustryConfigLoader.IndustryConfig;
import wfg.ltv_econ.economy.IndustryConfigLoader.OutputCom;
import wfg.ltv_econ.economy.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;

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
    /**
     * Map(industryID, Map(outputID, baseOutput))
     */
    private static Map<String, Map<String, Float>> baseOutputs = new HashMap<>(); 

    /**
     * <code>Map(industryID, Map(outputID, Map(inputID, baseInput)))</code>
     */
    private static Map<String, Map<String, Map<String, Float>>> baseInputs = new HashMap<>();

    /**
     * Map(inputID, List(outputID))
     */
    private static Map<String, List<String>> inputToOutput = new HashMap<>();

    // Map<commodityID, Set<industryID>>
    private static final Map<String, Set<String>> demandToInd = new HashMap<>();
    private static final Map<String, Set<String>> supplyToInd = new HashMap<>();

    public static Map<String, IndustryConfig> ind_config;
    public static LaborConfig labor_config;

    public static final String ABSTRACT_COM = "abstract";

    private IndustryIOs() {}
    static {
        init();
    }

    private static final void init() {
        ind_config = IndustryConfigLoader.loadAsMap();
        labor_config = LaborConfigLoader.loadAsClass();

        DynamicIndConfigs();
        
        ConfigInputOutputMaps();

        buildInputToOutput();

        buildInputOutputToIndustries();
    }

    /**
     * Creates dynamic configs for industries without a json config.
     */
    private static final void DynamicIndConfigs() {
        final String marketID = "ltv_dynamic_ind_test_market";
        final String abstractOutput = "atLeastOneOutputForAbstractInputs";
        final MarketAPI testMarket = Global.getFactory().createMarket(marketID, marketID, 6);

        Map<String, IndustryConfig> dynamicInds = new HashMap<>();

        BiConsumer<Industry, Map<String, Float>> populateInputs = (ind, inputs) -> {
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
                value = (float) Math.pow(0.1, 1f - (base.getModifiedValue() - (ind.getMarket().getSize() - 3)));
                inputs.put(mutable.getCommodityId(), value);
            });
        };

        for (IndustrySpecAPI indSpec : Global.getSettings().getAllIndustrySpecs()) { 
            if (getIndConfig(indSpec) != null) continue;

            String indID = indSpec.getId();
            Map<String, OutputCom> configOutputs = new HashMap<>();
            
            testMarket.addIndustry(indID);

            Industry ind = testMarket.getIndustry(indID);
            // In vanilla, each output uses each input
            Map<String, Float> inputs = new HashMap<>(6);
            List<String> outputs = new ArrayList<>(6);
            Set<String> illegalOutputs = new HashSet<>(6);

            populateInputs.accept(ind, inputs);
            for (MutableCommodityQuantity mutable : ind.getAllSupply()) {
                outputs.add(mutable.getCommodityId());
            }

            if (outputs.isEmpty()) outputs.add(abstractOutput);

            testMarket.setFreePort(true);
            ind.apply();

            for (MutableCommodityQuantity mutable : ind.getAllSupply()) {
                illegalOutputs.add(mutable.getCommodityId());
            }
            illegalOutputs.removeAll(outputs);

            boolean usesWorkers = EconomyEngine.isWorkerAssignable(ind);
            Map<String, Float> CCMoneyDist = usesWorkers ?
                inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> 1f)) : null;

            Map<String, Float> ConsumptionMap = !usesWorkers ?
                inputs.entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                ) : null;

            Consumer<String> addOutput = (outputID) -> {
                boolean illegal = illegalOutputs.contains(outputID);
                OutputCom optCom = new OutputCom(
                        outputID,
                        1,
                        CCMoneyDist,
                        !usesWorkers,
                        usesWorkers,
                        false,
                        illegal,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        ConsumptionMap
                );
                configOutputs.put(outputID, optCom);
            };

            outputs.forEach(addOutput);
            illegalOutputs.forEach(addOutput);

            IndustryConfig config = new IndustryConfig(
                true, configOutputs, OCCTag.AVERAGE, 1f
            );
            config.dynamic = true;

            ind_config.put(indID, config);
            dynamicInds.put(indID, config);
        }

        final List<String> conds = Global.getSettings().getAllMarketConditionSpecs()
            .stream().map(MarketConditionSpecAPI::getId).collect(Collectors.toList());


        Consumer<List<String>> applyAndTestCombo = condCombo -> {
            try {
                for (String condID : condCombo) {
                    MarketConditionSpecAPI spec = Global.getSettings().getMarketConditionSpec(condID);
                    if (spec.getScriptClass().contains("FoodShortage") ||
                        spec.getScriptClass().contains("LuddicPathCells") ||
                        spec.getScriptClass().contains("PirateActivity")
                    ) continue;
                    
                    testMarket.addCondition(condID);
                }
            } catch (Exception e) {
                Global.getLogger(IndustryIOs.class).warn("Condition combo failed: " + condCombo, e);
                for (String condID : condCombo) {
                    testMarket.removeCondition(condID);
                }
                return;
            }
            
            testMarket.reapplyConditions();
            testMarket.reapplyIndustries();

            for (Map.Entry<String, IndustryConfig> entry : dynamicInds.entrySet()) {
                String indID = entry.getKey();
                IndustryConfig config = entry.getValue();
                Industry ind = testMarket.getIndustry(indID);

                Set<String> currentOutputs = ind.getAllSupply().stream()
                    .map(MutableCommodityQuantity::getCommodityId)
                    .collect(Collectors.toSet());

                Set<String> baselineOutputs = config.outputs.keySet();

                Map<String, Float> inputs = new HashMap<>(6);
                populateInputs.accept(ind, inputs);

                Map<String, Float> CCMoneyDist = config.workerAssignable ?
                    inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, i -> 1f)) : null;

                Map<String, Float> ConsumptionMap = !config.workerAssignable ?
                    inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : null;

                // Outputs that appeared
                Set<String> appearingOutputs = new HashSet<>(currentOutputs);
                appearingOutputs.removeAll(baselineOutputs);

                for (String newOutput : appearingOutputs) {
                    OutputCom optCom = new OutputCom(
                        newOutput,
                        1,
                        CCMoneyDist,
                        !config.workerAssignable,
                        config.workerAssignable,
                        false,
                        false,
                        new ArrayList<>(),
                        new ArrayList<>(condCombo),
                        ConsumptionMap
                    );
                    config.outputs.put(newOutput, optCom);
                }

                // --- Outputs that disappeared ---
                Set<String> disappearingOutputs = new HashSet<>(baselineOutputs);
                disappearingOutputs.removeAll(currentOutputs);

                for (String missingOutput : disappearingOutputs) {
                    OutputCom optCom = new OutputCom(
                        missingOutput,
                        1,
                        CCMoneyDist,
                        !config.workerAssignable,
                        config.workerAssignable,
                        false,
                        false,
                        new ArrayList<>(condCombo),
                        new ArrayList<>(),
                        ConsumptionMap
                    );
                    config.outputs.put(missingOutput, optCom);
                }
            }

            for (String condID : condCombo) {
                testMarket.removeCondition(condID);
            }
            testMarket.reapplyConditions();
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
        for (int i = 0; i < conds.size(); i++) {
            for (int j = i + 1; j < conds.size(); j++) {
                for (int k = j + 1; k < conds.size(); k++) {
                    applyAndTestCombo.accept(Arrays.asList(conds.get(i), conds.get(j), conds.get(k)));
                }
            }
        }
    }

    private static final void ConfigInputOutputMaps() {
        for (Map.Entry<String, IndustryConfig> entry : ind_config.entrySet()) {
            Map<String, Float> outputMap = baseOutputs.computeIfAbsent(
                entry.getKey(), k -> new HashMap<>()
            );
            Map<String, Map<String, Float>> inputOuterMap = baseInputs.computeIfAbsent(
                entry.getKey(), k -> new HashMap<>()
            );

            final Map<String, OutputCom> outputs = entry.getValue().outputs;

            for (Map.Entry<String, OutputCom> outputEntry : outputs.entrySet()) {
                Map<String, Float> inputMap = inputOuterMap.computeIfAbsent(
                    outputEntry.getKey(), k -> new HashMap<>()
                );
                
                OutputCom output = outputEntry.getValue();

                if (output.usesWorkers && (output.CCMoneyDist == null || output.CCMoneyDist.isEmpty())) {
                    throw new RuntimeException(
                        "Labor-driven output " + output.comID + " in " + entry.getKey() +
                        " must define CCMoneyDist to calculate variable capital contribution."
                    );
                }

                float base = output.baseProd;

                if (output.usesWorkers && !output.isAbstract) {
                    base /= EconomyEngine.getWorkersPerUnit(output.comID, entry.getValue().occTag);
                }

                if ((output.CCMoneyDist != null && !output.CCMoneyDist.isEmpty())) {
                    CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(output.comID);
                    float Vcc = spec.getBasePrice() * labor_config.getRoCC(entry.getValue().occTag);
                    float totalWeight = 0;
                    for (float weight : output.CCMoneyDist.values()) {
                        totalWeight += weight;
                    }

                    boolean hasAbstractInput = false;
                    for (Map.Entry<String, Float> inputEntry : output.CCMoneyDist.entrySet()) {
                        String inputID = inputEntry.getKey();
                        if (inputID.equals(ABSTRACT_COM)) {
                            hasAbstractInput = true;
                            continue;
                        };

                        float weight = inputEntry.getValue() / totalWeight;
                        float inputValue = Vcc * weight;
                        float unitPrice = Global.getSettings().getCommoditySpec(inputID).getBasePrice();
                        float qty = inputValue * base / unitPrice;

                        inputMap.put(inputID, qty);
                    }
                    if (hasAbstractInput) {
                        float realUnits = 0;
                        for (float weight : inputMap.values()) {
                            realUnits += weight;
                        }
                        float abs_weight = output.CCMoneyDist.get(ABSTRACT_COM);
                        float value = abs_weight * realUnits / (totalWeight - abs_weight);
                        inputMap.put(ABSTRACT_COM, value);
                    }
                } else if (output.StaticInputsPerUnit != null && !output.StaticInputsPerUnit.isEmpty()) {
                    for (Map.Entry<String, Float> demandEntry : output.StaticInputsPerUnit.entrySet()) {
                        String inputID = demandEntry.getKey();
                        if (inputID.equals(ABSTRACT_COM)) continue;

                        float qty = demandEntry.getValue() * base;
                        inputMap.put(inputID, qty);
                    }
                }

                outputMap.put(outputEntry.getKey(), base);
            }
        }
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
        for (Map.Entry<String, IndustryConfig> e : ind_config.entrySet()) {
            String indID = e.getKey();
            IndustryConfig cfg = e.getValue();

            for (String outputID : cfg.outputs.keySet()) {
                supplyToInd.computeIfAbsent(outputID, k -> new HashSet<>()).add(indID);

                Map<String, Float> inputs = baseInputs.get(indID).get(outputID);
                if (inputs != null) {
                    for (String inputID : inputs.keySet()) {
                        demandToInd.computeIfAbsent(inputID, k -> new HashSet<>()).add(indID);
                    }
                }
            }
        }
    }

    private static final boolean isOutputValidForMarket(OutputCom output, MarketAPI market, String outputID) {
        if (output.checkLegality && market.isIllegal(outputID)) return false;

        for (String cond : output.ifMarketCondsAllFalse) {
            if (market.hasCondition(cond)) return false;
        }

        for (String cond : output.ifMarketCondsAllTrue) {
            if (!market.hasCondition(cond)) return false;
        }

        return true;
    }

    private static final float calculateScale(
        Industry ind, MarketAPI market, OutputCom output, String outputID, IndustryConfig cfg
    ) {
        float scale = 1f;

        if (output.usesWorkers && !output.isAbstract) {
            final WorkerRegistry reg = WorkerRegistry.getInstance();
            final WorkerIndustryData data = reg.getData(market.getId(), ind.getId());
            if (data != null) {
                scale *= data.getAssignedForOutput(outputID);
            }
        }

        final double base = cfg.dynamic ? 8.5 : 10;

        if (output.scaleWithMarketSize) scale *= Math.pow(base, market.getSize() - 3);

        return scale;
    }

    /**
     * Get the output of an industry in a given market.
     * Returns 0 if the output does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getOutput(Industry ind, String outputID) {
        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

        IndustryConfig cfg = ind_config.get(indID);
        if (cfg == null) return 0;
        OutputCom output = cfg.outputs.get(outputID);
        if (output == null || output.isAbstract) return 0;

        Map<String, Float> indMap = baseOutputs.get(indID);
        if (indMap == null) return 0;
        Float value = indMap.get(outputID);
        if (value == null) return 0;

        final MarketAPI market = ind.getMarket();

        if (!isOutputValidForMarket(output, market, outputID)) return 0;

        float scale = calculateScale(ind, market, output, outputID, cfg);

        return value * scale;
    }

    /**
     * Get the input required by a specific output of an industry in a given market.
     * Returns 0 if the output or input does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getInput(Industry ind, String outputID, String inputID) {
        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

        IndustryConfig cfg = ind_config.get(indID);
        if (cfg == null) return 0;
        OutputCom output = cfg.outputs.get(outputID);
        if (output == null) return 0;

        Map<String, Map<String, Float>> indMap = baseInputs.get(indID);
        if (indMap == null) return 0;
        Map<String, Float> inputMap = indMap.get(outputID);
        if (inputMap == null) return 0;

        Float value = inputMap.get(inputID);
        if (value == null) return 0;

        final MarketAPI market = ind.getMarket();

        if (!isOutputValidForMarket(output, market, outputID)) return 0;

        float scale = calculateScale(ind, market, output, outputID, cfg);

        return value * scale;
    }

    /**
     * Get the total demand of an industry for a given commodity across all outputs.
     */
    public static final float getSumInput(Industry ind, String inputID) {
        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

        Map<String, Map<String, Float>> indMap = baseInputs.get(indID);
        if (indMap == null) return 0;

        float total = 0f;
        for (String outputID : indMap.keySet()) {
            total += getInput(ind, outputID, inputID);
        }
        return total;
    }

    /** 
     * Returns the modified output map for a given industry.
     */
    public static final Map<String, Float> getOutputs(Industry ind, boolean includeAbstract) {
        if (ind == null || ind.getId() == null) return Collections.emptyMap();

        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

        final Map<String, Float> outputs = baseOutputs.get(indID);
        if (outputs == null) return Collections.emptyMap();

        Map<String, Float> scaledOutputs = new HashMap<>();
        for (String output : outputs.keySet()) {
            float value = getOutput(ind, output);
            if (includeAbstract || value > 0) scaledOutputs.put(output, value);
        }

        return scaledOutputs;
    }

    /** 
     * Returns the modified inputs map for a given industry and output.
     */
    public static final Map<String, Float> getInputs(Industry ind, String outputID, boolean includeAbstract) {
        if (ind == null || ind.getId() == null) return Collections.emptyMap();

        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

        final Map<String, Map<String, Float>> outputs = baseInputs.get(indID);
        if (outputs == null) return Collections.emptyMap();

        final Map<String, Float> baseInputMap = outputs.get(outputID);
        if (baseInputMap == null) return Collections.emptyMap();

        Map<String, Float> scaledInputs = new HashMap<>();
        for (Map.Entry<String, Float> entry : baseInputMap.entrySet()) {
            float value = getInput(ind, outputID, entry.getKey());
            if (includeAbstract || value > 0) scaledInputs.put(entry.getKey(), value);
        }

        return scaledInputs;
    }

    /**
     * Returns a list of all the inputs for a given industry.
     */
    public static final Set<String> getInputs(Industry ind, boolean includeAbstract) {
        if (ind == null || ind.getId() == null) return Collections.emptySet();

        final String indID = ind_config.get(ind.getId()) != null ? ind.getId() : getBaseIndustryID(ind);

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
        return ind_config.containsKey(ind.getId()) 
            || ind_config.containsKey(getBaseIndustryID(ind));
    }

    public static final boolean hasSupply(Industry ind, String comID) {
        String id = ind.getId();
        if (!supplyToInd.getOrDefault(comID, Collections.emptySet()).contains(id)) {
            id = getBaseIndustryID(ind);
        }
        return supplyToInd.getOrDefault(comID, Collections.emptySet()).contains(id);
    }

    public static final boolean hasDemand(Industry ind, String comID) {
        String id = ind.getId();
        if (!demandToInd.getOrDefault(comID, Collections.emptySet()).contains(id)) {
            id = getBaseIndustryID(ind);
        }
        return demandToInd.getOrDefault(comID, Collections.emptySet()).contains(id);
    }

    public static final IndustryConfig getIndConfig(Industry ind) {
        return getIndConfig(ind.getSpec());
    }

    public static final IndustryConfig getIndConfig(IndustrySpecAPI ind) {
        IndustryConfig indConfig = ind_config.get(ind.getId());

        if (indConfig == null) {
            indConfig = ind_config.get(getBaseIndustryID(ind));
        }

        return indConfig;
    }

    public static final String getBaseIndustryID(Industry ind) {
        return getBaseIndustryID(ind.getSpec());
    }

    public static final String getBaseIndustryID(IndustrySpecAPI ind) {
        IndustrySpecAPI currentInd = ind;

        while (true) {
            String downgradeId = currentInd.getDowngrade();
            if (downgradeId == null) break;

            currentInd = Global.getSettings().getIndustrySpec(downgradeId);
        }

        return currentInd.getId();
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
            "demandsToInd" + "\n" +
            demandToInd.toString() + "\n" +
            "--------------------------" + "\n" +
            "supplyToInd" + "\n" +
            supplyToInd.toString()
        );
    }
}
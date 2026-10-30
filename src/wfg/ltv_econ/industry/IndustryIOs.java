package wfg.ltv_econ.industry;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.campaign.econ.Market;

import rolflectionlib.util.RolfLectionUtil;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.config.IndustryConfigManager.OutputConfig;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.engine.EconomyEngine;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.IndustryWorkerData;
import wfg.native_ui.util.ArrayMap;

/**
 * <p>This class serves as a precomputed wrapper around {@link IndustryConfigManager#ind_config}. 
 * It stores base and effective production values alongside input requirements for all industries,
 * allowing fast, reusable access.</p>
 */
public final class IndustryIOs {
    private IndustryIOs() {};
    private static final Object MARKET_SIZE_FIELD = RolfLectionUtil.getFieldByName("size", Market.class);

    public static final class BaseIO {
        public final float outputValue;
        public final ArrayMap<String, Float> inputs;

        public BaseIO(float outputVal) {
            this(outputVal, 4);
        }

        public BaseIO(float outputVal, int inputCount) {
            this.outputValue = outputVal;
            inputs = new ArrayMap<>(inputCount);
        }

        @Override
        public String toString() {
            return '{' + "outputVal: " + outputValue + ", inputs: " + inputs + '}';
        }
    }

    /** Map(industryID, Map(outputID, BaseIO)) */
    private static final Map<String, ArrayMap<String, BaseIO>> baseIOs = new HashMap<>(IndustryConfigManager.ind_config.size());

    /**
     * Map(inputID, List(outputID))
     */
    private static final ArrayMap<String, List<String>> inputToOutput = new ArrayMap<>(EconomyConstants.econCommodityIDs.size());

    // Map<commodityID, Set<industryID>>
    private static final ArrayMap<String, Set<String>> inputToInd = new ArrayMap<>(EconomyConstants.econCommodityIDs.size());
    private static final ArrayMap<String, Set<String>> outputToInd = new ArrayMap<>(EconomyConstants.econCommodityIDs.size());

    public static final String ABSTRACT_COM = "abstract";
    public static final String DYNAMIC_OUTPUT = "output_for_input::";

    static {
        reload();
    }

    public static final void reload() {
        clearMaps();
        buildMaps();
    }

    private static final void clearMaps() {
        baseIOs.clear();
        inputToOutput.clear();
        inputToInd.clear();
        outputToInd.clear();
    }

    private static final void buildMaps() {
        for (Map.Entry<String, IndustryConfig> entry : IndustryConfigManager.ind_config.singleEntrySet()) {
            final String indID = entry.getKey();
            final ArrayMap<String, OutputConfig> outputs = entry.getValue().outputs;
            final var ioMap = baseIOs.computeIfAbsent(indID, k -> new ArrayMap<>(outputs.size()));

            for (OutputConfig config : outputs.values()) {
                final BaseIO baseIO = fillOutputMaps(entry.getValue().occTag, config, ioMap);
                registerInLookupMaps(indID, config.comID, baseIO.inputs);
            }
        }
    }

    /**
     * @return {@link BaseIO} for this output, as stored in {@code #ioMap}.
     */
    private static final BaseIO fillOutputMaps(String occTag, OutputConfig output,
        ArrayMap<String, BaseIO> ioMap
    ) {
        final boolean useCCMoneyDist = output.CCMoneyDist != null && !output.CCMoneyDist.isEmpty() && !output.isAbstract;
        final int inputCount = useCCMoneyDist ? output.CCMoneyDist.size() : output.InputsPerUnitOutput.size();
        final String outputID = output.comID;

        final float base = (!output.usesWorkers || output.isAbstract) ? output.baseProd :
            output.baseProd / EconomyInfo.getWorkersPerUnit(outputID, occTag);

        final BaseIO baseIO = new BaseIO(base, inputCount);
        final ArrayMap<String, Float> inputMap = baseIO.inputs;

        if (useCCMoneyDist) {
            final CommoditySpecAPI spec = settings.getCommoditySpec(outputID);
            final float Vcc = spec.getBasePrice() * EconConfig.getRoCC(occTag);

            float totalWeight = 0f;
            for (float weight : output.CCMoneyDist.values()) totalWeight += weight;

            boolean hasAbstractInput = false;
            for (Map.Entry<String, Float> inputEntry : output.CCMoneyDist.singleEntrySet()) {
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
                float realUnits = 0f;
                for (float w : inputMap.values()) realUnits += w;
                final float abs_weight = output.CCMoneyDist.get(ABSTRACT_COM);
                final float value = abs_weight * realUnits / (totalWeight - abs_weight);
                inputMap.put(ABSTRACT_COM, value);
            }

        } else {
            for (Map.Entry<String, Float> demandEntry : output.InputsPerUnitOutput.singleEntrySet()) {
                final String inputID = demandEntry.getKey();
                if (inputID.equals(ABSTRACT_COM)) continue;
                inputMap.put(inputID, demandEntry.getValue() * base);
            }
        }

        ioMap.put(outputID, baseIO);
        return baseIO;
    }

    /**
     * @param indID the industry the output belongs to.
     * @param outputID the output that was just filled by {@link #fillOutputMaps}
     * @param inputs the {@link BaseIO#inputs} produced by {@link #fillOutputMaps} for that output
     */
    private static final void registerInLookupMaps(String indID, String outputID, ArrayMap<String, Float> inputs) {
        outputToInd.computeIfAbsent(outputID, k -> new HashSet<>()).add(indID);
        for (String inputID : inputs.keySet()) {
            final List<String> outputs = inputToOutput.computeIfAbsent(inputID, k -> new ArrayList<>());
            if (!outputs.contains(outputID)) outputs.add(outputID);
            inputToInd.computeIfAbsent(inputID, k -> new HashSet<>()).add(indID);
        }
    }

    private static final float getIndApplyDeltaWithMarketSizeChange(
        MarketAPI market, Supplier<Float> appliedValue
    ) {
        final int marketSize = market.getSize();

        float firstValue = 0f;
        float secondValue = 0f;

        try {
            firstValue = appliedValue.get();
            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, marketSize + 1);
            secondValue = appliedValue.get();
            
        } catch (Exception e) {
            firstValue = 0f;
            secondValue = 0f;

        } finally {
            RolfLectionUtil.setPrivateVariable(MARKET_SIZE_FIELD, market, marketSize);
            try { appliedValue.get(); } catch (Exception ignored) {} // restore state
        }

        return Math.abs(secondValue - firstValue);
    }

    /**
     * @return <ul>
     *             <li>{@code 0} – success</li>
     *             <li>{@code 1} – output entry already exists</li>
     *             <li>{@code 2} – output is abstract</li>
     *         </ul>
     */
    public static final int createAndRegisterDynamicOutput(final Industry ind, String outputID) {
        final IndustryConfig cfg = IndustryConfigManager.getIndConfig(ind);
        if (cfg.outputs.containsKey(outputID)) return 1;
        if (settings.getCommoditySpec(outputID) == null) return 2;

        final boolean useWorkers = cfg.workerAssignable;
        final boolean scaleWithSize = getIndApplyDeltaWithMarketSizeChange(ind.getMarket(),
            () -> { ind.reapply(); return ind.getSupply(outputID).getQuantity().getModifiedValue(); }
        ) > 0.01f;

        final ArrayMap<String, Float> inputs = new ArrayMap<>(4);
            IndustryConfigManager.populateInputs(ind, inputs, scaleWithSize);

            final ArrayMap<String, Float> CCMoneyDist;
            if (useWorkers) {
                CCMoneyDist = new ArrayMap<>(inputs.size());
                for (Map.Entry<String, ?> entry : inputs.singleEntrySet()) {
                    CCMoneyDist.put(entry.getKey(), 1f);
                }
            } else {
                CCMoneyDist = null;
            }

            final ArrayMap<String, Float> InputsPerUnitOutput;
            if (!useWorkers) {
                InputsPerUnitOutput = new ArrayMap<>(inputs.size());
                for (Map.Entry<String, Float> entry : inputs.singleEntrySet()) {
                    InputsPerUnitOutput.put(entry.getKey(), entry.getValue());
                }
            } else {
                InputsPerUnitOutput = null;
            }

        final OutputConfig output = new OutputConfig(
            outputID, 1, CCMoneyDist,
            scaleWithSize, useWorkers, false, false,
            Collections.emptyList(), Collections.emptyList(),
            InputsPerUnitOutput, EconConfig.dynamicWorkerCapPerOutput,
            IndustryConfigManager.dynamicIndMarketScaleBase, -1, false
        );
        output.dynamic = true;
        output.dynamicOutputActive = (industry) -> {
            return industry.getSupply(outputID).getQuantity().getModifiedValue() > 0f;
        };

        cfg.outputs.put(outputID, output);

        final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind);
        final BaseIO baseIO = fillOutputMaps(cfg.occTag, output, baseIOs.get(indID));

        registerInLookupMaps(indID, outputID, baseIO.inputs);

        return 0;
    }

    /**
     * @return <ul>
     *             <li>{@code 0} – success</li>
     *             <li>{@code 1} – input entry already exists</li>
     *             <li>{@code 2} – input is abstract</li>
     *         </ul>
     */
    public static final int createAndRegisterDynamicInput(final Industry ind, String inputID) {
        final IndustryConfig cfg = IndustryConfigManager.getIndConfig(ind);
        final String outputID = DYNAMIC_OUTPUT + inputID;

        if (cfg.outputs.containsKey(outputID)) return 1;
        if (settings.getCommoditySpec(inputID) == null) return 2;

        final boolean scaleWithSize = getIndApplyDeltaWithMarketSizeChange(ind.getMarket(),
            () -> { ind.reapply(); return ind.getDemand(inputID).getQuantity().getModifiedValue(); }
        ) > 0.01f;

        
        final float inputValue = IndustryConfigManager.populateInput(ind.getDemand(inputID).getQuantity(), scaleWithSize);
        final ArrayMap<String, Float> InputsPerUnitOutput = new ArrayMap<>(1);
        InputsPerUnitOutput.put(inputID, inputValue);

        final OutputConfig output = new OutputConfig(
            outputID, 1, null,
            scaleWithSize, false, true, false,
            Collections.emptyList(), Collections.emptyList(),
            InputsPerUnitOutput, EconConfig.dynamicWorkerCapPerOutput,
            IndustryConfigManager.dynamicIndMarketScaleBase, -1, false
        );
        output.dynamic = true;
        output.dynamicOutputActive = (industry) -> {
            return industry.getDemand(inputID).getQuantity().getModifiedValue() > 0f;
        };

        cfg.outputs.put(outputID, output);

        final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind);
        final BaseIO baseIO = fillOutputMaps(cfg.occTag, output, baseIOs.get(indID));

        registerInLookupMaps(indID, outputID, baseIO.inputs);

        return 0;
    }

    public static final boolean isOutputActiveForMarket(final OutputConfig output, Industry ind) {
        if (output == null || ind.isDisrupted()) return false;
        final boolean buildingOnly = ind.isBuilding() && !ind.isUpgrading();

        if (buildingOnly && !output.activeDuringBuilding &&
            !ind.getSpec().getId().contains(Industries.POPULATION)
        ) return false;
        if (!buildingOnly && output.activeDuringBuilding) return false;

        return isOutputValidForMarket(output, ind);
    }

    public static final boolean isOutputValidForMarket(final OutputConfig output, Industry ind) {
        if (output == null) return false;

        final MarketAPI market = ind.getMarket();
        if (output.checkLegality && market.isIllegal(output.comID)) return false;
        if (output.dynamic && !output.dynamicOutputActive.isActive(ind)) return false;

        for (String cond : output.ifMarketCondsAllFalse) {
            if (market.hasCondition(cond)) return false;
        }

        for (String cond : output.ifMarketCondsAllTrue) {
            if (!market.hasCondition(cond)) return false;
        }

        return hasRelevantCondition(output.comID, market);
    }

    public static final boolean hasRelevantCondition(String comID, MarketAPI market) {
        boolean hasRelevantCondition = true;
        if (ResourceDepositsCondition.COMMODITY.containsValue(comID)) {
            hasRelevantCondition = false;
            for (MarketConditionAPI cond : market.getConditions()) {
                String condComID = ResourceDepositsCondition.COMMODITY.get(cond.getId());
                if (comID.equals(condComID)) {
                    return true;
                }
            }
        }
        return hasRelevantCondition;
    }

    public static final float computeScale(OutputConfig output, Industry ind) {
        return computeScale(ind.getSpec().getId(), ind.getMarket().getId(), ind.getMarket().getSize(), output);
    }

    public static final float computeScale(final IndustryWorkerData data, OutputConfig output, MarketAPI market) {
        return computeScale(data, market.getId(), market.getSize(), output);
    }

    public static final float computeScale(String industryId, String marketId, int marketSize, OutputConfig output) {
        return computeScale(WorkerRegistry.get(marketId, industryId), marketId, marketSize, output);
    }

    public static final float computeScale(final IndustryWorkerData data, String marketId, int marketSize, OutputConfig output) {
        if (!output.isAbstract) {
            final CommodityCell cell = EconomyEngine.instance().getComCell(output.comID, marketId); 
            if (cell != null && output.target > 0l && output.target < cell.getStored()) return 0f;
        }

        float scale = 1f;

        if (output.usesWorkers && !output.isAbstract) {
            scale *= data.getAssignedForOutput(output.comID);
        }

        if (output.scaleWithMarketSize) scale *= Math.pow(output.marketScaleBase, marketSize - 3);

        return scale;
    }

    private static final boolean isOutputActive(boolean validOnly, OutputConfig output, Industry ind) {
        return validOnly ? isOutputValidForMarket(output, ind) : isOutputActiveForMarket(output, ind);
    }

    public static final float getRealOutput(Industry ind, String outputID) {
        return getRealOutput(ind, WorkerRegistry.get(ind), outputID, false);
    }

    /**
     * Get the output of an industry in a given market.
     * Returns 0 if the output does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getRealOutput(Industry ind, IndustryWorkerData data, String outputID, boolean validOnly) {
        final IndustryConfig cfg = IndustryConfigManager.getIndConfig(ind);
        final OutputConfig output = cfg.outputs.get(outputID);

        if (output == null || output.isAbstract) return 0f;
        final float value = getBaseOutput(ind.getSpec(), outputID);
        if (value == 0f) return 0f;

        if (!isOutputActive(validOnly, output, ind)) return 0f;

        final float scale = computeScale(data, output, ind.getMarket());

        return value * scale;
    }

    public static final float getRealInput(Industry ind, String outputID, String inputID) {
        return getRealInput(ind, WorkerRegistry.get(ind), outputID, inputID, false);
    }

    /**
     * Get the input required by a specific output of an industry in a given market.
     * Returns 0 if the output or input does not exist, if the market conditions are not met,
     * or if legality prevents the output from being produced.
     */
    public static final float getRealInput(Industry ind, IndustryWorkerData data,  String outputID, String inputID, boolean validOnly) {
        final IndustryConfig cfg = IndustryConfigManager.getIndConfig(ind);
        final OutputConfig output = cfg.outputs.get(outputID);

        final float value = getBaseInput(ind.getSpec(), outputID, inputID);
        if (value == 0f) return 0f;

        if (!isOutputActive(validOnly, output, ind)) return 0f;

        final float scale = computeScale(data, output, ind.getMarket());

        return value * scale;
    }

    public static final float getRealSumInput(Industry ind, String inputID) {
        return getRealSumInput(WorkerRegistry.get(ind), ind, inputID, false);
    }

    /**
     * Get the total demand of an industry for a given commodity across all outputs.
     * @param validOnly does not check if the outputs are active, only that they are allowed.
     */
    public static final float getRealSumInput(IndustryWorkerData data, Industry ind, String inputID, boolean validOnly) {
        final ArrayMap<String, BaseIO> indIOs = getBaseIOs(ind.getSpec().getId());
        final IndustryConfig cfg = IndustryConfigManager.getIndConfig(ind);

        float total = 0f;
        for (Map.Entry<String, BaseIO> inputMap : indIOs.singleEntrySet()) {
            final float qty = inputMap.getValue().inputs.getOrDefault(inputID, 0f);
            if (qty == 0f) continue;

            final OutputConfig output = cfg.outputs.get(inputMap.getKey());
            if (!isOutputActive(validOnly, output, ind)) continue;

            final float scale = computeScale(data, output, ind.getMarket());

            total += qty * scale;
        }

        return total;
    }

    public static final ArrayMap<String, Float> getRealOutputs(Industry ind, boolean includeAbstract) {
        return getRealOutputs(ind, WorkerRegistry.get(ind), includeAbstract, false);
    }

    /** 
     * Returns the modified output map for a given industry.
     */
    public static final ArrayMap<String, Float> getRealOutputs(Industry ind, IndustryWorkerData data, boolean includeAbstract, boolean validOnly) {
        final ArrayMap<String, BaseIO> indIOs = getBaseIOs(ind.getSpec().getId());

        final ArrayMap<String, Float> scaledOutputs = new ArrayMap<>(indIOs.size());
        for (String output : indIOs.keySet()) {

            final float value = getRealOutput(ind, data, output, validOnly);

            if (includeAbstract || value > 0) scaledOutputs.put(output, value);
        }

        return scaledOutputs;
    }

    public static final ArrayMap<String, Float> getRealInputs(Industry ind, String outputID, boolean includeAbstract) {
        return getRealInputs(ind, WorkerRegistry.get(ind), outputID, includeAbstract, false);
    }

    /** 
     * Returns the modified inputs map for a given industry and output.
     */
    @SuppressWarnings("unchecked")
    public static final ArrayMap<String, Float> getRealInputs(Industry ind, IndustryWorkerData data,
        String outputID, boolean includeAbstract, boolean validOnly
    ) {
        final BaseIO base = getBaseIOs(ind.getSpec().getId()).get(outputID);
        if (base == null) return ArrayMap.EMPTY_MAP;

        final ArrayMap<String, Float> scaledInputs = new ArrayMap<>(base.inputs.size());
        for (String input : base.inputs.keySet()) {
            final float value = getRealInput(ind, data, outputID, input, validOnly);
            if (includeAbstract || value > 0) scaledInputs.put(input, value);
        }

        return scaledInputs;
    }

    /**
     * Returns a list of all the inputs for a given industry.
     */
    public static final Set<String> getRealInputs(Industry ind, boolean includeAbstract) {
        if (ind == null || ind.getSpec() == null) return Collections.emptySet();

        final String indID = IndustryConfigManager.getBaseIndIDifNoConfig(ind.getSpec());

        final Set<String> inputSet = new HashSet<>();
        for (BaseIO baseIO : baseIOs.get(indID).values()) {
            for (String input : baseIO.inputs.keySet()) {
                if (ABSTRACT_COM.equals(input) && !includeAbstract) continue;
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
        return getBaseOutput(ind.getId(), outputID);
    }

    /**
     * Get the pre-calculated base output of an industry for a given commodity.
     * Ignores market conditions, scaling, and legality.
     */
    public static final float getBaseOutput(String indID, String outputID) {
        final Map<String, BaseIO> indIOs = baseIOs.get(IndustryConfigManager.getBaseIndIDifNoConfig(indID));
        final BaseIO base = indIOs.get(outputID);
        return base != null ? base.outputValue : 0f;
    }

    /**
     * Get the pre-calculated base input required by an industry for a specific output.
     * Ignores market context.
     */
    public static final float getBaseInput(IndustrySpecAPI ind, String outputID, String inputID) {
        return getBaseInput(ind.getId(), outputID, inputID);
    }

    /**
     * Get the pre-calculated base input required by an industry for a specific output.
     * Ignores market context.
     */
    public static final float getBaseInput(String indID, String outputID, String inputID) {
        final BaseIO base = baseIOs.get(IndustryConfigManager.getBaseIndIDifNoConfig(indID)).get(outputID);
        if (base == null) return 0f;
        return base.inputs.getOrDefault(inputID, 0f);
    }

    /**
     * Returns the live internal map of compiled IO for the industry.
     * Map(outputID, BaseIO{outputValue, inputs}).
     */
    public static final ArrayMap<String, BaseIO> getBaseIOs(String indID) {
        return baseIOs.get(IndustryConfigManager.getBaseIndIDifNoConfig(indID));
    }

    @SuppressWarnings ("unchecked")
    public static final ArrayMap<String, Float> getBaseInputs(String indID, String outputID) {
        final BaseIO base = baseIOs.get(IndustryConfigManager.getBaseIndIDifNoConfig(indID)).get(outputID);
        return base != null ? base.inputs : ArrayMap.EMPTY_MAP;
    }
    
    public static final boolean hasOutput(String indID, String comID) {
        final String id = IndustryConfigManager.getBaseIndIDifNoConfig(indID);
        return outputToInd.getOrDefault(comID, Collections.emptySet()).contains(id);
    }

    public static final boolean hasInput(String indID, String inputID) {
        final String id = IndustryConfigManager.getBaseIndIDifNoConfig(indID);
        return inputToInd.getOrDefault(inputID, Collections.emptySet()).contains(id);
    }

    /**
     * @param outputID cannot be abstract.
     * @param productivity multiplier to inputs and outputs but not to workers.
     * 
     * @return the marginal profit per worker for a given output of an industry.
     */
    public static final float getMarginalProfitPerWorker(String indID, String outputID, float productivity) {
        final IndustryConfig indCfg = IndustryConfigManager.getIndConfig(indID);
        final OutputConfig output = indCfg.outputs.get(outputID);
        if (output == null || !output.usesWorkers || !indCfg.workerAssignable) {
            return Float.NEGATIVE_INFINITY;
        }

        final float outputAmount = getBaseOutput(indID, outputID);
        final float outputVal = settings.getCommoditySpec(outputID).getBasePrice() * outputAmount;

        float inputValSum = 0f;
        final BaseIO inputs = getBaseIOs(indID).get(outputID);
        if (inputs != null) {
            for (Map.Entry<String, Float> entry : inputs.inputs.singleEntrySet()) {
                final String inputID = entry.getKey();
                if (ABSTRACT_COM.equals(inputID)) continue;
                inputValSum += settings.getCommoditySpec(inputID).getBasePrice() * entry.getValue();
            }
        }

        final float wagePerWorker = EconConfig.avg_wage / EconomyConstants.MONTH;
        return (outputVal - inputValSum) * productivity - wagePerWorker;
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
            final String realBaseID = IndustryConfigManager.getBaseIndIDifNoConfig(ind.getSpec());
            for (String baseID : baseIDList) {
                if (realBaseID.equals(baseID)) return ind;
            }
        }
        throw new IllegalStateException(
            "Expected market '" + market.getId() + "' to contain an industry with baseID '"
            + baseIDList.toString() + "', but none was found."
        );
    }

    public static final void logMaps() {
        Global.getLogger(IndustryIOs.class).info(
            "==== IndustryIOs Data Log ====" + "\n" +
            "baseIOs" + "\n" +
            baseIOs.toString() + "\n" +
            "--------------------------" + "\n" +
            "inputsToInd" + "\n" +
            inputToInd.toString() + "\n" +
            "--------------------------" + "\n" +
            "outputsToInd" + "\n" +
            outputToInd.toString() + "\n" +
            "--------------------------"
        );
    }
}
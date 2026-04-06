package wfg.ltv_econ.config;

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

import wfg.ltv_econ.config.loader.IndustryConfigLoader;
import wfg.ltv_econ.config.loader.LaborConfigLoader;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.economy.CompatLayer;
import wfg.ltv_econ.economy.engine.EconomyInfo;
import wfg.native_ui.util.ArrayMap;
import wfg.ltv_econ.util.ConfigUtils;

import java.util.List;

import static wfg.ltv_econ.constants.Mods.LTV_ECON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IndustryConfigManager {
    private static final SettingsAPI settings = Global.getSettings();

    private static final ArrayMap<String, String> IndToBaseInd = new ArrayMap<>(EconomyConstants.industryIDs.size());

    public static final float dynamicIndMarketScaleBase = 6f;

    public static ArrayMap<String, IndustryConfig> ind_config;

    static {
        buildBaseIdMapping();

        reload();
    }

    public static final void reload() {
        ind_config = IndustryConfigLoader.loadAsMap(false);

        validateOrRebuildDynamicConfigs();
    }

    public static class IndustryConfig {
        public final boolean workerAssignable;
        public final boolean demandOnly;
        public final String occTag;
        public final ArrayMap<String, OutputConfig> outputs;

        public boolean dynamic = false;

        public IndustryConfig(boolean workerAssignable, ArrayMap<String, OutputConfig> outputs, String occTag,
            boolean demandOnly
        ) {
            this.workerAssignable = workerAssignable;
            this.demandOnly = demandOnly;
            this.outputs = outputs;
            this.occTag = occTag;
        }

        /**
         * Copy Constructor
         */
        public IndustryConfig(IndustryConfig config) {
            this.workerAssignable = config.workerAssignable;
            this.demandOnly = config.demandOnly;
            this.occTag = config.occTag;
            this.dynamic = config.dynamic;

            // Deep copy outputs map
            if (config.outputs != null) {
                ArrayMap<String, OutputConfig> copy = new ArrayMap<>(config.outputs.size());
                for (Entry<String, OutputConfig> e : config.outputs.singleEntrySet()) {
                    copy.put(e.getKey(), new OutputConfig(e.getValue()));
                }
                this.outputs = copy;
            } else {
                this.outputs = new ArrayMap<>(4);
            }
        }

        @Override
        public final String toString() {
            return '{' + " ,\n"
                + "workerAssignable: " + workerAssignable + " ,\n"
                + "occTag: " + occTag + " ,\n"
                + "demandOnly: " + demandOnly + " ,\n"
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

        public final ArrayMap<String, Float> CCMoneyDist; // Determines the share of money spent on each input.
        public final ArrayMap<String, Float> InputsPerUnitOutput;

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
            String comID, float baseProd, ArrayMap<String, Float> CCMoneyDist, boolean scaleWithMarketSize,
            boolean usesWorkers, boolean isAbstract, boolean checkLegality, List<String> ifMarketCondsAllFalse,
            List<String> ifMarketCondsAllTrue, ArrayMap<String, Float> InputsPerUnitOutput, float workerAssignableLimit,
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

    public static final String getBaseIndustryIDSpec(IndustrySpecAPI ind) {
        IndustrySpecAPI currentInd = ind;

        while (true) {
            String downgradeId = currentInd.getDowngrade();
            if (downgradeId == null || downgradeId.equals(currentInd.getId())) break;

            currentInd = settings.getIndustrySpec(downgradeId);
        }

        return currentInd.getId();
    }

    public static final boolean hasConfig(Industry ind) {
        return hasConfig(ind.getSpec());
    }

    public static final boolean hasConfig(IndustrySpecAPI ind) {
        return IndustryConfigManager.ind_config.containsKey(ind.getId()) 
            || IndustryConfigManager.ind_config.containsKey(getBaseIndustryID(ind.getId()));
    }

    public static final String getBaseIndustryID(String id) {
        return IndToBaseInd.get(id);
    }

    public static final String getBaseIndustryID(IndustrySpecAPI ind) {
        return getBaseIndustryID(ind.getId());
    }

    public static final String getBaseIndustryID(Industry ind) {
        return getBaseIndustryID(ind.getId());
    }

    public static final String getBaseIndIDifNoConfig(IndustrySpecAPI ind) {
        if (IndustryConfigManager.ind_config.containsKey(ind.getId())) {
            return ind.getId();
        }
        return getBaseIndustryID(ind.getId());
    }

    public static final IndustryConfig getIndConfig(Industry ind) {
        return getIndConfig(ind.getSpec());
    }

    public static final IndustryConfig getIndConfig(IndustrySpecAPI ind) {
        final IndustryConfig indConfig = IndustryConfigManager.ind_config.get(ind.getId());

        return indConfig != null ? indConfig :
            IndustryConfigManager.ind_config.get(getBaseIndustryID(ind.getId()));
    }

    private static final void buildBaseIdMapping() {
        IndToBaseInd.clear();
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            IndToBaseInd.put(spec.getId(), IndustryConfigManager.getBaseIndustryIDSpec(spec));
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
        final ArrayMap<String, IndustryConfig> dynamic_config = IndustryConfigLoader.loadAsMap(true);
        final Set<String> validIndustryIds = settings.getAllIndustrySpecs().stream()
            .map(IndustrySpecAPI::getId).collect(Collectors.toSet());

        boolean allIndustriesHaveConfig = true;
        final boolean current = settings.getModManager().getModSpec(LTV_ECON).getVersion()
            .equals(IndustryConfigLoader.getDynamicConfigVersion());

        // 1) Check that every existing spec has a config
        for (IndustrySpecAPI spec : settings.getAllIndustrySpecs()) {
            final String baseId = getBaseIndustryIDSpec(spec);

            if (!(dynamic_config.containsKey(spec.getId()) ||
                (baseId != null && dynamic_config.containsKey(baseId))) &&
                !hasConfig(spec)
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
            if (getIndConfig(indSpec) != null) continue;

            final String indID = indSpec.getId();
            final ArrayMap<String, OutputConfig> configOutputs = new ArrayMap<>(4);
            
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
                final boolean usesWorkers = EconomyInfo.isWorkerAssignableByDefault(ind1) && !hasNoRealOutputs;
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
                final ArrayMap<String, Float> inputs = new ArrayMap<>(4);
                populateInputs(ind1, inputs, !scaleWithMarketSize.isEmpty());

                final ArrayMap<String, Float> CCMoneyDist;
                if (usesWorkers) {
                    CCMoneyDist = new ArrayMap<>(inputs.size());
                    for (Entry<String, ?> entry : inputs.singleEntrySet()) {
                        CCMoneyDist.put(entry.getKey(), 1f);
                    }
                } else { CCMoneyDist = null; }

                final ArrayMap<String, Float> InputsPerUnitOutput;
                if (!usesWorkers) {
                    InputsPerUnitOutput = new ArrayMap<>(inputs.size());
                    for (Entry<String, Float> entry : inputs.singleEntrySet()) {
                        InputsPerUnitOutput.put(entry.getKey(), entry.getValue());
                    }
                } else { InputsPerUnitOutput = null; }
    
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

        IndustryConfigLoader.serializeAndWriteToCommon(dynamic_config);
    }
}
package wfg.ltv_econ.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.util.Pair;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.economy.IndustryConfigLoader.IndustryConfig;
import wfg.ltv_econ.economy.IndustryConfigLoader.OutputCom;
import wfg.ltv_econ.economy.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;

import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine extends BaseCampaignEventListener
    implements PlayerColonizationListener, ColonyDecivListener {

    public static final String ABSTRACT_COM = "abstract";

    private static EconomyEngine instance;

    private final Set<String> m_registeredMarkets;
    private final Map<String, CommodityInfo> m_comInfo;

    private transient Map<String, List<OutputComReference>> inputToDependentOutputs;
    public transient Map<String, IndustryConfig> ind_config;
    public transient LaborConfig labor_config;

    public static void createInstance() {
        if (instance == null) {
            instance = new EconomyEngine();
        }
    }

    public static void setInstance(EconomyEngine a) {
        instance = a;
    }

    public static EconomyEngine getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private EconomyEngine() {
        super(true);
        m_registeredMarkets = new HashSet<>();
        m_comInfo = new HashMap<>();

        for (MarketAPI market : getMarketsCopy()) {
            m_registeredMarkets.add(market.getId());
        }

        for (CommoditySpecAPI spec : Global.getSettings().getAllCommoditySpecs()) {
            if (spec.isNonEcon())
                continue;

            m_comInfo.put(spec.getId(), new CommodityInfo(spec));
        }

        readResolve();
    }

    public final Object readResolve() {
        ind_config = IndustryConfigLoader.loadAsMap();
        labor_config = LaborConfigLoader.loadAsClass();
        buildInputToOutputsMap();

        Global.getSector().getListenerManager().addListener(this, true);
        Global.getSector().addListener(this);

        return this;
    }

    protected int dayTracker = -1;

    public final void advance(float delta) {
        final int day = Global.getSector().getClock().getDay();

        if (dayTracker == -1) {
            dayTracker = day;
        }

        if (dayTracker == day)
            return;

        dayTracker = day;

        mainLoop(false);
    }

    public final void fakeAdvance() {
        mainLoop(true);
    }

    // Order matters here
    private final void mainLoop(boolean fakeAdvance) {
        refreshMarkets();

        if (!fakeAdvance) {
            assignWorkers();
        }

        for (MarketAPI market : EconomyEngine.getMarketsCopy()) {
            for (Industry ind : CommodityStats.getVisibleIndustries(market)) {
                if (ind instanceof BaseIndustry baseInd) {
                    applyIndustryPIOs(market, baseInd);
                }
            }
        }

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.reset();

            comInfo.update();
        }
        
        weightedOutputDeficitMods();

        for (CommodityInfo comInfo : m_comInfo.values()) {
            comInfo.trade();

            comInfo.advance(fakeAdvance);
        }
    }

    public final void registerMarket(String marketID) {
        if (m_registeredMarkets.add(marketID)) {
            for (CommodityInfo comInfo : m_comInfo.values()) {
                comInfo.addMarket(marketID);
            }

        }
        
        WorkerRegistry.getInstance().register(marketID);
    }

    public final void removeMarket(String marketID) {
        if (m_registeredMarkets.remove(marketID)) {
            for (CommodityInfo comInfo : m_comInfo.values()) {
                comInfo.removeMarket(marketID);
            }

            WorkerRegistry.getInstance().remove(marketID);
        }
    }

    public final void refreshMarkets() {
        for (MarketAPI market : getMarketsCopy()) {
            registerMarket(market.getId());
        }
    }

    public Set<String> getRegisteredMarkets() {
        return Collections.unmodifiableSet(m_registeredMarkets);
    }

    public static final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public void reportPlayerOpenedMarket() {
        fakeAdvance();
        Global.getLogger(getClass()).error("MarketOpened");
    }

    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        final String marketID = planet.getMarket().getId();
        registerMarket(marketID);
        Global.getLogger(getClass()).error("MarketColonized");
    }

    public void reportPlayerAbandonedColony(MarketAPI market) {
        removeMarket(market.getId());
        Global.getLogger(getClass()).error("MarketAbandoned");
    }

    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        removeMarket(market.getId());
        Global.getLogger(getClass()).error("MarketDecivilized");
    }

    public void reportColonyAboutToBeDecivilized(MarketAPI a, boolean b) {}

    public static final List<CommoditySpecAPI> getEconCommodities() {
        return Global.getSettings().getAllCommoditySpecs().stream()
                .filter(spec -> !spec.isNonEcon())
                .collect(Collectors.toList());
    }

    public final CommodityInfo getCommodityInfo(String comID) {
        return m_comInfo.get(comID);
    }

    public final boolean hasCommodity(String comID) {
        return m_comInfo.containsKey(comID);
    }

    public final CommodityStats getComStats(String comID, String marketID) {
        final CommodityInfo comInfo = m_comInfo.get(comID);

        if (comInfo == null) {
            throw new RuntimeException("Referencing a non-econ or missing commodity: " + comID);
        }

        final CommodityStats stats = comInfo.getStats(marketID);
        return stats;
    }

    public static final boolean isWorkerAssignable(Industry ind) {
        final IndustryConfig config = EconomyEngine.getInstance().ind_config.get(getBaseIndustryID(ind));
        if (config != null) {
            return config.workerAssignable;
        } else {
            return ind.isIndustry() && !ind.isStructure();
        }
    }

    public static final String getBaseIndustryID(Industry ind) {
        IndustrySpecAPI currentInd = ind.getSpec();

        while (true) {
            String downgradeId = currentInd.getDowngrade();
            if (downgradeId == null) break;

            currentInd = Global.getSettings().getIndustrySpec(downgradeId);
        }

        return currentInd.getId();
    }

    public static final IndustryConfig getIndConfig(Industry ind) {
        final EconomyEngine engine = EconomyEngine.getInstance();
        IndustryConfig indConfig = engine.ind_config.get(ind.getId());

        if (indConfig == null) {
            final String baseIndustryID = getBaseIndustryID(ind);
            indConfig = engine.ind_config.get(baseIndustryID);
        }

        return indConfig;
    }

    public static final float getWorkersPerUnit(String comID, OCCTag tag) {
        final EconomyEngine engine = EconomyEngine.getInstance();

        final float Pout = Global.getSettings().getCommoditySpec(comID).getBasePrice();
        final float LPV_day = EconomyEngine.getInstance().labor_config.LPV_day;
        final float RoVC = engine.labor_config.getRoVC(tag);

        return (Pout * RoVC) / LPV_day;
    } 

    public final void weightedOutputDeficitMods() {
        for (CommodityInfo comInfo : m_comInfo.values()) {
        for (CommodityStats stats : comInfo.getStatsMap().values()) {

            float totalDeficit = 0f;

            for (Map.Entry<String, MutableStat> industryEntry : stats.getLocalProductionStat().entrySet()) {
                String industryID = industryEntry.getKey();
                MutableStat industryStat = industryEntry.getValue();

                float industryOutput = industryStat.getModifiedValue();
                float totalMarketOutput = stats.getLocalProduction(false);

                if (industryOutput <= 0 || totalMarketOutput <= 0) continue;

                float industryShare = industryOutput / totalMarketOutput;

                Industry ind = stats.market.getIndustry(industryID);
                IndustryConfig config = EconomyEngine.getIndConfig(ind);
                Map<String, Float> inputWeights;

                if (config != null) {
                    OutputCom output = config.outputs.get(stats.comID);
                    if (output == null || output.isAbstract) continue;
    
                    float sum = 0;
                    if (output.usesWorkers ||
                        output.DynamicInputsPerUnit != null && !output.DynamicInputsPerUnit.isEmpty()
                    ) {
                        sum = output.DynamicInputsPerUnit.values().stream().reduce(0f, Float::sum);
                        inputWeights = output.DynamicInputsPerUnit;
                    } else {
                        sum = output.StaticInputsPerUnit.values().stream().reduce(0f, Float::sum);
                        inputWeights = output.StaticInputsPerUnit;
                    }
                    final float finalSum = sum;
                    inputWeights.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue() / finalSum
                        ));
                } else {
                    int size = ind.getAllDemand().size();
                    if (size < 1) continue;

                    inputWeights = ind.getAllDemand().stream()
                        .collect(Collectors.toMap(
                            k -> k.getCommodityId(),
                            e -> 1f / size
                        ));
                }

                float industryDeficit = 0f;
                for (Map.Entry<String, Float> inputEntry : inputWeights.entrySet()) {
                    float weight = inputEntry.getValue();
                    String inputID = inputEntry.getKey();
                    if (inputID.equals(ABSTRACT_COM)) continue;

                    CommodityStats inputStats = getComStats(inputID, stats.market.getId());

                    industryDeficit += weight * (1 - inputStats.getStoredCoverageRatio());
                }

                totalDeficit += industryDeficit * industryShare;
            }

            stats.localProdMult = Math.max(1f - totalDeficit, 0.01f);
        }
        }
    }

    private final void assignWorkers() {

        final WorkerRegistry reg = WorkerRegistry.getInstance();

        for (MarketAPI market : getMarketsCopy()) {
            if (market.isPlayerOwned() || market.isHidden()) continue;

            final List<Industry> workingIndustries = CommodityStats.getVisibleIndustries(market);
            if (workingIndustries.isEmpty() || !market.hasCondition(WorkerPoolCondition.ConditionID)) {
                continue;
            }

            List<WorkerIndustryData> workerAssignable = new ArrayList<>(8);

            for (Industry ind : workingIndustries) {
                WorkerIndustryData data = reg.getData(market.getId(), ind.getId());
                if (ind.isFunctional() && data != null) {
                    workerAssignable.add(data);

                    data.setWorkersAssigned(0);
                }
            }

            if (workerAssignable.isEmpty()) continue;

            for (WorkerIndustryData data : workerAssignable) {
                data.setWorkersAssigned(1 / (float) workerAssignable.size());
            }
        }
    }

    /**
     * Adds Production Inputs and Outputs using data/config/industry_config.json.
     * Only supports conditions inside {@link OutputCom}.
     * Other conditional inputs or outputs must be added by the subclass manually.
     */
    public static final void applyIndustryPIOs(MarketAPI market, BaseIndustry ind) {
        final int size = market.getSize();
        final EconomyEngine engine = EconomyEngine.getInstance();
        final WorkerRegistry reg = WorkerRegistry.getInstance();

        if (engine == null || reg == null) return;

        IndustryConfig indConfig = getIndConfig(ind);

        if (indConfig == null) return; // NO-OP if no config file exists.

        final Map<String, OutputCom> indMap = indConfig.outputs;
        final Map<String, Float> totalDemandMap = new HashMap<>();
        if (indMap == null || indMap.isEmpty()) return;

        final String CONFIG_MOD_ID = "ind_" + ind.getId() + CompatLayer.CONFIG_MOD_SUFFIX;

        for (Map.Entry<String, OutputCom> entry : indMap.entrySet()) {
            OutputCom output = entry.getValue();
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(output.comID);

            if (output.usesWorkers && (output.CCMoneyDist == null || output.CCMoneyDist.isEmpty())) {
                throw new RuntimeException("Labor-driven output " + output.comID + " in " + ind.getId() +
                    " must define CCMoneyDist to calculate variable capital contribution.");
            }

            float scale = 1f * output.baseProd;
            boolean skip = false;

            if (output.checkLegality && market.isIllegal(entry.getKey())) {
                skip = true;
            }
            for (String conditionID : output.ifMarketCondsFalse) {
                if (market.hasCondition(conditionID)) {
                    skip = true;
                    break;
                }
            }
            for (String conditionID : output.ifMarketCondsTrue) {
                if (!market.hasCondition(conditionID)) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            if (output.scaleWithMarketSize) scale *= Math.pow(10, size - 3);            

            if ((output.CCMoneyDist != null && !output.CCMoneyDist.isEmpty())) {
                float totalWeight = output.CCMoneyDist.values().stream().reduce(0f, Float::sum);
                float Vcc = spec.getBasePrice() * engine.labor_config.getRoCC(indConfig.occTag);

                for (Map.Entry<String, Float> inputEntry : output.CCMoneyDist.entrySet()) {
                    String inputID = inputEntry.getKey();
                    if (inputID.equals(ABSTRACT_COM)) continue;

                    float weight = inputEntry.getValue() / totalWeight;
                    float inputValue = Vcc * weight;
                    float unitPrice = Global.getSettings().getCommoditySpec(inputID).getBasePrice();
                    float qty = inputValue * scale / unitPrice;

                    output.DynamicInputsPerUnit.put(inputID, qty);
                    totalDemandMap.merge(inputID, qty, Float::sum);
                }
            } else if (output.StaticInputsPerUnit != null && !output.StaticInputsPerUnit.isEmpty()) {
                for (Map.Entry<String, Float> demandEntry : output.StaticInputsPerUnit.entrySet()) {
                    String inputID = demandEntry.getKey();
                    if (inputID.equals(ABSTRACT_COM)) continue;

                    float qty = demandEntry.getValue() * scale;
                    totalDemandMap.merge(inputID, qty, Float::sum);
                }
            }

            if (!output.isAbstract) {
                ind.getSupply(entry.getKey()).getQuantity().modifyFlat(
                    CONFIG_MOD_ID, scale, BaseIndustry.BASE_VALUE_TEXT
                );
            }
        }

        for (Map.Entry<String, Float> entry : totalDemandMap.entrySet()) {
            ind.getDemand(entry.getKey()).getQuantity().modifyFlat(
                CONFIG_MOD_ID, entry.getValue().floatValue(), BaseIndustry.BASE_VALUE_TEXT
            );
        }
    }

    public final long getTotalGlobalExports(String comID) {
        long totalGlobalExports = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final long total = getTotalGlobalExports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalExports / (float) total) * 100);
    }

    public final long getTotalGlobalImports(String comID) {
        long totalGlobalImports = 0;
        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            totalGlobalImports += stats.globalImports;
        }

        return totalGlobalImports;
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final long total = getTotalGlobalImports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) getComStats(comID, marketID).globalImports / (float) total) * 100);
    }

    public final long getTotalInFactionExports(String comID, FactionAPI faction) {
        long TotalFactionExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            TotalFactionExports += stats.inFactionExports;
        }

        return TotalFactionExports;
    }

    public final long getFactionTotalGlobalExports(String comID, FactionAPI faction) {
        long totalGlobalExports = 0;

        for (CommodityStats stats : m_comInfo.get(comID).getAllStats()) {
            if (!stats.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }

            totalGlobalExports += stats.globalExports;
        }

        return totalGlobalExports;
    }

    public static final Pair<String, Float> getMaxDeficit(MarketAPI market, String... commodityIds) {
		// 1 is no deficit and 0 is 100% deficit
		Pair<String, Float> result = new Pair<String, Float>();
		result.two = 1f;
		if (Global.CODEX_TOOLTIP_MODE || !EconomyEngine.isInitialized()) return result;

		for (String id : commodityIds) {
			final CommodityStats stats = EconomyEngine.getInstance().getComStats(id, market.getId());
			if (stats == null) {
				return result;
			}

			float available = stats.getStoredCoverageRatio();

			if (available < result.two) {
				result.one = id;
				result.two = available;
			}
		}
		return result;
	}

    public final void buildInputToOutputsMap() {
        inputToDependentOutputs = new HashMap<>();

        for (IndustryConfig indEntry : ind_config.values()) {

            for (Map.Entry<String, OutputCom> outputEntry : indEntry.outputs.entrySet()) {
                String outputId = outputEntry.getKey();
                OutputCom output = outputEntry.getValue();

                Map<String, Float> relevantInputs = output.usesWorkers ? 
                    output.CCMoneyDist : output.StaticInputsPerUnit;

                for (String inputID : relevantInputs.keySet()) {
                    inputToDependentOutputs
                    .computeIfAbsent(inputID, k -> new ArrayList<>())
                    .add(new OutputComReference(outputId, output));
                }
            }
        }
    }

    public static class OutputComReference {
        public final String outputId;
        public final OutputCom output;

        public OutputComReference(String outputId, OutputCom output) {
            this.outputId = outputId;
            this.output = output;
        }
    }
}
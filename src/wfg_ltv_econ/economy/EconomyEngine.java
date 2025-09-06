package wfg_ltv_econ.economy;

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
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;

import wfg_ltv_econ.conditions.WorkerPoolCondition;
import wfg_ltv_econ.economy.IndustryConfigLoader.IndustryConfig;
import wfg_ltv_econ.economy.IndustryConfigLoader.OutputCom;
import wfg_ltv_econ.economy.LaborConfigLoader.LaborConfig;
import wfg_ltv_econ.economy.WorkerRegistry.WorkerIndustryData;

/**
 * Handles the trade, consumption, production and all related logic
 */
public class EconomyEngine extends BaseCampaignEventListener
    implements PlayerColonizationListener, ColonyDecivListener {

    private static EconomyEngine instance;

    private final Set<String> m_registeredMarkets;
    private final Map<String, CommodityInfo> m_comInfo;

    private transient Map<String, List<OutputComReference>> commodityToOutputMap;
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
        buildCommodityOutputMap();

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

        weightedInputDeficitMods();

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

            WorkerRegistry.getInstance().register(marketID);
        }
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

    public final void weightedOutputDeficitMods() {
        for (CommodityInfo comInfo : m_comInfo.values()) {
            for (Map.Entry<String, CommodityStats> marketEntry : comInfo.getStatsMap().entrySet()) {
                CommodityStats stats = marketEntry.getValue();

                double outputMultiplier = 1f;

                List<OutputComReference> relevantOutputs = commodityToOutputMap.get(stats.comID);
                if (relevantOutputs == null) continue; // Non-existing entries are null

                for (OutputComReference ref : relevantOutputs) {
                    OutputCom output = ref.output;
                    if (output.isAbstract) continue;

                    Map<String, Float> weights = output.usesWorkers ? 
                        output.DynamicInputWeights : output.ConsumptionMap;

                    float contribution = weights.get(stats.comID);

                    CommodityStats outputStats = getComStats(ref.outputId, marketEntry.getKey());

                    outputMultiplier -= contribution * (1f - outputStats.getStoredCoverageRatio());
                }

                stats.localProdMult = (float) Math.max(outputMultiplier, 0.01f);
            }
        }
    }

    public final void weightedInputDeficitMods() {
        for (CommodityInfo comInfo : m_comInfo.values()) {
            for (Map.Entry<String, CommodityStats> marketEntry : comInfo.getStatsMap().entrySet()) {
                CommodityStats stats = marketEntry.getValue();
                double maxInputMultiplier = 1f;

                List<OutputComReference> relevantOutputs = commodityToOutputMap.get(stats.comID);
                if (relevantOutputs == null) continue; // Non-existing entries are null

                for (OutputComReference ref : relevantOutputs) {
                    if (ref.output.isAbstract) continue;

                    Map<String, Float> weights = ref.output.usesWorkers ? 
                        ref.output.DynamicInputWeights : ref.output.ConsumptionMap;

                    double contribution = weights.get(stats.comID);
                    CommodityStats outputStats = getComStats(ref.outputId, marketEntry.getKey());

                    maxInputMultiplier = Math.min(maxInputMultiplier, outputStats.localProdMult / contribution);
                }

                stats.demandBaseMult = (float) Math.max(maxInputMultiplier, 0f);
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

        final String baseIndustryID = getBaseIndustryID(ind);

        final IndustryConfig indConfig = engine.ind_config.get(baseIndustryID);

        if (indConfig == null) return; // NO-OP if no config file exists.

        final Map<String, OutputCom> indMap = indConfig.outputs;
        final Map<String, Float> totalDemandMap = new HashMap<>();
        if (indMap == null || indMap.isEmpty()) return;

        final String ABSTRACT_COM = "abstract";

        for (Map.Entry<String, OutputCom> entry : indMap.entrySet()) {
            OutputCom output = entry.getValue();
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(output.comID);

            float scale = 1f;
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

            if (!output.usesWorkers) {
                for (Map.Entry<String, Float> demandEntry : output.ConsumptionMap.entrySet()) {
                    String input = demandEntry.getKey();
                    if (input.equals(ABSTRACT_COM)) continue;

                    float demandAmount = demandEntry.getValue() * output.baseProd * scale;
                    totalDemandMap.merge(input, demandAmount, Float::sum);
                }
                if (!output.isAbstract) {
                    int finalSupply = (int) (output.baseProd * scale);
                    ind.supply(entry.getKey(), finalSupply);
                }
            } else {
                float Vcc = spec.getBasePrice() * engine.labor_config.getRoCC(indConfig.occTag);

                // Allocate constant capital to inputs
                float totalWeight = output.CCMoneyDist.values().stream().reduce(0f, Float::sum);
                for (Map.Entry<String, Float> ccEntry : output.CCMoneyDist.entrySet()) {
                    String inputID = ccEntry.getKey();
                    if (inputID.equals(ABSTRACT_COM)) continue;

                    float weight = ccEntry.getValue() / totalWeight;
                    float inputValue = Vcc * weight;

                    output.DynamicInputWeights.put(inputID, weight);

                    float unitPrice = Global.getSettings().getCommoditySpec(inputID).getBasePrice();
                    float qty = inputValue / unitPrice;

                    qty *= scale;

                    totalDemandMap.merge(inputID, qty, Float::sum);
                }

                // Handle outputs
                if (!output.isAbstract && EconomyEngine.isWorkerAssignable(ind)) {

                    float Pout = Global.getSettings().getCommoditySpec(entry.getKey()).getBasePrice();

                    float LPV_day = EconomyEngine.getInstance().labor_config.LPV_day;
                    float RoVC = engine.labor_config.getRoVC(indConfig.occTag);

                    float workersPerUnit = (Pout * RoVC) / LPV_day;

                    float supplyQty = scale / workersPerUnit;

                    final String baseID = "ind_" + ind.getId() + "_0";
                    ind.getSupply(entry.getKey()).getQuantity().modifyFlat(
                        baseID, supplyQty, BaseIndustry.BASE_VALUE_TEXT
                    );
                }
            }
        }

        for (Map.Entry<String, Float> entry : totalDemandMap.entrySet()) {
            ind.demand(entry.getKey(), entry.getValue().intValue());
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

    public final void buildCommodityOutputMap() {
        commodityToOutputMap = new HashMap<>();

        for (IndustryConfig indEntry : ind_config.values()) {

            for (Map.Entry<String, OutputCom> outputEntry : indEntry.outputs.entrySet()) {
                String outputId = outputEntry.getKey();
                OutputCom output = outputEntry.getValue();

                // Combine ConsumptionMap and InputWeights for lookup
                Map<String, Float> relevantInputs = output.usesWorkers ? 
                    output.DynamicInputWeights : output.ConsumptionMap;

                for (String inputID : relevantInputs.keySet()) {
                    commodityToOutputMap
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
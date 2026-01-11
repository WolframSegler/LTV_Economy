package wfg.ltv_econ.economy.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode;

import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.constants.SubmarketsID;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.PlayerFactionSettings;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.wrap_ui.util.NumFormat;
import static wfg.ltv_econ.constants.economyValues.*;
import static wfg.wrap_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.graid.CommodityGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidType;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.GroundRaidObjectivesListener;

/**
 * The {@link EconomyEngine} is the core controller for the LTV_Economy simulation.
 * <p>
 * It manages all economic activity across markets, including:
 * <ul>
 *     <li>Commodity production, demand, and trade flows</li>
 *     <li>Market credit balances and transactions</li>
 *     <li>Worker assignment and labor optimization</li>
 *     <li>Synchronization with the Starsector campaign economy</li>
 * </ul>
 *
 * <h3>Overview</h3>
 * Each {@link MarketAPI} in the sector is represented internally by:
 * <ul>
 *     <li>A credit balance (per-market budget)</li>
 *     <li>A set of active commodities ({@link CommodityDomain})</li>
 *     <li>Dynamic production and demand statistics ({@link CommodityCell})</li>
 * </ul>
 * The engine runs continuously as a listener to the campaign economy. It replaces
 * vanilla credit flows with its own localized financial model, while remaining compatible
 * with other game systems and mods.
 *
 * <h3>Main Responsibilities</h3>
 * <ul>
 *     <li>Maintain lists of all registered and player-owned markets.</li>
 *     <li>Track and update per-market credit balances.</li>
 *     <li>Run the economic update loop (production → demand → trade → post-processing).</li>
 *     <li>Handle market lifecycle events such as colonization, decivilization, and abandonment.</li>
 * </ul>
 *
 * <h3>Internal Structure</h3>
 * <ul>
 *     <li>{@code m_registeredMarkets} – All markets currently part of the simulation.</li>
 *     <li>{@code m_playerMarketData} – Subset of markets owned by the player with unique data attached.</li>
 *     <li>{@code m_marketCredits} – Per-market credit reserves.</li>
 *     <li>{@code m_comDomains} – Mapping of commodity IDs to {@link CommodityDomain} containers.</li>
 *     <li>{@code mainLoopExecutor} – A single-thread executor that runs the simulation asynchronously. Can be toggled.</li>
 * </ul>
 *
 * <h3>Main Loop</h3>
 * The {@link #realAdvance()} method executes the economic simulation for one tick:
 * <ol>
 *     <li>Refreshes the list of active markets.</li>
 *     <li>Resets and recalculates commodity production/demand via {@link CommodityDomain}.</li>
 *     <li>Assigns workers using a solver-based optimization step.</li>
 *     <li>Executes trade and adjusts credit balances accordingly.</li>
 *     <li>Advances all commodities to persist state.</li>
 * </ol>
 *
 * @author Wolfram Segler
 */
public class EconomyEngine extends BaseCampaignEventListener implements
    EveryFrameScript, PlayerColonizationListener, ColonyDecivListener, GroundRaidObjectivesListener,
    CoreUITabListener
{
    private static EconomyEngine instance;

    public final EconomyInfo info = new EconomyInfo(this);
    public final EconomyLogger logger = new EconomyLogger(this);
    final EconomyLoop loop = new EconomyLoop(this);
    
    final Set<String> m_registeredMarkets = new HashSet<>();
    final Map<String, CommodityDomain> m_comDomains = new HashMap<>();
    final Map<String, PlayerMarketData> m_playerMarketData = new HashMap<>();
    final Map<String, Long> m_marketCredits = new HashMap<>();

    private transient ExecutorService mainLoopExecutor;

    public final PlayerFactionSettings playerFactionSettings = new PlayerFactionSettings(); 

    public static void setInstance(EconomyEngine a) {
        instance = a;
    }

    public static EconomyEngine getInstance() {
        if (instance == null) return EconomyEngineSerializer.loadInstance(false);
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    EconomyEngine() {
        super(false);

        instance = this;

        for (String comID : EconomyInfo.getEconCommodityIDs()) {
            m_comDomains.put(comID, new CommodityDomain(comID));
        }

        for (MarketAPI market : EconomyInfo.getMarketsCopy()) registerMarket(market);

        readResolve();
    }

    public final Object readResolve() {
        mainLoopExecutor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "LTV-MainLoop");
            t.setDaemon(true);
            return t;
        });

        return this;
    }

    protected int dayKeyTracker = -1;
    protected boolean midDayApplied = false;

    public boolean isDone() { return false;}
    public boolean runWhilePaused() { return false;}
    public final void advance(float delta) {
        final CampaignClockAPI clock = Global.getSector().getClock();
        final int hour = clock.getHour();
        final int dayKey = (clock.getCycle() << 10) + (clock.getMonth() << 5) + clock.getDay();

        if (hour >= 6 && !midDayApplied) {
            applyPopulationStabilityMods();
            midDayApplied = true;
        }

        if (dayKeyTracker == -1) { dayKeyTracker = dayKey; return;}
        if (dayKeyTracker == dayKey) return;

        dayKeyTracker = dayKey;
        midDayApplied = false;

        if (EconomyConfig.MULTI_THREADING) {
            mainLoopExecutor.submit(() -> {
                try {
                    realAdvance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            realAdvance();
        }
    }

    private final void realAdvance() {
        loop.mainLoop(false, false);
    }

    public final void fakeAdvance() {
        loop.mainLoop(true, false);
    }

    public final void fakeAdvanceWithAssignWorkers() {
        loop.mainLoop(true, true);
    }

    public final void registerMarket(MarketAPI market) {
        // Order here is very important
        final String marketID = market.getId();
        WorkerRegistry.getInstance().register(market);
        if (!m_registeredMarkets.add(marketID)) return;

        m_marketCredits.put(marketID, (long) EconomyConfig.STARTING_CREDITS_FOR_MARKET);
        if (market.isPlayerOwned()) {
            m_playerMarketData.put(marketID, new PlayerMarketData(marketID));
            market.addSubmarket(SubmarketsID.STOCKPILES);
            market.removeSubmarket(Submarkets.LOCAL_RESOURCES);
        }

        m_comDomains.values().forEach(c -> c.addMarket(marketID));
    }

    public final void removeMarket(MarketAPI market) {
        final String marketID = market.getId();
        if (!m_registeredMarkets.remove(marketID)) return;

        for (CommodityDomain dom : m_comDomains.values()) {
            dom.removeMarket(marketID);
        }

        if (market.isPlayerOwned()) m_playerMarketData.remove(marketID);
        m_marketCredits.remove(marketID);
        WorkerRegistry.getInstance().remove(marketID);
    }

    public final void removeMarket(String marketID) {
        if (!m_registeredMarkets.remove(marketID)) return;

        for (CommodityDomain dom : m_comDomains.values()) {
            dom.removeMarket(marketID);
        }

        m_playerMarketData.remove(marketID);
        m_marketCredits.remove(marketID);
        WorkerRegistry.getInstance().remove(marketID);
    }

    public final void refreshMarkets() {
        loop.refreshMarkets();
    }

    public Set<String> getRegisteredMarkets() {
        return Collections.unmodifiableSet(m_registeredMarkets);
    }

    public Map<String, PlayerMarketData> getPlayerMarketData() {
        return Collections.unmodifiableMap(m_playerMarketData);
    }

    public final boolean isPlayerMarket(String marketID) {
        return m_playerMarketData.keySet().contains(marketID);
    }

    public final PlayerMarketData getPlayerMarketData(String marketID) {
        return m_playerMarketData.get(marketID);
    }

    public final PlayerMarketData addPlayerMarketData(String marketID) {
        if (m_playerMarketData.containsKey(marketID)) return m_playerMarketData.get(marketID);
        final PlayerMarketData data = new PlayerMarketData(marketID);
        m_playerMarketData.put(marketID, data);
        return data;
    }

    public void reportPlayerOpenedMarket() {
        fakeAdvance();
    }

    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        registerMarket(planet.getMarket());
    }

    public void reportPlayerAbandonedColony(MarketAPI market) {
        removeMarket(market);
    }

    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        removeMarket(market);
    }

    public void reportColonyAboutToBeDecivilized(MarketAPI a, boolean b) {}

    public final List<CommodityDomain> getComDomains() {
        return new ArrayList<>(m_comDomains.values());
    }

    public final CommodityDomain getComDomain(String comID) {
        return m_comDomains.get(comID);
    }

    public final boolean hasCommodity(String comID) {
        return m_comDomains.containsKey(comID);
    }

    public final CommodityCell getComCell(String comID, String marketID) {
        final CommodityDomain dom = m_comDomains.get(comID);

        if (dom == null) {
            throw new RuntimeException("Referencing a non-econ or missing commodity: " + comID);
        }

        final CommodityCell cell = dom.getCell(marketID);
        if (cell == null) return new CommodityCell(comID, marketID);
        return cell;
    }

    public final void addCredits(String marketID, long amount) {
        long current = m_marketCredits.getOrDefault(marketID, 0l);

        if (amount > 0 && current > Long.MAX_VALUE - amount) {
            current = Long.MAX_VALUE;

        } else if (amount < 0 && current < Long.MIN_VALUE - amount) {
            current = Long.MIN_VALUE;

        } else {
            current += amount;
        }

        m_marketCredits.put(marketID, current);
    }

    /**
     * Returns 0 if no market is registered
     */
    public final long getCredits(String marketID) {
        return m_marketCredits.getOrDefault(marketID, 0l);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        for (CommodityDomain dom : m_comDomains.values()) {
            dom.endMonth();
        }
        
        final MonthlyReport report = SharedData.getData().getCurrentReport();
        final FDNode marketsNode = report.getNode(MonthlyReport.OUTPOSTS);
		marketsNode.name = "Colonies";
		marketsNode.custom = MonthlyReport.OUTPOSTS;
		marketsNode.tooltipCreator = report.getMonthlyReportTooltip();

        for (PlayerMarketData data : m_playerMarketData.values()) {
            final MarketAPI market = data.market;
            final long netIncome = info.getNetIncome(market, true);
            final float r = data.getEffectiveProfitRatio();
            final float playerIncome = Math.max(netIncome, 0) * r;
            addCredits(data.marketID, (long) -playerIncome);
            
            final FDNode mNode = report.getNode(marketsNode, market.getId());
            mNode.name = market.getName() + " (" + market.getSize() + ")";
            mNode.custom = market;

            // Player cut node
            final FDNode playerIncomeNode = report.getNode(mNode, "player_share");
            playerIncomeNode.name = "Effective player share (" + Math.round(r * 100) + "%)";
            playerIncomeNode.icon = Global.getSettings().getSpriteName("icons", "ratio_chart");
            playerIncomeNode.income = 0.0001f;
            playerIncomeNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    tp.addPara(
                        "The ratio of monthly profits that get automatically transferred to you: %s.",
                        pad, highlight, NumFormat.formatCredit((long) playerIncome)
                    );
                    tp.addPara(
                        "The effective value can be below the chosen value if the colony is in debt.", 
                        pad
                    );
                    tp.addPara("All income values are modified by this value", pad);
                }
            };

            if (playerIncome < 1) playerIncomeNode.name += " - none";

            // Industries & structures
            final FDNode indNode = report.getNode(mNode, "industries"); 
            indNode.name = "Industries & structures";
            indNode.custom = MonthlyReport.INDUSTRIES;
            indNode.mapEntity = market.getPrimaryEntity();
            indNode.tooltipCreator = report.getMonthlyReportTooltip();
            indNode.getChildren().clear();
            for (Industry ind : market.getIndustries()) {
                final FDNode iNode = report.getNode(indNode, ind.getId());
                iNode.name = ind.getCurrentName();
                iNode.income += info.getIndustryIncome(ind).getModifiedInt() * r;
                iNode.upkeep += info.getIndustryUpkeep(ind).getModifiedInt() * r;
                iNode.custom = ind;
                iNode.mapEntity = market.getPrimaryEntity();
            }

            // Exports
            final FDNode exportNode = report.getNode(mNode, "exports"); 
            exportNode.name = "Exports";
            exportNode.custom = MonthlyReport.EXPORTS;
            exportNode.mapEntity = market.getPrimaryEntity();
            exportNode.getChildren().clear();
            exportNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    final String inFactionBonus = String.format("%d%%",
                        (int)(1f - EconomyConfig.FACTION_EXCHANGE_MULT)*100);

                    tp.addPara(
                        "Income from exports by this outpost or colony. " +
                        "Smuggling exports do not produce income. " +
                        "In-faction imports are %s cheaper than normal",
                        pad, highlight, inFactionBonus
                    );
                }
            };
            for (CommoditySpecAPI com : EconomyInfo.getEconCommodities()) {
                final FDNode eNode = report.getNode(exportNode, com.getId());
                final String comID = com.getId();
                eNode.name = com.getName();
                eNode.income += info.getExportIncome(market, comID, true) * r;
                eNode.upkeep += info.getImportExpense(market, comID, true) * r;
                eNode.mapEntity = market.getPrimaryEntity();
            }

            // Wages
            final FDNode wageNode = report.getNode(mNode, "wages"); 
            wageNode.name = "Wages";
            wageNode.mapEntity = market.getPrimaryEntity();
            wageNode.upkeep += info.getWagesForMarket(market)*MONTH * r;
            wageNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    tp.addPara(
                        "Monthly wages for workers at this colony.", pad
                    );
                }
            };
        }
    }

    /**
     * This method runs after {@link #reportEconomyTick}. Practically the same method but delayed.
     */
    @Override
    public void reportEconomyMonthEnd() {}

    public void modifyRaidObjectives(MarketAPI market, SectorEntityToken entity,
        List<GroundRaidObjectivePlugin> objectives, RaidType type, int marineTokens, int priority
    ) {}
	
	public void reportRaidObjectivesAchieved(RaidResultData data, InteractionDialogAPI dialog,
        Map<String, MemoryAPI> memoryMap
    ) {
        if (!data.market.isInEconomy()) return;
        for (GroundRaidObjectivePlugin objective : data.objectives) {
            if (objective instanceof CommodityGroundRaidObjectivePluginImpl obj) {
                if (!m_comDomains.containsKey(objective.getId())) continue;
                final CommodityCell cell = getComCell(obj.getId(), data.market.getId());
                cell.addStoredAmount(-obj.getQuantityLooted());
            }
        }
    }

    public void reportFleetSpawned(CampaignFleetAPI fleet) {

    }

    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {

    }

    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpDestination to) {

    }

    public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {
        
    }
    
    public void reportAboutToOpenCoreTab(CoreUITabId tabID, Object param) {
        if (tabID == CoreUITabId.CARGO) applyPopulationStabilityMods();
    }

    private final void applyPopulationStabilityMods() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            applyPopulationStabilityMods(market);
        }
    }

    public final void applyPopulationStabilityMods(MarketAPI market) {
        final String marketID = market.getId();
        final String popID = "ind_" + Industries.POPULATION + "_";
        final CommodityCell domCell = getComCell(Commodities.DOMESTIC_GOODS, marketID);
        final CommodityCell luxCell = getComCell(Commodities.LUXURY_GOODS, marketID);
        final CommodityCell foodCell = getComCell(Commodities.FOOD, marketID);
        final CommodityCell orgCell = getComCell(Commodities.ORGANICS, marketID);
        
        if (domCell.getStoredAvailabilityRatio() > 0.9) { // 90% or more
            market.getStability().modifyFlat(popID + 0, 1, "Domestic goods demand met");
        } else market.getStability().unmodifyFlat(popID + 0);

        final int luxuryThreshold = 3;
        if (luxCell.getStoredAvailabilityRatio() > 0.9 && market.getSize() > luxuryThreshold) {
            market.getStability().modifyFlat(popID + 1, 1, "Luxury goods demand met");
        } else market.getStability().unmodifyFlat(popID + 1);

        final boolean useOrganicsValues = foodCell.getStoredAvailabilityRatio() >
            orgCell.getStoredAvailabilityRatio() && !market.hasCondition(Conditions.HABITABLE);
        
        final String com = useOrganicsValues ? Commodities.FOOD : Commodities.ORGANICS;
        final float ratio = useOrganicsValues ? orgCell.getStoredAvailabilityRatio() :
           foodCell.getStoredAvailabilityRatio();

        if (ratio < 0.9) { // less than 90%
            final int stabilityPenalty = 
                ratio < 0.1 ? -3 :
                ratio < 0.4 ? -2 :
                ratio < 0.7 ? -1 : 0;
            market.getStability().modifyFlat(popID + 2, stabilityPenalty, BaseIndustry.getDeficitText(com));
        } else market.getStability().unmodifyFlat(popID + 2);
    }
}
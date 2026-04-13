package wfg.ltv_econ.economy.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode;
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode;

import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.constants.SubmarketsID;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.ltv_econ.economy.raids.CommodityCellGroundRaidObjective;
import wfg.ltv_econ.economy.raids.LtvShipWeaponsGroundRaidObjective;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.industry.IndustryTooltips;
import wfg.ltv_econ.intel.PlayerMarketBombardedIntel;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.ltv_econ.util.Arithmetic;
import wfg.native_ui.util.ArrayMap;
import wfg.native_ui.util.NumFormat;
import static wfg.ltv_econ.constants.EconomyConstants.*;
import static wfg.ltv_econ.constants.strings.Income.*;
import static wfg.native_ui.util.UIConstants.*;

import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidType;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.TempData;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.campaign.econ.Economy;
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.GroundRaidObjectivesListener;

/**
 * The {@link EconomyEngine} is the core controller for the LTV-Economy simulation.
 * <p>
 * It manages all economic activity across markets, including:
 * <ul>
 *     <li>Commodity production, demand, and trade flows</li>
 *     <li>Market credit balances and transactions</li>
 *     <li>Worker assignment and labor optimization</li>
 *     <li>Synchronization with the Starsector campaign economy</li>
 * </ul>
 *
 * The engine runs continuously as a listener to the campaign economy. It replaces
 * vanilla credit flows with its own localized financial model, while remaining compatible
 * with other game systems and mods.
 *
 * <h3>Main Responsibilities</h3>
 * <ul>
 *     <li>Maintain lists of all registered and player-owned markets.</li>
 *     <li>Track and update per-market credit balances.</li>
 *     <li>Run the economic update loop.</li>
 *     <li>Handle market lifecycle events such as colonization, decivilization, and abandonment.</li>
 * </ul>
 *
 * @author Wolfram Segler
 */
public class EconomyEngine implements Serializable, EveryFrameScript, PlayerColonizationListener, ColonyDecivListener,
    GroundRaidObjectivesListener, CoreUITabListener, EconomyTickListener, ColonyInteractionListener, ColonyPlayerHostileActListener
{
    public transient EconomyInfo info;
    public transient EconomyLogger logger;
    transient EconomyLoop loop;
    private transient ExecutorService mainLoopExecutor;
    
    final Set<String> registeredMarkets = new HashSet<>();
    final ArrayMap<String, CommodityDomain> comDomains = new ArrayMap<>(EconomyConstants.econCommodityIDs.size());
    final ArrayMap<String, PlayerMarketData> playerMarketData = new ArrayMap<>();
    final ArrayMap<String, Long> marketCredits = new ArrayMap<>(EconomyInfo.getMarketsCount());
    final ArrayMap<String, FactionShipInventory> factionShipInventories = new ArrayMap<>(EconomyConstants.visibleFactionIDs.size());
    final List<TradeMission> activeMissions = new ArrayList<>();
    final List<TradeMission> pastMissions = new ArrayList<>();

    protected int dayKeyTracker = -1;
    protected int cyclesSinceWorkerAssign = EconomyConfig.WORKER_ASSIGN_INTERVAL;
    protected int cyclesSinceTrade = EconomyConfig.TRADE_INTERVAL;
    protected int lastTradeCycle = EconomyConfig.TRADE_INTERVAL;
    protected boolean midDayApplied = false;

    public static EconomyEngine instance() {
        return LtvEconSaveData.instance().economyEngine;
    }

    public EconomyEngine() {
        for (String comID : EconomyConstants.econCommodityIDs) {
            comDomains.put(comID, new CommodityDomain(comID));
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

        info = new EconomyInfo(this);
        logger = new EconomyLogger(this);
        loop = new EconomyLoop(this);

        return this;
    }

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
            mainLoopExecutor.execute(this::realAdvance);
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
        WorkerRegistry.instance().register(market);
        MarketFinanceRegistry.instance().register(market);
        if (!registeredMarkets.add(marketID)) return;

        marketCredits.put(marketID, (long) EconomyConfig.STARTING_CREDITS_FOR_MARKET);
        if (market.isPlayerOwned()) {
            playerMarketData.put(marketID, new PlayerMarketData(marketID));
            market.addSubmarket(SubmarketsID.STOCKPILES);
            market.removeSubmarket(Submarkets.LOCAL_RESOURCES);
        }

        comDomains.values().forEach(c -> c.addMarket(marketID));
    }

    public final void removeMarket(MarketAPI market) {
        removeMarket(market.getId());
    }

    public final void removeMarket(String marketID) {
        if (!registeredMarkets.remove(marketID)) return;

        for (CommodityDomain dom : comDomains.values()) {
            dom.removeMarket(marketID);
        }

        playerMarketData.remove(marketID);
        marketCredits.remove(marketID);
        MarketFinanceRegistry.instance().remove(marketID);
        WorkerRegistry.instance().remove(marketID);
        for (TradeMission m : activeMissions) {
            if (m.src.getId().equals(marketID) || m.dest.getId().equals(marketID)) {
                m.status = MissionStatus.CANCELLED;
            }
        }
        pastMissions.removeIf(m -> m.src.getId().equals(marketID) || m.dest.getId().equals(marketID));
    }

    public final void refreshMarkets() {
        loop.refreshMarkets();
    }

    public final void refreshMarketsHard() {
        loop.refreshMarketsHard();
    }

    public Set<String> getRegisteredMarkets() {
        return Collections.unmodifiableSet(registeredMarkets);
    }

    public Map<String, PlayerMarketData> getPlayerMarketData() {
        return Collections.unmodifiableMap(playerMarketData);
    }

    public final boolean isPlayerMarket(String marketID) {
        return playerMarketData.containsKey(marketID);
    }

    public final PlayerMarketData getPlayerMarketData(String marketID) {
        return playerMarketData.get(marketID);
    }

    public final PlayerMarketData addPlayerMarketData(String marketID) {
        return playerMarketData.computeIfAbsent(marketID, m -> new PlayerMarketData(marketID));
    }

    public final List<CommodityDomain> getComDomains() {
        return new ArrayList<>(comDomains.values());
    }

    public final CommodityDomain getComDomain(String comID) {
        return comDomains.get(comID);
    }

    public final boolean hasCommodity(String comID) {
        return comDomains.containsKey(comID);
    }

    public final CommodityCell getComCell(String comID, String marketID) {
        final CommodityDomain dom = comDomains.get(comID);

        if (dom == null) {
            throw new IllegalArgumentException("Referencing a non-econ or missing commodity: " + comID);
        }

        return dom.getCell(marketID);
    }

    public final void addCredits(String marketID, double amount) {
        addCredits(marketID, (long) amount);
    }

    public final void addCredits(String marketID, long amount) {
        final long current = marketCredits.getOrDefault(marketID, 0l);
        final long newValue = Arithmetic.clamp(current + amount, Long.MIN_VALUE, Long.MAX_VALUE);
        marketCredits.put(marketID, newValue);
    }

    public final long getCredits(String marketID) {
        return marketCredits.getOrDefault(marketID, 0l);
    }

    public final FactionShipInventory getFactionShipInventory(String factionID) {
        return factionShipInventories.computeIfAbsent(factionID, k -> new FactionShipInventory(factionID));
    }

    public final List<TradeMission> getActiveMissions() {
        return Collections.unmodifiableList(activeMissions);
    }

    public final List<TradeMission> getPastMissions() {
        return Collections.unmodifiableList(pastMissions);
    }

    public final int getCyclesSinceTrade() {
        return cyclesSinceTrade;
    }

    public final void applyPopulationStabilityMods(MarketAPI market) {
        // TODO Call this using industry post apply hook after update
        if (!registeredMarkets.contains(market.getId())) return;
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

    // LISTENERS ------------------------------------------------------------------------------------

    public void reportEconomyTick(int iterIndex) {
        if (Economy.NUM_ITER_PER_MONTH - 1 != iterIndex) return; // isEndOfMonth

        final MarketFinanceRegistry financeReg = MarketFinanceRegistry.instance();
        final MonthlyReport report = SharedData.getData().getCurrentReport();

        endMonth();
        playerMarketData.values().forEach(PlayerMarketData::endMonth);
        factionShipInventories.values().forEach(FactionShipInventory::endMonth);
        financeReg.endMonth();
        
        final FDNode marketsNode = report.getNode(MonthlyReport.OUTPOSTS);
        marketsNode.name = "Colonies";
        marketsNode.custom = MonthlyReport.OUTPOSTS;
        marketsNode.tooltipCreator = report.getMonthlyReportTooltip();

        for (PlayerMarketData data : playerMarketData.values()) {
            final MarketAPI market = data.market;
            final long netIncome = financeReg.getLedger(market).getNetLastMonth();
            final float r = data.getEffectiveProfitRatio();
            final float playerIncome = Math.max(netIncome, 0) * r;
            addCredits(data.marketID, (long) -playerIncome);
            
            final FDNode mNode = report.getNode(marketsNode, market.getId());
            mNode.name = market.getName() + " (" + market.getSize() + ")";
            mNode.custom = market;

            // Industries & structures
            final FDNode indNode = report.getNode(mNode, "industries"); 
            indNode.name = "Industries & Structures";
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
                iNode.tooltipCreator = new TooltipCreator() {
                    public boolean isTooltipExpandable(Object params) {
                        return ind == null ? false : ind.isTooltipExpandable();
                    }

                    public float getTooltipWidth(Object params) {
                        return ind == null ? 400f : ind.getTooltipWidth();
                    }

                    public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                        if (ind != null) {
                            IndustryTooltips.createIndustryTooltip(IndustryTooltipMode.NORMAL, tp, expanded, ind);
                        }
                    }
                };
            }

            // Exports
            final FDNode exportNode = report.getNode(mNode, "exports"); 
            exportNode.name = "Exports & Imports";
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
            for (CommoditySpecAPI com : EconomyConstants.econCommoditySpecs) {
                final FDNode eNode = report.getNode(exportNode, com.getId());
                final String comID = com.getId();
                eNode.name = com.getName();
                eNode.income += info.getExportIncome(market, comID, true) * r;
                eNode.upkeep += info.getImportExpense(market, comID, true) * r;
                eNode.mapEntity = market.getPrimaryEntity();
                eNode.icon = com.getIconName();
            }

            // Wages
            final FDNode wageNode = report.getNode(mNode, "wages"); 
            wageNode.name = "Wages";
            wageNode.mapEntity = market.getPrimaryEntity();
            wageNode.icon = Global.getSettings().getSpriteName("income_report", "generic_expense");
            wageNode.upkeep = info.getDailyWages(market) * MONTH * r;
            wageNode.tooltipCreator = new TooltipCreator() {
                public boolean isTooltipExpandable(Object params) {return false;}

                public float getTooltipWidth(Object params) {return 400f;}

                public void createTooltip(TooltipMakerAPI tp, boolean expanded, Object params) {
                    tp.addPara(
                        "Monthly wages for workers at this colony.", pad
                    );
                }
            };
        
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
        }
    }

    /**
     * This method runs after {@link #reportEconomyTick} and unlike it, only runs once a month.
     */
    public void reportEconomyMonthEnd() {}

    public void reportPlayerOpenedMarket(MarketAPI market) {
        fakeAdvance();
        applyPopulationStabilityMods(market);
    }

    public void reportPlayerClosedMarket(MarketAPI market) {
        applyPopulationStabilityMods(market);
    }

    public void reportPlayerColonizedPlanet(PlanetAPI planet) {
        registerMarket(planet.getMarket());
    }

    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {};

    public void reportPlayerMarketTransaction(PlayerMarketTransaction trs) {};

    public void reportPlayerAbandonedColony(MarketAPI market) {
        removeMarket(market);
    }

    public void reportColonyDecivilized(MarketAPI market, boolean fullyDestroyed) {
        removeMarket(market);
    }

    public void reportColonyAboutToBeDecivilized(MarketAPI a, boolean b) {}

    public void modifyRaidObjectives(MarketAPI market, SectorEntityToken entity,
        List<GroundRaidObjectivePlugin> objectives, RaidType type, int marineTokens, int priority
    ) {
        if (priority != 1 || type != RaidType.VALUABLE || market == null) return;
		final List<CommodityCell> raidValuables = computeRaidValuables(market);

        objectives.removeIf(o -> comDomains.containsKey(o.getId()));

        for (CommodityCell cell : raidValuables) {
            final CommodityCellGroundRaidObjective curr = new CommodityCellGroundRaidObjective(cell);
            if (curr.getQuantity(1) <= 0) continue;
            objectives.add(curr);
        }
        
        final LtvShipWeaponsGroundRaidObjective weapons = new LtvShipWeaponsGroundRaidObjective(market);
        if (weapons.getQuantity(1) > 0) objectives.add(weapons);
    }
	
	public void reportRaidObjectivesAchieved(RaidResultData data, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {}
    
    public void reportAboutToOpenCoreTab(CoreUITabId tabID, Object param) {
        if (tabID == CoreUITabId.CARGO) applyPopulationStabilityMods();
    }

    public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, TempData actionData, CargoAPI cargo) {}
	public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, TempData actionData, Industry industry) {}
	
	public final void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, TempData actionData) {
        applyBombardmentStockpileReduction(market, false);
    }

	public final void reportSaturationBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, TempData actionData) {
        applyBombardmentStockpileReduction(market, true);
    }

    // PRIVATES ------------------------------------------------------------------------------------

    private final void applyBombardmentStockpileReduction(MarketAPI market, boolean isSaturation) {
        if (market == null) return;
        final String marketID = market.getId();
        
        float reduction = isSaturation ? 0.7f : 0.3f;
        reduction += (float) Math.random() * 0.2f - 0.1f;
        reduction = Arithmetic.clamp(reduction, 0.1f, 0.95f);
        
        for (CommodityDomain dom : comDomains.values()) {
            final CommodityCell cell = dom.getCell(marketID);
            if (cell == null) continue;

            cell.addStoredAmount(-reduction * cell.getStored());
        }

        if (isPlayerMarket(marketID)) {
            final PlayerMarketData data = getPlayerMarketData(marketID);
            final float penalty = isSaturation ? PlayerMarketData.BASELINE_VALUE : PlayerMarketData.BASELINE_VALUE / 4f;
            data.setHealth(data.getHealth() - penalty);
            data.setHappiness(data.getHappiness() - penalty);

            Global.getSector().getIntelManager().addIntel(
                new PlayerMarketBombardedIntel(data, reduction, penalty, isSaturation),
                false
            );
        }
    }

    private final void applyPopulationStabilityMods() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            applyPopulationStabilityMods(market);
        }
    }
    
    private final List<CommodityCell> computeRaidValuables(MarketAPI market) {
		final List<CommodityCell> result = new ArrayList<>();
        for (CommodityDomain dom : comDomains.values()) {
            if (dom.spec.isPersonnel() || dom.spec.isMeta()) continue;
            final CommodityCell cell = dom.getCell(market.getId());

            if (cell != null && cell.getStored() > 1.0) result.add(cell);
        }
		return result;
	}

    private final void endMonth() {
        final MarketFinanceRegistry reg = MarketFinanceRegistry.instance();

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!registeredMarkets.contains(market.getId())) continue;
            final MarketLedger ledger = reg.getLedger(market.getId());

            for (Industry ind : market.getIndustries()) {
                final int indIncome = info.getIndustryIncome(ind).getModifiedInt();
                if (indIncome != 0) {
                    ledger.add(INDUSTRY_INCOME_KEY + ind.getId(), indIncome, ind.getCurrentName() + " income");
                }

                final int indUpkeep = info.getIndustryUpkeep(ind).getModifiedInt();
                if (indUpkeep != 0) {
                    ledger.add(INDUSTRY_UPKEEP_KEY + ind.getId(), -indUpkeep, ind.getCurrentName() + " upkeep");
                }
            }
        }
    }
}
package wfg.ltv_econ.economy.engine;

import static wfg.native_ui.util.Globals.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.EconConfig.DebtDebuffTier;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.TradeCom;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.ShipAllocator;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.economy.planning.WorkforceAllocator;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry;
import wfg.ltv_econ.economy.registry.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.ArrayMap;

import static wfg.ltv_econ.constants.strings.Income.*;

public class EconomyLoop {
    private static final Logger log = Global.getLogger(EconomyLoop.class);
    public static final String KEY = "::";

    transient EconomyEngine engine;

    public EconomyLoop(EconomyEngine engine) { this.engine = engine; }    

    /** Not order agnostic */
    final void mainLoop(boolean fakeAdvance, boolean forceWorkerAssignment) {
        final boolean allocWorkers = engine.cyclesSinceWorkerAssign >= EconConfig.WORKER_ASSIGN_INTERVAL || forceWorkerAssignment;
        refreshMarkets();

        discoverInputsOutputs();

        engine.comDomains.values().forEach(CommodityDomain::reset);
        engine.factionShipInventories.values().forEach(FactionShipInventory::update);

        if (!fakeAdvance || forceWorkerAssignment) {
            if (allocWorkers) {
                WorkerRegistry.instance().resetWorkersAssigned(false);
                engine.comDomains.values().parallelStream().forEach(CommodityDomain::update);
                assignWorkers();

                engine.cyclesSinceWorkerAssign = 0;
            } else {
                engine.cyclesSinceWorkerAssign++;
            }
        }

        engine.comDomains.values().parallelStream().forEach(CommodityDomain::update);
        
        weightedOutputDeficitMods();

        engine.playerMarketData.values().forEach(PlayerMarketData::apply);

        if (!fakeAdvance) {
            engine.playerMarketData.values().forEach(PlayerMarketData::advance);

            handleTrade();
        }

        engine.comDomains.values().parallelStream().forEach(d -> d.informalTrade(fakeAdvance));

        if (!fakeAdvance) {
            engine.comDomains.values().forEach(CommodityDomain::advance);
            engine.factionShipInventories.values().forEach(FactionShipInventory::advance);

            applyWages();
            redistributeFactionCredits(LtvEconSaveData.instance().playerFactionSettings.redistributeCredits);
            applyDebtEffects();
        }
    }

    public final void refreshMarkets() {
        final List<MarketAPI> econMarkets = EconomyInfo.getMarketsCopy();
        final Set<String> econMarketIDs = econMarkets.stream()
            .map(MarketAPI::getId)
            .collect(Collectors.toSet());
        final Set<String> registeredMarkets = new HashSet<>(engine.registeredMarkets);

        for (MarketAPI market : econMarkets) {
            if (!registeredMarkets.contains(market.getId())) engine.registerMarket(market);
        }

        for (String marketID : registeredMarkets) {
            if (!econMarketIDs.contains(marketID)) engine.removeMarket(marketID);
        }
    }

    public final void refreshMarketsHard() {
        final List<MarketAPI> econMarkets = EconomyInfo.getMarketsCopy();
        final Set<String> econMarketIDs = econMarkets.stream()
            .map(MarketAPI::getId)
            .collect(Collectors.toSet());
        final Set<String> registeredMarkets = new HashSet<>(engine.registeredMarkets);

        for (MarketAPI market : econMarkets) {
            if (!registeredMarkets.contains(market.getId())) engine.registerMarket(market);
        }

        for (String marketID : registeredMarkets) {
            if (!econMarketIDs.contains(marketID)) engine.removeMarket(marketID);
        }

        for (MarketAPI market : econMarkets) {
            if (!market.isPlayerOwned()) continue;

            final String marketID = market.getId();
            if (!engine.playerMarketData.containsKey(marketID)) {
                engine.addPlayerMarketData(marketID);
            }
            for (CommodityDomain dom : engine.getComDomains()) {
                if (dom.getCell(marketID) == null) dom.addMarket(marketID);
            }
        }
    }

    private final void discoverInputsOutputs() {
        // TODO maybe replace with the industry apply hooks after the update
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            for (Industry ind : WorkerRegistry.getVisibleIndustries(market)) {
                for (var supply : ind.getAllSupply()) {
                    if (supply.getQuantity().getModifiedValue() > 0.01f) {
                        if (!IndustryIOs.hasOutput(ind, supply.getCommodityId())) {
                            IndustryIOs.createAndRegisterDynamicOutput(ind, supply.getCommodityId(), true);
                        }
                    }
                }

                for (var demand : ind.getAllDemand()) {
                    if (demand.getQuantity().getModifiedValue() > 0.01f) {
                        if (!IndustryIOs.hasInput(ind, demand.getCommodityId())) {
                            IndustryIOs.createAndRegisterDynamicInput(ind, demand.getCommodityId(), true);
                        }
                    }
                }
            }
        }
    }

    public final void assignWorkers() {
        final WorkerRegistry reg = WorkerRegistry.instance();
        final List<String> industryOutputPairs = IndustryMatrix.getIndustryOutputPairs();
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        markets.removeIf(MarketAPI::isPlayerOwned);

        final ArrayMap<MarketAPI, float[]> assignedWorkersPerMarket = WorkforceAllocator.computeWorkerAllocations(
            markets, industryOutputPairs
        );

        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.singleEntrySet()) {
            final MarketAPI market = entry.getKey();
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);

            final float[] assignments = entry.getValue();
            final long totalWorkers = cond.getWorkerPool();

            for (int i = 0; i < industryOutputPairs.size(); i++) {
                if (assignments[i] == 0f) continue;

                final String[] indAndOutputID = industryOutputPairs.get(i).split(KEY);

                final float ratio = (assignments[i] / totalWorkers);
                final String baseInd = IndustryConfigManager.getBaseIndustryID(indAndOutputID[0]);
                WorkerIndustryData data = reg.getData(market.getId(), baseInd);
                if (data == null) {
                    reg.register(market);
                    data = reg.getData(market.getId(), baseInd);
                }
                data.setRatioForOutput(indAndOutputID[1], ratio);
            }
        }
    }

    private final void weightedOutputDeficitMods() {
        engine.comDomains.values().stream()
            .flatMap(dom -> dom.getCellsMap().values().stream())
            .parallel()
            .forEach(this::computeComDeficits);
    }

    private final void computeComDeficits(CommodityCell cell) {
        float totalDeficit = 0f;
        final float totalMarketOutput = cell.getProduction(false);
        final float invMarketOutput = 1f / totalMarketOutput;

        for (Map.Entry<String, MutableStat> industryEntry : cell.getIndProductionStats().singleEntrySet()) {
            final String industryID = industryEntry.getKey();
            final MutableStat industryStat = industryEntry.getValue();

            final float industryOutput = industryStat.getModifiedValue();
            if (industryOutput <= 0 || totalMarketOutput <= 0) continue;

            final float industryShare = industryOutput * invMarketOutput;

            final Industry ind = cell.market.getIndustry(industryID);

            float sum = 0f;
            final ArrayMap<String, Float> inputWeights = IndustryIOs.getRealInputs(ind, cell.comID, true);
            if (inputWeights.isEmpty()) continue;
            for (float value : inputWeights.values()) {
                sum += value;
            }

            if (sum <= 0f) continue;

            float industryDeficit = 0f;
            for (Map.Entry<String, Float> inputEntry : inputWeights.singleEntrySet()) {
                final String inputID = inputEntry.getKey();
                if (IndustryIOs.ABSTRACT_COM.contains(inputID)) continue;
                
                final CommodityCell inputCell = engine.getComCell(inputID, cell.market.getId());

                final float weightNorm = inputEntry.getValue() / sum;

                industryDeficit += weightNorm * (1 - inputCell.getStoredAvailabilityRatio());
            }

            totalDeficit += industryDeficit * industryShare;
        }

        if (totalDeficit > 0f) {
            cell.getProductionStat().modifyMult(
                "deficits", Math.max(1f - totalDeficit, 0.01f), "Input shortages"
            );
        } else cell.getProductionStat().unmodifyMult("deficits");
    }

    private final void handleTrade() {
        advanceMissions();

        if (engine.cyclesSinceTrade >= engine.lastTradeCycle) {
            engine.cyclesSinceTrade = 0;
            engine.lastTradeCycle = EconConfig.TRADE_INTERVAL;
            dispatchTrade();
        } else {
            engine.cyclesSinceTrade++;
        }
    }

    private final void dispatchTrade() {
        final long startTime = System.nanoTime();
        engine.comDomains.values().forEach(CommodityDomain::createFormalTradeFlows);

        final ArrayMap<String, TradeMission> missions = new ArrayMap<>(32);
        for (CommodityDomain dom : engine.comDomains.values()) {
            final String comID = dom.comID;
            for (ComTradeFlow flow : dom.getTradeFlows()) {
                final TradeMission mission = missions.computeIfAbsent(
                    flow.exporterID + KEY + flow.importerID,
                    m -> new TradeMission(flow.exporter, flow.importer, flow.inFaction)
                );

                mission.cargo.add(new TradeCom(comID, flow.amount, flow.totalPrice));
                if (flow.comID.equals(Commodities.CREW) || flow.comID.equals(Commodities.MARINES)) {
                    mission.crewAmount += flow.amount;
                } else if (flow.comID.equals(Commodities.FUEL)) {
                    mission.fuelAmount += flow.amount;
                } else {
                    mission.cargoAmount += flow.amount;
                }
            }
        }

        final int totalMissions = missions.size();
        engine.activeMissions.ensureCapacity(engine.activeMissions.size() + totalMissions);
        for (int i = 0; i < totalMissions; i++) {

            final TradeMission mission = missions.valueAt(i);
            mission.startOffset = (i * EconConfig.TRADE_INTERVAL) / totalMissions;
            mission.combatPowerTarget = ShipAllocator.getRequiredCombatPower(mission);

            final FactionShipInventory inv = engine.getFactionShipInventory(
                mission.src.getFaction().getId()
            );
            if (inv.getIdleCargoCapacity() >= mission.cargoAmount &&
                inv.getIdleFuelCapacity() >= mission.fuelAmount &&
                inv.getIdleCrewCapacity() >= mission.crewAmount &&
                inv.getIdleCombatPower() >= mission.combatPowerTarget
            ) {
                allocShipsAndFuelToTradeMission(inv, mission);
                mission.usedFactionFleet = true;
            } else {
                allocIndependentFleetToTradeMission(mission);
                mission.usedFactionFleet = false;
            }
            engine.activeMissions.add(mission);
        }

        final String time = ((System.nanoTime() - startTime) / 1_000_000) + " ms";
        log.info("Dispatched " + totalMissions + " new trade missions in "+ time +" and added them to the queue");
    }

    private final void advanceMissions() {
        final Iterator<TradeMission> activeIt = engine.activeMissions.iterator();
        while (activeIt.hasNext()) {
            final TradeMission m = activeIt.next();
            if (m.src == null || m.dest == null) {
                activeIt.remove(); continue;
            }

            final FactionShipInventory inv = engine.getFactionShipInventory(m.src.getFactionId());

            switch (m.status) {
            case SCHEDULED:
                if (engine.cyclesSinceTrade == m.startOffset) {
                    m.status = MissionStatus.IN_SRC_ORBIT_LOADING;
                }
                break;

            case IN_SRC_ORBIT_LOADING:
                m.durRemaining--;
                if (m.totalDur - m.durRemaining >= (int) Math.ceil(m.transferDur)) {
                    m.status = MissionStatus.IN_TRANSIT;
                }
                break;

            case IN_TRANSIT:
                m.durRemaining--;
                if (m.durRemaining < (int) Math.ceil(m.transferDur)) {
                    m.status = MissionStatus.IN_DST_ORBIT_UNLOADING;
                }
                break;

            case IN_DST_ORBIT_UNLOADING:
                m.durRemaining--;
                if (m.durRemaining < 1) {
                    m.status = MissionStatus.DELIVERED;
                }
                break;
            
            case DELIVERED:
                if (!m.spawnedFleetFinishedJob) break;

                for (Entry<String, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    inv.freeShip(entry.getKey(), entry.getValue());
                }
                
                for (TradeCom cargo : m.cargo) {
                    final CommodityCell cell = engine.getComCell(cargo.comID, m.destID);
                    if (cell == null) continue;

                    if (m.inFaction) {
                        cell.inFactionImports += cargo.amount;
                    } else {
                        cell.globalImports += cargo.amount;
                    }
                }
                putMissionToPast(activeIt, m);
                break;

            case CANCELLED:
                for (TradeCom cargo : m.cargo) {
                    final CommodityCell cell = engine.getComCell(cargo.comID, m.srcID);
                    if (cell == null) continue;
                    cell.inFactionImports += cargo.amount;
                }
                for (Entry<String, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    inv.freeShip(entry.getKey(), entry.getValue());
                }
                putMissionToPast(activeIt, m);
                break;

            case LOST:
                for (Entry<String, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    inv.registerShipLoss(entry.getKey(), entry.getValue());
                }
                putMissionToPast(activeIt, m);
                break;
            }
        }
    
        final Iterator<TradeMission> pastIt = engine.pastMissions.iterator();
        while (pastIt.hasNext()) {
            final TradeMission m = pastIt.next();

            m.durRemaining--;
            if (m.durRemaining < 0) pastIt.remove();
        }
    }

    private final void allocShipsAndFuelToTradeMission(final FactionShipInventory inv, final TradeMission mission) {
        ShipAllocator.allocateShipsForTrade(inv, mission);

        float fuelCost = 0f;
        for (Entry<String, Integer> entry : mission.allocatedShips.singleEntrySet()) {
            fuelCost += inv.get(entry.getKey()).spec.getFuelPerLY() * entry.getValue() * mission.dist;
        }
        mission.fuelCost = fuelCost;

        final String marketID = mission.src.getId();
        final CommodityCell fuelCell = engine.getComCell(Commodities.FUEL, marketID);
        if (fuelCell.getStored() >= fuelCost) {
            mission.usedFuelFromStockpiles = true;
            fuelCell.addStoredAmount(-fuelCost);
            
        } else {
            final float unitPrice = fuelCell.getUnitPrice(PriceType.MARKET_BUYING, (int) fuelCost);
            final float cost = fuelCost * unitPrice * EconConfig.FORCED_FUEL_IMPORT_COST_MULT;
            final String key = TRADE_FUEL_PREMIUM_KEY;
            mission.credits.modifyFlat(key, -cost, getDesc(key));

            MarketFinanceRegistry.instance().getLedger(marketID).add(
                TRADE_FLEET_SHIPMENT_KEY, -mission.credits.computeEffective(0f), getDesc(TRADE_FLEET_SHIPMENT_KEY)
            );
        }
    }

    private final void allocIndependentFleetToTradeMission(final TradeMission mission) {
        ShipAllocator.allocateShipsForTarget(Global.getSector().getFaction(Factions.INDEPENDENT), mission);
        
        float fuelCost = 0f;
        for (Entry<String, Integer> entry : mission.allocatedShips.singleEntrySet()) {
            final ShipHullSpecAPI spec = settings.getHullSpec(entry.getKey());
            fuelCost += spec.getFuelPerLY() * entry.getValue() * mission.dist;
        }
        mission.fuelCost = fuelCost;
        
        float totalValue = 0f;
        for (TradeCom flow : mission.cargo) {
            totalValue += flow.totalPrice;
        }

        final float perTonFee = EconConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE * mission.getTotalAmount();
        final float valueFee = EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT * totalValue;
        final float hazardPay = EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE
            + EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT * mission.combatPowerTarget;

        mission.credits.modifyFlat(INDEPENDENT_BASE_FEE_KEY, -EconConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE, getDesc(INDEPENDENT_BASE_FEE_KEY));
        mission.credits.modifyFlat(INDEPENDENT_PER_TON_KEY, -perTonFee, getDesc(INDEPENDENT_PER_TON_KEY));
        mission.credits.modifyFlat(INDEPENDENT_VALUE_PERCENT_KEY, -valueFee, getDesc(INDEPENDENT_VALUE_PERCENT_KEY));
        mission.credits.modifyFlat(INDEPENDENT_HAZARD_PAY_KEY, -hazardPay, getDesc(INDEPENDENT_HAZARD_PAY_KEY));

        MarketFinanceRegistry.instance().getLedger(mission.src.getId()).add(
            TRADE_FLEET_SHIPMENT_KEY, mission.credits.computeEffective(0f), getDesc(TRADE_FLEET_SHIPMENT_KEY)
        );
    }

    private final void applyWages() {
        final MarketFinanceRegistry registry = MarketFinanceRegistry.instance();
        final EconomyAPI econ = Global.getSector().getEconomy(); 
        final List<String> toRemove = new ArrayList<>(4);

        for (String marketID : engine.registeredMarkets) {
            if (econ.getMarket(marketID) == null) {
                toRemove.add(marketID);
            } else {
                final float wageCost = engine.info.getDailyWages(econ.getMarket(marketID));
                registry.getLedger(marketID).add(WORKER_WAGES_KEY, -wageCost, getDesc(WORKER_WAGES_KEY));
            }
        }

        for (String marketID : toRemove) {
            engine.removeMarket(marketID);
        }
    }

    private final void redistributeFactionCredits(boolean includePlayerFaction) {
        final double REDISTRIBUTION_STRENGTH = 0.2;
        final int DAYS_AFTER_REDISTRIBUTION = 30;

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!includePlayerFaction && faction.isPlayerFaction()) continue;

            final List<MarketAPI> markets = new ArrayList<>();
            for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
                if (market.getDaysInExistence() < DAYS_AFTER_REDISTRIBUTION) continue;
                if (market.getFaction().equals(faction)) markets.add(market);
            }
            if (markets.size() < 2) continue;

            final int n = markets.size();
            long totalCredits = 0l;
            double totalWeight = 0l;
            double[] weights = new double[n];
            long[] credits = new long[n];

            for (int i = 0; i < n; i++) {
                MarketAPI m = markets.get(i);
                final double w = Math.pow(10, m.getSize() - 3);
                weights[i] = w;
                totalWeight += w;

                final long c = engine.getCredits(m.getId());
                credits[i] = c;
                totalCredits += c;
            }
            if (totalWeight <= 0l) continue;

            for (int i = 0; i < n; i++) {
                final double desired = totalCredits * (weights[i] / totalWeight);
                final double diff = desired - credits[i];
                final long change = Math.round(diff * REDISTRIBUTION_STRENGTH);

                engine.addCredits(markets.get(i).getId(), change);
            }
        }
    }

    private final void applyDebtEffects() {
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            final DebtDebuffTier appliedTier = getDebtDebuffTier(market.getId());

            // TODO test later
            for (MarketImmigrationModifier immigMod : market.getTransientImmigrationModifiers()) {
                if (immigMod instanceof DebtEffectMarketImmigration debtMod) {
                    market.removeTransientImmigrationModifier(debtMod);
                    break;
                }
            }

            if (appliedTier == null) {
                market.getStability().unmodify(DEBT_DEBUFF_KEY);
                market.getUpkeepMult().unmodify(DEBT_DEBUFF_KEY);
            } else {
                market.getStability().modifyFlat(
                    DEBT_DEBUFF_KEY,
                    appliedTier.stabilityPenalty(),
                    DEBT_STABILITY_DEBUFF_DESC
                );
                market.getUpkeepMult().modifyPercent(
                    DEBT_DEBUFF_KEY,
                    appliedTier.upkeepMultiplierPercent(),
                    DEBT_STABILITY_DEBUFF_DESC
                );
                market.addTransientImmigrationModifier(new DebtEffectMarketImmigration(appliedTier.immigrationModifier()));
            }
        }
    }

    private final DebtDebuffTier getDebtDebuffTier(String marketID) {
        final long credits = engine.getCredits(marketID);
        DebtDebuffTier appliedTier = null;

        for (DebtDebuffTier tier : EconConfig.DEBT_DEBUFF_TIERS) {
            if (credits < tier.threshold()) appliedTier = tier;
            else break;
        }

        return appliedTier;
    }

    private final void putMissionToPast(Iterator<TradeMission> it, TradeMission mission) {
        engine.pastMissions.add(mission);
        it.remove();

        mission.durRemaining = EconConfig.HISTORY_LENGTH;
    }

    private static class DebtEffectMarketImmigration implements MarketImmigrationModifier {
        private float mod;
        public DebtEffectMarketImmigration(float mod) {
            this.mod = mod;
        }
        public final void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
            incoming.getWeight().modifyFlat(DEBT_DEBUFF_KEY, mod, DEBT_IMMIGRATION_DEBUFF_DESC);
        }
    }
}
package wfg.ltv_econ.economy.engine;

import static wfg.ltv_econ.constant.CommoditiesID.*;
import static wfg.ltv_econ.constant.strings.Income.*;
import static wfg.ltv_econ.constant.strings.LocalizedStrings.str;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;

import wfg.ltv_econ.condition.WorkerPoolCondition;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.EconConfig.DebtDebuffTier;
import wfg.ltv_econ.config.PlanConfig.WorkerAllocationPlan;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.constant.strings.Consumption;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.economy.MarketPopulationData;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.TradeCom;
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
import wfg.ltv_econ.ui.marketInfo.dialogs.ServiceSectorDialog;
import wfg.ltv_econ.util.ArrayMutableStat;
import wfg.native_ui.util.ArrayMap;

public class EconomyLoop {
    private static final Logger log = Global.getLogger(EconomyLoop.class);
    private static final int DAYS_AFTER_SHIP_FREE_IN_COMPLETED = 7;
    private static final float FUEL_TARGET_TRADE_ALPHA = 0.5f;
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
                applyServiceSectorEffects();

                engine.cyclesSinceWorkerAssign = 0;
            } else {
                engine.cyclesSinceWorkerAssign++;
            }
        }

        engine.comDomains.values().parallelStream().forEach(CommodityDomain::update);
        
        weightedOutputDeficitMods();

        engine.marketPopData.values().forEach(MarketPopulationData::apply);

        if (!fakeAdvance) {
            engine.marketPopData.values().forEach(MarketPopulationData::advance);

            handleTrade();
            
            engine.comDomains.values().forEach(CommodityDomain::advance);
            engine.factionShipInventories.values().forEach(FactionShipInventory::advance);

            applyWages();
            redistributeFactionCredits(LtvEconSaveData.instance().playerFactionSettings.redistributeCredits);
            applyDebtEffects();
        }
    }

    final void refreshMarkets() {
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

    final void refreshMarketsHard() {
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
            if (!engine.marketPopData.containsKey(marketID)) {
                engine.addMarketPopulationData(marketID);
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
                        if (!IndustryIOs.hasOutput(ind.getSpec().getId(), supply.getCommodityId())) {
                            IndustryIOs.createAndRegisterDynamicOutput(ind, supply.getCommodityId(), true);
                        }
                    }
                }

                for (var demand : ind.getAllDemand()) {
                    if (demand.getQuantity().getModifiedValue() > 0.01f) {
                        if (!IndustryIOs.hasInput(ind.getSpec().getId(), demand.getCommodityId())) {
                            IndustryIOs.createAndRegisterDynamicInput(ind, demand.getCommodityId(), true);
                        }
                    }
                }
            }
        }
    }

    private final void assignWorkers() {
        final WorkerRegistry reg = WorkerRegistry.instance();
        final List<String> industryOutputPairs = IndustryMatrix.getIndustryOutputPairs();
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        markets.removeIf(MarketAPI::isPlayerOwned);

        final ArrayMap<MarketAPI, float[]> assignedWorkersPerMarket = WorkforceAllocator.computeWorkerAllocation(
            markets, industryOutputPairs
        );

        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.singleEntrySet()) {
            final MarketAPI market = entry.getKey();
            final String marketID = market.getId();
            final WorkerIndustryData popData = reg.getRegisterData(marketID, Industries.POPULATION);
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);
            final float[] assignments = entry.getValue();
            final long totalWorkers = cond.getWorkerPool();

            float remaining = 1f;
            for (int i = 0; i < industryOutputPairs.size(); i++) {
                if (assignments[i] == 0f) continue;

                final String[] indAndOutputID = industryOutputPairs.get(i).split(KEY);
                final float ratio = (assignments[i] / totalWorkers);
                remaining -= ratio;

                final WorkerIndustryData data = reg.getRegisterData(marketID, indAndOutputID[0]);
                data.setRatioForOutput(indAndOutputID[1], ratio);
            }

            if (remaining <= 0f) continue; // Service sectors

            final String[] serviceOrder = {
                SERVICE_LOGISTICS,
                SERVICE_HEALTHCARE,
                SERVICE_SECURITY,
                SERVICE_PUBLIC_INFO,
                SERVICE_CULTURE
            };
            final float[] serviceLimits = {
                ServiceSectorDialog.LOGISTICS_LIM,
                ServiceSectorDialog.HEALTHCARE_LIM,
                ServiceSectorDialog.SECURITY_LIM,
                ServiceSectorDialog.PUBLIC_INFO_LIM,
                ServiceSectorDialog.CULTURE_LIM
            };

            for (int i = 0; i < serviceOrder.length && remaining > 0f; i++) {
                final float toAssign = Math.min(remaining, serviceLimits[i]);
                popData.setRatioForOutput(serviceOrder[i], toAssign);
                remaining -= toAssign;
            }
        }
    }

    final void assignPlayerWorkers(WorkerAllocationPlan plan) {
        final WorkerRegistry reg = WorkerRegistry.instance();
        final List<MarketAPI> markets = EconomyInfo.getMarketsCopy();
        markets.removeIf(m -> !m.isPlayerOwned());
        markets.removeIf(m -> LtvEconSaveData.instance().playerFactionSettings.excludedMarketsFromWorkerAllocation.contains(m.getId()));
        final List<String> industryOutputPairs = IndustryMatrix.getIndustryOutputPairs();

        reg.resetWorkersAssignedByMarket(markets);

        final ArrayMap<MarketAPI, float[]> assignedWorkersPerMarket = WorkforceAllocator
            .computeWorkerAllocationCustom(markets, industryOutputPairs, plan);

        for (Map.Entry<MarketAPI, float[]> entry : assignedWorkersPerMarket.singleEntrySet()) {
            final MarketAPI market = entry.getKey();
            final String marketID = market.getId();
            final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);
            final float[] assignments = entry.getValue();
            final long totalWorkers = cond.getWorkerPool();

            for (int i = 0; i < industryOutputPairs.size(); i++) {
                if (assignments[i] == 0f) continue;

                final String[] indAndOutputID = industryOutputPairs.get(i).split(KEY);
                final float ratio = (assignments[i] / totalWorkers);

                final WorkerIndustryData data = reg.getRegisterData(marketID, indAndOutputID[0]);
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
                "deficits", Math.max(1f - totalDeficit, 0.01f), str("econLoopInputShortages")
            );
        } else cell.getProductionStat().unmodifyMult("deficits");
    }

    private final void handleTrade() {
        advanceMissions();

        if (engine.cyclesSinceTrade >= engine.lastTradeCycle) {
            engine.cyclesSinceTrade = 0;
            engine.lastTradeCycle = EconConfig.TRADE_INTERVAL;
            dispatchTrade();
            engine.comDomains.values().parallelStream().forEach(d -> d.informalTrade(true));
        } else {
            engine.cyclesSinceTrade++;
            engine.comDomains.values().parallelStream().forEach(d -> d.informalTrade(false));
        }
    }

    private final void dispatchTrade() {
        final long startTime = System.nanoTime();
        engine.comDomains.values().forEach(CommodityDomain::createFormalTradeFlows);

        final ArrayMap<String, TradeMission> missions = new ArrayMap<>(32);

        for (CommodityDomain dom : engine.comDomains.values()) {
            final String comID = dom.comID;
            for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
                final TradeMission mission = missions.computeIfAbsent(
                    flow.exporterID + KEY + flow.importerID,
                    m -> new TradeMission(flow.exporter, flow.importer, flow.inFaction)
                );

                mission.cargo.add(new TradeCom(comID, flow.amount, flow.totalPrice));
                if (comID.equals(Commodities.CREW) || comID.equals(Commodities.MARINES)) {
                    mission.crewAmount += flow.amount;
                } else if (comID.equals(Commodities.FUEL)) {
                    mission.fuelAmount += flow.amount;
                } else {
                    mission.cargoAmount += flow.amount;
                }
            }
        }

        final HashMap<MarketAPI, Float> fuelDemands = new HashMap<>(EconomyInfo.getMarketsCount());
        final int totalMissions = missions.size();
        engine.activeMissions.ensureCapacity(engine.activeMissions.size() + totalMissions);
        for (int i = 0; i < totalMissions; i++) {

            final TradeMission mission = missions.valueAt(i);
            mission.startOffset = (i * EconConfig.TRADE_INTERVAL) / totalMissions;
            mission.combatPowerTarget = ShipAllocator.getRequiredCombatPower(mission);

            final FactionShipInventory inv = engine.getFactionShipInventory(
                mission.src.getFaction().getId()
            );
            final boolean canUseFactionFleet = inv.getIdleCargoCapacity() >= mission.cargoAmount
                && inv.getIdleFuelCapacity() >= mission.fuelAmount
                && inv.getIdleCrewCapacity() >= mission.crewAmount
                && inv.getIdleCombatPower() >= mission.combatPowerTarget;

            allocShipsAndFuelToTradeMission(inv, mission, canUseFactionFleet);
            engine.activeMissions.add(mission);

            fuelDemands.merge(mission.src, mission.fuelCost, Float::sum);
        }
        updateFuelDemands(fuelDemands);

        engine.info.tradeFlowCache.clear();

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
                m.durRemaining++;
                if (!m.spawnedFleetFinishedJob && m.durRemaining <= DAYS_AFTER_SHIP_FREE_IN_COMPLETED) break;

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

    private final void allocShipsAndFuelToTradeMission(final FactionShipInventory inv, final TradeMission mission, boolean isFactionFleet) {
        final String marketID = mission.src.getId();
        if (isFactionFleet) {
            ShipAllocator.allocateShipsForTrade(inv, mission);
        } else {
            ShipAllocator.allocateShipsForTarget(Global.getSector().getFaction(Factions.INDEPENDENT), mission);
        }
        mission.fuelCost = ShipAllocator.getRequiredFuelForShips(mission.allocatedShips, mission.dist);
        mission.usedFactionFleet = isFactionFleet;

        final CommodityCell fuelCell = engine.getComCell(Commodities.FUEL, marketID);
        if (fuelCell.getStored() >= mission.fuelCost) {
            mission.usedFuelFromStockpiles = true;
            fuelCell.addStoredAmount(-mission.fuelCost);
            
        } else {
            final float fuelCostFee = mission.fuelCost * fuelCell.spec.getBasePrice()
                * (isFactionFleet ? EconConfig.FORCED_FUEL_IMPORT_COST_MULT : 1f);
            
            mission.credits.modifyFlat(
                isFactionFleet ? TRADE_FUEL_PREMIUM_KEY : INDEPENDENT_FUEL_COST_KEY,
                -fuelCostFee,
                getDesc(isFactionFleet ? TRADE_FUEL_PREMIUM_KEY : INDEPENDENT_FUEL_COST_KEY)
            );
        }

        if (!isFactionFleet) {
            float totalValue = 0f;
            for (TradeCom flow : mission.cargo) totalValue += flow.totalPrice;

            final float perTonFee = EconConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE * mission.getTotalAmount();
            final float valueFee = EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT * totalValue;
            final float hazardPay = EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE
                + EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT * mission.combatPowerTarget;

            mission.credits.modifyFlat(INDEPENDENT_BASE_FEE_KEY, -EconConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE, getDesc(INDEPENDENT_BASE_FEE_KEY));
            mission.credits.modifyFlat(INDEPENDENT_PER_TON_KEY, -perTonFee, getDesc(INDEPENDENT_PER_TON_KEY));
            mission.credits.modifyFlat(INDEPENDENT_VALUE_PERCENT_KEY, -valueFee, getDesc(INDEPENDENT_VALUE_PERCENT_KEY));
            mission.credits.modifyFlat(INDEPENDENT_HAZARD_PAY_KEY, -hazardPay, getDesc(INDEPENDENT_HAZARD_PAY_KEY));
        }

        MarketFinanceRegistry.instance().getLedger(marketID).add(
            TRADE_FLEET_SHIPMENT_KEY, mission.credits.computeEffective(0f), getDesc(TRADE_FLEET_SHIPMENT_KEY)
        );
    }

    private final void updateFuelDemands(HashMap<MarketAPI, Float> fuelDemands) {
        for (Map.Entry<MarketAPI, Float> entry : fuelDemands.entrySet()) {
            final ArrayMutableStat mutable = engine.getComCell(Commodities.FUEL, entry.getKey().getId()).getTargetQuantumStat();

            final StatMod statMod = mutable.getFlatStatMod(Consumption.FUEL_TARGET_TRADE_KEY);
            final float oldTarget = statMod != null ? statMod.value : 0f;
            final float newTarget = FUEL_TARGET_TRADE_ALPHA * entry.getValue() / EconConfig.TRADE_INTERVAL + (1f - FUEL_TARGET_TRADE_ALPHA) * oldTarget;
            mutable.modifyFlat(
                Consumption.FUEL_TARGET_TRADE_KEY, newTarget,
                Consumption.getDesc(Consumption.FUEL_TARGET_TRADE_KEY)
            );
        }
    }

    private final void applyWages() {
        final MarketFinanceRegistry registry = MarketFinanceRegistry.instance();
        final EconomyAPI econ = Global.getSector().getEconomy();

        for (String marketID : engine.registeredMarkets) {
            final MarketAPI market = econ.getMarket(marketID);
            if (market != null) {
                final float wageCost = engine.info.getDailyWages(market);
                registry.getLedger(marketID).add(WORKER_WAGES_KEY, -wageCost, getDesc(WORKER_WAGES_KEY));
            }
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

            DebtEffectMarketImmigration debtMod = null;
            for (MarketImmigrationModifier immigMod : market.getTransientImmigrationModifiers()) {
                if (immigMod instanceof DebtEffectMarketImmigration) {
                    debtMod = (DebtEffectMarketImmigration) immigMod;
                    debtMod.mod = 0f;
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

                if (debtMod != null) debtMod.mod = appliedTier.immigrationModifier();
                else market.addTransientImmigrationModifier(new DebtEffectMarketImmigration(appliedTier.immigrationModifier()));
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

    private final void applyServiceSectorEffects() {
        // TODO make the solver give value to these outputs
        for (MarketAPI market : EconomyInfo.getMarketsCopy()) {
            applyServiceSectorEffectsToMarket(market);
        }
    }

    final void applyServiceSectorEffectsToMarket(MarketAPI market) {
        final String marketID = market.getId();
        final WorkerIndustryData idata = WorkerRegistry.instance().getRegisterData(marketID, Industries.POPULATION);

        final float logiRatio = idata.getAssignedRatioForOutput(SERVICE_LOGISTICS);
        final float healthRatio = idata.getAssignedRatioForOutput(SERVICE_HEALTHCARE);
        final float secRatio = idata.getAssignedRatioForOutput(SERVICE_SECURITY);
        final float pubInfoRatio = idata.getAssignedRatioForOutput(SERVICE_PUBLIC_INFO);
        final float cultureRatio = idata.getAssignedRatioForOutput(SERVICE_CULTURE);

        for (CommodityDomain dom : engine.comDomains.values()) {
            final CommodityCell cell = dom.getCell(marketID);
            cell.getConsumptionStat().modifyMult(SERVICE_LOGISTICS, 1f - logiRatio, str("serviceSectorLogisticsDesc"));
        }
        for (MarketImmigrationModifier immigMod : market.getTransientImmigrationModifiers()) {
            if (immigMod instanceof ServiceSectorMarketImmigration) {
                market.removeTransientImmigrationModifier(immigMod);
                break;
            }
        }
        if (healthRatio * 20f >= 1f) {
            market.addTransientImmigrationModifier(new ServiceSectorMarketImmigration((int) (healthRatio * 20f)));
        }

        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(
            SERVICE_SECURITY, secRatio * 100f, str("serviceSectorSecurityDesc")
        );
        market.getAccessibilityMod().modifyFlat(SERVICE_PUBLIC_INFO, pubInfoRatio, str("serviceSectorPublicInfoDesc"));

        final float workerPool = WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
        final double monthlyRevenue = cultureRatio * workerPool * LaborConfig.LPV_month * 0.5f / EconomyConstants.MONTH;
        if (monthlyRevenue > 0d) {
            MarketFinanceRegistry.instance().getLedger(marketID).add(SERVICE_CULTURE, monthlyRevenue, str("serviceSectorCultureDesc"));
        }
        
        final MarketPopulationData mData = engine.getMarketPopulationData(marketID);
        if (mData != null) {
            mData.healthDelta.modifyFlat(SERVICE_HEALTHCARE, healthRatio / 2f, str("serviceSectorHealthcareDesc"));
            mData.classConsciousnessDelta.modifyFlat(SERVICE_SECURITY, -secRatio / 10f, str("serviceSectorSecurityDesc"));
            mData.socialCohesionDelta.modifyFlat(SERVICE_PUBLIC_INFO, pubInfoRatio / 2f, str("serviceSectorPublicInfoDesc"));
            mData.socialCohesionDelta.modifyFlat(SERVICE_CULTURE, cultureRatio / 4f, str("serviceSectorCultureDesc"));
            mData.happinessDelta.modifyFlat(SERVICE_CULTURE, cultureRatio * 2f, str("serviceSectorCultureDesc"));
        }
    }

    private static class DebtEffectMarketImmigration implements MarketImmigrationModifier {
        protected float mod;
        public DebtEffectMarketImmigration(float mod) {
            this.mod = mod;
        }
        public final void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
            incoming.getWeight().modifyFlat(DEBT_DEBUFF_KEY, mod, DEBT_IMMIGRATION_DEBUFF_DESC);
        }
    }

    private static class ServiceSectorMarketImmigration implements MarketImmigrationModifier {
        protected float mod;
        public ServiceSectorMarketImmigration(float mod) {
            this.mod = mod;
        }
        public final void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
            incoming.getWeight().modifyFlat(SERVICE_HEALTHCARE, mod, str("serviceSectorHealthcareDesc"));
        }
    }
}
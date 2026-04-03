package wfg.ltv_econ.economy.engine;

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
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Strings;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.EconomyConfigLoader.DebtDebuffTier;
import wfg.ltv_econ.configs.EconomyConfigLoader.EconomyConfig;
import wfg.ltv_econ.economy.PlayerMarketData;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.economy.fleet.FactionShipInventory;
import wfg.ltv_econ.economy.fleet.ShipAllocator;
import wfg.ltv_econ.economy.fleet.ShipTypeData;
import wfg.ltv_econ.economy.fleet.TradeMission;
import wfg.ltv_econ.economy.fleet.TradeMission.MissionStatus;
import wfg.ltv_econ.economy.planning.IndustryMatrix;
import wfg.ltv_econ.economy.planning.WorkforceAllocator;
import wfg.ltv_econ.industry.IndustryIOs;
import wfg.ltv_econ.serializable.LtvEconSaveData;
import wfg.native_ui.util.ArrayMap;

public class EconomyLoop {
    private static final Logger log = Global.getLogger(EconomyLoop.class);
    public static final String KEY = "::";

    transient EconomyEngine engine;

    public EconomyLoop(EconomyEngine engine) { this.engine = engine; }    

    final void mainLoop(boolean fakeAdvance, boolean forceWorkerAssignment) {
        final boolean allocWorkers = engine.cyclesSinceWorkerAssign >= EconomyConfig.WORKER_ASSIGN_INTERVAL || forceWorkerAssignment;
        refreshMarkets();

        discoverInputsOutputs();

        engine.comDomains.values().forEach(CommodityDomain::reset);

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

        if (!fakeAdvance) {
            engine.playerMarketData.values().forEach(PlayerMarketData::advance);

            handleTrade();
        }

        engine.comDomains.values().parallelStream().forEach(d -> d.informalTrade(fakeAdvance));

        if (!fakeAdvance) {
            engine.comDomains.values().forEach(CommodityDomain::advance);

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
                if (!dom.hasLedger(marketID)) dom.addLedger(marketID);
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
                final String baseInd = IndustryIOs.getBaseIndustryID(indAndOutputID[0]);
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

        for (Map.Entry<String, MutableStat> industryEntry : cell.getFlowProdIndStats().singleEntrySet()) {
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
            engine.lastTradeCycle = EconomyConfig.TRADE_INTERVAL;
            dispatchTrade();
        } else {
            engine.cyclesSinceTrade++;
        }
    }

    private final void dispatchTrade() {
        engine.comDomains.values().forEach(CommodityDomain::createFormalTradeFlows);

        final ArrayMap<String, TradeMission> missions = new ArrayMap<>();
        for (CommodityDomain dom : engine.comDomains.values()) {
            for (ComTradeFlow flow : dom.getTradeFlows()) {
                final TradeMission mission = missions.computeIfAbsent(
                    flow.exporter.getId() + KEY + flow.importer.getId(),
                    m -> new TradeMission(flow.exporter, flow.importer, flow.inFaction)
                );

                mission.cargo.add(flow);
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
        for (int i = 0; i < totalMissions; i++) {

            final TradeMission mission = missions.valueAt(i);
            mission.startOffset = (i * EconomyConfig.TRADE_INTERVAL) / totalMissions;
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

        log.info("Dispatched " + totalMissions + " new trade missions and added them to the queue");
    }

    private final void advanceMissions() {
        final Iterator<TradeMission> it = engine.activeMissions.iterator();
        while (it.hasNext()) {
            final TradeMission m = it.next();

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

                for (Entry<ShipTypeData, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    entry.getKey().freeShip(entry.getValue());
                }
                
                for (ComTradeFlow cargo : m.cargo) {
                    final CommodityCell cell = engine.getComCell(cargo.comID, m.dest.getId());
                    if (cell == null) continue;

                    if (m.inFaction) {
                        cell.inFactionImports += cargo.amount;
                    } else {
                        cell.globalImports += cargo.amount;
                    }
                }
                engine.pastMissions.add(m);
                it.remove();
                break;

            case CANCELLED:
                for (ComTradeFlow cargo : m.cargo) {
                    final CommodityCell cell = engine.getComCell(cargo.comID, m.src.getId());
                    if (cell == null) continue;
                    cell.inFactionImports += cargo.amount;
                }
                for (Entry<ShipTypeData, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    entry.getKey().freeShip(entry.getValue());
                }
                engine.pastMissions.add(m);
                it.remove();
                break;

            case LOST:
                for (Entry<ShipTypeData, Integer> entry : m.allocatedShips.singleEntrySet()) {
                    entry.getKey().registerShipLoss(entry.getValue());
                }
                engine.pastMissions.add(m);
                it.remove();
                break;
            }
        }
    }

    private final void allocShipsAndFuelToTradeMission(final FactionShipInventory inv, final TradeMission mission) {
        ShipAllocator.allocateShipsForTrade(inv, mission);

        float fuelCost = 0f;
        for (Entry<ShipTypeData, Integer> entry : mission.allocatedShips.singleEntrySet()) {
            fuelCost += entry.getKey().spec.getFuelPerLY() * entry.getValue() * mission.dist;
        }
        mission.fuelCost = fuelCost;

        final String marketID = mission.src.getId();
        final CommodityCell fuelCell = engine.getComCell(Commodities.FUEL, marketID);
        if (fuelCell.getStored() >= fuelCost) {
            mission.usedFuelFromStockpiles = true;
            fuelCell.addStoredAmount(-fuelCost); // TODO create demand for this
            
        } else {
            final float unitPrice = fuelCell.getUnitPrice(PriceType.MARKET_BUYING, (int) fuelCost);
            final float cost = fuelCost * unitPrice * EconomyConfig.FORCED_FUEL_IMPORT_COST_MULT;
            mission.credits.modifyFlat("fuel_fee", cost,
                "Fuel premium (" + Strings.X + EconomyConfig.FORCED_FUEL_IMPORT_COST_MULT + ")"
            );

            engine.addCredits(marketID, -(long) mission.credits.computeEffective(0f));
        }
    }

    private final void allocIndependentFleetToTradeMission(final TradeMission mission) {
        ShipAllocator.allocateShipsForTrade(Global.getSector().getFaction(Factions.INDEPENDENT), mission);

        float totalValue = 0f;
        for (ComTradeFlow flow : mission.cargo) {
            totalValue += flow.amount * flow.unitPrice;
        }

        final float perTonFee = EconomyConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE * mission.getTotalAmount();
        final float valueFee = EconomyConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT * totalValue;
        final float hazardPay = EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE
            + EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT * mission.combatPowerTarget;

        mission.credits.modifyFlat("base_fee", EconomyConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE, "Independent hauler base fee");
        mission.credits.modifyFlat("per_ton_fee", perTonFee, "Per-ton transport fee");
        mission.credits.modifyFlat("value_fee", valueFee, "Percentage of cargo value (" + Strings.X + EconomyConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT + ")");
        mission.credits.modifyFlat("hazard_pay", hazardPay, "Hazard pay (base + multiplier for required escort strength)");

        engine.addCredits(mission.src.getId(), -(long) mission.credits.computeEffective(0f));
    }

    private final void applyWages() {
        final EconomyAPI econ = Global.getSector().getEconomy(); 
        final List<String> toRemove = new ArrayList<>(4);

        for (String marketID : engine.registeredMarkets) {
            if (econ.getMarket(marketID) == null) {
                toRemove.add(marketID);
            } else {
                engine.addCredits(marketID, (int) -engine.info.getWagesForMarket(econ.getMarket(marketID)));
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
            final long credits = engine.getCredits(market.getId());
            DebtDebuffTier appliedTier = null;

            for (DebtDebuffTier tier : EconomyConfig.DEBT_DEBUFF_TIERS) {
                if (credits < tier.threshold()) appliedTier = tier;
                else break;
            }

            final String src = "ltv_econ_debt_debuff";

            if (appliedTier == null) {
                market.getStability().unmodify(src);
                market.getUpkeepMult().unmodify(src);
                market.getPopulation().getWeight().unmodify(src);
            } else {
                market.getStability().modifyFlat(
                    src,
                    appliedTier.stabilityPenalty(),
                    "market debt"
                );
                market.getUpkeepMult().modifyPercent(
                    src,
                    appliedTier.upkeepMultiplierPercent(),
                    "inefficiencies caused by market debt"
                );
                market.getPopulation().getWeight().modifyFlat(
                    src,
                    appliedTier.immigrationModifier(),
                    "Unattractiveness caused by market debt"
                );
            }
        }
    }
}
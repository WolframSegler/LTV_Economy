package wfg.ltv_econ.economy.engine;

import static wfg.native_ui.util.Globals.settings;
import static wfg.ltv_econ.constants.strings.Income.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.constants.EconomyConstants;
import wfg.ltv_econ.constants.strings.LocalizedStrings;
import wfg.ltv_econ.economy.commodity.ComTradeFlow;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;

/**
 * <p><b>Trade Cycle Standard</b></p>
 * <p>All trade‑volume displays (exports, imports, market share) 
 *    refer to the <i>most recently completed trade cycle</i> - 
 *    a discrete event occurring every {@link EconConfig#TRADE_INTERVAL} days.</p>
 * <p>Credit values are monthly aggregates.</p>
 */
public class EconomyInfo {
    transient EconomyEngine engine;
    final transient HashMap<String, Double> tradeFlowCache = new HashMap<>(256);

    EconomyInfo(EconomyEngine engine) { this.engine = engine; }

    public final double getInFactionExports(String comID) {
        final String key = 0 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        double total = 0.0;
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (flow.inFaction) total += flow.amount;
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final String key = 1 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.intValue();

        double total = 0.0;
        double marketAmount = 0.0;
        for (ComTradeFlow flow : EconomyEngine.instance().getComDomain(comID).getSanitizedTradeFlows()) {
            total += flow.amount;
            if (flow.exporter.getId().equals(marketID)) {
                marketAmount += flow.amount;
            }
        }
        if (total <= 0.0) return 0;
        total = (marketAmount / total) * 100;

        tradeFlowCache.put(key, total);
        return (int) total;
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final String key = 2 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.intValue();

        double total = 0.0;
        double marketAmount = 0.0;
        for (ComTradeFlow flow : EconomyEngine.instance().getComDomain(comID).getSanitizedTradeFlows()) {
            total += flow.amount;
            if (flow.importer.getId().equals(marketID)) {
                marketAmount += flow.amount;
            }
        }
        if (total <= 0.0) return 0;
        total = (marketAmount / total) * 100;

        tradeFlowCache.put(key, total);
        return (int) total;
    }

    public final double getExportAmount(String comID, String marketID) {
        final String key = 3 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double amount = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.exporter.getId().equals(marketID)) {
                amount += flow.amount;
            }
        }
        amount += dom.getInformalExports(marketID);

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getImportAmount(String comID, String marketID) {
        final String key = 4 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double amount = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.importer.getId().equals(marketID)) {
                amount += flow.amount;
            }
        }
        amount += dom.getInformalImports(marketID);

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getGlobalExportAmount(String comID, String marketID) {
        final String key = 5 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double amount = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.exporter.getId().equals(marketID)) {
                if (!flow.inFaction) amount += flow.amount;
            }
        }
        amount += dom.getInformalExports(marketID);

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getGlobalImportAmount(String comID, String marketID) {
        final String key = 6 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double amount = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.importer.getId().equals(marketID)) {
                if (!flow.inFaction) amount += flow.amount;
            }
        }
        amount += dom.getInformalImports(marketID);

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getInFactionExportAmount(String comID, String marketID) {
        final String key = 7 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        double amount = 0.0;
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (flow.exporter.getId().equals(marketID)) {
                if (flow.inFaction) amount += flow.amount;
            }
        }

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getInFactionImportAmount(String comID, String marketID) {
        final String key = 8 + EconomyLoop.KEY + comID + EconomyLoop.KEY + marketID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        double amount = 0.0;
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (flow.importer.getId().equals(marketID)) {
                if (flow.inFaction) amount += flow.amount;
            }
        }

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final double getFactionInFactionExports(String comID, String factionID) {
        final String key = 9 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        double amount = 0;

        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (flow.inFaction && flow.importer.getFactionId().equals(factionID)) {
                amount += flow.amount;
            }
        }

        tradeFlowCache.put(key, amount);
        return amount;
    }

    public final float getFactionImportShare(String comID, String factionID) {
        
        final double total = getGlobalImports(comID);
        if (total == 0.0) return 0f;
        final double imports = getFactionGlobalImports(comID, factionID);

        return (float) (imports / total);
    }

    public final double getFactionFormalGlobalImports(String comID, String factionID) {
        final String key = 10 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        double total = 0.0;
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (!flow.inFaction && flow.importer.getFactionId().equals(factionID)) {
                total += flow.amount;
            }
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final float getFactionExportShare(String comID, String factionID) {
        final double total = getGlobalExports(comID);
        if (total == 0) return 0;
        final double exports = getFactionGlobalExports(comID, factionID);

        return (float) (exports / total);
    }

    public final float getInformalImportShare(String comID) {
        final double total = getGlobalImports(comID);
        if (total == 0.0) return 0f;

        double imports = 0.0;
        for (float amount : engine.getComDomain(comID).getInformalImports().values()) {
            imports += amount;
        }

        return (float) (imports / total);
    }

    public final float getInformalExportShare(String comID) {
        final double total = getGlobalExports(comID);
        if (total == 0) return 0;

        double exports = 0.0;
        for (float amount : engine.getComDomain(comID).getInformalExports().values()) {
            exports += amount;
        }
        
        return (float) (exports / total);
    }

    public final double getFactionFormalGlobalExports(String comID, String factionID) {
        final String key = 11 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        double total = 0.0;
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (!flow.inFaction && flow.exporter.getFactionId().equals(factionID)) {
                total += flow.amount;
            }
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final long getFactionNetComSpending(String comID, String factionID) {
        final EconomyAPI econ = Global.getSector().getEconomy();
        long netCreditFlow = 0l;
        
        for (MarketLedger ledger : MarketFinanceRegistry.instance().getRegistry()) {
            final MarketAPI market = econ.getMarket(ledger.marketID);
            if (!market.getFactionId().equals(factionID)) continue;

            netCreditFlow += ledger.getLastMonth(TRADE_EXPORT_KEY + comID);
            netCreditFlow += ledger.getLastMonth(TRADE_IMPORT_KEY + comID);
        }
        return netCreditFlow;
    }

    public final double getFactionComStockpiles(String comID, String factionID) {
        double totalStockpiles = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalStockpiles += cell.getStored();
            }
        }
        return totalStockpiles;
    }

    public final float getFactionTargetQuantum(String comID, String factionID) {
        float totalDemand = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalDemand += cell.getTargetQuantum(true);
            }
        }
        return totalDemand;
    }

    public final float getFactionComProd(String comID, String factionID) {
        float totalDemand = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalDemand += cell.getProduction(true);
            }
        }
        return totalDemand;
    }

    public final double getFactionComBalance(String comID, String factionID) {
        double balance = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                balance += cell.getQuantumRealBalance();
            }
        }
        return balance;
    }

    public final float getFactionImportSufficiency(String comID, String factionID) {
        final String key = 12 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.floatValue();

        final EconomyAPI econ = Global.getSector().getEconomy();
        final CommodityDomain dom = engine.getComDomain(comID);
        if (dom == null) return 1f;

        double totalImports = 0.0;
        double inFactionImports = 0.0;

        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (!flow.importer.getFactionId().equals(factionID)) continue;

            totalImports += flow.amount;
            if (flow.inFaction) {
                inFactionImports += flow.amount;
            }
        }

        for (var entry : dom.getInformalImports().entrySet()) {
            final MarketAPI market = econ.getMarket(entry.getKey());
            if (market != null && market.getFactionId().equals(factionID)) {
                totalImports += entry.getValue();
            }
        }

        if (totalImports == 0.0) {
            tradeFlowCache.put(key, 1.0);
            return 1f;
        } else {
            totalImports = Math.min(1.0, inFactionImports / totalImports);
        }

        tradeFlowCache.put(key, totalImports);
        return (float) totalImports;
    }

    public final double getFactionGlobalImports(String comID, String factionID) {
        final String key = 13 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        final CommodityDomain dom = engine.getComDomain(comID);
        final EconomyAPI econ = Global.getSector().getEconomy();

        double total = 0.0;

        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.inFaction) continue;
            if (flow.importer.getFactionId().equals(factionID)) {
                total += flow.amount;
            }
        }
        for (var entry : dom.getInformalImports().entrySet()) {
            if (econ.getMarket(entry.getKey()).getFactionId().equals(factionID)) {
                total += entry.getValue();
            }
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final double getFactionGlobalExports(String comID, String factionID) {
        final String key = 14 + EconomyLoop.KEY + comID + EconomyLoop.KEY + factionID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();

        final CommodityDomain dom = engine.getComDomain(comID);
        final EconomyAPI econ = Global.getSector().getEconomy();

        double total = 0.0;

        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (flow.inFaction) continue;
            if (flow.exporter.getFactionId().equals(factionID)) {
                total += flow.amount;
            }
        }
        for (var entry : dom.getInformalExports().entrySet()) {
            final MarketAPI market = econ.getMarket(entry.getKey());
            if (market.getFactionId().equals(factionID)) {
                total += entry.getValue();
            }
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public static final long getGlobalWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            
            total += WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
        }
        return total;
    }

    public static final long getFactionWorkerCount(String factionID) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (market.getFactionId().equals(factionID)) {
                total += WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
            }
        }
        return total;
    }

    public final long getGlobalDemand(String comID) {
        double total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getTargetQuantum(true);

        return (long) total;
    }

    public final long getGlobalProduction(String comID) {
        final CommodityDomain dom = engine.getComDomain(comID);
        double total = 0;

        for (CommodityCell cell : dom.getAllCells())
        total += cell.getProduction(true);

        return (long) (total + dom.getInformalNode().prod);
    }

    public final long getGlobalSurplus(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getSurplusAfterTargetQuantum();

        return total;
    }

    public final long getGlobalDeficit(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getTargetQuantumUnmet();

        return total;
    }

    public final double getGlobalImports(String comID) {
        final String key = 15 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double total = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (!flow.inFaction) total += flow.amount;
        }
        for (double amount : dom.getInformalImports().values()) {
            total += amount;
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final double getGlobalExports(String comID) {
        final String key = 16 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double total = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (!flow.inFaction) total += flow.amount;
        }
        for (double amount : dom.getInformalExports().values()) {
            total += amount;
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final double getTotalImports(String comID) {
        final String key = 17 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double total = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (!flow.inFaction) total += flow.amount;
        }
        for (double amount : dom.getInformalImports().values()) {
            total += amount;
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final double getTotalExports(String comID) {
        final String key = 18 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.doubleValue();
        final CommodityDomain dom = engine.getComDomain(comID);

        double total = 0.0;
        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            if (!flow.inFaction) total += flow.amount;
        }
        for (double amount : dom.getInformalExports().values()) {
            total += amount;
        }

        tradeFlowCache.put(key, total);
        return total;
    }

    public final double getGlobalInformalImports(String comID) {
        double total = 0.0;
        for (double amount : engine.getComDomain(comID).getInformalImports().values()) {
            total += amount;
        }
        return total;
    }

    public final double getGlobalInformalExports(String comID) {
        double total = 0.0;
        for (double amount : engine.getComDomain(comID).getInformalExports().values()) {
            total += amount;
        }
        return total;
    }
    
    public final long getGlobalTradeVolume(String comID) {
        final String key = 19 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.longValue();
        final CommodityDomain dom = engine.getComDomain(comID);
        double total = 0;

        for (ComTradeFlow flow : dom.getSanitizedTradeFlows()) {
            total += flow.amount;
        }
        for (float amount : dom.getInformalExports().values()) {
            total += amount;
        }

        tradeFlowCache.put(key, total);
        return (long) total;
    }

    public final float getGlobalAveragePrice(String comID, int units) {
        float total = 0;

        final Collection<CommodityCell> allCells = engine.getComDomain(comID).getAllCells();
        for (CommodityCell cell : allCells) total += cell.getUnitPrice(PriceType.NEUTRAL, units);

        return total / (float) allCells.size();
    }

    public final long getGlobalStockpiles(String comID) {
        double total = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells())
        total += cell.getStored();

        return (long) total;
    }

    public final int getGlobalExporterCount(String comID) {
        final String key = 20 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.intValue();

        final Set<String> exporters = new HashSet<>();
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (!flow.inFaction) exporters.add(flow.exporterID);
        }

        tradeFlowCache.put(key, (double) exporters.size());
        return exporters.size();
    }

    public final int getGlobalImporterCount(String comID) {
        final String key = 21 + EconomyLoop.KEY + comID;
        final Double value = tradeFlowCache.get(key);
        if (value != null) return value.intValue();

        final Set<String> importer = new HashSet<>();
        for (ComTradeFlow flow : engine.getComDomain(comID).getSanitizedTradeFlows()) {
            if (!flow.inFaction) importer.add(flow.importerID);
        }

        tradeFlowCache.put(key, (double) importer.size());
        return importer.size();
    }

    public final long getExportIncome(MarketAPI market, boolean lastMonth) {
        return getExportIncome(market.getId(), lastMonth);
    }

    public final long getExportIncome(String marketID, boolean lastMonth) {
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketID);

        long exportIncome = 0;
        for (String comID : EconomyConstants.econCommodityIDs) {
            final String key = TRADE_EXPORT_KEY + comID;
            exportIncome += lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
        }

        return exportIncome;
    }

    public final long getExportIncome(MarketAPI market, String comID, boolean lastMonth) {
        return getExportIncome(market.getId(), comID, lastMonth);
    }

    public final long getExportIncome(String marketID, String comID, boolean lastMonth) {
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketID);
        final String key = TRADE_EXPORT_KEY + comID;
        return lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
    }

    public final long getImportExpense(MarketAPI market, boolean lastMonth) {
        return getImportExpense(market.getId(), lastMonth);
    }

    public final long getImportExpense(String marketID, boolean lastMonth) {
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketID);

        long importCost = 0;
        for (String comID : EconomyConstants.econCommodityIDs) {
            final String key = TRADE_IMPORT_KEY + comID;
            importCost -= lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key);
        }
        return importCost;
    }

    public final long getImportExpense(MarketAPI market, String comID, boolean lastMonth) {
        return getImportExpense(market.getId(), comID, lastMonth);
    }

    public final long getImportExpense(String marketID, String comID, boolean lastMonth) {
        final MarketLedger ledger = MarketFinanceRegistry.instance().getLedger(marketID);
        final String key = TRADE_IMPORT_KEY + comID;
        return -(lastMonth ? ledger.getLastMonth(key) : ledger.getCurrentMonth(key));
    }

    public final float getDailyWages(final MarketAPI market) {
        final String marketID = market.getId();

        final WorkerPoolCondition cond = WorkerPoolCondition.getPoolCondition(market);
        final float wage = cond.getWorkerPool() * (1f - cond.getFreeWorkerRatio()) *
            (LaborConfig.LPV_day / (engine.isPlayerMarket(marketID) ?
            engine.playerMarketData.get(marketID).getRoSV() : LaborConfig.RoSV)
        );

        return wage * market.getUpkeepMult().getModifiedValue();
    }

    public final int getIndustryIncome(MarketAPI market) {
        float income = 0f;
        for (Industry ind : market.getIndustries()) {
            income += getIndustryIncome(ind).getModifiedValue();
        }

        return (int) income;
    }

    public final int getIndustryUpkeep(MarketAPI market) {
        float upkeep = 0f;
        for (Industry ind : market.getIndustries()) {
            upkeep += getIndustryUpkeep(ind).getModifiedValue();
        }

        return (int) upkeep;
    }

    public final MutableStat getIndustryIncome(Industry ind) {
        final MutableStat income = new MutableStat(ind.getSpec().getIncome());
        income.modifyMult("market_size", ind.getMarket().getSize() - 2);
        income.modifyMult("market_income_mods", ind.getMarket().getIncomeMult().getModifiedValue());

        return income;
    }

    public final MutableStat getIndustryUpkeep(Industry ind) {
        final MutableStat upkeep = new MutableStat(ind.getSpec().getUpkeep());
        upkeep.modifyMult("market_size", ind.getMarket().getSize() - 2);
		upkeep.modifyMultAlways(
            "ind_hazard", ind.getMarket().getUpkeepMult().getModifiedValue(), LocalizedStrings.str("marketUpkeepMultTxt")
        );

        return upkeep;
    }

    public static final boolean isWorkerAssignableByDefault(Industry ind) {
        return ind.isIndustry() && !ind.isStructure();
    }

    public static final boolean isWorkerAssignable(Industry ind) {
        return IndustryConfigManager.getIndConfig(ind).workerAssignable;
    }

    public static final float getWorkersPerUnit(String comID, String tag) {
        final float Pout = settings.getCommoditySpec(comID).getBasePrice();
        final float RoVC = LaborConfig.getRoVC(tag);

        return (Pout * RoVC) / LaborConfig.LPV_day;
    }

    public static final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }

    public static final int getMarketsCount() {
        return Global.getSector().getEconomy().getNumMarkets();
    }

    public static final long getGlobalPopulationCount() {
        long total = 0l;

        for (MarketAPI market : getMarketsCopy()) {
            total += Math.pow(10, market.getSize());
        }

        return total;
    }

    public static final long getFactionPopulationCount(String factionID) {
        long total = 0l;

        for (MarketAPI market : getMarketsCopy()) {
            if (market.getFactionId().equals(factionID)) {
                total += Math.pow(10, market.getSize());
            }
        }

        return total;
    }
}
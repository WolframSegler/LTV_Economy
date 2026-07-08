package wfg.ltv_econ.economy.engine;

import static wfg.ltv_econ.constant.strings.Income.*;
import static wfg.native_ui.util.Globals.settings;

import java.util.Collection;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.config.IndustryConfigManager;
import wfg.ltv_econ.config.LaborConfig;
import wfg.ltv_econ.constant.EconomyConstants;
import wfg.ltv_econ.constant.strings.LocalizedStrings;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.BasePriceCalculator.TransactionDirection;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry;
import wfg.ltv_econ.economy.registry.MarketFinanceRegistry.MarketLedger;
import wfg.ltv_econ.economy.registry.WorkerPoolRegistry.WorkerPool;

/**
 * <p>Credit values are monthly aggregates.</p>
 */
public class EconomyInfo {
    transient EconomyEngine engine;

    EconomyInfo(EconomyEngine engine) { this.engine = engine; }

    public final double getInFactionExports(String comID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            total += cell.inFactionExports;
        }
        return total;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        double total = 0d;
        double marketAmount = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            final double exports = cell.inFactionExports + cell.globalExports;
            total += exports;
            if (cell.marketID.equals(marketID)) {
                marketAmount += exports;
            }
        }
        if (total <= 0d) return 0;
        return (int) ((marketAmount / total) * 100d);
    }

    public final int getImportMarketShare(String comID, String marketID) {
        double total = 0d;
        double marketAmount = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            final double imports = cell.inFactionImports + cell.globalImports;
            total += imports;
            if (cell.marketID.equals(marketID)) {
                marketAmount += imports;
            }
        }
        if (total <= 0d) return 0;
        return (int) ((marketAmount / total) * 100d);
    }

    public final double getFactionInFactionExports(String comID, String factionID) {
        double amount = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.market.getFactionId().equals(factionID)) {
                amount += cell.inFactionExports;
            }
        }
        return amount;
    }

    public final float getFactionImportShare(String comID, String factionID) {
        
        final double total = getGlobalImports(comID);
        if (total == 0d) return 0f;
        final double imports = getFactionGlobalImports(comID, factionID);

        return (float) (imports / total);
    }

    public final double getFactionFormalGlobalImports(String comID, String factionID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.market.getFactionId().equals(factionID)) {
                total += cell.globalImports;
            }
        }
        return total;
    }

    public final float getFactionExportShare(String comID, String factionID) {
        final double total = getGlobalExports(comID);
        if (total == 0) return 0;
        final double exports = getFactionGlobalExports(comID, factionID);

        return (float) (exports / total);
    }

    public final double getFactionFormalGlobalExports(String comID, String factionID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.market.getFactionId().equals(factionID)) {
                total += cell.globalExports;
            }
        }
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
                balance += cell.getQuantumNetChange();
            }
        }
        return balance;
    }

    public final float getFactionImportSufficiency(String comID, String factionID) {
        double totalImports = 0d;
        double inFactionImports = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (!cell.market.getFactionId().equals(factionID)) continue;
            totalImports += cell.getTotalImports();
            inFactionImports += cell.inFactionImports;
        }
        if (totalImports <= 0d) return 1f;
        return (float) Math.min(1d, inFactionImports / totalImports);
    }

    public final double getFactionGlobalImports(String comID, String factionID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.market.getFactionId().equals(factionID)) {
                total += cell.globalImports + cell.informalImports;
            }
        }
        return total;
    }

    public final double getFactionGlobalExports(String comID, String factionID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.market.getFactionId().equals(factionID)) {
                total += cell.globalExports + cell.informalExports;
            }
        }
        return total;
    }

    public static final long getGlobalWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            
            total += WorkerPoolRegistry.get(market).getWorkerPool();
        }
        return total;
    }

    public static final long getGlobalUnemployedWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            final WorkerPool pool = WorkerPoolRegistry.get(market);

            total += (long) (pool.getFreeWorkerRatio() * pool.getWorkerPool());
        }
        return total;
    }

    public static final long getFactionWorkerCount(String factionID) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (market.getFactionId().equals(factionID)) {
                total += WorkerPoolRegistry.get(market).getWorkerPool();
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

    public final long getGlobalProductionSurplus(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getSurplusAfterTargetQuantum();

        return total;
    }

    public final long getGlobalSurplus(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getStoredSurplus();

        return total;
    }

    public final long getGlobalExcess(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getStoredExcess();

        return total;
    }

    public final long getGlobalShortfall(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getStoredShortfall();

        return total;
    }

    public final long getGlobalDeficit(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getStoredDeficit();

        return total;
    }

    public final double getGlobalImports(String comID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            total += cell.globalImports + cell.informalImports;
        }
        return total;
    }

    public final double getGlobalExports(String comID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            total += cell.globalExports + cell.informalExports;
        }
        return total;
    }

    public final double getTotalImports(String comID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            total += cell.getTotalImports();
        }
        return total;
    }

    public final double getTotalExports(String comID) {
        double total = 0d;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            total += cell.getTotalExports();
        }
        return total;
    }

    public final double getGlobalInformalImports(String comID) {
        double total = 0d;
        for (double amount : engine.getComDomain(comID).getInformalImports().values()) {
            total += amount;
        }
        return total;
    }

    public final double getGlobalInformalExports(String comID) {
        double total = 0d;
        for (double amount : engine.getComDomain(comID).getInformalExports().values()) {
            total += amount;
        }
        return total;
    }

    public final float getGlobalAveragePrice(String comID, int units) {
        float total = 0;

        final Collection<CommodityCell> allCells = engine.getComDomain(comID).getAllCells();
        for (CommodityCell cell : allCells) total += cell.getUnitPrice(TransactionDirection.NEUTRAL, units);

        return total / (float) allCells.size();
    }

    public final long getGlobalStockpiles(String comID) {
        double total = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells())
        total += cell.getStored();

        return (long) total;
    }

    public final int getGlobalExporterCount(String comID) {
        int count = 0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.globalExports > 0f) count++;
        }
        return count;
    }

    public final int getGlobalImporterCount(String comID) {
        int count = 0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells()) {
            if (cell.globalImports > 0f) count++;
        }
        return count;
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

        final WorkerPool cond = WorkerPoolRegistry.get(marketID);
        final float wage = cond.getWorkerPool() * (1f - cond.getFreeWorkerRatio()) *
            (LaborConfig.LPV_day / (engine.isPlayerMarket(marketID) ?
            engine.marketPopData.get(marketID).getRoSV() : LaborConfig.RoSV)
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
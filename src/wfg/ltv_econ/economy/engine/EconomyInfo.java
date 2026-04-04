package wfg.ltv_econ.economy.engine;

import static wfg.ltv_econ.constants.EconomyConstants.*;

import java.util.Collection;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.economy.commodity.CommodityCell;
import wfg.ltv_econ.economy.commodity.CommodityDomain;
import wfg.ltv_econ.economy.commodity.IncomeLedger;
import wfg.ltv_econ.economy.commodity.CommodityCell.PriceType;
import wfg.ltv_econ.industry.IndustryIOs;

public class EconomyInfo {
    transient EconomyEngine engine;

    EconomyInfo(EconomyEngine engine) { this.engine = engine; }

    public final double getInFactionExports(String comID) {
        double total = 0.0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.inFactionExports;

        return total;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final double total = getGlobalExports(comID);
        if (total == 0.0) return 0;

        return (int) ((engine.getComCell(comID, marketID).globalExports / total) * 100);
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final double total = getGlobalImports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) engine.getComCell(comID, marketID).globalImports / (float) total) * 100);
    }

    public final double getFactionInFactionExports(String comID, String factionID) {
        double TotalFactionExports = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                TotalFactionExports += cell.inFactionExports;
            }
        }

        return TotalFactionExports;
    }

    public final float getFactionImportShare(String comID, String factionID) {
        final double total = getGlobalImports(comID);
        if (total == 0.0) return 0f;
        final double imports = getFactionGlobalImports(comID, factionID);

        return (float) (imports / total);
    }

    public final float getFactionExportShare(String comID, String factionID) {
        final double total = getGlobalExports(comID);
        if (total == 0) return 0;
        final double exports = getFactionGlobalExports(comID, factionID);

        return (float) (exports / total);
    }

    public final float getFactionImportShareWithInformal(String comID, String factionID) {
        final double total = getGlobalImports(comID) + engine.getComDomain(comID).getInformalNode().imports;
        if (total == 0.0) return 0f;
        final double imports = getFactionGlobalImports(comID, factionID);

        return (float) (imports / total);
    }

    public final float getFactionExportShareWithInformal(String comID, String factionID) {
        final double total = getGlobalExports(comID) + engine.getComDomain(comID).getInformalNode().exports;
        if (total == 0) return 0;
        final double exports = getFactionGlobalExports(comID, factionID);
        
        return (float) (exports / total);
    }

    public final float getInformalImportShare(String comID) {
        final double total = getGlobalImports(comID) + engine.getComDomain(comID).getInformalNode().imports;
        if (total == 0.0) return 0f;
        final double imports = engine.getComDomain(comID).getInformalNode().imports;

        return (float) (imports / total);
    }

    public final float getInformalExportShare(String comID) {
        final double total = getGlobalExports(comID) + engine.getComDomain(comID).getInformalNode().exports;
        if (total == 0) return 0;
        final double exports = engine.getComDomain(comID).getInformalNode().exports;
        
        return (float) (exports / total);
    }

    public final long getPlayerFactionCreditFlow(String comID) {
        long netCreditFlow = 0;

        for (IncomeLedger ledger : engine.comDomains.get(comID).getAllLedgers()) {
            netCreditFlow += ledger.lastMonthExportIncome - ledger.lastMonthImportExpense;
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
        float totalImports = 0;
        float inFactionImports = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalImports += cell.getTotalImports();
                inFactionImports += cell.inFactionImports;
            }
        }

        if (totalImports == 0f) return 1f;
        return Math.min(1f, inFactionImports / totalImports);
    }

    public final double getFactionGlobalImports(String comID, String factionID) {
        double totalGlobalImports = 0.0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalGlobalImports += cell.globalImports;
            }
        }

        return totalGlobalImports;
    }

    public final double getFactionGlobalExports(String comID, String factionID) {
        double totalGlobalExports = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells()) {
            if (cell.market.getFaction().getId().equals(factionID)) {
                totalGlobalExports += cell.globalExports;
            }
        }

        return totalGlobalExports;
    }

    public static final long getGlobalWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            
            total += WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
        }
        return total;
    }

    public final long getGlobalDemand(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getTargetQuantum(true);

        return total;
    }

    public final long getGlobalProduction(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getProduction(true);

        return total;
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
        double total = 0.0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.globalImports;

        return total;
    }

    public final double getGlobalExports(String comID) {
        double total = 0.0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.globalExports;

        return total;
    }

    public final double getGlobalInformalImports(String comID) {
        double total = 0.0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.informalImports;

        return total;
    }

    public final double getGlobalInformalExports(String comID) {
        double total = 0.0;
        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.informalExports;

        return total;
    }
    
    public final long getGlobalTradeVolume(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getTotalExports();

        return total;
    }

    public final float getGlobalAveragePrice(String comID, int units) {
        float total = 0;

        final Collection<CommodityCell> allCells = engine.getComDomain(comID).getAllCells();
        for (CommodityCell cell : allCells)
        total += cell.getUnitPrice(PriceType.NEUTRAL, units);

        return total / (float) allCells.size();
    }

    public final long getGlobalStockpiles(String comID) {
        double total = 0;

        for (CommodityCell cell : engine.comDomains.get(comID).getAllCells())
        total += cell.getStored();

        return (long) total;
    }

    /**
     * Works properly only for player colonies.
     *
     * <p>{@code getIncomeMult()} does not affect trade income. It represents
     * administrative efficiency and applies only to abstract income and upkeep.</p>
     */
    public final long getNetIncome(MarketAPI market, boolean lastMonth) {
        final long exportIncome = getExportIncome(market, lastMonth);
        final long importCost = getImportExpense(market, lastMonth);
        final int wageCost = (int) getWagesForMarket(market)*MONTH;
        final int indIncome = getIndustryIncome(market);
        final int indUpkeep = getIndustryUpkeep(market);
        final int hazardPay = market.isImmigrationIncentivesOn() ?
            (int) market.getImmigrationIncentivesCost() : 0;

        return exportIncome + indIncome - hazardPay - importCost - wageCost - indUpkeep;
    }

    /**
     * Works properly only for player colonies.
     *
     * <p>{@code getIncomeMult()} does not affect trade income. It represents
     * administrative efficiency and applies only to abstract income and upkeep.</p>
     */
    public final long getGrossIncome(MarketAPI market, boolean lastMonth) {
        final long exportIncome = getExportIncome(market, lastMonth);
        final int indIncome = getIndustryIncome(market);

        return exportIncome + indIncome;
    }

    /*
     * Works properly only for player colonies. 
     */
    public final long getExportIncome(MarketAPI market, boolean lastMonth) {
        long exportIncome = 0;
        if (engine.playerMarketData.keySet().contains(market.getId())) {
            for (CommodityDomain dom : engine.comDomains.values()) {
                final IncomeLedger ledger = dom.getLedger(market.getId());
                exportIncome += lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
            }
        }

        return exportIncome;
    }

    public final long getExportIncome(MarketAPI market, String comID, boolean lastMonth) {
        if (engine.playerMarketData.keySet().contains(market.getId())) {

            final IncomeLedger ledger = engine.comDomains.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
        }
        return 0;
    }

    /*
     * Works properly only for player colonies. 
     */
    public final long getImportExpense(MarketAPI market, boolean lastMonth) {
        long importCost = 0;
        if (engine.playerMarketData.keySet().contains(market.getId())) {
            for (CommodityDomain dom : engine.comDomains.values()) {
                final IncomeLedger ledger = dom.getLedger(market.getId());
                importCost += lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
            }
        }

        return importCost;
    }

    public final long getImportExpense(MarketAPI market, String comID, boolean lastMonth) {
        if (engine.playerMarketData.keySet().contains(market.getId())) {
            final IncomeLedger ledger = engine.comDomains.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
        }
        return 0;
    }

    /**
     * Per day value
     */
    public final float getWagesForMarket(final MarketAPI market) {
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
            "ind_hazard", ind.getMarket().getUpkeepMult().getModifiedValue(), "Market upkeep multiplier"
        );

        return upkeep;
    }

    public static final boolean isWorkerAssignable(Industry ind) {
        final IndustryConfig config = IndustryIOs.getIndConfig(ind);
        if (config != null) {
            return config.workerAssignable;
        } else {
            return ind.isIndustry() && !ind.isStructure();
        }
    }

    public static final float getWorkersPerUnit(String comID, String tag) {
        final float Pout = Global.getSettings().getCommoditySpec(comID).getBasePrice();
        final float RoVC = LaborConfig.getRoVC(tag);

        return (Pout * RoVC) / LaborConfig.LPV_day;
    }

    public static final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }
}
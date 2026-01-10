package wfg.ltv_econ.economy.engine;

import static wfg.ltv_econ.constants.economyValues.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;

import wfg.ltv_econ.conditions.WorkerPoolCondition;
import wfg.ltv_econ.configs.IndustryConfigManager.IndustryConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.LaborConfig;
import wfg.ltv_econ.configs.LaborConfigLoader.OCCTag;
import wfg.ltv_econ.economy.CommodityCell;
import wfg.ltv_econ.economy.CommodityCell.PriceType;
import wfg.ltv_econ.economy.CommodityDomain;
import wfg.ltv_econ.economy.IncomeLedger;
import wfg.ltv_econ.economy.WorkerRegistry;
import wfg.ltv_econ.economy.WorkerRegistry.WorkerIndustryData;
import wfg.ltv_econ.industry.IndustryIOs;

public class EconomyInfo {
    transient EconomyEngine engine;

    EconomyInfo(EconomyEngine engine) { this.engine = engine; }

    public Object readResolve() {
        this.engine = EconomyEngine.getInstance();
        return this;
    }

    public final double getTotalGlobalExports(String comID) {
        double total = 0;
        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) 
        total += cell.globalExports;

        return total;
    }

    public final double getTotalFactionExports(String comID) {
        double total = 0;
        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells())
        total += cell.inFactionExports;

        return total;
    }

    public final int getExportMarketShare(String comID, String marketID) {
        final double total = getTotalGlobalExports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) engine.getComCell(comID, marketID).globalExports / (float) total) * 100);
    }

    public final double getTotalGlobalImports(String comID) {
        double totalGlobalImports = 0;
        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) {
            totalGlobalImports += cell.globalImports;
        }

        return totalGlobalImports;
    }

    public final int getImportMarketShare(String comID, String marketID) {
        final double total = getTotalGlobalImports(comID);
        if (total == 0)
            return 0;

        return (int) (((float) engine.getComCell(comID, marketID).globalImports / (float) total) * 100);
    }

    public final double getTotalInFactionExports(String comID, FactionAPI faction) {
        double TotalFactionExports = 0;

        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) {
            if (!cell.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }
            TotalFactionExports += cell.inFactionExports;
        }

        return TotalFactionExports;
    }

    public final float getFactionTotalExportMarketShare(String comID, String factionID) {
        final double total = getTotalGlobalExports(comID);
        if (total == 0) return 0;
        double totalGlobalExports = 0;

        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) {
            if (!cell.market.getFaction().getId().equals(factionID)) {
                continue;
            }
            totalGlobalExports += cell.globalExports;
        }
        return (float) totalGlobalExports / (float) total;
    }

    public final float getFactionTotalImportMarketShare(String comID, String factionID) {
        final double total = getTotalGlobalImports(comID);
        if (total == 0) return 0;
        double totalGlobalImports = 0;

        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) {
            if (!cell.market.getFaction().getId().equals(factionID)) {
                continue;
            }
            totalGlobalImports += cell.globalImports;
        }
        return (float) (totalGlobalImports / total);
    }

    public final double getFactionTotalGlobalExports(String comID, FactionAPI faction) {
        double totalGlobalExports = 0;

        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells()) {
            if (!cell.market.getFaction().getId().equals(faction.getId())) {
                continue;
            }

            totalGlobalExports += cell.globalExports;
        }

        return totalGlobalExports;
    }

    /**
     * Returns the sum of all the available commodity counts of a market
     */
    public final double getMarketActivity(MarketAPI market) {
        double totalActivity = 0;
        for (CommodityDomain dom : engine.m_comDomains.values()) {
            if (!engine.getRegisteredMarkets().contains(market.getId())) {
                engine.registerMarket(market);
            }
            final CommodityCell cell = dom.getCell(market.getId());

            totalActivity += cell.getFlowAvailable();
        }

        return totalActivity;
    }

    public static final long getGlobalWorkerCount(boolean includePlayerMarkets) {
        long total = 0;
        for (MarketAPI market : getMarketsCopy()) {
            if (!includePlayerMarkets && market.isPlayerOwned()) continue;
            
            total += WorkerPoolCondition.getPoolCondition(market).getWorkerPool();
        }
        return total;
    }

    /**
     * Includes over-imports.
     */
    public final float getGlobalTradeRatio(MarketAPI market) {
        if (!market.isInEconomy()) return 0f;

        final double activity = getMarketActivity(market);

        float ratio = 0f;

        for (CommodityDomain dom : engine.m_comDomains.values()) {
            final CommodityCell cell = dom.getCell(market.getId());

            ratio += Math.abs(cell.globalImports - cell.getFlowAvailable()) / activity;
        }

        return ratio;
    }

    public final long getGlobalDemand(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getBaseDemand(true);

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
        total += cell.getFlowCanNotExport();

        return total;
    }

    public final long getGlobalDeficit(String comID) {
        long total = 0;

        for (CommodityCell cell : engine.getComDomain(comID).getAllCells())
        total += cell.getFlowDeficit();

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

        for (CommodityCell cell : engine.m_comDomains.get(comID).getAllCells())
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

    /*
     * Works properly only for player colonies. 
     */
    public final long getExportIncome(MarketAPI market, boolean lastMonth) {
        long exportIncome = 0;
        if (engine.m_playerMarketData.keySet().contains(market.getId())) {
            for (CommodityDomain dom : engine.m_comDomains.values()) {
                final IncomeLedger ledger = dom.getLedger(market.getId());
                exportIncome += lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
            }
        }

        return exportIncome;
    }

    public final long getExportIncome(MarketAPI market, String comID, boolean lastMonth) {
        if (engine.m_playerMarketData.keySet().contains(market.getId())) {

            final IncomeLedger ledger = engine.m_comDomains.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthExportIncome : ledger.monthlyExportIncome;
        }
        return 0;
    }

    /*
     * Works properly only for player colonies. 
     */
    public final long getImportExpense(MarketAPI market, boolean lastMonth) {
        long importCost = 0;
        if (engine.m_playerMarketData.keySet().contains(market.getId())) {
            for (CommodityDomain dom : engine.m_comDomains.values()) {
                final IncomeLedger ledger = dom.getLedger(market.getId());
                importCost += lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
            }
        }

        return importCost;
    }

    public final long getImportExpense(MarketAPI market, String comID, boolean lastMonth) {
        if (engine.m_playerMarketData.keySet().contains(market.getId())) {
            final IncomeLedger ledger = engine.m_comDomains.get(comID).getLedger(market.getId());
            return lastMonth ? ledger.lastMonthImportExpense : ledger.monthlyImportExpense;
        }
        return 0;
    }

    /**
     * Per day value
     */
    public final float getWagesForMarket(MarketAPI market) {
        final String marketID = market.getId();
        float wage = 0f;

        for (WorkerIndustryData data : WorkerRegistry.getInstance().getIndustriesUsingWorkers(marketID)) {
            wage += data.getWorkersAssigned() * (LaborConfig.LPV_day / (engine.isPlayerMarket(marketID) ?
            engine.m_playerMarketData.get(marketID).getRoSV() : LaborConfig.RoSV));
        }

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

    public static final float getWorkersPerUnit(String comID, OCCTag tag) {
        final float Pout = Global.getSettings().getCommoditySpec(comID).getBasePrice();
        final float LPV_day = LaborConfig.LPV_day;
        final float RoVC = LaborConfig.getRoVC(tag);

        return (Pout * RoVC) / LPV_day;
    }

    public static final List<CommoditySpecAPI> getEconCommodities() {
        return Global.getSettings().getAllCommoditySpecs().stream()
            .filter(spec -> !spec.isNonEcon())
            .collect(Collectors.toList());
    }

    public static final List<String> getEconCommodityIDs() {
        return Global.getSettings().getAllCommoditySpecs().stream()
            .filter(spec -> !spec.isNonEcon())
            .map(CommoditySpecAPI::getId)
            .collect(Collectors.toList());
    }

    public static final List<MarketAPI> getMarketsCopy() {
        return Global.getSector().getEconomy().getMarketsCopy();
    }
}
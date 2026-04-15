package wfg.ltv_econ.config;

import java.util.List;

import wfg.ltv_econ.config.loader.EconomyConfigLoader;
import wfg.ltv_econ.ui.marketInfo.dialogs.ManagePopulationDialog;

public class EconConfig {
    private EconConfig() {};

    static {
        EconomyConfigLoader.loadConfig();
    }

    /**
     * Multi Threading used for the main loop. The simplex solver expensive.
     */
    public static boolean MULTI_THREADING;

    /**
     * Should the workers be assigned again after a game load.
     */
    public static boolean ASSIGN_WORKERS_ON_LOAD;

    /**
     * The amount of credits each market begins with after creation.
     */
    public static int STARTING_CREDITS_FOR_MARKET;

    /**
     * Cost of covering a deficit with slack for the linear solver.
     */
    public static double ECON_DEFICIT_COST;

    /**
     * The cost multiplier for workers used to satisfy local demand.
     */
    public static double LOCAL_WORKER_COST_MULT;

    /**
     * The cost multiplier for workers used to satisfy Faction demand.
     */
    public static double FACTION_WORKER_COST_MULT;

    /**
     * Applied to the ceiling of local production coefficient.
     */
    public static double LOCAL_PROD_BUFFER;

    /**
     * Applied to the ceiling of faction production coefficient.
     */
    public static double FACTION_PROD_BUFFER;

    /**
     * Applied to the demand vector of worker-independent industries.
     */
    public static double PRODUCTION_BUFFER;

    /**
     * each market aims to have <code>x</code> days worth of stockpiles.
     */
    public static int DAYS_TO_COVER;

    /**
     * each market imports <code>x</code> times the daily demand at most to build up stockpiles.
     */
    public static int DAYS_TO_COVER_PER_IMPORT; 

    /**
     * Multiplicative discount applied to trade between markets of the same faction.
     */
    public static float FACTION_EXCHANGE_MULT;

    /**
     * The minimum faction relationship required to trade.
     */
    public static float MIN_RELATION_TO_TRADE;

    /**
     * The method {@link #assignWorkers()} will be called once every <code>x</code> days.
     */
    public static int WORKER_ASSIGN_INTERVAL;

    /**
     * The trade methods will be called once every <code>x</code> days.S
     */
    public static int TRADE_INTERVAL;

    /**
     * The length of arrays that store commodity history.
     */
    public static int HISTORY_LENGTH;

    /**
     * Debt debuffs by tiers for markets.
     */
    public static List<DebtDebuffTier> DEBT_DEBUFF_TIERS;

    /**
     * List of markets who will not get the manufacturing industry.
     */
    public static List<String> MANUFACTURING_EXCLUSION_LIST;

    /**
     * Multiplier applied to preferredStockpiles to determine the minimum stock level
     * a market must exceed before it is allowed to export this commodity.
     */
    public static float EXPORT_THRESHOLD_FACTOR;

    /**
     * Determines the visibility of policies under the {@link ManagePopulationDialog} dialog.
     */
    public static boolean SHOW_MARKET_POLICIES;

    /**
     * The drop in reputation when an embargo is imposed.
     */
    public static float EMBARGO_REP_DROP;

    /**
     * Iteration count for the worker allocation solver.
     */
    public static int ECON_ALLOCATION_PASSES;

    /**
     * Ratio of ideal workers that will be assigned unconditionally.
     */
    public static float MIN_WORKER_FRACTION;

    /** 
     * Multiplier controlling daily production retained in local stockpiles before exporting.
     */
    public static float PRODUCTION_HOLD_FACTOR;

    /**
     * The ratio of goods sold to the open market that finds its way to the market stockpiles.
     */
    public static float OPEN_MARKET_TO_STOCKPILES_RATIO;

    /**
     * Determines the use of worker productivity when calculating fair share of workers.
     */
    public static boolean USE_PRODUCTION_FAIRNESS;

    /**
     * Monthly credits withdraw limit for player colonies.
     */
    public static int CREDIT_WITHDRAWAL_LIMIT;

    /**
     * Limit to the ratio of profits from colonies that can go to the player.
     */
    public static float AUTO_TRANSFER_PROFIT_LIMIT;

    /**
     * Multiplier to maintenance of faction inventory ships. 
     */
    public static float IDLE_SHIP_MAINTENANCE_MULT;

    /**
     * Monthly wages for faction inventory ships crew.
     */
    public static float CREW_WAGE_PER_MONTH;

    /**
     * Multiplier for faction inventory ships crews wages.
     */
    public static float IDLE_CREW_WAGE_MULT;

    /**
     * Trade fleets travel speed.
     */
    public static int TRAVEL_SPEED_LY_DAY;

    /**
     * The cost multiplier for importing fuel when the stockpiles are empty.
     */
    public static float FORCED_FUEL_IMPORT_COST_MULT;

    public static float INDEPENDENT_TRADE_FLEET_BASE_FEE;
    public static float INDEPENDENT_TRADE_FLEET_PER_TON_FEE;
    public static float INDEPENDENT_TRADE_FLEET_PERCENT_CUT;
    public static float INDEPENDENT_TRADE_FLEET_HAZARD_BASE;
    public static float INDEPENDENT_TRADE_FLEET_HAZARD_MULT;

    /**
     * The maximum fraction of a colony's stockpile that can be looted in a single raid.
     */
    public static double RAID_STOCKPILES_ACCESS_RATIO;
    public static double RAID_BASE_EFF;

    public static record DebtDebuffTier(
        long threshold,
        int stabilityPenalty,
        int upkeepMultiplierPercent,
        int immigrationModifier
    ) {}
}
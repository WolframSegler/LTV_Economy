package wfg.ltv_econ.configs;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import static wfg.ltv_econ.constants.Mods.*;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.ui.marketInfo.dialogs.ManagePopulationDialog;

public class EconomyConfigLoader {
    private static final SettingsAPI settings = Global.getSettings();
    private static final String CONFIG_PATH = "./data/config/ltvEcon/economy_config.json";

    private static JSONObject config;

    private static final void load() {
        try {
            config = settings.getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load economy config: " + CONFIG_PATH, ex);
        }
    }

    public static final JSONObject getConfig() {
        if (config == null) load();
        return config;
    }

    public static final void loadConfig() {
        final JSONObject root = getConfig();

        try {
        EconomyConfig.MULTI_THREADING = root.getBoolean("MULTI_THREADING");
        EconomyConfig.ASSIGN_WORKERS_ON_LOAD = root.getBoolean("ASSIGN_WORKERS_ON_LOAD");
        EconomyConfig.STARTING_CREDITS_FOR_MARKET = root.getInt("STARTING_CREDITS_FOR_MARKET");
        EconomyConfig.LOCAL_WORKER_COST_MULT = root.getDouble("LOCAL_WORKER_COST_MULT");
        EconomyConfig.FACTION_WORKER_COST_MULT = root.getDouble("FACTION_WORKER_COST_MULT");
        EconomyConfig.ECON_DEFICIT_COST = root.getDouble("ECON_DEFICIT_COST");
        EconomyConfig.LOCAL_PROD_BUFFER = 1f + root.getDouble("LOCAL_PROD_BUFFER");
        EconomyConfig.FACTION_PROD_BUFFER = 1f + root.getDouble("FACTION_PROD_BUFFER");
        EconomyConfig.PRODUCTION_BUFFER = 1f + root.getDouble("PRODUCTION_BUFFER");
        EconomyConfig.DAYS_TO_COVER = root.getInt("DAYS_TO_COVER");
        EconomyConfig.DAYS_TO_COVER_PER_IMPORT = root.getInt("DAYS_TO_COVER_PER_IMPORT");
        EconomyConfig.FACTION_EXCHANGE_MULT = (float) root.getDouble("FACTION_EXCHANGE_MULT");
        EconomyConfig.WORKER_ASSIGN_INTERVAL = root.getInt("WORKER_ASSIGN_INTERVAL");
        EconomyConfig.EXPORT_THRESHOLD_FACTOR = (float) root.getDouble("EXPORT_THRESHOLD_FACTOR");
        EconomyConfig.SHOW_MARKET_POLICIES = root.getBoolean("SHOW_MARKET_POLICIES");
        EconomyConfig.EMBARGO_REP_DROP = (float) root.getDouble("EMBARGO_REP_DROP");
        EconomyConfig.MIN_RELATION_TO_TRADE = (float) root.getDouble("MIN_RELATION_TO_TRADE");
        EconomyConfig.ECON_ALLOCATION_PASSES = root.getInt("ECON_ALLOCATION_PASSES");
        EconomyConfig.MIN_WORKER_FRACTION = (float) root.getDouble("MIN_WORKER_FRACTION");
        EconomyConfig.PRODUCTION_HOLD_FACTOR = (float) root.getDouble("PRODUCTION_HOLD_FACTOR");
        EconomyConfig.OPEN_MARKET_TO_STOCKPILES_RATIO = (float) root.getDouble("OPEN_MARKET_TO_STOCKPILES_RATIO");
        EconomyConfig.USE_PRODUCTION_FAIRNESS = root.getBoolean("USE_PRODUCTION_FAIRNESS");
        EconomyConfig.CREDIT_WITHDRAWAL_LIMIT = root.getInt("CREDIT_WITHDRAWAL_LIMIT");
        EconomyConfig.AUTO_TRANSFER_PROFIT_LIMIT = (float) root.getDouble("AUTO_TRANSFER_PROFIT_LIMIT");
        EconomyConfig.IDLE_SHIP_MAINTENANCE_MULT = (float) root.getDouble("IDLE_SHIP_MAINTENANCE_MULT");
        EconomyConfig.CREW_WAGE_PER_MONTH = (float) root.getDouble("CREW_WAGE_PER_MONTH");
        EconomyConfig.IDLE_CREW_WAGE_MULT = (float) root.getDouble("IDLE_CREW_WAGE_MULT");
        EconomyConfig.TRADE_INTERVAL = root.getInt("TRADE_INTERVAL");
        EconomyConfig.HISTORY_LENGTH = root.getInt("HISTORY_LENGTH");
        EconomyConfig.TRAVEL_SPEED_LY_DAY = root.getInt("TRAVEL_SPEED_LY_DAY");
        EconomyConfig.FORCED_FUEL_IMPORT_COST_MULT = (float) root.getDouble("FORCED_FUEL_IMPORT_COST_MULT");

        final JSONArray debtArr = root.getJSONArray("DEBT_DEBUFF_TIERS");
        EconomyConfig.DEBT_DEBUFF_TIERS = new ArrayList<>(debtArr.length());
        for (int i = 0; i < debtArr.length(); i++) {
            final JSONObject o = debtArr.getJSONObject(i);
            EconomyConfig.DEBT_DEBUFF_TIERS.add(
                new DebtDebuffTier(
                    (long) o.getDouble("threshold"),
                    o.getInt("stabilityPenalty"),
                    o.getInt("upkeepMultiplierPercent"),
                    o.getInt("immigrationModifier")
                )
            );
        }

        final JSONArray exclusionArr = root.getJSONArray("MANUFACTURING_EXCLUSION_LIST");
        EconomyConfig.MANUFACTURING_EXCLUSION_LIST = new ArrayList<>(exclusionArr.length());
        for (int i = 0; i < exclusionArr.length(); i++) {
            EconomyConfig.MANUFACTURING_EXCLUSION_LIST.add(exclusionArr.getString(i));
        }

        if (settings.getModManager().isModEnabled(LUNA_LIB)) {
            loadFromLunaSettings();
        }

        } catch (Exception e) {
        throw new RuntimeException(
            "Failed to load economy configuration from " + CONFIG_PATH + ": "
            + e.getMessage(), e
        );
    }
    }

    public static final void loadFromLunaSettings() {
        EconomyConfig.MULTI_THREADING = LunaSettings.getBoolean(LTV_ECON, "economy_multiThreading");
        EconomyConfig.ASSIGN_WORKERS_ON_LOAD = LunaSettings.getBoolean(LTV_ECON, "economy_assignOnLoad");
        EconomyConfig.STARTING_CREDITS_FOR_MARKET = LunaSettings.getInt(LTV_ECON, "economy_startingCredits");
        EconomyConfig.CREDIT_WITHDRAWAL_LIMIT = LunaSettings.getInt(LTV_ECON, "economy_withdrawalLimit");
        EconomyConfig.AUTO_TRANSFER_PROFIT_LIMIT = LunaSettings.getDouble(LTV_ECON, "economy_autoTransferLimit").floatValue();
        EconomyConfig.ECON_DEFICIT_COST = LunaSettings.getDouble(LTV_ECON, "economy_deficitCost");
        EconomyConfig.LOCAL_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "economy_localWorkerCost");
        EconomyConfig.FACTION_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "economy_factionWorkerCost");
        EconomyConfig.LOCAL_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_localProdBuffer");
        EconomyConfig.FACTION_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_factionProdBuffer");
        EconomyConfig.PRODUCTION_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_prodBuffer");
        EconomyConfig.DAYS_TO_COVER = LunaSettings.getInt(LTV_ECON, "economy_daysToCover");
        EconomyConfig.DAYS_TO_COVER_PER_IMPORT = LunaSettings.getInt(LTV_ECON, "economy_perImportDaysCover");
        EconomyConfig.FACTION_EXCHANGE_MULT = LunaSettings.getDouble(LTV_ECON, "economy_factionExchangeMult").floatValue();
        EconomyConfig.WORKER_ASSIGN_INTERVAL = LunaSettings.getInt(LTV_ECON, "economy_assignInterval");
        EconomyConfig.EXPORT_THRESHOLD_FACTOR = LunaSettings.getDouble(LTV_ECON, "economy_exportThreshold").floatValue();
        EconomyConfig.EMBARGO_REP_DROP = LunaSettings.getDouble(LTV_ECON, "economy_embargoRepDrop").floatValue();
        EconomyConfig.MIN_RELATION_TO_TRADE = LunaSettings.getDouble(LTV_ECON,"economy_minTradeRelation").floatValue();
        EconomyConfig.ECON_ALLOCATION_PASSES = LunaSettings.getInt(LTV_ECON, "economy_allocPasses");
        EconomyConfig.MIN_WORKER_FRACTION = LunaSettings.getDouble(LTV_ECON, "economy_minWorkerFraction").floatValue();
        EconomyConfig.IDLE_SHIP_MAINTENANCE_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_idleShipMaintenanceMult").floatValue();
        EconomyConfig.CREW_WAGE_PER_MONTH = LunaSettings.getDouble(LTV_ECON, "fleet_crewWages").floatValue();
        EconomyConfig.IDLE_CREW_WAGE_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_idleCrewWagesMult").floatValue();
        EconomyConfig.TRADE_INTERVAL = LunaSettings.getInt(LTV_ECON, "economy_tradeInterval");
        EconomyConfig.TRAVEL_SPEED_LY_DAY = LunaSettings.getInt(LTV_ECON, "fleet_travelSpeedLyDay");
        EconomyConfig.FORCED_FUEL_IMPORT_COST_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_forcedFuelCostMult").floatValue();

        final String fairness = LunaSettings.getString(LTV_ECON, "economy_prodFairness");
        EconomyConfig.USE_PRODUCTION_FAIRNESS = fairness.equals("Commodities Produced");
    }

    public static class EconomyConfig {

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

        static {
            EconomyConfigLoader.loadConfig();
        }

        private EconomyConfig() {};
    }

    public static record DebtDebuffTier(
        long threshold,
        int stabilityPenalty,
        int upkeepMultiplierPercent,
        int immigrationModifier
    ) {}

    private EconomyConfigLoader() {};
}
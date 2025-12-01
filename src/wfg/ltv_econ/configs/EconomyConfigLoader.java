package wfg.ltv_econ.configs;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

public class EconomyConfigLoader {
    private static final String CONFIG_PATH = "./data/config/economy_config.json";

    private static JSONObject config;

    private static void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load economy config: " + CONFIG_PATH, ex);
        }
    }

    public static JSONObject getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void loadConfig() {
        final JSONObject root = getConfig();

        try {
            EconomyConfig.MULTI_THREADING = root.getBoolean("MULTI_THREADING");
            EconomyConfig.STARTING_CREDITS_FOR_MARKET = root.getInt("STARTING_CREDITS_FOR_MARKET");
            EconomyConfig.DAILY_MAINTENANCE_SUPPLY = (float) root.getDouble("DAILY_MAINTENANCE_SUPPLY");
            EconomyConfig.CONCENTRATION_COST = root.getDouble("CONCENTRATION_COST");
            EconomyConfig.IDEAL_SPREAD_TOLERANCE = root.getDouble("IDEAL_SPREAD_TOLERANCE");
            EconomyConfig.MARKET_MODIFIER_SCALER = root.getDouble("MARKET_MODIFIER_SCALER");
            EconomyConfig.PRODUCTION_BUFFER = 1f + root.getDouble("PRODUCTION_BUFFER");
            EconomyConfig.DAYS_TO_COVER = root.getInt("DAYS_TO_COVER");
            EconomyConfig.DAYS_TO_COVER_PER_IMPORT = root.getInt("DAYS_TO_COVER_PER_IMPORT");
            EconomyConfig.FACTION_EXCHANGE_MULT = (float) root.getDouble("FACTION_EXCHANGE_MULT");
            EconomyConfig.VOLATILITY_WINDOW = root.getInt("VOLATILITY_WINDOW");
            EconomyConfig.WORKER_ASSIGN_INTERVAL = root.getInt("WORKER_ASSIGN_INTERVAL");

            final JSONArray arr = root.getJSONArray("DEBT_DEBUFF_TIERS");
            EconomyConfig.DEBT_DEBUFF_TIERS = new long[arr.length() * 2];
            for (int i = 0; i < arr.length(); i++) {
                final JSONObject o = arr.getJSONObject(i);

                final int base = i * 2;
                EconomyConfig.DEBT_DEBUFF_TIERS[base] = (long) o.getDouble("threshold");
                EconomyConfig.DEBT_DEBUFF_TIERS[base + 1] = o.getInt("stabilityPenalty");
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load economy configuration from " + CONFIG_PATH + ": "
                + e.getMessage(), e
            );
        }
    }

    public static class EconomyConfig {

        /**
         * Multi Threading used for the main loop. The matrix calculations are expensive.
         */
        public static boolean MULTI_THREADING;

        /**
         * The amount of credits each market begins with after creation.
         */
        public static int STARTING_CREDITS_FOR_MARKET;

        /**
         * The default daily units of supply consumed by industries who have the default maintenance turned on.
         */
        public static float DAILY_MAINTENANCE_SUPPLY;

        /**
         * Higher values lead to more worker concentration, less ideal spread and less unemployment.
         */
        public static double CONCENTRATION_COST;

        /**
         * The range of allowed deviance from the ideal spread the simplex solver can have.
         */
        public static double IDEAL_SPREAD_TOLERANCE;

        /**
         * Multiplier for Output scalers when determining worker distribution.
         */
        public static double MARKET_MODIFIER_SCALER;

        /**
         * Applied to the demand vector of worker-independent industries
         */
        public static double PRODUCTION_BUFFER;

        /**
         * each market aims to have <code>x</code> days worth of stockpiles
         */
        public static int DAYS_TO_COVER;

        /**
         * each market imports <code>x</code> times the daily demand at most to build up stockpiles
         */
        public static int DAYS_TO_COVER_PER_IMPORT; 

        /**
         * Multiplicative discount applied to trade between markets of the same faction
         */
        public static float FACTION_EXCHANGE_MULT;

        /**
         * The <code>x</code> amount of days which will be used calculate the volatility of a commodity.
         */
        public static int VOLATILITY_WINDOW;

        /**
         * The method {@link #assignWorkers()} will be called once every <code>x</code> days.
         */
        public static int WORKER_ASSIGN_INTERVAL;

        /**
         * Debt debuff tiers for markets:
         *   [i] = threshold in credits (negative value, long)
         *   [i + 1] = stability penalty applied if market debt < threshold
         * 
         * The list should be sorted from smallest (least negative) to largest (most negative) threshold.
         */
        public static long[] DEBT_DEBUFF_TIERS;

        static {
            EconomyConfigLoader.loadConfig();
        }
    }
}
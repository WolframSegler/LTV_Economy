package wfg.ltv_econ.configs;

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
            EconomyConfig.CONCENTRATION_COST = root.getDouble("CONCENTRATION_COST");
            EconomyConfig.IDEAL_SPREAD_TOLERANCE = root.getDouble("IDEAL_SPREAD_TOLERANCE");
            EconomyConfig.MARKET_MODIFIER_SCALER = root.getDouble("MARKET_MODIFIER_SCALER");
            EconomyConfig.PRODUCTION_BUFFER = 1f + root.getDouble("PRODUCTION_BUFFER");
            EconomyConfig.DAYS_TO_COVER = root.getInt("DAYS_TO_COVER");
            EconomyConfig.DAYS_TO_COVER_PER_IMPORT = root.getInt("DAYS_TO_COVER_PER_IMPORT");
            EconomyConfig.FACTION_EXCHANGE_MULT = root.getInt("FACTION_EXCHANGE_MULT");

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

        static {
            EconomyConfigLoader.loadConfig();
        }
    }
}
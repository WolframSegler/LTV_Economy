package wfg.ltv_econ.configs;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;

import wfg.ltv_econ.ui.dialogs.ManagePopulationDialog;
import static wfg.ltv_econ.constants.Mods.*;

import lunalib.lunaSettings.LunaSettings;

public class EconomyConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/economy_config.json";

    private static JSONObject config;

    private static final void load() {
        try {
            config = Global.getSettings().getMergedJSON(CONFIG_PATH);
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
        EconomyConfig.STARTING_CREDITS_FOR_MARKET = root.getInt("STARTING_CREDITS_FOR_MARKET");
        EconomyConfig.CONCENTRATION_COST = root.getDouble("CONCENTRATION_COST");
        EconomyConfig.ECON_DEFICIT_COST = root.getDouble("ECON_DEFICIT_COST");
        EconomyConfig.PRODUCTION_BUFFER = 1f + root.getDouble("PRODUCTION_BUFFER");
        EconomyConfig.DAYS_TO_COVER = root.getInt("DAYS_TO_COVER");
        EconomyConfig.DAYS_TO_COVER_PER_IMPORT = root.getInt("DAYS_TO_COVER_PER_IMPORT");
        EconomyConfig.FACTION_EXCHANGE_MULT = (float) root.getDouble("FACTION_EXCHANGE_MULT");
        EconomyConfig.VOLATILITY_WINDOW = root.getInt("VOLATILITY_WINDOW");
        EconomyConfig.WORKER_ASSIGN_INTERVAL = root.getInt("WORKER_ASSIGN_INTERVAL");
        EconomyConfig.EXPORT_THRESHOLD_FACTOR = (float) root.getDouble("EXPORT_THRESHOLD_FACTOR");
        EconomyConfig.SHOW_MARKET_POLICIES = root.getBoolean("SHOW_MARKET_POLICIES");
        EconomyConfig.EMBARGO_REP_DROP = (float) root.getDouble("EMBARGO_REP_DROP");
        EconomyConfig.ECON_ALLOCATION_PASSES = root.getInt("ECON_ALLOCATION_PASSES");
        EconomyConfig.MIN_WORKER_FRACTION = (float) root.getDouble("MIN_WORKER_FRACTION");

        final JSONArray arr = root.getJSONArray("DEBT_DEBUFF_TIERS");
        EconomyConfig.DEBT_DEBUFF_TIERS = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            final JSONObject o = arr.getJSONObject(i);
            EconomyConfig.DEBT_DEBUFF_TIERS.add(
                new DebtDebuffTier(
                    (long) o.getDouble("threshold"),
                    o.getInt("stabilityPenalty"),
                    o.getInt("upkeepMultiplierPercent"),
                    o.getInt("immigrationModifier")
                )
            );
        }

        if (Global.getSettings().getModManager().isModEnabled(LUNA_LIB)) {
            EconomyConfig.MULTI_THREADING = LunaSettings.getBoolean(LTV_ECON, "economy_multiThreading");
            EconomyConfig.STARTING_CREDITS_FOR_MARKET = LunaSettings.getInt(LTV_ECON, "economy_startingCredits");
            EconomyConfig.CONCENTRATION_COST = LunaSettings.getDouble(LTV_ECON, "economy_concentrationCost");
            EconomyConfig.ECON_DEFICIT_COST = LunaSettings.getDouble(LTV_ECON, "economy_deficitCost");
            EconomyConfig.PRODUCTION_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_prodBuffer");
            EconomyConfig.DAYS_TO_COVER = LunaSettings.getInt(LTV_ECON, "economy_daysToCover");
            EconomyConfig.DAYS_TO_COVER_PER_IMPORT = LunaSettings.getInt(LTV_ECON, "economy_toCoverPerImport");
            EconomyConfig.FACTION_EXCHANGE_MULT = LunaSettings.getDouble(LTV_ECON, "economy_factionExchangeMult").floatValue();
            EconomyConfig.WORKER_ASSIGN_INTERVAL = LunaSettings.getInt(LTV_ECON, "economy_assignInterval");
            EconomyConfig.EXPORT_THRESHOLD_FACTOR = LunaSettings.getDouble(LTV_ECON, "economy_exportThreshold").floatValue();
            EconomyConfig.EMBARGO_REP_DROP = LunaSettings.getDouble(LTV_ECON, "economy_embargoRepDrop").floatValue();
            EconomyConfig.ECON_ALLOCATION_PASSES = LunaSettings.getInt(LTV_ECON, "economy_allocPasses");
            EconomyConfig.MIN_WORKER_FRACTION = LunaSettings.getDouble(LTV_ECON, "economy_minWorkerFraction").floatValue();
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
         * Higher values lead to more worker concentration, less ideal spread and less unemployment.
         */
        public static double CONCENTRATION_COST;

        /**
         * Cost of covering a deficit with slack for the linear solver.
         */
        public static double ECON_DEFICIT_COST;

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
         * Debt debuffs by tiers for markets
         */
        public static List<DebtDebuffTier> DEBT_DEBUFF_TIERS;

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

        static {
            EconomyConfigLoader.loadConfig();
        }
    }

    public static record DebtDebuffTier(
        long threshold,
        int stabilityPenalty,
        int upkeepMultiplierPercent,
        int immigrationModifier
    ) {}
}
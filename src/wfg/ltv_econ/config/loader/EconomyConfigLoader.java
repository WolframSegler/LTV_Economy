package wfg.ltv_econ.config.loader;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import static wfg.ltv_econ.constants.Mods.*;
import static wfg.native_ui.util.Globals.settings;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.EconConfig.DebtDebuffTier;

public class EconomyConfigLoader {
    private static final String CONFIG_PATH = "./data/config/ltvEcon/economy_config.json";

    private static JSONObject config;

    private EconomyConfigLoader() {};
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
        EconConfig.MULTI_THREADING = root.getBoolean("MULTI_THREADING");
        EconConfig.ASSIGN_WORKERS_ON_LOAD = root.getBoolean("ASSIGN_WORKERS_ON_LOAD");
        EconConfig.STARTING_CREDITS_FOR_MARKET = root.getInt("STARTING_CREDITS_FOR_MARKET");
        EconConfig.LOCAL_WORKER_COST_MULT = root.getDouble("LOCAL_WORKER_COST_MULT");
        EconConfig.FACTION_WORKER_COST_MULT = root.getDouble("FACTION_WORKER_COST_MULT");
        EconConfig.ECON_DEFICIT_COST = root.getDouble("ECON_DEFICIT_COST");
        EconConfig.LOCAL_PROD_BUFFER = 1f + root.getDouble("LOCAL_PROD_BUFFER");
        EconConfig.FACTION_PROD_BUFFER = 1f + root.getDouble("FACTION_PROD_BUFFER");
        EconConfig.PRODUCTION_BUFFER = 1f + root.getDouble("PRODUCTION_BUFFER");
        EconConfig.DAYS_TO_COVER = root.getInt("DAYS_TO_COVER");
        EconConfig.DAYS_TO_COVER_PER_IMPORT = root.getInt("DAYS_TO_COVER_PER_IMPORT");
        EconConfig.FACTION_EXCHANGE_MULT = (float) root.getDouble("FACTION_EXCHANGE_MULT");
        EconConfig.WORKER_ASSIGN_INTERVAL = root.getInt("WORKER_ASSIGN_INTERVAL");
        EconConfig.EXPORT_THRESHOLD_FACTOR = (float) root.getDouble("EXPORT_THRESHOLD_FACTOR");
        EconConfig.SHOW_MARKET_POLICIES = root.getBoolean("SHOW_MARKET_POLICIES");
        EconConfig.EMBARGO_REP_DROP = (float) root.getDouble("EMBARGO_REP_DROP");
        EconConfig.MIN_RELATION_TO_TRADE = (float) root.getDouble("MIN_RELATION_TO_TRADE");
        EconConfig.ECON_ALLOCATION_PASSES = root.getInt("ECON_ALLOCATION_PASSES");
        EconConfig.PRODUCTION_HOLD_FACTOR = (float) root.getDouble("PRODUCTION_HOLD_FACTOR");
        EconConfig.OPEN_MARKET_TO_STOCKPILES_RATIO = (float) root.getDouble("OPEN_MARKET_TO_STOCKPILES_RATIO");
        EconConfig.CREDIT_WITHDRAWAL_LIMIT = root.getInt("CREDIT_WITHDRAWAL_LIMIT");
        EconConfig.AUTO_TRANSFER_PROFIT_LIMIT = (float) root.getDouble("AUTO_TRANSFER_PROFIT_LIMIT");
        EconConfig.IDLE_SHIP_MAINTENANCE_MULT = (float) root.getDouble("IDLE_SHIP_MAINTENANCE_MULT");
        EconConfig.CREW_WAGE_PER_MONTH = (float) root.getDouble("CREW_WAGE_PER_MONTH");
        EconConfig.IDLE_CREW_WAGE_MULT = (float) root.getDouble("IDLE_CREW_WAGE_MULT");
        EconConfig.TRADE_INTERVAL = root.getInt("TRADE_INTERVAL");
        EconConfig.HISTORY_LENGTH = root.getInt("HISTORY_LENGTH");
        EconConfig.TRAVEL_SPEED_LY_DAY = root.getInt("TRAVEL_SPEED_LY_DAY");
        EconConfig.FORCED_FUEL_IMPORT_COST_MULT = (float) root.getDouble("FORCED_FUEL_IMPORT_COST_MULT");
        EconConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_BASE_FEE");
        EconConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_PER_TON_FEE");
        EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_PERCENT_CUT");
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_HAZARD_BASE");
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_HAZARD_MULT");
        EconConfig.RAID_STOCKPILES_ACCESS_RATIO = root.getDouble("RAID_STOCKPILES_ACCESS_RATIO");
        EconConfig.RAID_BASE_EFF = root.getDouble("RAID_BASE_EFF");
        EconConfig.TRADE_MISSION_MAX_DISPLAYED_SHIPS = root.getInt("TRADE_MISSION_MAX_DISPLAYED_SHIPS");
        EconConfig.TRADE_MAP_MIN_AMOUNT_FILTER = root.getInt("TRADE_MAP_MIN_AMOUNT_FILTER");
        EconConfig.MAX_VISIBLE_PLANNED_ORDERS = root.getInt("MAX_VISIBLE_PLANNED_ORDERS");
        EconConfig.INFORMAL_TRADE_SHARE = (float) root.getDouble("INFORMAL_TRADE_SHARE");
        EconConfig.NPC_FACTION_ASSEMBLY_LINES = root.getInt("NPC_FACTION_ASSEMBLY_LINES");
        EconConfig.PLAYER_FACTION_ASSEMBLY_LINES = root.getInt("PLAYER_FACTION_ASSEMBLY_LINES");
        EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER = (float) root.getDouble("SHIP_ALLOC_CAPACITY_TARGET_BUFFER");
        EconConfig.SHIP_ALLOC_MARKET_WEIGHT_PER_SIZE = (float) root.getDouble("SHIP_ALLOC_MARKET_WEIGHT_PER_SIZE");
        EconConfig.SHIP_ALLOC_MARKET_SIZE_EXPONENT = (float) root.getDouble("SHIP_ALLOC_MARKET_SIZE_EXPONENT");
        EconConfig.SHIP_ALLOC_AGGRESSION_COMBAT_MULT = (float) root.getDouble("SHIP_ALLOC_AGGRESSION_COMBAT_MULT");
        EconConfig.SHIP_ALLOC_THREAT_RELATIONSHIP_MULT = (float) root.getDouble("SHIP_ALLOC_THREAT_RELATIONSHIP_MULT");
        EconConfig.SHIP_ALLOC_MIN_COMBAT_POWER = (float) root.getDouble("SHIP_ALLOC_MIN_COMBAT_POWER");
        EconConfig.SCRAP_REFUND_FRACTION = (float) root.getDouble("SCRAP_REFUND_FRACTION");

        final JSONArray debtArr = root.getJSONArray("DEBT_DEBUFF_TIERS");
        EconConfig.DEBT_DEBUFF_TIERS = new ArrayList<>(debtArr.length());
        for (int i = 0; i < debtArr.length(); i++) {
            final JSONObject o = debtArr.getJSONObject(i);
            EconConfig.DEBT_DEBUFF_TIERS.add(
                new DebtDebuffTier(
                    (long) o.getDouble("threshold"),
                    o.getInt("stabilityPenalty"),
                    o.getInt("upkeepMultiplierPercent"),
                    o.getInt("immigrationModifier")
                )
            );
        }

        final JSONArray exclusionArr = root.getJSONArray("MANUFACTURING_EXCLUSION_LIST");
        EconConfig.MANUFACTURING_EXCLUSION_LIST = new ArrayList<>(exclusionArr.length());
        for (int i = 0; i < exclusionArr.length(); i++) {
            EconConfig.MANUFACTURING_EXCLUSION_LIST.add(exclusionArr.getString(i));
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
        EconConfig.MULTI_THREADING = LunaSettings.getBoolean(LTV_ECON, "MULTI_THREADING");
        EconConfig.ASSIGN_WORKERS_ON_LOAD = LunaSettings.getBoolean(LTV_ECON, "ASSIGN_WORKERS_ON_LOAD");
        EconConfig.STARTING_CREDITS_FOR_MARKET = LunaSettings.getInt(LTV_ECON, "STARTING_CREDITS_FOR_MARKET");
        EconConfig.CREDIT_WITHDRAWAL_LIMIT = LunaSettings.getInt(LTV_ECON, "CREDIT_WITHDRAWAL_LIMIT");
        EconConfig.AUTO_TRANSFER_PROFIT_LIMIT = LunaSettings.getDouble(LTV_ECON, "AUTO_TRANSFER_PROFIT_LIMIT").floatValue();
        EconConfig.ECON_DEFICIT_COST = LunaSettings.getDouble(LTV_ECON, "ECON_DEFICIT_COST");
        EconConfig.LOCAL_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "LOCAL_WORKER_COST_MULT");
        EconConfig.FACTION_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "FACTION_WORKER_COST_MULT");
        EconConfig.LOCAL_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "LOCAL_PROD_BUFFER");
        EconConfig.FACTION_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "FACTION_PROD_BUFFER");
        EconConfig.PRODUCTION_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "PRODUCTION_BUFFER");
        EconConfig.DAYS_TO_COVER = LunaSettings.getInt(LTV_ECON, "DAYS_TO_COVER");
        EconConfig.DAYS_TO_COVER_PER_IMPORT = LunaSettings.getInt(LTV_ECON, "DAYS_TO_COVER_PER_IMPORT");
        EconConfig.FACTION_EXCHANGE_MULT = LunaSettings.getDouble(LTV_ECON, "FACTION_EXCHANGE_MULT").floatValue();
        EconConfig.WORKER_ASSIGN_INTERVAL = LunaSettings.getInt(LTV_ECON, "WORKER_ASSIGN_INTERVAL");
        EconConfig.EXPORT_THRESHOLD_FACTOR = LunaSettings.getDouble(LTV_ECON, "EXPORT_THRESHOLD_FACTOR").floatValue();
        EconConfig.EMBARGO_REP_DROP = LunaSettings.getDouble(LTV_ECON, "EMBARGO_REP_DROP").floatValue();
        EconConfig.MIN_RELATION_TO_TRADE = LunaSettings.getDouble(LTV_ECON,"MIN_RELATION_TO_TRADE").floatValue();
        EconConfig.ECON_ALLOCATION_PASSES = LunaSettings.getInt(LTV_ECON, "ECON_ALLOCATION_PASSES");
        EconConfig.IDLE_SHIP_MAINTENANCE_MULT = LunaSettings.getDouble(LTV_ECON, "IDLE_SHIP_MAINTENANCE_MULT").floatValue();
        EconConfig.CREW_WAGE_PER_MONTH = LunaSettings.getDouble(LTV_ECON, "CREW_WAGE_PER_MONTH").floatValue();
        EconConfig.IDLE_CREW_WAGE_MULT = LunaSettings.getDouble(LTV_ECON, "IDLE_CREW_WAGE_MULT").floatValue();
        EconConfig.TRADE_INTERVAL = LunaSettings.getInt(LTV_ECON, "TRADE_INTERVAL");
        EconConfig.TRAVEL_SPEED_LY_DAY = LunaSettings.getInt(LTV_ECON, "TRAVEL_SPEED_LY_DAY");
        EconConfig.FORCED_FUEL_IMPORT_COST_MULT = LunaSettings.getDouble(LTV_ECON, "FORCED_FUEL_IMPORT_COST_MULT").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE = LunaSettings.getDouble(LTV_ECON, "INDEPENDENT_TRADE_FLEET_BASE_FEE").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE = LunaSettings.getDouble(LTV_ECON, "INDEPENDENT_TRADE_FLEET_PER_TON_FEE").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT = LunaSettings.getDouble(LTV_ECON, "INDEPENDENT_TRADE_FLEET_PERCENT_CUT").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE = LunaSettings.getDouble(LTV_ECON, "INDEPENDENT_TRADE_FLEET_HAZARD_BASE").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT = LunaSettings.getDouble(LTV_ECON, "INDEPENDENT_TRADE_FLEET_HAZARD_MULT").floatValue();
        EconConfig.RAID_STOCKPILES_ACCESS_RATIO = LunaSettings.getDouble(LTV_ECON, "RAID_STOCKPILES_ACCESS_RATIO");
        EconConfig.RAID_BASE_EFF = LunaSettings.getDouble(LTV_ECON, "RAID_BASE_EFF");
        EconConfig.TRADE_MISSION_MAX_DISPLAYED_SHIPS = LunaSettings.getInt(LTV_ECON, "TRADE_MISSION_MAX_DISPLAYED_SHIPS");
        EconConfig.TRADE_MAP_MIN_AMOUNT_FILTER = LunaSettings.getInt(LTV_ECON, "TRADE_MAP_MIN_AMOUNT_FILTER");
        EconConfig.MAX_VISIBLE_PLANNED_ORDERS = LunaSettings.getInt(LTV_ECON, "MAX_VISIBLE_PLANNED_ORDERS");
        EconConfig.INFORMAL_TRADE_SHARE = LunaSettings.getDouble(LTV_ECON, "INFORMAL_TRADE_SHARE").floatValue();
        EconConfig.NPC_FACTION_ASSEMBLY_LINES = LunaSettings.getInt(LTV_ECON, "NPC_FACTION_ASSEMBLY_LINES");
        EconConfig.PLAYER_FACTION_ASSEMBLY_LINES = LunaSettings.getInt(LTV_ECON, "PLAYER_FACTION_ASSEMBLY_LINES");
        EconConfig.SHIP_ALLOC_CAPACITY_TARGET_BUFFER = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_CAPACITY_TARGET_BUFFER").floatValue();
        EconConfig.SHIP_ALLOC_MARKET_WEIGHT_PER_SIZE = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_MARKET_WEIGHT_PER_SIZE").floatValue();
        EconConfig.SHIP_ALLOC_MARKET_SIZE_EXPONENT = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_MARKET_SIZE_EXPONENT").floatValue();
        EconConfig.SHIP_ALLOC_AGGRESSION_COMBAT_MULT = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_AGGRESSION_COMBAT_MULT").floatValue();
        EconConfig.SHIP_ALLOC_THREAT_RELATIONSHIP_MULT = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_THREAT_RELATIONSHIP_MULT").floatValue();
        EconConfig.SHIP_ALLOC_MIN_COMBAT_POWER = LunaSettings.getDouble(LTV_ECON, "SHIP_ALLOC_MIN_COMBAT_POWER").floatValue();
        EconConfig.SCRAP_REFUND_FRACTION = LunaSettings.getDouble(LTV_ECON, "SCRAP_REFUND_FRACTION").floatValue();
    }
}
package wfg.ltv_econ.config.loader;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import static wfg.ltv_econ.constants.Mods.*;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.config.EconConfig;
import wfg.ltv_econ.config.EconConfig.DebtDebuffTier;

public class EconomyConfigLoader {
    private static final SettingsAPI settings = Global.getSettings();
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
        EconConfig.MIN_WORKER_FRACTION = (float) root.getDouble("MIN_WORKER_FRACTION");
        EconConfig.PRODUCTION_HOLD_FACTOR = (float) root.getDouble("PRODUCTION_HOLD_FACTOR");
        EconConfig.OPEN_MARKET_TO_STOCKPILES_RATIO = (float) root.getDouble("OPEN_MARKET_TO_STOCKPILES_RATIO");
        EconConfig.USE_PRODUCTION_FAIRNESS = root.getBoolean("USE_PRODUCTION_FAIRNESS");
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
        EconConfig.MULTI_THREADING = LunaSettings.getBoolean(LTV_ECON, "economy_multiThreading");
        EconConfig.ASSIGN_WORKERS_ON_LOAD = LunaSettings.getBoolean(LTV_ECON, "economy_assignOnLoad");
        EconConfig.STARTING_CREDITS_FOR_MARKET = LunaSettings.getInt(LTV_ECON, "economy_startingCredits");
        EconConfig.CREDIT_WITHDRAWAL_LIMIT = LunaSettings.getInt(LTV_ECON, "economy_withdrawalLimit");
        EconConfig.AUTO_TRANSFER_PROFIT_LIMIT = LunaSettings.getDouble(LTV_ECON, "economy_autoTransferLimit").floatValue();
        EconConfig.ECON_DEFICIT_COST = LunaSettings.getDouble(LTV_ECON, "economy_deficitCost");
        EconConfig.LOCAL_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "economy_localWorkerCost");
        EconConfig.FACTION_WORKER_COST_MULT = LunaSettings.getDouble(LTV_ECON, "economy_factionWorkerCost");
        EconConfig.LOCAL_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_localProdBuffer");
        EconConfig.FACTION_PROD_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_factionProdBuffer");
        EconConfig.PRODUCTION_BUFFER = 1f + LunaSettings.getDouble(LTV_ECON, "economy_prodBuffer");
        EconConfig.DAYS_TO_COVER = LunaSettings.getInt(LTV_ECON, "economy_daysToCover");
        EconConfig.DAYS_TO_COVER_PER_IMPORT = LunaSettings.getInt(LTV_ECON, "economy_perImportDaysCover");
        EconConfig.FACTION_EXCHANGE_MULT = LunaSettings.getDouble(LTV_ECON, "economy_factionExchangeMult").floatValue();
        EconConfig.WORKER_ASSIGN_INTERVAL = LunaSettings.getInt(LTV_ECON, "economy_assignInterval");
        EconConfig.EXPORT_THRESHOLD_FACTOR = LunaSettings.getDouble(LTV_ECON, "economy_exportThreshold").floatValue();
        EconConfig.EMBARGO_REP_DROP = LunaSettings.getDouble(LTV_ECON, "economy_embargoRepDrop").floatValue();
        EconConfig.MIN_RELATION_TO_TRADE = LunaSettings.getDouble(LTV_ECON,"economy_minTradeRelation").floatValue();
        EconConfig.ECON_ALLOCATION_PASSES = LunaSettings.getInt(LTV_ECON, "economy_allocPasses");
        EconConfig.MIN_WORKER_FRACTION = LunaSettings.getDouble(LTV_ECON, "economy_minWorkerFraction").floatValue();
        EconConfig.IDLE_SHIP_MAINTENANCE_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_idleShipMaintenanceMult").floatValue();
        EconConfig.CREW_WAGE_PER_MONTH = LunaSettings.getDouble(LTV_ECON, "fleet_crewWages").floatValue();
        EconConfig.IDLE_CREW_WAGE_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_idleCrewWagesMult").floatValue();
        EconConfig.TRADE_INTERVAL = LunaSettings.getInt(LTV_ECON, "economy_tradeInterval");
        EconConfig.TRAVEL_SPEED_LY_DAY = LunaSettings.getInt(LTV_ECON, "fleet_travelSpeedLyDay");
        EconConfig.FORCED_FUEL_IMPORT_COST_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_forcedFuelCostMult").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE = LunaSettings.getDouble(LTV_ECON, "fleet_independentBaseFee").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE = LunaSettings.getDouble(LTV_ECON, "fleet_independentPerTonFee").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT = LunaSettings.getDouble(LTV_ECON, "fleet_independentPercentCut").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE = LunaSettings.getDouble(LTV_ECON, "fleet_independentHazardBase").floatValue();
        EconConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_independentHazardMult").floatValue();
        EconConfig.RAID_STOCKPILES_ACCESS_RATIO = LunaSettings.getDouble(LTV_ECON, "raids_raidStockpileAccessRatio");
        EconConfig.RAID_BASE_EFF = LunaSettings.getDouble(LTV_ECON, "raids_raidBaseEff");

        final String fairness = LunaSettings.getString(LTV_ECON, "economy_prodFairness");
        EconConfig.USE_PRODUCTION_FAIRNESS = fairness.equals("Commodities Produced");
    }
}
package wfg.ltv_econ.config.loader;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import static wfg.ltv_econ.constants.Mods.*;

import lunalib.lunaSettings.LunaSettings;
import wfg.ltv_econ.config.EconomyConfig;
import wfg.ltv_econ.config.EconomyConfig.DebtDebuffTier;

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
        EconomyConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_BASE_FEE");
        EconomyConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_PER_TON_FEE");
        EconomyConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_PERCENT_CUT");
        EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_HAZARD_BASE");
        EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT = (float) root.getDouble("INDEPENDENT_TRADE_FLEET_HAZARD_MULT");
        EconomyConfig.RAID_STOCKPILES_ACCESS_RATIO = root.getDouble("RAID_STOCKPILES_ACCESS_RATIO");

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
        EconomyConfig.INDEPENDENT_TRADE_FLEET_BASE_FEE = LunaSettings.getDouble(LTV_ECON, "fleet_independentBaseFee").floatValue();
        EconomyConfig.INDEPENDENT_TRADE_FLEET_PER_TON_FEE = LunaSettings.getDouble(LTV_ECON, "fleet_independentPerTonFee").floatValue();
        EconomyConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT = LunaSettings.getDouble(LTV_ECON, "fleet_independentPercentCut").floatValue();
        EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_BASE = LunaSettings.getDouble(LTV_ECON, "fleet_independentHazardBase").floatValue();
        EconomyConfig.INDEPENDENT_TRADE_FLEET_HAZARD_MULT = LunaSettings.getDouble(LTV_ECON, "fleet_independentHazardMult").floatValue();
        EconomyConfig.RAID_STOCKPILES_ACCESS_RATIO = LunaSettings.getDouble(LTV_ECON, "raids_raidStockpileAccessRatio");
        EconomyConfig.RAID_BASE_EFF = LunaSettings.getDouble(LTV_ECON, "raids_raidBaseEff");

        final String fairness = LunaSettings.getString(LTV_ECON, "economy_prodFairness");
        EconomyConfig.USE_PRODUCTION_FAIRNESS = fairness.equals("Commodities Produced");
    }
}
package wfg.ltv_econ.constants.strings;

import com.fs.starfarer.api.impl.campaign.ids.Strings;

import wfg.ltv_econ.config.EconConfig;

import static wfg.ltv_econ.constants.strings.LocalizedStrings.*;

public class Income {
    public static final String FACTION_CREW_WAGES_KEY = "faction_fleet_crew_wages";
    public static final String FACTION_CREW_WAGES_DESC = str("FACTION_CREW_WAGES_DESC");

    public static final String FACTION_SHIP_PRODUCTION_KEY = "faction_ship_production";
    public static final String FACTION_SHIP_PRODUCTION_DESC = str("FACTION_SHIP_PRODUCTION_DESC");

    public static final String TRADE_FUEL_PREMIUM_KEY = "fuel_premium";
    public static final String TRADE_FUEL_PREMIUM_DESC = str("TRADE_FUEL_PREMIUM_DESC");

    public static final String TRADE_FLEET_SHIPMENT_KEY = "trade_fleet_shipment";
    public static final String TRADE_FLEET_SHIPMENT_DESC = str("TRADE_FLEET_SHIPMENT_DESC");

    public static final String INDEPENDENT_BASE_FEE_KEY = "independent_base_fee";
    public static final String INDEPENDENT_BASE_FEE_DESC = str("INDEPENDENT_BASE_FEE_DESC");

    public static final String INDEPENDENT_PER_TON_KEY = "independent_per_ton";
    public static final String INDEPENDENT_PER_TON_DESC = str("INDEPENDENT_PER_TON_DESC");

    public static final String INDEPENDENT_VALUE_PERCENT_KEY = "independent_value_percent";
    public static final String INDEPENDENT_VALUE_PERCENT_DESC = str("INDEPENDENT_VALUE_PERCENT_DESC");

    public static final String INDEPENDENT_HAZARD_PAY_KEY = "independent_hazard_pay";
    public static final String INDEPENDENT_HAZARD_PAY_DESC = str("INDEPENDENT_HAZARD_PAY_DESC");

    public static final String INDEPENDENT_FUEL_COST_KEY = "independent_fuel_cost";
    public static final String INDEPENDENT_FUEL_COST_DESC = str("INDEPENDENT_FUEL_COST_DESC");

    public static final String INDEPENDENT_PATROL_COST_KEY = "independent_patrol_cost";
    public static final String INDEPENDENT_PATROL_COST_DESC = str("INDEPENDENT_PATROL_COST_DESC");

    public static final String WORKER_WAGES_KEY = "wages";
    public static final String WORKER_WAGES_DESC = str("WORKER_WAGES_DESC");

    public static final String COLONY_HAZARD_PAY_KEY = "hazard_pay";
    public static final String COLONY_HAZARD_PAY_DESC = str("COLONY_HAZARD_PAY_DESC");

    public static final String POLICY_COST_KEY = "policy_cost_";
    public static final String POLICY_COST_DESC = str("POLICY_COST_DESC");

    public static final String PLAYER_MARKET_TRANSACTION_KEY = "player_market";
    public static final String PLAYER_MARKET_TRANSACTION_DESC = str("PLAYER_MARKET_TRANSACTION_DESC");

    public static final String TRADE_EXPORT_KEY = "trade_export_";
    public static final String TRADE_EXPORT_DESC = str("TRADE_EXPORT_DESC");

    public static final String TRADE_IMPORT_KEY = "trade_import_";
    public static final String TRADE_IMPORT_DESC = str("TRADE_IMPORT_DESC");

    public static final String INDUSTRY_INCOME_KEY = "industry_income_";
    public static final String INDUSTRY_INCOME_DESC = str("INDUSTRY_INCOME_DESC");
    public static final String INDUSTRY_UPKEEP_KEY = "industry_upkeep";
    public static final String INDUSTRY_UPKEEP_DESC = str("INDUSTRY_UPKEEP_DESC");

    public static final String DEBT_DEBUFF_KEY = "ltv_econ_debt_debuff";
    public static final String DEBT_STABILITY_DEBUFF_DESC = str("DEBT_STABILITY_DEBUFF_DESC");
    public static final String DEBT_UPKEEP_DEBUFF_DESC = str("DEBT_UPKEEP_DEBUFF_DESC");
    public static final String DEBT_IMMIGRATION_DEBUFF_DESC = str("DEBT_IMMIGRATION_DEBUFF_DESC");

    public static String getDesc(String key) {
        switch (key) {
            case FACTION_CREW_WAGES_KEY: return FACTION_CREW_WAGES_DESC;
            case FACTION_SHIP_PRODUCTION_KEY: return FACTION_SHIP_PRODUCTION_DESC;
            case TRADE_FUEL_PREMIUM_KEY: return String.format(TRADE_FUEL_PREMIUM_DESC, Strings.X + EconConfig.FORCED_FUEL_IMPORT_COST_MULT);
            case TRADE_FLEET_SHIPMENT_KEY: return TRADE_FLEET_SHIPMENT_DESC;
            case INDEPENDENT_BASE_FEE_KEY: return INDEPENDENT_BASE_FEE_DESC;
            case INDEPENDENT_PER_TON_KEY: return INDEPENDENT_PER_TON_DESC;
            case INDEPENDENT_VALUE_PERCENT_KEY: return String.format(INDEPENDENT_VALUE_PERCENT_DESC, Strings.X + EconConfig.INDEPENDENT_TRADE_FLEET_PERCENT_CUT);
            case INDEPENDENT_HAZARD_PAY_KEY: return INDEPENDENT_HAZARD_PAY_DESC;
            case INDEPENDENT_FUEL_COST_KEY: return INDEPENDENT_FUEL_COST_DESC;
            case WORKER_WAGES_KEY: return WORKER_WAGES_DESC;
            case POLICY_COST_KEY: return POLICY_COST_DESC;
            case PLAYER_MARKET_TRANSACTION_KEY: return PLAYER_MARKET_TRANSACTION_DESC;
            case TRADE_EXPORT_KEY: return TRADE_EXPORT_DESC;
            case TRADE_IMPORT_KEY: return TRADE_IMPORT_DESC;
            case INDUSTRY_INCOME_KEY: return INDUSTRY_INCOME_DESC;
            case INDUSTRY_UPKEEP_KEY: return INDUSTRY_UPKEEP_DESC;
            case COLONY_HAZARD_PAY_KEY: return COLONY_HAZARD_PAY_DESC;
            case INDEPENDENT_PATROL_COST_KEY: return INDEPENDENT_PATROL_COST_DESC;
            default: return "";
        }
    }
}